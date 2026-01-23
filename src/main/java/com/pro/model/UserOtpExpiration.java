package com.pro.model;

import com.pro.model.Enum.OtpMethod;
import com.pro.utils.HasTimestamps;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_otp_expiration")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class UserOtpExpiration extends HasTimestamps {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "user_otp_expiration_id_seq")
    @SequenceGenerator(name = "user_otp_expiration_id_seq", sequenceName = "user_otp_expiration_id_seq", allocationSize = 1)
    private Long id;

    private String username; // Phone number or email

    @Enumerated(EnumType.STRING)
    private OtpMethod method; // SMS, WHATSAPP, EMAIL

    private String otp;

    private LocalDateTime expirationTime;
    private LocalDateTime nextResendTime;

    @Builder.Default
    private Boolean isUsed = false;
}
