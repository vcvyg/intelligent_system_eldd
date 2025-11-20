package org.example.persion.controller.medical;

import lombok.Data;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;

@RestController
public class MedicalChatWsController {
    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    // 子女端发送消息时调用，推送给医护端
    @MessageMapping("/chat/sendToMedical")
    public void sendToMedical(ChatMsg msg) {
        msg.setTime(LocalDateTime.now().toString());
        msg.setMe(false); // 来自子女
        // 推送到医护端（可按userId分组，这里简单推送所有医护）
        messagingTemplate.convertAndSend("/topic/medical-chat-" + msg.getToUserId(), msg);
    }

    @Data
    public static class ChatMsg {
        private Long toUserId; // 医护userId
        private String content;
        private String time;
        private boolean me;
        private String fromName;
    }
}
