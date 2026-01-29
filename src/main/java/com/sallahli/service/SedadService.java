package com.sallahli.service;

import com.sallahli.dto.PaymentDTO;
import com.sallahli.dto.payment.PaymentConfirmationDTO;
import com.sallahli.dto.payment.SedadRequestDTO;
import com.sallahli.mapper.PaymentMapper;
import com.sallahli.model.Enum.*;
import com.sallahli.model.OnlineTransaction;
import com.sallahli.model.Payment;
import com.sallahli.repository.OnlineTransactionRepository;
import com.sallahli.repository.PaymentRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.*;

@Service
@Slf4j
public class SedadService {

    @Value("${gimtel.sedad.key:test_key}")
    private String sedadApiKey;

    @Value("${gimtel.sedad.url:http://api.sedad.com}")
    private String sedadApiUrl;

    @Value("${gimtel.sedad.code-abonnement:TEST_CODE}")
    private String sedadApiCode;

    @Value("${gimtel.sedad.marsadrive-key:test_drive_key}")
    private String marsaDriveKey;

    private final OnlineTransactionRepository onlineTransactionRepository;
    private final PaymentRepository paymentRepository;
    private final PaymentMapper paymentMapper;

    public SedadService(OnlineTransactionRepository onlineTransactionRepository,
                       PaymentRepository paymentRepository,
                       PaymentMapper paymentMapper) {
        this.onlineTransactionRepository = onlineTransactionRepository;
        this.paymentRepository = paymentRepository;
        this.paymentMapper = paymentMapper;
    }

    @Transactional
    public PaymentDTO createSedadPayment(SedadRequestDTO request) {
        if (!paymentRequestFieldsPresent(request)) {
            throw new com.sallahli.exceptions.BadRequestException("Missing required fields in the payload.");
        }

        Payment payment = new Payment();
        payment.setPaymentMethodType(PaymentMethodType.SEDAD);
        payment.setStatus(TransactionStatus.INCOMPLETE);
        payment.setAmount(request.getAmount());
        payment.setTotal(request.getAmount());
        payment.setPaymentPurpose(request.getPaymentPurpose());

        // Note: Client wallet payments are not supported - only Pro wallets

        String fullUrl = sedadApiUrl + "/demande_paiement";
        log.info("Calling Sedad API: {}", fullUrl);

        try {
            // Simulate API call - in real implementation, make actual HTTP request
            Map<String, Object> simulatedResponse = new HashMap<>();
            simulatedResponse.put("data", Map.of("code_paiement", "SIM_" + generateIdFacture()));

            String codePaiement = "SIM_" + generateIdFacture();

            OnlineTransaction onlineTransaction = mapToFactureSedad(request);
            onlineTransaction.setPaymentCode(codePaiement);
            onlineTransaction = onlineTransactionRepository.save(onlineTransaction);

            payment.setOnlineTransaction(onlineTransaction);
            payment.setPaymentRef(codePaiement);
            payment = paymentRepository.save(payment);

            return paymentMapper.toDto(payment);

        } catch (Exception e) {
            log.error("Sedad payment creation error: {}", e.getMessage());
            throw new RuntimeException("Sedad payment failed: " + e.getMessage());
        }
    }

    private boolean paymentRequestFieldsPresent(SedadRequestDTO dto) {
        return dto.getMontant() > 0 &&
               StringUtils.hasText(dto.getNomPayeur()) &&
               StringUtils.hasText(dto.getPrenomPayeur()) &&
               StringUtils.hasText(dto.getTelephonePayeur()) &&
               dto.getDate() != null;
    }

    private OnlineTransaction mapToFactureSedad(SedadRequestDTO sedadRequestDTO) {
        OnlineTransaction onlineTransaction = new OnlineTransaction();
        onlineTransaction.setOperationId(generateIdFacture());
        onlineTransaction.setCustomerName(sedadRequestDTO.getNomPayeur() + " " + sedadRequestDTO.getPrenomPayeur());
        onlineTransaction.setAmount(sedadRequestDTO.getAmount());
        onlineTransaction.setClientPhone(sedadRequestDTO.getTelephonePayeur());
        onlineTransaction.setBankType(BankType.Sedad);
        return onlineTransaction;
    }

    @Transactional
    public ResponseEntity<?> confirmSedadPayment(PaymentConfirmationDTO dto, String apiKey) {
        String expectedApiKey = "Api-Key " + marsaDriveKey;

        if (!expectedApiKey.equals(apiKey.trim())) {
            log.warn("Invalid API key.");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Invalid API key.");
        }

        if (!areAllRequiredFieldsPresent(dto)) {
            log.warn("Missing required fields in the payload.");
            return ResponseEntity.badRequest().body("Missing required fields in the payload.");
        }

        try {
            OnlineTransaction onlineTransaction = onlineTransactionRepository.findByOperationId(dto.getIdFacture());
            if (onlineTransaction == null) {
                log.warn("Online transaction with idFacture={} not found", dto.getIdFacture());
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Online transaction not found");
            }

            updateSedadOnlineTransaction(onlineTransaction, dto);
            onlineTransactionRepository.save(onlineTransaction);

            Payment payment = paymentRepository.findByOnlineTransactionId(onlineTransaction.getId());
            if (payment == null) {
                log.warn("Payment not found for transaction {}", onlineTransaction.getId());
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Payment not found");
            }

            if (!TransactionStatus.INCOMPLETE.equals(payment.getStatus())) {
                payment.setStatus(TransactionStatus.FAILED);
                paymentRepository.save(payment);
                log.warn("Payment expected status INCOMPLETE, received {}", payment.getStatus());
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid payment status");
            }

            Map<String, Object> responseBody = new HashMap<>();
            responseBody.put("code", HttpStatus.OK.value());
            responseBody.put("Message", "OK");
            responseBody.put("numero_recu", dto.getNumeroRecu());

            payment.setStatus(TransactionStatus.SUCCEEDED);
            payment.setPaymentRef(dto.getNumeroRecu());
            paymentRepository.save(payment);

            

            return ResponseEntity.ok(responseBody);

        } catch (Exception e) {
            log.error("Payment confirmation failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Payment confirmation failed: " + e.getMessage());
        }
    }

    private boolean areAllRequiredFieldsPresent(PaymentConfirmationDTO dto) {
        return StringUtils.hasText(dto.getIdFacture()) &&
               StringUtils.hasText(dto.getIdTransaction()) &&
               dto.getDatePaiement() != null &&
               dto.getMontant() > 0 &&
               StringUtils.hasText(dto.getTelephoneCommercant()) &&
               StringUtils.hasText(dto.getNumeroRecu());
    }

    private void updateSedadOnlineTransaction(OnlineTransaction onlineTransaction, PaymentConfirmationDTO dto) {
        onlineTransaction.setTransactionId(dto.getIdTransaction());
        onlineTransaction.setPaymentRef(dto.getNumeroRecu());
        onlineTransaction.setStatus("CONFIRMED");
    }

    private String generateIdFacture() {
        Random random = new Random();
        int number = random.nextInt(10_000_000);
        return "f" + String.format("%07d", number);
    }
}
