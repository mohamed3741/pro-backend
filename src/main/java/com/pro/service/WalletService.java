package com.pro.service;

import com.pro.dto.sallahli.ProDTO;
import com.pro.mapper.ProMapper;
import com.pro.model.Enum.WalletStatus;
import com.pro.model.Enum.WalletTransactionType;
import com.pro.model.Pro;
import com.pro.model.ProWalletTransaction;
import com.pro.repository.ProRepository;
import com.pro.repository.ProWalletTransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class WalletService {

    private final ProRepository proRepository;
    private final ProWalletTransactionRepository walletTransactionRepository;
    private final ProMapper proMapper;

    @Transactional(readOnly = true)
    public Long getWalletBalance(Long proId) {
        Pro pro = proRepository.findById(proId)
                .orElseThrow(() -> new com.pro.exceptions.NotFoundException("Pro not found with id: " + proId));
        return pro.getWalletBalanceMru();
    }

    @Transactional(readOnly = true)
    public List<ProWalletTransaction> getWalletTransactions(Long proId) {
        return walletTransactionRepository.findTransactionsByProId(proId);
    }

    @Transactional
    public void handleWalletRecharge(Long paymentId, WalletStatus status) {
        // This would be called by payment services when recharge is confirmed
        // Implementation depends on how payments are linked to wallet transactions
        log.info("Handling wallet recharge for payment ID: {} with status: {}", paymentId, status);
    }

    @Transactional
    public ProWalletTransaction recordWalletTransaction(Long proId, WalletTransactionType type,
                                                       Long amountMru, String reason,
                                                       String referenceType, Long referenceId) {
        Pro pro = proRepository.findById(proId)
                .orElseThrow(() -> new com.pro.exceptions.NotFoundException("Pro not found with id: " + proId));

        // Calculate new balance
        long balanceChange = type == WalletTransactionType.CREDIT ? amountMru : -amountMru;
        long newBalance = pro.getWalletBalanceMru() + balanceChange;

        if (newBalance < 0) {
            throw new com.pro.exceptions.BadRequestException("Insufficient wallet balance");
        }

        // Update pro balance
        pro.setWalletBalanceMru(newBalance);
        proRepository.save(pro);

        // Create transaction record
        ProWalletTransaction transaction = ProWalletTransaction.builder()
                .pro(pro)
                .type(type)
                .amountMru(amountMru)
                .reason(reason)
                .referenceType(referenceType)
                .referenceId(referenceId)
                .balanceAfterMru(newBalance)
                .build();

        ProWalletTransaction savedTransaction = walletTransactionRepository.save(transaction);
        log.info("Wallet transaction recorded: proId={}, type={}, amount={}, newBalance={}",
                proId, type, amountMru, newBalance);

        return savedTransaction;
    }

    @Transactional
    public boolean deductLeadCost(Long proId, Long leadPriceMru, Long leadId) {
        Pro pro = proRepository.findById(proId)
                .orElseThrow(() -> new com.pro.exceptions.NotFoundException("Pro not found with id: " + proId));

        if (pro.getWalletBalanceMru() < leadPriceMru) {
            log.warn("Insufficient balance for pro {}: has {}, needs {}", proId, pro.getWalletBalanceMru(), leadPriceMru);
            return false;
        }

        return recordWalletTransaction(proId, WalletTransactionType.DEBIT, leadPriceMru,
                "LEAD_PURCHASE", "REQUEST", leadId) != null;
    }

    @Transactional
    public ProWalletTransaction creditFreeLeads(Long proId, Long amountMru, String reason) {
        return recordWalletTransaction(proId, WalletTransactionType.CREDIT, amountMru,
                reason, "SYSTEM", null);
    }

    @Transactional
    public Pro applyWalletToPayment(com.pro.model.Payment payment) {
        // This method would modify the payment to use wallet balance
        // Implementation depends on the payment structure
        // For now, return the pro associated with the payment
        if (payment.getProId() != null) {
            return proRepository.findById(payment.getProId()).orElse(null);
        }
        return null;
    }

    @Transactional(readOnly = true)
    public boolean hasLowBalance(Long proId) {
        Pro pro = proRepository.findById(proId)
                .orElseThrow(() -> new com.pro.exceptions.NotFoundException("Pro not found with id: " + proId));

        return pro.getWalletBalanceMru() <= pro.getLowBalanceThresholdMru();
    }

    @Transactional
    public void updateLowBalanceThreshold(Long proId, Long threshold) {
        Pro pro = proRepository.findById(proId)
                .orElseThrow(() -> new com.pro.exceptions.NotFoundException("Pro not found with id: " + proId));

        pro.setLowBalanceThresholdMru(threshold);
        proRepository.save(pro);
    }

    @Transactional(readOnly = true)
    public ProDTO getProWithWalletInfo(Long proId) {
        Pro pro = proRepository.findById(proId)
                .orElseThrow(() -> new com.pro.exceptions.NotFoundException("Pro not found with id: " + proId));

        ProDTO proDTO = proMapper.toDto(pro);
        // Add additional wallet info if needed in DTO
        return proDTO;
    }
}
