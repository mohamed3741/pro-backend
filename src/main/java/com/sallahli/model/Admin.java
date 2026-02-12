package com.sallahli.model;

import com.sallahli.model.Enum.AdminRole;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

@Entity
@Table(name = "admin")
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class Admin extends User implements Archivable {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "admin_id_seq")
    @SequenceGenerator(name = "admin_id_seq", sequenceName = "admin_id_seq", allocationSize = 1)
    private Long id;

    private String profilePhoto;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AdminRole role;

    private String department;

    @Builder.Default
    private Boolean isActive = true;

    @Builder.Default
    private Boolean archived = false;

    private LocalDateTime lastLoginAt;
}
