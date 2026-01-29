package com.sallahli.controller;

import com.sallahli.model.ProWalletTransaction;
import com.sallahli.service.ProWalletService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/pros/{proId}/wallet")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Pro Wallet Management", description = "APIs for managing professional wallet operations")
public class ProWalletController {

    private final ProWalletService proWalletService;

    // ========================================================================
    // Balance Operations
    // ========================================================================

    @GetMapping("/balance")
    @PreAuthorize("hasAnyRole('PRO', 'ADMIN')")
    @Operation(summary = "Get wallet balance", description = "Returns current wallet balance")
    public ResponseEntity<Map<String, Long>> getBalance(@PathVariable Long proId) {
        log.debug("REST request to get wallet balance for pro {}", proId);
        Long balance = proWalletService.getBalance(proId);
        Map<String, Long> response = new HashMap<>();
        response.put("balance", balance);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/check-balance")
    @PreAuthorize("hasAnyRole('PRO', 'ADMIN')")
    @Operation(summary = "Check sufficient balance", description = "Checks if pro has sufficient balance for an amount")
    public ResponseEntity<Map<String, Boolean>> hasSufficientBalance(
            @PathVariable Long proId,
            @Parameter(description = "Required amount") @RequestParam Long amount) {
        log.debug("REST request to check if pro {} has balance >= {}", proId, amount);
        boolean sufficient = proWalletService.hasSufficientBalance(proId, amount);
        Map<String, Boolean> response = new HashMap<>();
        response.put("sufficient", sufficient);
        return ResponseEntity.ok(response);
    }

    // ========================================================================
    // Transaction History
    // ========================================================================

    @GetMapping("/transactions")
    @PreAuthorize("hasAnyRole('PRO', 'ADMIN')")
    @Operation(summary = "Get transaction history", description = "Returns all wallet transactions for a pro")
    public ResponseEntity<List<ProWalletTransaction>> getTransactionHistory(@PathVariable Long proId) {
        log.debug("REST request to get wallet transactions for pro {}", proId);
        return ResponseEntity.ok(proWalletService.getTransactionHistory(proId));
    }

    @GetMapping("/transactions/range")
    @PreAuthorize("hasAnyRole('PRO', 'ADMIN')")
    @Operation(summary = "Get transactions in date range", description = "Returns wallet transactions within date range")
    public ResponseEntity<List<ProWalletTransaction>> getTransactionsInDateRange(
            @PathVariable Long proId,
            @Parameter(description = "Start date") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @Parameter(description = "End date") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        log.debug("REST request to get wallet transactions for pro {} between {} and {}", proId, startDate, endDate);
        return ResponseEntity.ok(proWalletService.getTransactionsInDateRange(proId, startDate, endDate));
    }

    @GetMapping("/summary")
    @PreAuthorize("hasAnyRole('PRO', 'ADMIN')")
    @Operation(summary = "Get wallet summary", description = "Returns total credits and debits")
    public ResponseEntity<Map<String, Long>> getWalletSummary(@PathVariable Long proId) {
        log.debug("REST request to get wallet summary for pro {}", proId);
        Map<String, Long> summary = new HashMap<>();
        summary.put("balance", proWalletService.getBalance(proId));
        summary.put("totalCredits", proWalletService.getTotalCredits(proId));
        summary.put("totalDebits", proWalletService.getTotalDebits(proId));
        return ResponseEntity.ok(summary);
    }

    // ========================================================================
    // Admin Wallet Operations
    // ========================================================================

    @PostMapping("/recharge")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Recharge wallet", description = "Adds funds to pro wallet from payment")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Wallet recharged successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid amount")
    })
    public ResponseEntity<ProWalletTransaction> rechargeWallet(
            @PathVariable Long proId,
            @Parameter(description = "Amount to add") @RequestParam Long amount,
            @Parameter(description = "Payment ID reference") @RequestParam(required = false) Long paymentId) {
        log.debug("REST request to recharge wallet for pro {} with amount {}", proId, amount);
        return ResponseEntity.ok(proWalletService.rechargeWallet(proId, amount, paymentId));
    }

    @PostMapping("/free-credits")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Add free credits", description = "Adds promotional credits to wallet")
    public ResponseEntity<ProWalletTransaction> addFreeCredits(
            @PathVariable Long proId,
            @Parameter(description = "Amount to add") @RequestParam Long amount,
            @Parameter(description = "Reason for free credits") @RequestParam(required = false) String reason) {
        log.debug("REST request to add free credits for pro {} with amount {}", proId, amount);
        return ResponseEntity.ok(proWalletService.addFreeCredits(proId, amount, reason));
    }

    @PostMapping("/refund")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Refund to wallet", description = "Refunds money to pro wallet")
    public ResponseEntity<ProWalletTransaction> refund(
            @PathVariable Long proId,
            @Parameter(description = "Amount to refund") @RequestParam Long amount,
            @Parameter(description = "Reason for refund") @RequestParam String reason,
            @Parameter(description = "Reference type") @RequestParam(required = false) String referenceType,
            @Parameter(description = "Reference ID") @RequestParam(required = false) Long referenceId) {
        log.debug("REST request to refund {} to pro {} wallet", amount, proId);
        return ResponseEntity.ok(proWalletService.refund(proId, amount, reason, referenceType, referenceId));
    }

    @PostMapping("/adjustment")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Admin adjustment", description = "Makes an admin adjustment (positive or negative) to wallet")
    public ResponseEntity<ProWalletTransaction> adjustment(
            @PathVariable Long proId,
            @Parameter(description = "Amount (positive or negative)") @RequestParam Long amount,
            @Parameter(description = "Reason for adjustment") @RequestParam String reason) {
        log.debug("REST request to adjust pro {} wallet by {}", proId, amount);
        return ResponseEntity.ok(proWalletService.adjustment(proId, amount, reason));
    }
}
