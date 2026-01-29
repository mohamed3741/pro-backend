package com.sallahli.service;

import com.sallahli.dto.PaymentDTO;
import com.sallahli.dto.payment.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.stereotype.Service;

@Service("clientPaymentService")
@Data
@AllArgsConstructor
public class PaymentService {

    private final SedadService sedadService;
    private final BankilyService bankilyService;
    private final MasriviService masriviService;

    public PaymentDTO createPayment(PaymentRequestDto request) {
        switch (request.getPaymentMethodType()) {
            case SEDAD -> {
                return sedadService.createSedadPayment((SedadRequestDTO) request);
            }
            case BANKILY -> {
                return bankilyService.createBankilyPayment((BankilyRequestDTO) request);
            }
            case MASRIVI -> {
                return masriviService.createMasriviPayment((MasriviRequestDTO) request);
            }
            // Note: Client wallet payments are not supported - only Pro wallets
            default -> {
                throw new IllegalArgumentException("Payment method not supported yet: " + request.getPaymentMethodType());
            }
        }
    }


}
