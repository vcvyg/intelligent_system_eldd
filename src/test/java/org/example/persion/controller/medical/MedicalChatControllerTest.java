package org.example.persion.controller.medical;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.example.persion.common.Result;
import org.example.persion.repository.ChatMessageMapper;
import org.example.persion.repository.ElderlyFamilyRelationMapper;
import org.example.persion.repository.ElderlyInfoMapper;
import org.example.persion.repository.ElderlyMedicalRelationMapper;
import org.example.persion.repository.UserMapper;
import org.example.persion.vo.MessageVO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MedicalChatControllerTest {

    @Mock
    private ElderlyInfoMapper elderlyInfoMapper;

    @Mock
    private ChatMessageMapper chatMessageMapper;

    @Mock
    private ElderlyFamilyRelationMapper familyRelationMapper;

    @Mock
    private ElderlyMedicalRelationMapper medicalRelationMapper;

    @Mock
    private UserMapper userMapper;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void rejectsMessagesForGroupsOutsideCurrentMedicalAssignments() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(41L, null, List.of())
        );
        when(elderlyInfoMapper.selectElderlyListByMedicalUserId(41L)).thenReturn(List.of());

        MedicalChatController controller = new MedicalChatController(
                elderlyInfoMapper,
                chatMessageMapper,
                familyRelationMapper,
                medicalRelationMapper,
                userMapper,
                messagingTemplate,
                redisTemplate
        );

        Result<Page<MessageVO>> result = controller.getGroupMessages(77L, 1, 20);

        assertEquals(403, result.getCode());
        assertEquals("无权访问该聊天群组", result.getMessage());
    }
}
