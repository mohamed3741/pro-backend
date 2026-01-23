package com.sallahli.model;

import com.sallahli.utils.HasTimestamps;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "zone")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class Zone extends HasTimestamps {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "zone_id_seq")
    @SequenceGenerator(name = "zone_id_seq", sequenceName = "zone_id_seq", allocationSize = 1)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    private String city;

    @Column(nullable = false)
    @Builder.Default
    private String country = "Mauritania";

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;
}

