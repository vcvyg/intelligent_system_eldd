package org.example.persion.service.impl;

import org.example.persion.common.exception.BusinessException;
import org.example.persion.dto.AlertHandleDTO;
import org.example.persion.entity.AlertRecord;
import org.example.persion.repository.AlertRecordMapper;
import org.example.persion.repository.ElderlyInfoMapper;
import org.example.persion.repository.RoomMapper;
import org.example.persion.vo.AlertRecordVO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AlertServiceImplTest {

    @Mock
    private AlertRecordMapper alertRecordMapper;

    @Mock
    private ElderlyInfoMapper elderlyInfoMapper;

    @Mock
    private RoomMapper roomMapper;

    @InjectMocks
    private AlertServiceImpl alertService;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void handleAlertRejectsClosedTask() {
        AlertRecord alert = new AlertRecord();
        alert.setStatus("已处理");
        when(alertRecordMapper.selectById(8L)).thenReturn(alert);

        AlertHandleDTO dto = new AlertHandleDTO();
        dto.setAlertId(8L);
        dto.setHandleResult("已联系家属");

        assertThrows(BusinessException.class, () -> alertService.handleAlert(dto));
        verify(alertRecordMapper, never()).updateById(any(AlertRecord.class));
    }

    @Test
    void getMyAlertTasksUsesCurrentMedicalUser() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(23L, null, List.of())
        );
        List<AlertRecordVO> tasks = List.of(new AlertRecordVO());
        when(alertRecordMapper.selectTaskListForMedicalUser(23L)).thenReturn(tasks);

        assertSame(tasks, alertService.getMyAlertTasks());
    }
}
