package com.pro.controller.sallahli;

import com.pro.dto.sallahli.CustomerRequestDTO;
import com.pro.dto.sallahli.request.CustomerRequestCreationRequest;
import com.pro.service.CustomerRequestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/sallahli/requests")
@RequiredArgsConstructor
@Tag(name = "Customer Requests", description = "APIs for managing customer service requests")
public class CustomerRequestController {

    private final CustomerRequestService customerRequestService;

    @PostMapping
    @Operation(summary = "Create a new customer request")
    public ResponseEntity<CustomerRequestDTO> createRequest(@Valid @RequestBody CustomerRequestCreationRequest request) {
        CustomerRequestDTO createdRequest = customerRequestService.createCustomerRequest(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdRequest);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get customer request by ID")
    public ResponseEntity<CustomerRequestDTO> getRequest(@PathVariable Long id) {
        CustomerRequestDTO request = customerRequestService.getRequestById(id);
        return ResponseEntity.ok(request);
    }

    @GetMapping("/client/{clientId}")
    @Operation(summary = "Get all requests for a client")
    public ResponseEntity<List<CustomerRequestDTO>> getClientRequests(@PathVariable Long clientId) {
        List<CustomerRequestDTO> requests = customerRequestService.getClientRequests(clientId);
        return ResponseEntity.ok(requests);
    }

    @PutMapping("/{id}/cancel")
    @Operation(summary = "Cancel a customer request")
    public ResponseEntity<CustomerRequestDTO> cancelRequest(@PathVariable Long id, @RequestParam Long clientId) {
        CustomerRequestDTO request = customerRequestService.cancelRequest(id, clientId);
        return ResponseEntity.ok(request);
    }

    @PostMapping("/expire-old")
    @Operation(summary = "Expire old open requests (Admin/System task)")
    public ResponseEntity<Void> expireOldRequests() {
        customerRequestService.expireOldRequests();
        return ResponseEntity.ok().build();
    }
}
