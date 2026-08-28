package org.example.persion.service;

import org.example.persion.dto.RecommendationFeedbackDTO;
import org.example.persion.vo.RecommendationItemVO;

import java.util.List;

public interface RecommendationService {

    List<RecommendationItemVO> preview(Long elderlyId, Long familyUserId);

    int deliver(Long elderlyId);

    List<RecommendationItemVO> familyFeed(Long familyUserId, Long elderlyId);

    void feedback(Long familyUserId, RecommendationFeedbackDTO dto);
}
