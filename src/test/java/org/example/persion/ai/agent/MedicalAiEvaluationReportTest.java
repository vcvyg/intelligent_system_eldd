package org.example.persion.ai.agent;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Produces a CI artifact from the frozen deterministic-planner dataset.
 *
 * <p>The report is intentionally based on executable cases rather than hand-written numbers,
 * so README/resume claims can be traced back to a repeatable evaluation run.</p>
 */
class MedicalAiEvaluationReportTest {

    private final MedicalAiPlanner planner = new MedicalAiPlanner();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void writesPlannerEvaluationArtifacts() throws Exception {
        List<EvaluationCase> cases;
        try (InputStream input = getClass().getResourceAsStream("/medical-ai-evaluation-cases.json")) {
            assertNotNull(input, "evaluation dataset must exist");
            cases = objectMapper.readValue(input, new TypeReference<List<EvaluationCase>>() {});
        }

        int exactMatches = 0;
        int truePositive = 0;
        int falsePositive = 0;
        int falseNegative = 0;
        List<Map<String, Object>> caseResults = new ArrayList<>();

        for (EvaluationCase testCase : cases) {
            List<String> actual = planner.plan(testCase.question()).toolNames();
            boolean exact = testCase.expectedTools().equals(actual);
            if (exact) exactMatches++;

            Set<String> expectedSet = new LinkedHashSet<>(testCase.expectedTools());
            Set<String> actualSet = new LinkedHashSet<>(actual);
            for (String tool : actualSet) {
                if (expectedSet.contains(tool)) truePositive++;
                else falsePositive++;
            }
            for (String tool : expectedSet) {
                if (!actualSet.contains(tool)) falseNegative++;
            }

            Map<String, Object> caseResult = new LinkedHashMap<>();
            caseResult.put("id", testCase.id());
            caseResult.put("expectedTools", testCase.expectedTools());
            caseResult.put("actualTools", actual);
            caseResult.put("exactMatch", exact);
            caseResults.add(caseResult);
        }

        double exactMatchRate = ratio(exactMatches, cases.size());
        double precision = ratio(truePositive, truePositive + falsePositive);
        double recall = ratio(truePositive, truePositive + falseNegative);
        double f1 = precision + recall == 0 ? 0 : (2 * precision * recall) / (precision + recall);

        Map<String, Object> metrics = new LinkedHashMap<>();
        metrics.put("datasetCases", cases.size());
        metrics.put("exactMatches", exactMatches);
        metrics.put("exactMatchRate", exactMatchRate);
        metrics.put("microPrecision", precision);
        metrics.put("microRecall", recall);
        metrics.put("microF1", f1);

        Map<String, Object> report = new LinkedHashMap<>();
        report.put("generatedAt", Instant.now().toString());
        report.put("plannerMode", "deterministic-baseline");
        report.put("metrics", metrics);
        report.put("cases", caseResults);

        Path target = Path.of("target");
        Files.createDirectories(target);
        objectMapper.writerWithDefaultPrettyPrinter()
                .writeValue(target.resolve("medical-ai-evaluation-report.json").toFile(), report);

        String markdown = "# Medical AI Agent Evaluation\n\n"
                + "- Dataset cases: " + cases.size() + "\n"
                + "- Exact match: " + exactMatches + "/" + cases.size() + " (" + percent(exactMatchRate) + ")\n"
                + "- Micro precision: " + percent(precision) + "\n"
                + "- Micro recall: " + percent(recall) + "\n"
                + "- Micro F1: " + percent(f1) + "\n\n"
                + "This report is generated from `medical-ai-evaluation-cases.json` during CI.\n";
        Files.writeString(target.resolve("medical-ai-evaluation-report.md"), markdown);

        assertEquals(cases.size(), exactMatches, "frozen planner dataset must keep exact tool selection");
    }

    private double ratio(int numerator, int denominator) {
        if (denominator == 0) return 0;
        return ((double) numerator) / denominator;
    }

    private String percent(double value) {
        return String.format(java.util.Locale.ROOT, "%.2f%%", value * 100);
    }

    private record EvaluationCase(String id, String question, List<String> expectedTools) {
    }
}
