package org.example.persion.controller.family;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.example.persion.common.Result;
import org.example.persion.repository.ChatMessageMapper;
import org.example.persion.repository.ElderlyFamilyRelationMapper;
import org.example.persion.repository.ElderlyInfoMapper;
import org.example.persion.repository.ElderlyMedicalRelationMapper;
import org.example.persion.vo.MessageVO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FamilyChatControllerTest {

    @Mock
    private ElderlyInfoMapper elderlyInfoMapper;

    @Mock
    private ElderlyFamilyRelationMapper familyRelationMapper;

    @Mock
    private ElderlyMedicalRelationMapper medicalRelationMapper;

    @Mock
    private ChatMessageMapper chatMessageMapper;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void rejectsMessagesForGroupsOutsideCurrentFamily() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(31L, null, List.of())
        );
        when(elderlyInfoMapper.selectElderlyListByFamilyUserId(31L)).thenReturn(List.of());

        FamilyChatController controller = new FamilyChatController(
                elderlyInfoMapper,
                familyRelationMapper,
                medicalRelationMapper,
                chatMessageMapper
        );

        Result<Page<MessageVO>> result = controller.getGroupMessages(99L, 1, 20);

        assertEquals(403, result.getCode());
        assertEquals("无权访问该聊天群组", result.getMessage());
    }
}
