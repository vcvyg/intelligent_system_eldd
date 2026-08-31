package org.example.persion.vo;

import java.util.List;

/**
 * Recommendation delivery/feedback performance for a bounded observation window.
 *
 * <p>The snapshot is intentionally aggregated and does not expose medical facts,
 * chat content or free-form user text. It is used by the admin dashboard and the
 * read-only Agent tool to explain recommendation effectiveness and strategy signals.</p>
 */
public record RecommendationPerformanceVO(
        int windowDays,
        int deliveryCount,
        int clickCount,
        int usefulCount,
        int notInterestedCount,
        double clickThroughRate,
        double usefulRate,
        double negativeRate,
        List<CategoryPerformance> categories,
        List<String> strategySuggestions
) {

    public record CategoryPerformance(
            String category,
            int deliveryCount,
            int clickCount,
            int usefulCount,
            int notInterestedCount,
            double clickThroughRate,
            double usefulRate,
            double negativeRate
    ) {
    }
}
