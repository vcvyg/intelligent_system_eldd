package org.example.persion.ai.tool;

import org.springframework.stereotype.Component;

/**
 * 健康数据查询 Tool 占位实现。
 *
 * <p>业务查询逻辑暂时仍由原 Service 负责，后续迁移时只移动执行部分。</p>
 */
@Component
public class HealthQueryTool implements MedicalAiTool {

    @Override
    public String name() {
        return "health_recent";
    }

    @Override
    public boolean supports(String question) {
        if (question == null) {
            return false;
        }
        return question.contains("健康")
                || question.contains("心率")
                || question.contains("血压")
                || question.contains("指标");
    }
}
