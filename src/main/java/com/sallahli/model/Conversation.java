package com.sallahli.model;

import com.sallahli.model.Enum.ConversationType;
import com.sallahli.utils.HasTimestamps;
import jakarta.persistence.*;
import lombok.*;

import java.io.Serial;
import java.util.List;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@ToString(onlyExplicitlyIncluded = true)
public class Conversation extends HasTimestamps {

    @Serial
    private static final long serialVersionUID = 4611494663837523772L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private ConversationType type;

    @Column(name = "is_closed")
    private boolean closed = false;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "conversation", orphanRemoval = true, cascade = CascadeType.ALL)
    private List<ConversationParticipant> participants;

    @OneToMany(mappedBy = "conversation", orphanRemoval = true)
    private List<Message> messages;
}
