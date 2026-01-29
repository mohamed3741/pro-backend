package com.sallahli.service;

import com.sallahli.dto.PaymentDTO;
import com.sallahli.dto.payment.BankilyRequestDTO;
import com.sallahli.mapper.PaymentMapper;
import com.sallahli.model.Enum.BankType;
import com.sallahli.model.OnlineTransaction;
import com.sallahli.model.Payment;
import com.sallahli.model.Enum.PaymentMethodType;
import com.sallahli.model.Enum.TransactionStatus;
import com.sallahli.repository.OnlineTransactionRepository;
import com.sallahli.repository.PaymentRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
public class BankilyService {

    @Value("${bankily.username:test_user}")
    private String username;

    @Value("${bankily.password:test_pass}")
    private String password;

    @Value("${bankily.auth-url:http://api.bankily.com/auth}")
    private String authUrl;

    @Value("${bankily.payment-url:http://api.bankily.com/payment}")
    private String paymentUrl;

    @Value("${bankily.check-transaction-url:http://api.bankily.com/check}")
    private String checkTransactionUrl;

    private final OnlineTransactionRepository onlineTransactionRepository;
    private final PaymentRepository paymentRepository;
    private final PaymentMapper paymentMapper;

    public BankilyService(OnlineTransactionRepository onlineTransactionRepository,
                         PaymentRepository paymentRepository,
                         PaymentMapper paymentMapper) {
        this.onlineTransactionRepository = onlineTransactionRepository;
        this.paymentRepository = paymentRepository;
        this.paymentMapper = paymentMapper;
    }

    @Transactional
    public PaymentDTO createBankilyPayment(BankilyRequestDTO request) {
        String accessToken = authenticateAndGetToken();

        Payment payment = new Payment();
        payment.setPaymentMethodType(PaymentMethodType.BANKILY);
        payment.setAmount(request.getAmount());
        payment.setTotal(request.getAmount());
        payment.setPaymentPurpose(request.getPaymentPurpose());

        // Note: Client wallet payments are not supported - only Pro wallets

        Map<String, Object> paymentRequest = new HashMap<>();
        String operationId = UUID.randomUUID().toString();
        paymentRequest.put("clientPhone", request.getClientPhone());
        paymentRequest.put("passcode", request.getPassCode());
        paymentRequest.put("operationId", operationId);
        paymentRequest.put("amount", payment.getAmount());
        paymentRequest.put("language", "FR");

        try {
            // Simulate API call - in real implementation, make actual HTTP request
            log.info("Bankily payment request: {}", paymentRequest);

            OnlineTransaction ot = new OnlineTransaction();
            ot.setAmount(request.getAmount());
            ot.setClientPhone(request.getClientPhone());
            ot.setOperationId(operationId);
            ot.setBankType(BankType.Bankily);
            ot.setCustomerName(request.getFirstName() + " " + request.getLastName());
            ot.setTransactionId("SIMULATED_" + UUID.randomUUID().toString().substring(0, 8));
            ot.setErrorMessage("Simulated transaction");
            ot.setErrorCode("0");

            String status = checkTransactionStatus(operationId, accessToken);
            if (status == null) {
                ot.setStatus("FAILED");
                ot = onlineTransactionRepository.save(ot);
                payment.setOnlineTransaction(ot);
                payment.setStatus(TransactionStatus.FAILED);
                return paymentMapper.toDto(paymentRepository.save(payment));
            }

            switch (status) {
                case "TS" -> {
                    ot.setStatus("CONFIRMED");
                    ot = onlineTransactionRepository.save(ot);
                    payment.setOnlineTransaction(ot);
                    payment.setStatus(TransactionStatus.SUCCEEDED);
                    return paymentMapper.toDto(paymentRepository.save(payment));
                }
                case "TA" -> {
                    ot.setStatus("SUSPENDED");
                    ot = onlineTransactionRepository.save(ot);
                    payment.setOnlineTransaction(ot);
                    payment.setStatus(TransactionStatus.IN_PROGRESS);
                    return paymentMapper.toDto(paymentRepository.save(payment));
                }
                default -> {
                    ot.setStatus("FAILED");
                    ot = onlineTransactionRepository.save(ot);
                    payment.setOnlineTransaction(ot);
                    payment.setStatus(TransactionStatus.FAILED);
                    return paymentMapper.toDto(paymentRepository.save(payment));
                }
            }
        } catch (Exception e) {
            log.error("Bankily payment error: {}", e.getMessage());
            throw new RuntimeException("Bankily payment failed: " + e.getMessage());
        }
    }

    private String checkTransactionStatus(String operationId, String accessToken) {
        try {
            // Simulate transaction status check
            log.info("Checking Bankily transaction status for operationId: {}", operationId);

            // For simulation, randomly return different statuses
            double random = Math.random();
            if (random < 0.7) {
                return "TS"; // Transaction Successful
            } else if (random < 0.9) {
                return "TA"; // Transaction Accepted (pending)
            } else {
                return "TF"; // Transaction Failed
            }

        } catch (Exception e) {
            log.error("Error checking transaction status: {}", e.getMessage());
            return null;
        }
    }

    private String authenticateAndGetToken() {
        try {
            // Simulate authentication
            log.info("Authenticating with Bankily API");
            return "SIMULATED_ACCESS_TOKEN_" + UUID.randomUUID().toString().substring(0, 8);

        } catch (Exception e) {
            log.error("Bankily authentication failed: {}", e.getMessage());
            throw new RuntimeException("Failed to authenticate with Bankily.");
        }
    }
}
