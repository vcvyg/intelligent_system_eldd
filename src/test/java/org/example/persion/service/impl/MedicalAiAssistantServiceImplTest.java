package org.example.persion.service.impl;

import org.example.persion.ai.tool.HealthQueryTool;
import org.example.persion.ai.tool.MedicalAiToolRegistry;
import org.example.persion.common.exception.BusinessException;
import org.example.persion.dto.MedicalAiChatRequest;
import org.example.persion.entity.ElderlyInfo;
import org.example.persion.entity.FamilyServiceRecord;
import org.example.persion.entity.HealthData;
import org.example.persion.enums.ServiceProgressStatus;
import org.example.persion.repository.AlertRecordMapper;
import org.example.persion.repository.ElderlyInfoMapper;
import org.example.persion.repository.FamilyServiceRecordMapper;
import org.example.persion.repository.HealthDataMapper;
import org.example.persion.vo.AlertRecordVO;
import org.example.persion.vo.ElderlyInfoVO;
import org.example.persion.vo.MedicalAiAnswerVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MedicalAiAssistantServiceImplTest {

    private static final Long MEDICAL_USER_ID = 7L;
    private static final Long ELDERLY_ID = 11L;

    @Mock
    private ElderlyInfoMapper elderlyInfoMapper;
    @Mock
    private HealthDataMapper healthDataMapper;
    @Mock
    private AlertRecordMapper alertRecordMapper;
    @Mock
    private FamilyServiceRecordMapper familyServiceRecordMapper;

    private MedicalAiAssistantServiceImpl service;
    private ElderlyInfo elderly;

    @BeforeEach
    void setUp() {
        MedicalAiToolRegistry toolRegistry = new MedicalAiToolRegistry(
                List.of(new HealthQueryTool(healthDataMapper))
        );
        service = new MedicalAiAssistantServiceImpl(
                elderlyInfoMapper,
                healthDataMapper,
                alertRecordMapper,
                familyServiceRecordMapper,
                toolRegistry
        );

        elderly = new ElderlyInfo();
        elderly.setId(ELDERLY_ID);
        elderly.setName("王阿姨");
        elderly.setAge(72);
        elderly.setGender("女");
        elderly.setMedicalHistory("高血压随访");
        when(elderlyInfoMapper.selectElderlyListByMedicalUserId(MEDICAL_USER_ID)).thenReturn(List.of(elderly));
    }

    @Test
    void compositeQuestionRunsRegisteredHealthToolAndCombinesFacts() {
        ElderlyInfoVO detail = new ElderlyInfoVO();
        detail.setId(ELDERLY_ID);
        detail.setName("王阿姨");
        detail.setRoomNumber("3-206");
        detail.setRoomType("双人间");
        when(elderlyInfoMapper.selectElderlyWithRoom(ELDERLY_ID)).thenReturn(detail);

        HealthData health = new HealthData();
        health.setElderlyId(ELDERLY_ID);
        health.setHeartRate(new BigDecimal("76"));
        health.setBloodPressureHigh(new BigDecimal("128"));
        health.setBloodPressureLow(new BigDecimal("78"));
        health.setMeasureTime(LocalDateTime.now().minusHours(1));
        when(healthDataMapper.findByDateTimeRange(any(), any(), eq(ELDERLY_ID))).thenReturn(List.of(health));

        AlertRecordVO alert = new AlertRecordVO();
        alert.setElderlyId(ELDERLY_ID);
        alert.setAlertType("心率异常");
        alert.setAlertContent("心率短时偏高");
        alert.setStatus("待处理");
        alert.setAlertTime(LocalDateTime.now().minusHours(2));
        when(alertRecordMapper.selectByElderlyId(ELDERLY_ID)).thenReturn(List.of(alert));

        MedicalAiChatRequest request = request(ELDERLY_ID, null, "王阿姨住哪，最近心率和告警怎么样？");
        MedicalAiAnswerVO answer = service.chat(MEDICAL_USER_ID, request);

        assertTrue(answer.getAnswer().contains("3-206"));
        assertTrue(answer.getAnswer().contains("76 bpm"));
        assertTrue(answer.getAnswer().contains("心率异常"));
        assertTrue(hasTool(answer, "room_lookup"));
        assertTrue(hasTool(answer, "health_recent"));
        assertTrue(hasTool(answer, "alerts_recent"));
        assertTrue(answer.getSources().contains("health_data / 近7天健康记录"));
    }

    @Test
    void sessionKeepsCurrentPatientForPronounFollowUp() {
        ElderlyInfoVO detail = new ElderlyInfoVO();
        detail.setRoomNumber("2-101");
        when(elderlyInfoMapper.selectElderlyWithRoom(ELDERLY_ID)).thenReturn(detail);

        MedicalAiAnswerVO first = service.chat(
                MEDICAL_USER_ID,
                request(ELDERLY_ID, null, "住哪个房间？")
        );

        when(healthDataMapper.findByDateTimeRange(any(), any(), eq(ELDERLY_ID))).thenReturn(List.of());
        FamilyServiceRecord serviceRecord = new FamilyServiceRecord();
        serviceRecord.setElderlyId(ELDERLY_ID);
        serviceRecord.setServiceDate(LocalDate.now().plusDays(1));
        serviceRecord.setServiceTime(LocalTime.of(9, 30));
        serviceRecord.setServiceType("助浴服务");
        serviceRecord.setStatus(ServiceProgressStatus.PENDING);
        when(familyServiceRecordMapper.selectList(any())).thenReturn(List.of(serviceRecord));

        MedicalAiAnswerVO second = service.chat(
                MEDICAL_USER_ID,
                request(null, first.getSessionId(), "那她近期有什么照护安排？")
        );

        assertEquals(ELDERLY_ID, second.getElderlyId());
        assertTrue(second.getAnswer().contains("助浴服务"));
        assertTrue(second.getAnswer().contains("没有独立的“护理计划”表"));
        assertTrue(hasTool(second, "care_schedule"));
    }

    @Test
    void resetSessionRemovesPatientContext() {
        ElderlyInfoVO detail = new ElderlyInfoVO();
        detail.setRoomNumber("2-101");
        when(elderlyInfoMapper.selectElderlyWithRoom(ELDERLY_ID)).thenReturn(detail);

        MedicalAiAnswerVO first = service.chat(
                MEDICAL_USER_ID,
                request(ELDERLY_ID, null, "住哪个房间？")
        );
        service.resetSession(MEDICAL_USER_ID, first.getSessionId());

        MedicalAiAnswerVO afterReset = service.chat(
                MEDICAL_USER_ID,
                request(null, first.getSessionId(), "那她最近怎么样？")
        );

        assertTrue(afterReset.getAnswer().contains("请先选择"));
        assertTrue(hasTool(afterReset, "patient_scope"));
        assertTrue(afterReset.getSources().contains("当前医护负责老人列表"));
    }

    @Test
    void missingPatientContextOnlyReturnsAssignedScope() {
        MedicalAiAnswerVO answer = service.chat(
                MEDICAL_USER_ID,
                request(null, null, "最近整体情况怎么样？")
        );

        assertTrue(answer.getAnswer().contains("当前可查询：王阿姨"));
        assertTrue(hasTool(answer, "patient_scope"));
        assertEquals(List.of("当前医护负责老人列表"), answer.getSources());
    }

    @Test
    void rejectsPatientOutsideCurrentMedicalScope() {
        BusinessException error = assertThrows(BusinessException.class, () ->
                service.chat(MEDICAL_USER_ID, request(999L, null, "最近健康怎么样？"))
        );
        assertEquals(403, error.getCode());
        assertTrue(error.getMessage().contains("无权访问"));
    }

    @Test
    void blocksDiagnosisAndMedicationDecisionRequests() {
        MedicalAiAnswerVO answer = service.chat(
                MEDICAL_USER_ID,
                request(ELDERLY_ID, null, "她这个情况应该怎么用药，要不要调整剂量？")
        );

        assertTrue(answer.getAnswer().contains("不能替代专业诊断"));
        assertTrue(hasTool(answer, "medical_safety_guard"));
        assertTrue(answer.getTools().stream().anyMatch(tool -> "blocked".equals(tool.getStatus())));
    }

    private MedicalAiChatRequest request(Long elderlyId, String sessionId, String message) {
        MedicalAiChatRequest request = new MedicalAiChatRequest();
        request.setElderlyId(elderlyId);
        request.setSessionId(sessionId);
        request.setMessage(message);
        return request;
    }

    private boolean hasTool(MedicalAiAnswerVO answer, String tool) {
        return answer.getTools().stream().anyMatch(item -> tool.equals(item.getTool()));
    }
}
