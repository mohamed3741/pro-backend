package com.sallahli.service;

import com.sallahli.dto.payment.MasriviPaymentResponse;
import com.sallahli.dto.payment.MasriviRequestDTO;
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
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Slf4j
public class MasriviService {

    @Value("${masrivi.merchant-id:TEST_MERCHANT}")
    private String merchantId;

    @Value("${masrivi.session-url:http://api.masrivi.com/session}")
    private String sessionUrl;

    private final PaymentRepository paymentRepository;
    private final WalletService walletService;
    private final OnlineTransactionRepository onlineTransactionRepository;
    private final PaymentMapper paymentMapper;

    public MasriviService(PaymentRepository paymentRepository,
                         WalletService walletService,
                         OnlineTransactionRepository onlineTransactionRepository,
                         PaymentMapper paymentMapper) {
        this.paymentRepository = paymentRepository;
        this.walletService = walletService;
        this.onlineTransactionRepository = onlineTransactionRepository;
        this.paymentMapper = paymentMapper;
    }

    @Transactional
    public MasriviPaymentResponse createMasriviPayment(MasriviRequestDTO request) {
        String url = sessionUrl + "/" + merchantId;
        log.info("Calling Masrivi session URL: {}", url);

        try {
            // Simulate API call - in real implementation, make actual HTTP request
            String sessionId = UUID.randomUUID().toString();
            log.info("Masrivi session created with ID: {}", sessionId);

            OnlineTransaction onlineTransaction = mapToOnlineTransaction(request);
            onlineTransaction = onlineTransactionRepository.save(onlineTransaction);

            Payment payment = new Payment();
            payment.setOnlineTransaction(onlineTransaction);
            payment.setPaymentMethodType(PaymentMethodType.MASRIVI);
            payment.setStatus(TransactionStatus.INCOMPLETE);
            payment.setAmount(request.getAmount());
            payment.setTotal(request.getAmount());
            payment.setPaymentPurpose(request.getPaymentPurpose());

        // Note: Client wallet payments are not supported - only Pro wallets

            payment = paymentRepository.save(payment);

            return createMasriviPaymentResponse(payment, sessionId, onlineTransaction);

        } catch (Exception e) {
            log.error("Session initiation error: {}", e.getMessage());
            throw new RuntimeException("Session initiation error: " + e.getMessage());
        }
    }

    private MasriviPaymentResponse createMasriviPaymentResponse(Payment payment, String sessionId, OnlineTransaction onlineTransaction) {
        MasriviPaymentResponse response = new MasriviPaymentResponse();
        response.setId(payment.getId());
        response.setStatus(payment.getStatus());
        response.setAmount(payment.getAmount());
        response.setSessionId(sessionId);
        response.setPurchaseRef(onlineTransaction.getOperationId());
        response.setSessionUrl(sessionUrl);
        response.setMerchantId(merchantId);
        return response;
    }

    private OnlineTransaction mapToOnlineTransaction(MasriviRequestDTO request) {
        OnlineTransaction onlineTransaction = new OnlineTransaction();
        onlineTransaction.setAmount(request.getAmount());
        onlineTransaction.setClientPhone(request.getPhoneNumber());
        onlineTransaction.setOperationId(UUID.randomUUID().toString());
        onlineTransaction.setBankType(BankType.Masrivi);
        return onlineTransaction;
    }

    @Transactional
    public ResponseEntity<?> processMasriviNotification(String status, String purchaseRef, String paymentRef, String payid, String cname, String error) {
        try {
            OnlineTransaction onlineTransaction = onlineTransactionRepository.findByOperationId(purchaseRef);
            if (onlineTransaction == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Online transaction not found");
            }

            Payment payment = paymentRepository.findByOnlineTransactionId(onlineTransaction.getId());
            if (payment == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Payment not found");
            }

            if ("OK".equals(status)) {
                onlineTransaction.setStatus("CONFIRMED");
                onlineTransaction.setPaymentRef(paymentRef);
                onlineTransaction.setTransactionId(payid);
                onlineTransaction.setCustomerName(cname);
                onlineTransaction.setErrorCode("0");
                payment.setStatus(TransactionStatus.SUCCEEDED);
                payment = paymentRepository.save(payment);

                // Handle payment success based on purpose
                switch (payment.getPaymentPurpose()) {
                    case RECHARGE_WALLET -> walletService.handleWalletRecharge(payment.getId(), com.sallahli.model.Enum.WalletStatus.CONFIRMED);
                }

                return ResponseEntity.ok("CONFIRMED");
            } else {
                onlineTransaction.setStatus("CANCELED");
                onlineTransaction.setTransactionId(payid);
                onlineTransaction.setErrorCode("1");
                onlineTransaction.setErrorMessage(error);
                payment.setStatus(TransactionStatus.FAILED);
                payment = paymentRepository.save(payment);

                // Handle payment failure
                switch (payment.getPaymentPurpose()) {
                    case RECHARGE_WALLET -> walletService.handleWalletRecharge(payment.getId(), com.sallahli.model.Enum.WalletStatus.CANCELED);
                }

                return ResponseEntity.ok("CANCELED");
            }

        } catch (Exception e) {
            log.error("Payment confirmation failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Payment confirmation failed: " + e.getMessage());
        }
    }

    public String generateHtmlResponse(String title, String message, String status) {
        return """
                <!DOCTYPE html>
                <html lang="en">
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <title>Masrivi Payment Status</title>
                    <style>
                        body { font-family: Arial, sans-serif; display: flex; justify-content: center; align-items: center; height: 100vh; margin: 0; background-color: #f4f4f4; }
                        .container { text-align: center; padding: 20px; border-radius: 10px; box-shadow: 0 0 10px rgba(0,0,0,0.1); background-color: #fff; max-width: 400px; margin: auto; }
                        .container h1 { color: #333; }
                        .container p { color: #666; }
                        .success { color: green; }
                        .error { color: red; }
                        .warning { color: orange; }
                        .btn { display: inline-block; margin-top: 20px; padding: 10px 20px; font-size: 16px; color: #fff; background-color: #007bff; border: none; border-radius: 5px; text-decoration: none; cursor: pointer; }
                        .btn:hover { background-color: #0056b3; }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <h1 class="%s">%s</h1>
                        <p>%s</p>
                        <a href="/" class="btn">Return to Home</a>
                    </div>
                </body>
                </html>
                """.formatted(status, title, message);
    }
}
