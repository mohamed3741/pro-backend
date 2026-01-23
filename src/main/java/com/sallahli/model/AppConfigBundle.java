package com.sallahli.model;

import com.sallahli.model.Enum.AppType;
import com.sallahli.model.Enum.PlatformType;
import com.sallahli.utils.HasTimestamps;
import jakarta.persistence.*;
import lombok.*;
import org.apache.commons.codec.digest.DigestUtils;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.io.Serializable;

@Entity
@Table(name = "app_config_bundle", indexes = {
    @Index(name = "idx_is_active", columnList = "is_active"),
    @Index(name = "idx_hash", columnList = "hash")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class AppConfigBundle extends HasTimestamps implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "app_config_id_seq")
    @SequenceGenerator(name = "app_config_id_seq", sequenceName = "app_config_id_seq", allocationSize = 1)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private AppType app;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private PlatformType platform;

    @Column(length = 2)
    private String country;

    @Column(name = "min_version")
    private String minVersion;

    @Column(name = "max_version")
    private String maxVersion;

    @Column(name = "is_active")
    @Builder.Default
    private boolean active = true;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "config_json", columnDefinition = "jsonb")
    private String configJson;

    @Column(name = "created_by")
    private String createdBy;

    private String hash;

    private String description;

    @PrePersist
    @PreUpdate
    public void calculateHash() {
        if (configJson != null) {
            this.hash = DigestUtils.sha256Hex(configJson);
        }
    }
}

