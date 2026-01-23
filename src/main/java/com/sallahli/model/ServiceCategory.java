package com.sallahli.model;

import com.sallahli.utils.HasTimestamps;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "service_category")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class ServiceCategory extends HasTimestamps implements Archivable {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "service_category_id_seq")
    @SequenceGenerator(name = "service_category_id_seq", sequenceName = "service_category_id_seq", allocationSize = 1)
    private Long id;

    @Column(nullable = false, unique = true)
    private String code; // PLUMBING, ELECTRICITY, HVAC

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "icon_media_id")
    private Media iconMedia;

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;

    @Builder.Default
    private Boolean archived = false;
}

