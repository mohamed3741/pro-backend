package com.pro.controller.sallahli;

import com.pro.service.WalletService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/sallahli/wallet")
@RequiredArgsConstructor
@Tag(name = "Wallet Management", description = "APIs for managing professional wallets")
public class WalletController {

    private final WalletService walletService;

    @GetMapping("/pro/{proId}/balance")
    @Operation(summary = "Get wallet balance for a professional")
    public ResponseEntity<Long> getWalletBalance(@PathVariable Long proId) {
        Long balance = walletService.getWalletBalance(proId);
        return ResponseEntity.ok(balance);
    }

    @GetMapping("/pro/{proId}/low-balance-alert")
    @Operation(summary = "Check if professional has low wallet balance")
    public ResponseEntity<Boolean> hasLowBalance(@PathVariable Long proId) {
        boolean lowBalance = walletService.hasLowBalance(proId);
        return ResponseEntity.ok(lowBalance);
    }

    @PutMapping("/pro/{proId}/threshold")
    @Operation(summary = "Update low balance threshold for a professional")
    public ResponseEntity<Void> updateLowBalanceThreshold(@PathVariable Long proId, @RequestParam Long threshold) {
        walletService.updateLowBalanceThreshold(proId, threshold);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/pro/{proId}/credit")
    @Operation(summary = "Credit wallet with free leads (Admin only)")
    public ResponseEntity<Void> creditFreeLeads(@PathVariable Long proId,
                                               @RequestParam Long amountMru,
                                               @RequestParam String reason) {
        walletService.creditFreeLeads(proId, amountMru, reason);
        return ResponseEntity.ok().build();
    }
}
