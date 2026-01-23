package com.sallahli.dto.chat;

import com.sallahli.model.Enum.ConversationType;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConversationDto {

    private Long id;
    private ConversationType type;
    private boolean closed;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<ConversationParticipantDto> participants;
    private List<MessageDto> messages;
    private MessageDto lastMessage;
    private Long unreadCount;
}
