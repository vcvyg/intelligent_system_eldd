package org.example.persion.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.example.persion.common.exception.BusinessException;
import org.example.persion.dto.RecommendationFeedbackDTO;
import org.example.persion.entity.ElderlyFamilyRelation;
import org.example.persion.entity.FamilyServiceRecord;
import org.example.persion.entity.HealthData;
import org.example.persion.entity.RecommendationContent;
import org.example.persion.entity.RecommendationDelivery;
import org.example.persion.entity.RecommendationFeedback;
import org.example.persion.entity.User;
import org.example.persion.enums.ServiceProgressStatus;
import org.example.persion.repository.AlertRecordMapper;
import org.example.persion.repository.ElderlyFamilyRelationMapper;
import org.example.persion.repository.FamilyServiceRecordMapper;
import org.example.persion.repository.HealthDataMapper;
import org.example.persion.repository.RecommendationContentMapper;
import org.example.persion.repository.RecommendationDeliveryMapper;
import org.example.persion.repository.RecommendationFeedbackMapper;
import org.example.persion.service.RecommendationService;
import org.example.persion.vo.AlertRecordVO;
import org.example.persion.vo.RecommendationItemVO;
import org.example.persion.vo.RecommendationPerformanceVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RecommendationServiceImpl implements RecommendationService {

    private static final int TOP_K = 3;

    private final RecommendationContentMapper contentMapper;
    private final RecommendationDeliveryMapper deliveryMapper;
    private final RecommendationFeedbackMapper feedbackMapper;
    private final ElderlyFamilyRelationMapper familyRelationMapper;
    private final HealthDataMapper healthDataMapper;
    private final AlertRecordMapper alertRecordMapper;
    private final FamilyServiceRecordMapper familyServiceRecordMapper;

    @Override
    public List<RecommendationItemVO> preview(Long elderlyId, Long familyUserId) {
        requireElderly(elderlyId);
        return rank(elderlyId, familyUserId);
    }

    @Override
    public RecommendationPerformanceVO performance(Long elderlyId, Long familyUserId, int windowDays) {
        requireElderly(elderlyId);
        int boundedDays = Math.max(7, Math.min(windowDays <= 0 ? 30 : windowDays, 90));
        LocalDateTime since = LocalDateTime.now().minusDays(boundedDays);

        LambdaQueryWrapper<RecommendationDelivery> deliveryQuery = new LambdaQueryWrapper<RecommendationDelivery>()
                .eq(RecommendationDelivery::getElderlyId, elderlyId)
                .ge(RecommendationDelivery::getCreateTime, since)
                .orderByDesc(RecommendationDelivery::getCreateTime);
        if (familyUserId != null) {
            deliveryQuery.eq(RecommendationDelivery::getFamilyUserId, familyUserId);
        }

        List<RecommendationDelivery> deliveries = deliveryMapper.selectList(deliveryQuery);
        deliveries = deliveries == null ? List.of() : deliveries;

        Set<Long> contentIds = deliveries.stream()
                .map(RecommendationDelivery::getContentId)
                .filter(id -> id != null)
                .collect(Collectors.toSet());
        Map<Long, RecommendationContent> contentById = new HashMap<>();
        if (!contentIds.isEmpty()) {
            List<RecommendationContent> contents = contentMapper.selectBatchIds(contentIds);
            if (contents != null) {
                for (RecommendationContent content : contents) {
                    if (content != null && content.getId() != null) {
                        contentById.put(content.getId(), content);
                    }
                }
            }
        }

        PerformanceBucket total = new PerformanceBucket("ALL");
        Map<String, PerformanceBucket> byCategory = new LinkedHashMap<>();
        for (RecommendationDelivery delivery : deliveries) {
            total.accept(delivery);
            RecommendationContent content = contentById.get(delivery.getContentId());
            String category = content == null || content.getCategory() == null || content.getCategory().isBlank()
                    ? "UNKNOWN"
                    : content.getCategory();
            byCategory.computeIfAbsent(category, PerformanceBucket::new).accept(delivery);
        }

        List<RecommendationPerformanceVO.CategoryPerformance> categories = byCategory.values().stream()
                .map(PerformanceBucket::toView)
                .sorted(Comparator
                        .comparingInt(RecommendationPerformanceVO.CategoryPerformance::deliveryCount)
                        .reversed()
                        .thenComparing(RecommendationPerformanceVO.CategoryPerformance::category))
                .toList();

        RecommendationPerformanceVO.CategoryPerformance totalView = total.toView();
        List<String> suggestions = buildStrategySuggestions(totalView, categories);
        return new RecommendationPerformanceVO(
                boundedDays,
                totalView.deliveryCount(),
                totalView.clickCount(),
                totalView.usefulCount(),
                totalView.notInterestedCount(),
                totalView.clickThroughRate(),
                totalView.usefulRate(),
                totalView.negativeRate(),
                categories,
                suggestions
        );
    }

    @Override
    @Transactional
    public int deliver(Long elderlyId) {
        requireElderly(elderlyId);
        List<User> familyUsers = familyRelationMapper.selectUsersByElderlyId(elderlyId);
        if (familyUsers == null || familyUsers.isEmpty()) {
            return 0;
        }

        int created = 0;
        LocalDateTime dayStart = LocalDate.now().atStartOfDay();
        LocalDateTime nextDay = dayStart.plusDays(1);

        for (User familyUser : familyUsers) {
            if (familyUser == null || familyUser.getId() == null) continue;
            for (RecommendationItemVO item : rank(elderlyId, familyUser.getId())) {
                Long existing = deliveryMapper.selectCount(new LambdaQueryWrapper<RecommendationDelivery>()
                        .eq(RecommendationDelivery::getElderlyId, elderlyId)
                        .eq(RecommendationDelivery::getFamilyUserId, familyUser.getId())
                        .eq(RecommendationDelivery::getContentId, item.getContentId())
                        .ge(RecommendationDelivery::getCreateTime, dayStart)
                        .lt(RecommendationDelivery::getCreateTime, nextDay));
                if (existing != null && existing > 0) continue;

                RecommendationDelivery delivery = new RecommendationDelivery();
                delivery.setElderlyId(elderlyId);
                delivery.setFamilyUserId(familyUser.getId());
                delivery.setContentId(item.getContentId());
                delivery.setChannel("IN_APP");
                delivery.setStatus("DELIVERED");
                delivery.setScore(item.getScore());
                delivery.setReason(item.getReason());
                delivery.setExposedAt(LocalDateTime.now());
                deliveryMapper.insert(delivery);
                created++;
            }
        }
        return created;
    }

    @Override
    public List<RecommendationItemVO> familyFeed(Long familyUserId, Long elderlyId) {
        requireFamilyAccess(familyUserId, elderlyId);
        List<RecommendationDelivery> deliveries = deliveryMapper.selectList(
                new LambdaQueryWrapper<RecommendationDelivery>()
                        .eq(RecommendationDelivery::getFamilyUserId, familyUserId)
                        .eq(RecommendationDelivery::getElderlyId, elderlyId)
                        .ne(RecommendationDelivery::getStatus, "NOT_INTERESTED")
                        .orderByDesc(RecommendationDelivery::getCreateTime)
        );
        if (deliveries == null || deliveries.isEmpty()) return List.of();

        List<RecommendationItemVO> result = new ArrayList<>();
        for (RecommendationDelivery delivery : deliveries) {
            RecommendationContent content = contentMapper.selectById(delivery.getContentId());
            if (content == null || Integer.valueOf(0).equals(content.getEnabled())) continue;
            result.add(toItem(content, delivery.getId(), delivery.getScore(), delivery.getReason(), delivery.getStatus()));
        }
        return result;
    }

    @Override
    @Transactional
    public void feedback(Long familyUserId, RecommendationFeedbackDTO dto) {
        if (dto == null || dto.getDeliveryId() == null || dto.getElderlyId() == null) {
            throw new BusinessException(400, "反馈参数不完整");
        }
        requireFamilyAccess(familyUserId, dto.getElderlyId());

        RecommendationDelivery delivery = deliveryMapper.selectById(dto.getDeliveryId());
        if (delivery == null
                || !dto.getElderlyId().equals(delivery.getElderlyId())
                || !familyUserId.equals(delivery.getFamilyUserId())) {
            throw new BusinessException(403, "无权操作该推荐记录");
        }

        String type = normalizeFeedback(dto.getFeedbackType());
        List<RecommendationFeedback> deliveryFeedbacks = feedbackMapper.selectList(
                new LambdaQueryWrapper<RecommendationFeedback>()
                        .eq(RecommendationFeedback::getDeliveryId, delivery.getId())
                        .eq(RecommendationFeedback::getFamilyUserId, familyUserId)
                        .eq(RecommendationFeedback::getElderlyId, dto.getElderlyId())
        );
        RecommendationFeedback feedback = deliveryFeedbacks == null ? null : deliveryFeedbacks.stream()
                .max(Comparator.comparing(
                        RecommendationFeedback::getId,
                        Comparator.nullsFirst(Long::compareTo)
                ))
                .orElse(null);

        boolean existingFeedback = feedback != null;
        if (!existingFeedback) {
            feedback = new RecommendationFeedback();
            feedback.setElderlyId(dto.getElderlyId());
            feedback.setFamilyUserId(familyUserId);
            feedback.setContentId(delivery.getContentId());
            feedback.setDeliveryId(delivery.getId());
        }
        feedback.setFeedbackType(type);
        feedback.setWeight("USEFUL".equals(type) ? 1 : "NOT_INTERESTED".equals(type) ? -1 : 0);
        if (existingFeedback) {
            feedbackMapper.updateById(feedback);
        } else {
            feedbackMapper.insert(feedback);
        }

        if ("NOT_INTERESTED".equals(type)) {
            delivery.setStatus("NOT_INTERESTED");
        } else if ("USEFUL".equals(type)) {
            delivery.setStatus("USEFUL");
        } else if ("CLICK".equals(type)) {
            delivery.setStatus("CLICKED");
            delivery.setClickedAt(LocalDateTime.now());
        }
        deliveryMapper.updateById(delivery);
    }

    private List<RecommendationItemVO> rank(Long elderlyId, Long familyUserId) {
        List<RecommendationContent> contents = contentMapper.selectList(
                new LambdaQueryWrapper<RecommendationContent>()
                        .eq(RecommendationContent::getEnabled, 1)
                        .orderByDesc(RecommendationContent::getBaseScore)
        );
        if (contents == null || contents.isEmpty()) return List.of();

        LocalDateTime now = LocalDateTime.now();
        List<HealthData> health = healthDataMapper.findByDateTimeRange(now.minusDays(7), now, elderlyId);
        boolean sparseHealth = health == null || health.size() < 3;

        List<AlertRecordVO> alerts = alertRecordMapper.selectByElderlyId(elderlyId);
        long openAlerts = alerts == null ? 0 : alerts.stream().filter(this::isOpenAlert).count();

        List<FamilyServiceRecord> services = familyServiceRecordMapper.selectList(
                new LambdaQueryWrapper<FamilyServiceRecord>()
                        .eq(FamilyServiceRecord::getElderlyId, elderlyId)
                        .in(FamilyServiceRecord::getStatus, ServiceProgressStatus.PENDING, ServiceProgressStatus.PROCESSING)
        );
        boolean hasPendingService = services != null && !services.isEmpty();

        List<RecommendationFeedback> feedbacks = familyUserId == null ? List.of() : feedbackMapper.selectList(
                new LambdaQueryWrapper<RecommendationFeedback>()
                        .eq(RecommendationFeedback::getFamilyUserId, familyUserId)
                        .eq(RecommendationFeedback::getElderlyId, elderlyId)
        );
        feedbacks = feedbacks == null ? List.of() : feedbacks;

        Map<Long, RecommendationContent> contentById = new HashMap<>();
        for (RecommendationContent content : contents) contentById.put(content.getId(), content);

        Set<Long> hiddenContent = new HashSet<>();
        Map<String, Integer> categoryWeight = new HashMap<>();
        Map<String, Integer> categoryNegativeCount = new HashMap<>();
        Set<Long> usefulContent = new HashSet<>();
        for (RecommendationFeedback feedback : feedbacks) {
            RecommendationContent related = contentById.get(feedback.getContentId());
            if (related == null) continue;
            String category = related.getCategory();
            if ("NOT_INTERESTED".equals(feedback.getFeedbackType())) {
                hiddenContent.add(feedback.getContentId());
                categoryWeight.merge(category, -12, Integer::sum);
                categoryNegativeCount.merge(category, 1, Integer::sum);
            } else if ("USEFUL".equals(feedback.getFeedbackType())) {
                usefulContent.add(feedback.getContentId());
                categoryWeight.merge(category, 8, Integer::sum);
            }
        }

        List<RecommendationItemVO> scored = new ArrayList<>();
        for (RecommendationContent content : contents) {
            if (hiddenContent.contains(content.getId())) continue;
            if (categoryNegativeCount.getOrDefault(content.getCategory(), 0) >= 2) continue;

            BigDecimal score = content.getBaseScore() == null ? BigDecimal.valueOf(50) : content.getBaseScore();
            List<String> reasons = new ArrayList<>();
            reasons.add("基础关怀优先级");

            if (sparseHealth && "HEALTH_CHECK".equals(content.getCategory())) {
                score = score.add(BigDecimal.valueOf(18));
                reasons.add("近7天健康记录较少");
            }
            if (openAlerts > 0 && "SAFETY".equals(content.getCategory())) {
                score = score.add(BigDecimal.valueOf(25));
                reasons.add("存在未闭环告警");
            }
            if (openAlerts > 0 && "HEALTH_CHECK".equals(content.getCategory())) {
                score = score.add(BigDecimal.valueOf(8));
                reasons.add("近期存在健康提醒");
            }
            if (hasPendingService && "CARE_SERVICE".equals(content.getCategory())) {
                score = score.add(BigDecimal.valueOf(15));
                reasons.add("存在待执行生活服务");
            }
            int preference = categoryWeight.getOrDefault(content.getCategory(), 0);
            if (preference != 0) {
                score = score.add(BigDecimal.valueOf(preference));
                reasons.add(preference > 0 ? "家属曾对同类内容表示有用" : "同类内容近期被降低偏好");
            }
            if (usefulContent.contains(content.getId())) {
                score = score.add(BigDecimal.valueOf(12));
                reasons.add("该内容曾被标记有用");
            }

            scored.add(toItem(content, null, score, String.join("；", reasons), null));
        }

        scored.sort(Comparator.comparing(RecommendationItemVO::getScore).reversed()
                .thenComparing(RecommendationItemVO::getContentId));
        return diversify(scored, TOP_K);
    }

    private List<String> buildStrategySuggestions(
            RecommendationPerformanceVO.CategoryPerformance total,
            List<RecommendationPerformanceVO.CategoryPerformance> categories) {
        List<String> suggestions = new ArrayList<>();
        if (total.deliveryCount() < 5) {
            suggestions.add("当前样本较少，先保持人工复核与频控，积累更多投放反馈后再调整策略");
        } else {
            if (total.clickThroughRate() < 0.20) {
                suggestions.add("整体点击反馈偏低，可优先优化内容标题、行动入口与投放时机");
            }
            if (total.negativeRate() >= 0.25) {
                suggestions.add("不感兴趣反馈偏高，建议降低重复触达并扩大内容类别多样性");
            }
        }

        RecommendationPerformanceVO.CategoryPerformance best = categories.stream()
                .filter(item -> item.deliveryCount() >= 2)
                .max(Comparator
                        .comparingDouble(RecommendationPerformanceVO.CategoryPerformance::usefulRate)
                        .thenComparingDouble(RecommendationPerformanceVO.CategoryPerformance::clickThroughRate))
                .orElse(null);
        if (best != null && (best.usefulRate() > 0 || best.clickThroughRate() > 0)) {
            suggestions.add("类别 " + best.category() + " 的正向反馈相对更好，可作为下一轮候选策略的优先参考");
        }

        RecommendationPerformanceVO.CategoryPerformance worst = categories.stream()
                .filter(item -> item.deliveryCount() >= 2)
                .max(Comparator.comparingDouble(RecommendationPerformanceVO.CategoryPerformance::negativeRate))
                .orElse(null);
        if (worst != null && worst.negativeRate() >= 0.25
                && (best == null || !worst.category().equals(best.category()))) {
            suggestions.add("类别 " + worst.category() + " 的负反馈偏高，建议降权或减少连续投放");
        }

        if (suggestions.isEmpty()) {
            suggestions.add("当前整体反馈稳定，继续保持类别打散、按日幂等和人工复核，并观察后续趋势");
        }
        return suggestions;
    }

    private List<RecommendationItemVO> diversify(List<RecommendationItemVO> scored, int limit) {
        LinkedHashMap<String, RecommendationItemVO> firstByCategory = new LinkedHashMap<>();
        for (RecommendationItemVO item : scored) {
            firstByCategory.putIfAbsent(item.getCategory(), item);
        }

        List<RecommendationItemVO> selected = new ArrayList<>();
        for (RecommendationItemVO item : firstByCategory.values()) {
            if (selected.size() >= limit) break;
            selected.add(item);
        }
        if (selected.size() < limit) {
            for (RecommendationItemVO item : scored) {
                if (selected.size() >= limit) break;
                if (!selected.contains(item)) selected.add(item);
            }
        }
        selected.sort(Comparator.comparing(RecommendationItemVO::getScore).reversed());
        return selected;
    }

    private RecommendationItemVO toItem(RecommendationContent content,
                                        Long deliveryId,
                                        BigDecimal score,
                                        String reason,
                                        String feedbackState) {
        return new RecommendationItemVO(
                content.getId(), deliveryId, content.getTitle(), content.getSummary(), content.getCategory(),
                score, reason, content.getActionLabel(), content.getActionUrl(), feedbackState
        );
    }

    private void requireFamilyAccess(Long familyUserId, Long elderlyId) {
        if (familyUserId == null || elderlyId == null) {
            throw new BusinessException(401, "未获取到当前用户或老人信息");
        }
        Long count = familyRelationMapper.selectCount(new LambdaQueryWrapper<ElderlyFamilyRelation>()
                .eq(ElderlyFamilyRelation::getFamilyUserId, familyUserId)
                .eq(ElderlyFamilyRelation::getElderlyId, elderlyId));
        if (count == null || count <= 0) {
            throw new BusinessException(403, "无权访问该老人的推荐内容");
        }
    }

    private void requireElderly(Long elderlyId) {
        if (elderlyId == null) throw new BusinessException(400, "老人ID不能为空");
    }

    private String normalizeFeedback(String value) {
        String type = value == null ? "" : value.trim().toUpperCase();
        if (!Set.of("USEFUL", "NOT_INTERESTED", "CLICK").contains(type)) {
            throw new BusinessException(400, "不支持的反馈类型");
        }
        return type;
    }

    private boolean isOpenAlert(AlertRecordVO alert) {
        String status = alert.getStatus();
        if (status == null) return true;
        return !(status.contains("已处理") || status.contains("已关闭") || status.contains("已忽略") || status.equalsIgnoreCase("CLOSED"));
    }

    private static double rate(int numerator, int denominator) {
        if (denominator <= 0 || numerator <= 0) return 0.0;
        return Math.round((numerator * 1.0 / denominator) * 1000.0) / 1000.0;
    }

    private static final class PerformanceBucket {
        private final String category;
        private int deliveryCount;
        private int clickCount;
        private int usefulCount;
        private int notInterestedCount;

        private PerformanceBucket(String category) {
            this.category = category;
        }

        private void accept(RecommendationDelivery delivery) {
            deliveryCount++;
            if (delivery.getClickedAt() != null || "CLICKED".equalsIgnoreCase(delivery.getStatus())) {
                clickCount++;
            }
            if ("USEFUL".equalsIgnoreCase(delivery.getStatus())) {
                usefulCount++;
            }
            if ("NOT_INTERESTED".equalsIgnoreCase(delivery.getStatus())) {
                notInterestedCount++;
            }
        }

        private RecommendationPerformanceVO.CategoryPerformance toView() {
            return new RecommendationPerformanceVO.CategoryPerformance(
                    category,
                    deliveryCount,
                    clickCount,
                    usefulCount,
                    notInterestedCount,
                    rate(clickCount, deliveryCount),
                    rate(usefulCount, deliveryCount),
                    rate(notInterestedCount, deliveryCount)
            );
        }
    }
}
