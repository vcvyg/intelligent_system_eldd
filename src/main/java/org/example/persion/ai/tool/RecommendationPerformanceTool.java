package org.example.persion.ai.tool;

import lombok.RequiredArgsConstructor;
import org.example.persion.service.RecommendationService;
import org.example.persion.vo.RecommendationPerformanceVO;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;

/**
 * Recommendation effectiveness / strategy insight Tool.
 *
 * <p>The Agent can inspect aggregated delivery and feedback signals, but cannot
 * mutate recommendation strategy or trigger delivery from this Tool.</p>
 */
@Component
@RequiredArgsConstructor
public class RecommendationPerformanceTool implements MedicalAiTool {

    private final RecommendationService recommendationService;

    @Override
    public String name() {
        return "recommendation_performance";
    }

    @Override
    public boolean supports(String question) {
        if (question == null) return false;
        String q = question.toLowerCase(Locale.ROOT);
        return containsAny(q,
                "推荐效果", "投放效果", "推荐表现", "投放表现", "点击率", "反馈", "不感兴趣",
                "推荐策略", "投放策略", "策略优化", "哪类内容", "效果怎么样", "performance");
    }

    @Override
    public MedicalAiToolResult execute(MedicalAiToolContext context) {
        RecommendationPerformanceVO snapshot = recommendationService.performance(context.elderlyId(), null, 30);
        if (snapshot.deliveryCount() == 0) {
            return new MedicalAiToolResult(
                    "推荐投放效果",
                    "近30天还没有可分析的推荐投放记录，暂时无法判断点击、正向反馈或负反馈趋势。",
                    "empty",
                    "没有投放样本",
                    List.of("近30天 recommendation_delivery")
            );
        }

        String headline = String.format(Locale.ROOT,
                "近%d天共投放%d条，点击率%.1f%%，有用反馈率%.1f%%，不感兴趣率%.1f%%。",
                snapshot.windowDays(),
                snapshot.deliveryCount(),
                snapshot.clickThroughRate() * 100,
                snapshot.usefulRate() * 100,
                snapshot.negativeRate() * 100
        );
        String categorySummary = snapshot.categories().stream()
                .limit(3)
                .map(item -> String.format(Locale.ROOT,
                        "%s：%d条/点击%.1f%%/有用%.1f%%/负反馈%.1f%%",
                        item.category(), item.deliveryCount(), item.clickThroughRate() * 100,
                        item.usefulRate() * 100, item.negativeRate() * 100))
                .reduce((left, right) -> left + "；" + right)
                .orElse("暂无分类表现");
        String suggestions = String.join("；", snapshot.strategySuggestions());

        return new MedicalAiToolResult(
                "推荐投放效果",
                headline + " 分类表现：" + categorySummary + "。策略观察：" + suggestions + "。",
                "ok",
                "聚合近30天投放、点击与显式反馈，生成只读策略观察",
                List.of("recommendation_delivery / recommendation_content 聚合指标")
        );
    }

    private boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) return true;
        }
        return false;
    }
}
