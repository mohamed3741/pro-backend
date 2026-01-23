package com.sallahli.dto.payment;

import lombok.Data;
import lombok.ToString;

import java.time.LocalDate;

@Data
@ToString(onlyExplicitlyIncluded = true)
public class PaymentConfirmationDTO {
    private String idFacture;
    private String idTransaction;
    private LocalDate datePaiement;
    private Integer montant;
    private String telephoneCommercant;
    private String numeroRecu;
}
