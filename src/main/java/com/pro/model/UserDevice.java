package com.pro.model;

import com.pro.model.Enum.OsType;
import com.pro.model.Enum.ProfileType;
import com.pro.utils.HasTimestamps;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "user_device")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class UserDevice extends HasTimestamps {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "user_device_id_seq")
    @SequenceGenerator(name = "user_device_id_seq", sequenceName = "user_device_id_seq", allocationSize = 1)
    private Long id;

    private String token; // Device token for push notifications

    @Enumerated(EnumType.STRING)
    private OsType osType; // IOS, ANDROID

    private String lang; // Language code

    @Enumerated(EnumType.STRING)
    private ProfileType profileType; // CLIENT, PRO, ADMIN

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id")
    private Client client;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pro_id")
    private Pro pro;
}
