package org.example.persion.ai.tool;

import lombok.RequiredArgsConstructor;
import org.example.persion.entity.ElderlyInfo;
import org.example.persion.repository.ElderlyInfoMapper;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class ProfileQueryTool implements MedicalAiTool {

    private final ElderlyInfoMapper elderlyInfoMapper;

    @Override
    public String name() {
        return "patient_profile";
    }

    @Override
    public boolean supports(String question) {
        if (question == null) return false;
        String q = question.toLowerCase();
        return containsAny(q,
                "档案", "年龄", "性别", "病史", "既往史", "基础病", "过敏", "病情", "profile");
    }

    @Override
    public MedicalAiToolResult execute(MedicalAiToolContext context) {
        ElderlyInfo elderly = elderlyInfoMapper.selectById(context.elderlyId());
        if (elderly == null) {
            return new MedicalAiToolResult(
                    "档案信息",
                    "系统没有查到该老人档案。",
                    "empty",
                    "老人档案不存在",
                    List.of("老人档案")
            );
        }

        List<String> facts = new ArrayList<>();
        if (elderly.getAge() != null) facts.add(elderly.getAge() + "岁");
        if (elderly.getGender() != null && !elderly.getGender().isBlank()) facts.add(elderly.getGender());

        String question = context.question() == null ? "" : context.question();
        if (containsAny(question, "病史", "既往史", "基础病", "病情")) {
            facts.add("系统病史：" + emptyAs(elderly.getMedicalHistory(), "暂无登记"));
        }
        if (containsAny(question, "过敏", "过敏史")) {
            facts.add("过敏史：" + emptyAs(elderly.getAllergyHistory(), "暂无登记"));
        }
        if (facts.isEmpty()) {
            facts.add("已定位到当前负责老人档案");
        }

        return new MedicalAiToolResult(
                "档案信息",
                String.join("；", facts) + "。",
                "ok",
                "读取必要的老人档案字段",
                List.of("老人档案")
        );
    }

    private String emptyAs(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) return true;
        }
        return false;
    }
}
