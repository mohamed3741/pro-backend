package com.sallahli.dto.chat;

import com.sallahli.model.Enum.ConversationType;
import lombok.*;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConversationByParticipantRequest {

    private List<ConversationType> types;
    private boolean closed;
}
