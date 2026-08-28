package org.example.persion.service;

import org.example.persion.ai.event.CareSignalEvent;
import org.example.persion.vo.RecommendationTriggerVO;

import java.util.List;

public interface RecommendationTriggerService {

    void record(CareSignalEvent event);

    List<RecommendationTriggerVO> pending(Long elderlyId);

    RecommendationTriggerVO approve(Long triggerId, Long reviewerId, String reason);

    RecommendationTriggerVO reject(Long triggerId, Long reviewerId, String reason);

    boolean hasPending(Long elderlyId);

    boolean hasApproved(Long elderlyId);

    void markDelivered(Long elderlyId);
}
