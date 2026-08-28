package org.example.persion.service;

import org.example.persion.ai.event.CareSignalEvent;
import org.example.persion.vo.RecommendationTriggerVO;

import java.util.List;

public interface RecommendationTriggerService {

    void record(CareSignalEvent event);

    List<RecommendationTriggerVO> pending(Long elderlyId);

    void markDelivered(Long elderlyId);
}
