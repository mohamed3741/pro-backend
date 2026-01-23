package com.sallahli.model;

import com.sallahli.utils.HasTimestamps;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "client")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class Client extends HasTimestamps implements Archivable {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "client_id_seq")
    @SequenceGenerator(name = "client_id_seq", sequenceName = "client_id_seq", allocationSize = 1)
    private Long id;

    @Column(nullable = false)
    private String tel;

    private String username;
    private String email;

    private String firstName;
    private String lastName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "logo_id")
    private Media logo;

    private Long userId;
    private String customerId;

    @Builder.Default
    private Boolean archived = false;

    @Builder.Default
    private Integer wallet = 0;

    @Builder.Default
    private Boolean ccOnly = false;

    @Builder.Default
    private Boolean isActive = true;

    @Builder.Default
    private Boolean isTelVerified = false;

    private String nationality;
    private String nationalityCode;

    private LocalDate birthDate;

    @Builder.Default
    private Boolean guest = false;

    private String gender;

    @Builder.Default
    private Boolean adsAccepted = false;

    private Long referralCode;
    private Long referredBy;

    @Builder.Default
    private Long referralCounter = 0L;

    private String loginProvider;
}

