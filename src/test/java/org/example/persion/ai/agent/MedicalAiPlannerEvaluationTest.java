package org.example.persion.ai.agent;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class MedicalAiPlannerEvaluationTest {

    private final MedicalAiPlanner planner = new MedicalAiPlanner();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void frozenPlannerDatasetKeepsExactToolSelection() throws Exception {
        List<EvaluationCase> cases;
        try (InputStream input = getClass().getResourceAsStream("/medical-ai-evaluation-cases.json")) {
            assertNotNull(input, "evaluation dataset must exist");
            cases = objectMapper.readValue(input, new TypeReference<List<EvaluationCase>>() {});
        }

        int passed = 0;
        StringBuilder failures = new StringBuilder();
        for (EvaluationCase testCase : cases) {
            List<String> actual = planner.plan(testCase.question()).toolNames();
            if (testCase.expectedTools().equals(actual)) {
                passed++;
            } else {
                failures.append(testCase.id())
                        .append(": expected=")
                        .append(testCase.expectedTools())
                        .append(", actual=")
                        .append(actual)
                        .append("; ");
            }
        }

        String report = "planner exact-match accuracy=" + passed + "/" + cases.size() + "; " + failures;
        assertEquals(cases.size(), passed, report);
    }

    private record EvaluationCase(String id, String question, List<String> expectedTools) {
    }
}
