package com.sallahli.model;

import com.sallahli.model.Enum.UserRoleEnum;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@IdClass(ConversationParticipantId.class)
@ToString(onlyExplicitlyIncluded = true)
public class ConversationParticipant {

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conversation_id", nullable = false)
    private Conversation conversation;

    @Id
    private Long userId;

    @Id
    @Enumerated(EnumType.STRING)
    private UserRoleEnum role;
}
