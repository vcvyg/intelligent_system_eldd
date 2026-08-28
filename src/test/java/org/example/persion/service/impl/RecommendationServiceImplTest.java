package org.example.persion.service.impl;

import org.example.persion.common.exception.BusinessException;
import org.example.persion.dto.RecommendationFeedbackDTO;
import org.example.persion.entity.RecommendationContent;
import org.example.persion.entity.RecommendationDelivery;
import org.example.persion.entity.RecommendationFeedback;
import org.example.persion.entity.User;
import org.example.persion.repository.AlertRecordMapper;
import org.example.persion.repository.ElderlyFamilyRelationMapper;
import org.example.persion.repository.FamilyServiceRecordMapper;
import org.example.persion.repository.HealthDataMapper;
import org.example.persion.repository.RecommendationContentMapper;
import org.example.persion.repository.RecommendationDeliveryMapper;
import org.example.persion.repository.RecommendationFeedbackMapper;
import org.example.persion.vo.AlertRecordVO;
import org.example.persion.vo.RecommendationItemVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecommendationServiceImplTest {

    private static final Long ELDERLY_ID = 11L;
    private static final Long FAMILY_ID = 22L;

    @Mock private RecommendationContentMapper contentMapper;
    @Mock private RecommendationDeliveryMapper deliveryMapper;
    @Mock private RecommendationFeedbackMapper feedbackMapper;
    @Mock private ElderlyFamilyRelationMapper familyRelationMapper;
    @Mock private HealthDataMapper healthDataMapper;
    @Mock private AlertRecordMapper alertRecordMapper;
    @Mock private FamilyServiceRecordMapper familyServiceRecordMapper;

    private RecommendationServiceImpl service;
    private RecommendationContent healthContent;
    private RecommendationContent safetyContent;
    private RecommendationContent careContent;
    private RecommendationContent wellnessContent;

    @BeforeEach
    void setUp() {
        service = new RecommendationServiceImpl(
                contentMapper,
                deliveryMapper,
                feedbackMapper,
                familyRelationMapper,
                healthDataMapper,
                alertRecordMapper,
                familyServiceRecordMapper
        );

        healthContent = content(1L, "HEALTH", "健康测量提醒", "HEALTH_CHECK", 65);
        safetyContent = content(2L, "SAFETY", "关注近期安全提醒", "SAFETY", 62);
        careContent = content(3L, "CARE", "看看近期生活服务安排", "CARE_SERVICE", 58);
        wellnessContent = content(4L, "WELLNESS", "保持轻量活动", "WELLNESS", 52);

        when(contentMapper.selectList(any())).thenReturn(List.of(
                healthContent, safetyContent, careContent, wellnessContent
        ));
        when(healthDataMapper.findByDateTimeRange(any(), any(), any())).thenReturn(List.of());
        when(familyServiceRecordMapper.selectList(any())).thenReturn(List.of());
    }

    @Test
    void sparseHealthAndOpenAlertBoostRelevantCategories() {
        AlertRecordVO alert = new AlertRecordVO();
        alert.setStatus("待处理");
        alert.setAlertType("心率异常");
        alert.setAlertTime(LocalDateTime.now());
        when(alertRecordMapper.selectByElderlyId(ELDERLY_ID)).thenReturn(List.of(alert));
        when(feedbackMapper.selectList(any())).thenReturn(List.of());

        List<RecommendationItemVO> result = service.preview(ELDERLY_ID, FAMILY_ID);

        assertEquals(3, result.size());
        assertEquals("HEALTH_CHECK", result.get(0).getCategory());
        assertTrue(result.stream().anyMatch(item -> "SAFETY".equals(item.getCategory())));
        assertTrue(result.get(0).getReason().contains("近7天健康记录较少"));
        assertTrue(result.stream()
                .filter(item -> "SAFETY".equals(item.getCategory()))
                .findFirst().orElseThrow().getReason().contains("存在未闭环告警"));
    }

    @Test
    void notInterestedContentIsRemovedFromNextRanking() {
        when(alertRecordMapper.selectByElderlyId(ELDERLY_ID)).thenReturn(List.of());
        RecommendationFeedback feedback = new RecommendationFeedback();
        feedback.setContentId(healthContent.getId());
        feedback.setFeedbackType("NOT_INTERESTED");
        when(feedbackMapper.selectList(any())).thenReturn(List.of(feedback));

        List<RecommendationItemVO> result = service.preview(ELDERLY_ID, FAMILY_ID);

        assertFalse(result.stream().anyMatch(item -> healthContent.getId().equals(item.getContentId())));
    }

    @Test
    void deliverCreatesTopThreeForLinkedFamilyAndIsReadyForIdempotencyCheck() {
        when(alertRecordMapper.selectByElderlyId(ELDERLY_ID)).thenReturn(List.of());
        when(feedbackMapper.selectList(any())).thenReturn(List.of());
        User family = new User();
        family.setId(FAMILY_ID);
        when(familyRelationMapper.selectUsersByElderlyId(ELDERLY_ID)).thenReturn(List.of(family));
        when(deliveryMapper.selectCount(any())).thenReturn(0L);
        when(deliveryMapper.insert(any(RecommendationDelivery.class))).thenReturn(1);

        int created = service.deliver(ELDERLY_ID);

        assertEquals(3, created);
        verify(deliveryMapper, times(3)).insert(any(RecommendationDelivery.class));
        verify(deliveryMapper, times(3)).selectCount(any());
    }

    @Test
    void familyFeedbackRejectsUnrelatedElderly() {
        RecommendationFeedbackDTO dto = new RecommendationFeedbackDTO();
        dto.setElderlyId(ELDERLY_ID);
        dto.setDeliveryId(99L);
        dto.setFeedbackType("USEFUL");
        when(familyRelationMapper.selectCount(any())).thenReturn(0L);

        BusinessException error = assertThrows(BusinessException.class, () -> service.feedback(FAMILY_ID, dto));

        assertEquals(403, error.getCode());
        assertTrue(error.getMessage().contains("无权访问"));
    }

    private RecommendationContent content(Long id, String code, String title, String category, int score) {
        RecommendationContent content = new RecommendationContent();
        content.setId(id);
        content.setCode(code);
        content.setTitle(title);
        content.setSummary(title + "说明");
        content.setCategory(category);
        content.setBaseScore(BigDecimal.valueOf(score));
        content.setEnabled(1);
        return content;
    }
}
