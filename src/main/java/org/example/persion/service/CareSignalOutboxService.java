package org.example.persion.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.RequiredArgsConstructor;
import org.example.persion.ai.event.CareSignalEvent;
import org.example.persion.entity.CareSignalOutbox;
import org.example.persion.repository.CareSignalOutboxMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Transactional Outbox for proactive-care domain signals.
 *
 * <p>The payload is deliberately minimal. If downstream recommendation processing fails,
 * the business write stays committed and the signal is retried independently.</p>
 */
@Service
@RequiredArgsConstructor
public class CareSignalOutboxService {

    static final String PENDING = "PENDING";
    static final String RETRY = "RETRY";
    static final String PROCESSING = "PROCESSING";
    static final String PROCESSED = "PROCESSED";
    static final String DEAD_LETTER = "DEAD_LETTER";
    private static final int MAX_RETRY = 5;
    private static final int CLAIM_LEASE_MINUTES = 2;

    private final CareSignalOutboxMapper outboxMapper;

    /**
     * Called synchronously from the business transaction so the outbox row commits atomically
     * with the health/alert/service change. Duplicate business references are ignored.
     */
    public void enqueue(CareSignalEvent event) {
        if (event == null || event.elderlyId() == null || event.signalType() == null || event.signalType().isBlank()) {
            return;
        }

        Long existing = outboxMapper.selectCount(new LambdaQueryWrapper<CareSignalOutbox>()
                .eq(CareSignalOutbox::getEventKey, event.eventKey()));
        if (existing != null && existing > 0) return;

        CareSignalOutbox row = new CareSignalOutbox();
        row.setEventKey(event.eventKey());
        row.setElderlyId(event.elderlyId());
        row.setSignalType(event.signalType());
        row.setReferenceId(event.referenceId());
        row.setOccurredAt(event.occurredAt() == null ? LocalDateTime.now() : event.occurredAt());
        row.setStatus(PENDING);
        row.setRetryCount(0);
        outboxMapper.insert(row);
    }

    /**
     * Claims a small ordered batch. Conditional updates make duplicate worker execution harmless
     * when multiple application instances poll the same table. PROCESSING rows whose worker died
     * are returned to RETRY after a short lease instead of remaining stuck forever.
     */
    @Transactional
    public List<CareSignalOutbox> claimBatch(int requestedLimit) {
        int limit = Math.max(1, Math.min(requestedLimit, 100));
        LocalDateTime now = LocalDateTime.now();
        recoverAbandonedClaims(now);

        List<CareSignalOutbox> candidates = outboxMapper.selectList(
                new LambdaQueryWrapper<CareSignalOutbox>()
                        .in(CareSignalOutbox::getStatus, PENDING, RETRY)
                        .and(wrapper -> wrapper.isNull(CareSignalOutbox::getNextRetryAt)
                                .or()
                                .le(CareSignalOutbox::getNextRetryAt, now))
                        .orderByAsc(CareSignalOutbox::getCreateTime)
                        .orderByAsc(CareSignalOutbox::getId)
                        .last("OFFSET 0 ROWS FETCH NEXT " + limit + " ROWS ONLY")
        );
        if (candidates == null || candidates.isEmpty()) return List.of();

        List<CareSignalOutbox> claimed = new ArrayList<>();
        for (CareSignalOutbox candidate : candidates) {
            int updated = outboxMapper.update(null,
                    new LambdaUpdateWrapper<CareSignalOutbox>()
                            .eq(CareSignalOutbox::getId, candidate.getId())
                            .in(CareSignalOutbox::getStatus, PENDING, RETRY)
                            .set(CareSignalOutbox::getStatus, PROCESSING)
                            .set(CareSignalOutbox::getNextRetryAt, now.plusMinutes(CLAIM_LEASE_MINUTES))
                            .set(CareSignalOutbox::getUpdateTime, now));
            if (updated > 0) {
                candidate.setStatus(PROCESSING);
                candidate.setNextRetryAt(now.plusMinutes(CLAIM_LEASE_MINUTES));
                claimed.add(candidate);
            }
        }
        return claimed;
    }

    @Transactional
    public void markProcessed(Long id) {
        if (id == null) return;
        LocalDateTime now = LocalDateTime.now();
        outboxMapper.update(null, new LambdaUpdateWrapper<CareSignalOutbox>()
                .eq(CareSignalOutbox::getId, id)
                .eq(CareSignalOutbox::getStatus, PROCESSING)
                .set(CareSignalOutbox::getStatus, PROCESSED)
                .set(CareSignalOutbox::getProcessedAt, now)
                .set(CareSignalOutbox::getNextRetryAt, null)
                .set(CareSignalOutbox::getLastErrorType, null)
                .set(CareSignalOutbox::getUpdateTime, now));
    }

    @Transactional
    public void markRetry(Long id, Throwable failure) {
        if (id == null) return;
        CareSignalOutbox row = outboxMapper.selectById(id);
        if (row == null) return;

        int retryCount = (row.getRetryCount() == null ? 0 : row.getRetryCount()) + 1;
        boolean exhausted = retryCount >= MAX_RETRY;
        LocalDateTime now = LocalDateTime.now();
        long delaySeconds = Math.min(300L, 5L * (1L << Math.min(retryCount - 1, 5)));
        String errorType = failure == null ? "UnknownFailure" : failure.getClass().getSimpleName();
        if (errorType.length() > 80) errorType = errorType.substring(0, 80);

        outboxMapper.update(null, new LambdaUpdateWrapper<CareSignalOutbox>()
                .eq(CareSignalOutbox::getId, id)
                .eq(CareSignalOutbox::getStatus, PROCESSING)
                .set(CareSignalOutbox::getStatus, exhausted ? DEAD_LETTER : RETRY)
                .set(CareSignalOutbox::getRetryCount, retryCount)
                .set(CareSignalOutbox::getNextRetryAt, exhausted ? null : now.plusSeconds(delaySeconds))
                .set(CareSignalOutbox::getLastErrorType, errorType)
                .set(CareSignalOutbox::getUpdateTime, now));
    }

    private void recoverAbandonedClaims(LocalDateTime now) {
        outboxMapper.update(null, new LambdaUpdateWrapper<CareSignalOutbox>()
                .eq(CareSignalOutbox::getStatus, PROCESSING)
                .le(CareSignalOutbox::getNextRetryAt, now)
                .set(CareSignalOutbox::getStatus, RETRY)
                .set(CareSignalOutbox::getNextRetryAt, now)
                .set(CareSignalOutbox::getLastErrorType, "AbandonedClaim")
                .set(CareSignalOutbox::getUpdateTime, now));
    }
}
