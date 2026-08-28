package org.example.persion.ai.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.persion.entity.CareSignalOutbox;
import org.example.persion.service.CareSignalOutboxService;
import org.example.persion.service.RecommendationTriggerService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Polls the transactional outbox and materializes recommendation review triggers.
 *
 * <p>Each downstream attempt runs outside the outbox claim transaction. If processing fails,
 * the outbox row is moved to RETRY with bounded exponential backoff; after five failures it
 * becomes DEAD_LETTER for manual inspection.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CareSignalOutboxWorker {

    private final CareSignalOutboxService outboxService;
    private final RecommendationTriggerService triggerService;

    @Value("${medical.ai.care-outbox.enabled:true}")
    private boolean enabled;

    @Value("${medical.ai.care-outbox.batch-size:25}")
    private int batchSize;

    @Scheduled(fixedDelayString = "${medical.ai.care-outbox.fixed-delay-ms:10000}")
    public void drain() {
        if (!enabled) return;

        List<CareSignalOutbox> batch;
        try {
            batch = outboxService.claimBatch(batchSize);
        } catch (RuntimeException exception) {
            log.warn("care_signal_outbox_claim_failed errorType={}", exception.getClass().getSimpleName());
            return;
        }

        for (CareSignalOutbox row : batch) {
            try {
                triggerService.record(new CareSignalEvent(
                        row.getElderlyId(),
                        row.getSignalType(),
                        row.getReferenceId(),
                        row.getOccurredAt()
                ));
                outboxService.markProcessed(row.getId());
                log.info("care_signal_outbox_processed outboxId={} signalType={}", row.getId(), row.getSignalType());
            } catch (RuntimeException exception) {
                outboxService.markRetry(row.getId(), exception);
                log.warn("care_signal_outbox_retry outboxId={} signalType={} errorType={}",
                        row.getId(), row.getSignalType(), exception.getClass().getSimpleName());
            }
        }
    }
}
