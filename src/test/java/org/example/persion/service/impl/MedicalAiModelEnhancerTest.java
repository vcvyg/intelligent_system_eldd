package org.example.persion.service.impl;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MedicalAiModelEnhancerTest {

    private final MedicalAiModelEnhancer enhancer = new MedicalAiModelEnhancer();

    @Test
    void acceptsRewriteThatKeepsGroundedNumbers() {
        String facts = "近7天共查到 3 条记录，最新心率 76 bpm。";
        String rewrite = "系统近7天共有3条记录，最新心率为76 bpm。";

        assertTrue(enhancer.isSafeRewrite(rewrite, facts));
    }

    @Test
    void rejectsNewNumericFactIntroducedByModel() {
        String facts = "最新心率 76 bpm。";
        String rewrite = "最新心率为76 bpm，建议30分钟后复测。";

        assertFalse(enhancer.isSafeRewrite(rewrite, facts));
    }

    @Test
    void rejectsMedicalDecisionLanguageEvenWithoutNewNumbers() {
        String facts = "系统记录显示心率偏高，请由医护人员结合现场情况判断。";
        String rewrite = "系统记录显示心率偏高，建议服用相关药物。";

        assertFalse(enhancer.isSafeRewrite(rewrite, facts));
    }
}
