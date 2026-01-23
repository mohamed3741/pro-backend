package com.sallahli.dto.payment;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.time.LocalDate;

@Data
@ToString(onlyExplicitlyIncluded = true)
@EqualsAndHashCode(callSuper = true)
public class SedadRequestDTO extends PaymentRequestDto {
    private double montant;
    private String nomPayeur;
    private String prenomPayeur;
    private String telephonePayeur;
    private LocalDate date;
    private String codeAbonnement;
    private String idFacture;
}
