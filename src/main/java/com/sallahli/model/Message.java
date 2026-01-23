package com.sallahli.model;

import com.sallahli.model.Enum.UserRoleEnum;
import com.sallahli.utils.HasTimestamps;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.io.Serial;

@Entity
@Table
@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@ToString(onlyExplicitlyIncluded = true)
public class Message extends HasTimestamps {

    @Serial
    private static final long serialVersionUID = -7006724478861651760L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conversation_id", nullable = false)
    private Conversation conversation;

    private Long senderId;

    @Enumerated(EnumType.STRING)
    private UserRoleEnum senderRole; // driver, client, support

    private String content;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "media_id")
    private Media media;

    private boolean isRead = false;
}
