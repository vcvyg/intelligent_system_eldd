package org.example.persion.vo;

import lombok.Data;

@Data
public class MessageVO {
    private Long id;
    private Long groupId;
    private Long senderId;
    private String senderName;
    private String senderRole;
    private String content;
    private String messageType;
    private String time;
    private boolean me; // Is this message sent by the current user?
    private String audioUrl; // 音频文件URL
    private String imageUrl; // 图片文件URL
    private Integer duration; // 音频时长（秒）
}
