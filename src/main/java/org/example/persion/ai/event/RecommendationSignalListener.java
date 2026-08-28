package org.example.persion.ai.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.persion.service.CareSignalOutboxService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Persists proactive-care signals into the transactional outbox before the business transaction commits.
 *
 * <p>The downstream recommendation trigger is intentionally not created here. A separate retry worker
 * drains the outbox after commit, so recommendation infrastructure outages cannot silently lose events.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RecommendationSignalListener {

    private final CareSignalOutboxService outboxService;

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT, fallbackExecution = true)
    public void onCareSignal(CareSignalEvent event) {
        outboxService.enqueue(event);
        log.info("care_signal_outbox_enqueued signalType={} referenceId={}", event.signalType(), event.referenceId());
    }
}
