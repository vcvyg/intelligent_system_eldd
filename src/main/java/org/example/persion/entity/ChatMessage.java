package org.example.persion.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("chat_message")
public class ChatMessage extends BaseEntity {

    private Long groupId; // 关联的老人ID

    private Long senderId; // 发送者用户ID

    private String senderName; // 发送者姓名（冗余，方便查询）

    private String senderRole; // 发送者角色（冗余，方便查询）

    private String messageType; // 消息类型: text, image, audio

    private String content; // 消息内容或文件URL
    
    private String audioUrl; // 音频文件URL（当messageType为audio时使用）
    
    private String imageUrl; // 图片文件URL（当messageType为image时使用）
    
    private String fileName; // 文件名（当messageType为file时使用）
    
    private String fileUrl; // 文件URL（当messageType为file时使用）
    
    private Integer duration; // 音频时长（秒，当messageType为audio时使用）
}
