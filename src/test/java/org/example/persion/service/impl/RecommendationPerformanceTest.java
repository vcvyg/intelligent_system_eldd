package org.example.persion.service.impl;

import org.example.persion.entity.RecommendationContent;
import org.example.persion.entity.RecommendationDelivery;
import org.example.persion.repository.AlertRecordMapper;
import org.example.persion.repository.ElderlyFamilyRelationMapper;
import org.example.persion.repository.FamilyServiceRecordMapper;
import org.example.persion.repository.HealthDataMapper;
import org.example.persion.repository.RecommendationContentMapper;
import org.example.persion.repository.RecommendationDeliveryMapper;
import org.example.persion.repository.RecommendationFeedbackMapper;
import org.example.persion.vo.RecommendationPerformanceVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecommendationPerformanceTest {

    private static final Long ELDERLY_ID = 11L;

    @Mock private RecommendationContentMapper contentMapper;
    @Mock private RecommendationDeliveryMapper deliveryMapper;
    @Mock private RecommendationFeedbackMapper feedbackMapper;
    @Mock private ElderlyFamilyRelationMapper familyRelationMapper;
    @Mock private HealthDataMapper healthDataMapper;
    @Mock private AlertRecordMapper alertRecordMapper;
    @Mock private FamilyServiceRecordMapper familyServiceRecordMapper;

    private RecommendationServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new RecommendationServiceImpl(
                contentMapper,
                deliveryMapper,
                feedbackMapper,
                familyRelationMapper,
                healthDataMapper,
                alertRecordMapper,
                familyServiceRecordMapper
        );
    }

    @Test
    void aggregatesDeliveryClickAndExplicitFeedbackIntoStrategySnapshot() {
        RecommendationContent health = content(1L, "HEALTH_CHECK");
        RecommendationContent safety = content(2L, "SAFETY");
        when(contentMapper.selectBatchIds(anyCollection())).thenReturn(List.of(health, safety));

        RecommendationDelivery useful = delivery(1L, "USEFUL");
        useful.setClickedAt(LocalDateTime.now().minusDays(1));
        RecommendationDelivery negative = delivery(2L, "NOT_INTERESTED");
        when(deliveryMapper.selectList(any())).thenReturn(List.of(useful, negative));

        RecommendationPerformanceVO result = service.performance(ELDERLY_ID, null, 30);

        assertEquals(2, result.deliveryCount());
        assertEquals(1, result.clickCount());
        assertEquals(1, result.usefulCount());
        assertEquals(1, result.notInterestedCount());
        assertEquals(0.5, result.clickThroughRate());
        assertEquals(0.5, result.usefulRate());
        assertEquals(0.5, result.negativeRate());
        assertEquals(2, result.categories().size());
        assertFalse(result.strategySuggestions().isEmpty());
    }

    private RecommendationDelivery delivery(Long contentId, String status) {
        RecommendationDelivery delivery = new RecommendationDelivery();
        delivery.setElderlyId(ELDERLY_ID);
        delivery.setFamilyUserId(22L);
        delivery.setContentId(contentId);
        delivery.setStatus(status);
        delivery.setCreateTime(LocalDateTime.now().minusDays(2));
        return delivery;
    }

    private RecommendationContent content(Long id, String category) {
        RecommendationContent content = new RecommendationContent();
        content.setId(id);
        content.setCategory(category);
        content.setEnabled(1);
        return content;
    }
}
