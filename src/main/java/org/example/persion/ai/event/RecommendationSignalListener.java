package org.example.persion.ai.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.persion.service.RecommendationTriggerService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 业务事务提交后记录主动关怀触发器。
 *
 * <p>推荐候选只进入人工复核队列，不在事件监听器里直接向家属投放。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RecommendationSignalListener {

    private final RecommendationTriggerService triggerService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onCareSignal(CareSignalEvent event) {
        try {
            triggerService.record(event);
            log.info("care_recommendation_signal signalType={} referenceId={}", event.signalType(), event.referenceId());
        } catch (RuntimeException exception) {
            log.warn("care recommendation signal persistence failed: {}", exception.getClass().getSimpleName());
        }
    }
}
