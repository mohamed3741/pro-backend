package com.pro.model;

import com.pro.model.Enum.NotificationType;
import com.pro.utils.HasTimestamps;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "notification")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class Notification extends HasTimestamps {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "notification_id_seq")
    @SequenceGenerator(name = "notification_id_seq", sequenceName = "notification_id_seq", allocationSize = 1)
    private Long id;

    private LocalDateTime readAt;

    @Column(columnDefinition = "TEXT")
    private String content;

    private String title;

    @Enumerated(EnumType.STRING)
    private NotificationType type;

    private Long businessId; // Related entity ID
    private String servedApp; // Which app should receive this
    private Long readBy; // User who read it
    private Long partnerId;

    @Builder.Default
    private Boolean read = false;

    private String channelName;
    private String channelToken;
}
