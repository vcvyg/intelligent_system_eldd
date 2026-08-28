package org.example.persion.ai.agent;

import java.util.Optional;

/**
 * Optional semantic planning model used by the hybrid medical AI planner.
 *
 * <p>The contract only returns a read-only Tool plan. It never executes tools,
 * reads business data or produces medical facts.</p>
 */
@FunctionalInterface
public interface MedicalAiPlanningModel {

    Optional<MedicalAiPlan> plan(String question);
}
