package org.example.persion.service;

import org.example.persion.dto.MedicalAiChatRequest;
import org.example.persion.vo.MedicalAiAnswerVO;
import org.example.persion.vo.MedicalAiPatientVO;

import java.util.List;

public interface MedicalAiAssistantService {
    List<MedicalAiPatientVO> listAssignedPatients(Long medicalUserId);

    MedicalAiAnswerVO chat(Long medicalUserId, MedicalAiChatRequest request);

    void resetSession(Long medicalUserId, String sessionId);
}
