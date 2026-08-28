package org.example.persion.service;

import org.example.persion.ai.action.MedicalAiActionProposalStore;
import org.example.persion.common.exception.BusinessException;
import org.example.persion.entity.AlertRecord;
import org.example.persion.entity.ElderlyInfo;
import org.example.persion.repository.AlertRecordMapper;
import org.example.persion.repository.ElderlyInfoMapper;
import org.example.persion.vo.MedicalAiActionProposalVO;
import org.example.persion.vo.MedicalAiActionResultVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MedicalAiActionServiceTest {

    @Mock private AlertRecordMapper alertRecordMapper;
    @Mock private ElderlyInfoMapper elderlyInfoMapper;
    @Mock private AlertService alertService;
    @Mock private MedicalAiActionProposalStore proposalStore;

    @Test
    void proposalDoesNotWriteUntilSecondExplicitConfirmation() {
        AlertRecord alert = alert(33L, 12L, "待处理");
        when(alertRecordMapper.selectById(33L)).thenReturn(alert);
        when(elderlyInfoMapper.selectElderlyListByMedicalUserId(7L)).thenReturn(List.of(elderly(12L)));
        when(proposalStore.put(anyString(), eq(7L), eq(12L), eq(33L), eq(MedicalAiActionProposalStore.START_ALERT_PROCESSING)))
                .thenReturn(LocalDateTime.now().plusMinutes(10));

        MedicalAiActionService service = service();
        MedicalAiActionProposalVO proposal = service.proposeStartAlert(7L, 33L);

        assertTrue(proposal.confirmationRequired());
        assertEquals("START_ALERT_PROCESSING", proposal.actionType());
        verify(alertService, never()).processAlert(33L);
    }

    @Test
    void validOneTimeProposalExecutesWriteAfterPermissionRecheck() {
        AlertRecord alert = alert(33L, 12L, "待处理");
        when(proposalStore.consume("proposal-1")).thenReturn(Optional.of(
                new MedicalAiActionProposalStore.PendingAction(
                        7L, 12L, 33L, MedicalAiActionProposalStore.START_ALERT_PROCESSING,
                        LocalDateTime.now().plusMinutes(5)
                )
        ));
        when(alertRecordMapper.selectById(33L)).thenReturn(alert);
        when(elderlyInfoMapper.selectElderlyListByMedicalUserId(7L)).thenReturn(List.of(elderly(12L)));

        MedicalAiActionResultVO result = service().confirm(7L, "proposal-1");

        assertEquals("EXECUTED", result.status());
        verify(alertService).processAlert(33L);
    }

    @Test
    void expiredOrConsumedProposalCannotExecuteWrite() {
        when(proposalStore.consume("gone")).thenReturn(Optional.empty());

        MedicalAiActionService service = service();

        assertThrows(BusinessException.class, () -> service.confirm(7L, "gone"));
        verify(alertService, never()).processAlert(org.mockito.ArgumentMatchers.anyLong());
    }

    @Test
    void confirmationRechecksCurrentPatientAssignment() {
        AlertRecord alert = alert(33L, 12L, "待处理");
        when(proposalStore.consume("proposal-2")).thenReturn(Optional.of(
                new MedicalAiActionProposalStore.PendingAction(
                        7L, 12L, 33L, MedicalAiActionProposalStore.START_ALERT_PROCESSING,
                        LocalDateTime.now().plusMinutes(5)
                )
        ));
        when(alertRecordMapper.selectById(33L)).thenReturn(alert);
        when(elderlyInfoMapper.selectElderlyListByMedicalUserId(7L)).thenReturn(List.of());

        assertThrows(BusinessException.class, () -> service().confirm(7L, "proposal-2"));
        verify(alertService, never()).processAlert(33L);
    }

    private MedicalAiActionService service() {
        return new MedicalAiActionService(alertRecordMapper, elderlyInfoMapper, alertService, proposalStore);
    }

    private AlertRecord alert(Long id, Long elderlyId, String status) {
        AlertRecord alert = new AlertRecord();
        alert.setId(id);
        alert.setElderlyId(elderlyId);
        alert.setStatus(status);
        return alert;
    }

    private ElderlyInfo elderly(Long id) {
        ElderlyInfo elderly = new ElderlyInfo();
        elderly.setId(id);
        return elderly;
    }
}
