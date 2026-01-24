package com.sallahli.model;

import com.sallahli.utils.HasTimestamps;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "address")
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class Address extends HasTimestamps {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "address_id_seq")
    @SequenceGenerator(name = "address_id_seq", sequenceName = "address_id_seq", allocationSize = 1)
    private Long id;

    private String name;

    @Column(precision = 10, scale = 7)
    private Double latitude;

    @Column(precision = 10, scale = 7)
    private Double longitude;

    private String country;
    private String postalCode;
    private String timezone;
    
    @Column(columnDefinition = "TEXT")
    private String formattedAddress;

    private String tel;
    private String customLocality;
    
    @Column(columnDefinition = "TEXT")
    private String description;

    private String title;
    private String route;
    private Integer buildingNumber;
    private String buildingName;

    @Builder.Default
    private Boolean archived = false;
}

