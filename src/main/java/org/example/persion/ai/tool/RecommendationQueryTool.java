package org.example.persion.ai.tool;

import lombok.RequiredArgsConstructor;
import org.example.persion.service.RecommendationService;
import org.example.persion.vo.RecommendationItemVO;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 主动关怀推荐预览 Tool。
 *
 * <p>医护端只读取可解释的推荐结果，不在 Agent 内执行真实投放。</p>
 */
@Component
@RequiredArgsConstructor
public class RecommendationQueryTool implements MedicalAiTool {

    private final RecommendationService recommendationService;

    @Override
    public String name() {
        return "recommendation_preview";
    }

    @Override
    public boolean supports(String question) {
        if (question == null) return false;
        String q = question.toLowerCase();
        return containsAny(q, "推荐", "主动关怀", "适合推", "推什么", "关怀内容", "recommend");
    }

    @Override
    public MedicalAiToolResult execute(MedicalAiToolContext context) {
        List<RecommendationItemVO> items = recommendationService.preview(context.elderlyId(), null);
        if (items == null || items.isEmpty()) {
            return new MedicalAiToolResult(
                    "主动关怀推荐",
                    "当前没有可用的主动关怀推荐内容。",
                    "empty",
                    "推荐结果为空",
                    List.of("主动关怀推荐中心")
            );
        }

        String body = items.stream().limit(3).map(item ->
                item.getTitle() + "：" + item.getReason()
        ).collect(Collectors.joining("；"));

        return new MedicalAiToolResult(
                "主动关怀推荐",
                "当前可优先考虑：" + body + "。以上为推荐预览，不会由 AI 自动向家属投放。",
                "ok",
                "读取 Top " + Math.min(3, items.size()) + " 条可解释推荐",
                List.of("主动关怀内容池 / 健康、告警、服务信号")
        );
    }

    private boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) return true;
        }
        return false;
    }
}
