package com.pro.model;

import com.pro.utils.HasTimestamps;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "notification_attribute")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class NotificationAttribute extends HasTimestamps {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "notification_attribute_id_seq")
    @SequenceGenerator(name = "notification_attribute_id_seq", sequenceName = "notification_attribute_id_seq", allocationSize = 1)
    private Long id;

    private String key;
    private String value;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "notification_id", nullable = false)
    private Notification notification;
}
