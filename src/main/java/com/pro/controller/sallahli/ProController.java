package com.pro.controller.sallahli;

import com.pro.dto.sallahli.ProDTO;
import com.pro.dto.sallahli.request.ProOnboardingRequest;
import com.pro.service.ProService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/sallahli/pros")
@RequiredArgsConstructor
@Tag(name = "Professional Management", description = "APIs for managing professionals (pros)")
public class ProController {

    private final ProService proService;

    @PostMapping("/onboard")
    @Operation(summary = "Onboard a new professional")
    public ResponseEntity<ProDTO> onboardPro(@Valid @RequestBody ProOnboardingRequest request) {
        ProDTO pro = proService.onboardPro(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(pro);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get professional by ID")
    public ResponseEntity<ProDTO> getPro(@PathVariable Long id) {
        ProDTO pro = proService.findById(id);
        return ResponseEntity.ok(pro);
    }

    @GetMapping("/by-tel/{tel}")
    @Operation(summary = "Get professional by phone number")
    public ResponseEntity<ProDTO> getProByTel(@PathVariable String tel) {
        ProDTO pro = proService.findByTel(tel);
        return ResponseEntity.ok(pro);
    }

    @GetMapping("/online")
    @Operation(summary = "Get all online professionals")
    public ResponseEntity<List<ProDTO>> getOnlinePros() {
        List<ProDTO> pros = proService.getOnlinePros();
        return ResponseEntity.ok(pros);
    }

    @PutMapping("/{id}/online-status")
    @Operation(summary = "Update professional online status")
    public ResponseEntity<ProDTO> updateOnlineStatus(@PathVariable Long id, @RequestParam boolean online) {
        ProDTO pro = proService.updateOnlineStatus(id, online);
        return ResponseEntity.ok(pro);
    }

    @PostMapping("/{id}/kyc/approve")
    @Operation(summary = "Approve professional KYC (Admin only)")
    public ResponseEntity<ProDTO> approveProKyc(@PathVariable Long id, @RequestParam Long approvedBy) {
        ProDTO pro = proService.approveProKyc(id, approvedBy);
        return ResponseEntity.ok(pro);
    }

    @PostMapping("/{id}/kyc/reject")
    @Operation(summary = "Reject professional KYC (Admin only)")
    public ResponseEntity<ProDTO> rejectProKyc(@PathVariable Long id) {
        ProDTO pro = proService.rejectProKyc(id);
        return ResponseEntity.ok(pro);
    }

    @GetMapping("/stats/approved-count")
    @Operation(summary = "Get count of approved active professionals")
    public ResponseEntity<Long> getApprovedActiveProsCount() {
        Long count = proService.countApprovedActivePros();
        return ResponseEntity.ok(count);
    }

    @GetMapping("/stats/average-rating")
    @Operation(summary = "Get average rating across all professionals")
    public ResponseEntity<Double> getAverageRating() {
        Double avgRating = proService.getAverageRating();
        return ResponseEntity.ok(avgRating);
    }
}
