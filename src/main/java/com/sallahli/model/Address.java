package com.sallahli.model;

import com.sallahli.utils.HasTimestamps;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "address")
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true, exclude = { "clients" })
public class Address extends HasTimestamps implements Archivable {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "address_id_seq")
    @SequenceGenerator(name = "address_id_seq", sequenceName = "address_id_seq", allocationSize = 1)
    private Long id;

    private String name;

    @Column(precision = 10, scale = 7)
    private Double latitude;

    @Column(precision = 10, scale = 7)
    private Double longitude;

    @Column(length = 100)
    private String country;

    @Column(name = "postal_code", length = 50)
    private String postalCode;

    @Column(length = 50)
    private String timezone;

    @Column(name = "formatted_address", columnDefinition = "TEXT")
    private String formattedAddress;

    @Column(length = 30)
    private String tel;

    @Column(name = "custom_locality")
    private String customLocality;

    @Column(columnDefinition = "TEXT")
    private String description;

    private String title;
    private String route;

    @Column(name = "building_number")
    private Integer buildingNumber;

    @Column(name = "building_name")
    private String buildingName;

    @Builder.Default
    private Boolean archived = false;

    @ManyToMany(mappedBy = "addresses")
    @Builder.Default
    private List<Client> clients = new ArrayList<>();
}
