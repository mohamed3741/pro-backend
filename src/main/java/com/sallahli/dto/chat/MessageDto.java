package com.sallahli.dto.chat;

import com.sallahli.dto.MediaDTO;
import com.sallahli.model.Enum.UserRoleEnum;
import lombok.*;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MessageDto {

    private Long id;
    private Long conversationId;
    private Long senderId;
    private UserRoleEnum senderRole;
    private String content;
    private MediaDTO media;
    private boolean isRead;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Additional sender info
    private String senderFirstName;
    private String senderLastName;
    private String senderUsername;
    private String senderLogo;
}
