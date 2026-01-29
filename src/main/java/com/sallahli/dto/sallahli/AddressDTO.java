package com.sallahli.dto.sallahli;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddressDTO {

    private Long id;
    private String name;
    private Double latitude;
    private Double longitude;
    private String country;
    private String postalCode;
    private String timezone;
    private String formattedAddress;
    private String tel;
    private String customLocality;
    private String description;
    private String title;
    private String route;
    private Integer buildingNumber;
    private String buildingName;
    private Boolean archived;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
