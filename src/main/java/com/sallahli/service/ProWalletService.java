package com.sallahli.service;

import com.sallahli.exceptions.BadRequestException;
import com.sallahli.exceptions.NotFoundException;
import com.sallahli.model.Enum.WalletTransactionType;
import com.sallahli.model.Pro;
import com.sallahli.model.ProWalletTransaction;
import com.sallahli.repository.ProRepository;
import com.sallahli.repository.ProWalletTransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Service for managing Pro wallet operations.
 * Handles credits, debits, refunds, and transaction history.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ProWalletService {

    private final ProRepository proRepository;
    private final ProWalletTransactionRepository walletTransactionRepository;

    // ========================================================================
    // Wallet Operations
    // ========================================================================

    /**
     * Credit (add money) to a pro's wallet.
     */
    @Transactional
    public ProWalletTransaction credit(Long proId, Long amount, String reason, String referenceType, Long referenceId) {
        if (amount <= 0) {
            throw new BadRequestException("Amount must be positive");
        }

        Pro pro = findPro(proId);
        Long newBalance = pro.getWalletBalance() + amount;
        pro.setWalletBalance(newBalance);
        proRepository.save(pro);

        ProWalletTransaction transaction = createTransaction(
                pro, WalletTransactionType.CREDIT, amount, reason, referenceType, referenceId, newBalance);

        log.info("Credited {} to pro {} wallet. New balance: {}", amount, proId, newBalance);

        return transaction;
    }

    /**
     * Recharge wallet from a payment.
     */
    @Transactional
    public ProWalletTransaction rechargeWallet(Long proId, Long amount, Long paymentId) {
        return credit(proId, amount, "RECHARGE", "PAYMENT", paymentId);
    }

    /**
     * Add free leads (promotional credits).
     */
    @Transactional
    public ProWalletTransaction addFreeCredits(Long proId, Long amount, String reason) {
        return credit(proId, amount, reason != null ? reason : "FREE_LEADS", "PROMOTION", null);
    }

    /**
     * Deduct (remove money) from a pro's wallet.
     */
    @Transactional
    public ProWalletTransaction debit(Long proId, Long amount, String reason, String referenceType, Long referenceId) {
        if (amount <= 0) {
            throw new BadRequestException("Amount must be positive");
        }

        Pro pro = findPro(proId);

        if (pro.getWalletBalance() < amount) {
            throw new BadRequestException("Insufficient wallet balance. Required: " + amount +
                    ", Available: " + pro.getWalletBalance());
        }

        Long newBalance = pro.getWalletBalance() - amount;
        pro.setWalletBalance(newBalance);
        proRepository.save(pro);

        ProWalletTransaction transaction = createTransaction(
                pro, WalletTransactionType.DEBIT, amount, reason, referenceType, referenceId, newBalance);

        log.info("Debited {} from pro {} wallet. New balance: {}", amount, proId, newBalance);

        // Check low balance threshold
        if (newBalance <= pro.getLowBalanceThreshold()) {
            log.warn("Pro {} wallet balance ({}) is at or below threshold ({})",
                    proId, newBalance, pro.getLowBalanceThreshold());
            // TODO: Send low balance notification
        }

        return transaction;
    }

    /**
     * Deduct for lead purchase.
     */
    @Transactional
    public ProWalletTransaction deductForLeadPurchase(Long proId, Long amount, Long requestId) {
        return debit(proId, amount, "LEAD_PURCHASE", "REQUEST", requestId);
    }

    /**
     * Refund money to a pro's wallet.
     */
    @Transactional
    public ProWalletTransaction refund(Long proId, Long amount, String reason, String referenceType, Long referenceId) {
        if (amount <= 0) {
            throw new BadRequestException("Amount must be positive");
        }

        Pro pro = findPro(proId);
        Long newBalance = pro.getWalletBalance() + amount;
        pro.setWalletBalance(newBalance);
        proRepository.save(pro);

        ProWalletTransaction transaction = createTransaction(
                pro, WalletTransactionType.REFUND, amount, reason, referenceType, referenceId, newBalance);

        log.info("Refunded {} to pro {} wallet. New balance: {}", amount, proId, newBalance);

        return transaction;
    }

    /**
     * Refund for a cancelled lead.
     */
    @Transactional
    public ProWalletTransaction refundForCancelledLead(Long proId, Long amount, Long requestId) {
        return refund(proId, amount, "LEAD_CANCELLED", "REQUEST", requestId);
    }

    /**
     * Admin adjustment (can be positive or negative).
     */
    @Transactional
    public ProWalletTransaction adjustment(Long proId, Long amount, String reason) {
        Pro pro = findPro(proId);
        Long newBalance = pro.getWalletBalance() + amount;

        if (newBalance < 0) {
            throw new BadRequestException("Adjustment would result in negative balance");
        }

        pro.setWalletBalance(newBalance);
        proRepository.save(pro);

        ProWalletTransaction transaction = createTransaction(
                pro, WalletTransactionType.ADJUSTMENT, Math.abs(amount), reason, "ADMIN", null, newBalance);

        log.info("Adjusted pro {} wallet by {}. New balance: {}", proId, amount, newBalance);

        return transaction;
    }

    // ========================================================================
    // Query Methods
    // ========================================================================

    /**
     * Get current wallet balance.
     */
    @Transactional(readOnly = true)
    public Long getBalance(Long proId) {
        Pro pro = findPro(proId);
        return pro.getWalletBalance();
    }

    /**
     * Check if pro has sufficient balance.
     */
    @Transactional(readOnly = true)
    public boolean hasSufficientBalance(Long proId, Long requiredAmount) {
        Pro pro = findPro(proId);
        return pro.getWalletBalance() >= requiredAmount;
    }

    /**
     * Get transaction history for a pro.
     */
    @Transactional(readOnly = true)
    public List<ProWalletTransaction> getTransactionHistory(Long proId) {
        return walletTransactionRepository.findByProIdOrderByCreatedAtDesc(proId);
    }

    /**
     * Get transactions in date range.
     */
    @Transactional(readOnly = true)
    public List<ProWalletTransaction> getTransactionsInDateRange(Long proId, LocalDateTime startDate,
            LocalDateTime endDate) {
        return walletTransactionRepository.findTransactionsByProIdAndDateRange(proId, startDate, endDate);
    }

    /**
     * Get total credits for a pro.
     */
    @Transactional(readOnly = true)
    public Long getTotalCredits(Long proId) {
        Long total = walletTransactionRepository.getTotalCreditsByProId(proId);
        return total != null ? total : 0L;
    }

    /**
     * Get total debits for a pro.
     */
    @Transactional(readOnly = true)
    public Long getTotalDebits(Long proId) {
        Long total = walletTransactionRepository.getTotalDebitsByProId(proId);
        return total != null ? total : 0L;
    }

    // ========================================================================
    // Helper methods
    // ========================================================================

    private Pro findPro(Long proId) {
        return proRepository.findById(proId)
                .orElseThrow(() -> new NotFoundException("Pro not found with id: " + proId));
    }

    private ProWalletTransaction createTransaction(Pro pro, WalletTransactionType type, Long amount,
            String reason, String referenceType, Long referenceId,
            Long balanceAfter) {
        ProWalletTransaction transaction = ProWalletTransaction.builder()
                .pro(pro)
                .type(type)
                .amount(amount)
                .reason(reason)
                .referenceType(referenceType)
                .referenceId(referenceId)
                .balanceAfter(balanceAfter)
                .build();

        return walletTransactionRepository.save(transaction);
    }
}
