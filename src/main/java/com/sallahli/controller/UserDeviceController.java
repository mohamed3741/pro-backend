package com.sallahli.controller;

import com.sallahli.dto.sallahli.request.UserDeviceRegistrationRequest;
import com.sallahli.model.Enum.OsType;
import com.sallahli.model.Enum.ProfileType;
import com.sallahli.model.UserDevice;
import com.sallahli.service.UserDeviceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequestMapping("/devices")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "User Device Management", description = "APIs for managing push notification device registrations")
public class UserDeviceController {

    private final UserDeviceService userDeviceService;

    // ========================================================================
    // Device Registration
    // ========================================================================

    @PostMapping("/register")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Register device", description = "Register or update a device for push notifications")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Device registered successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request")
    })
    public ResponseEntity<UserDevice> registerDevice(@RequestBody UserDeviceRegistrationRequest request) {
        log.debug("REST request to register device: OS={}, Profile={}",
                request.getOsType(), request.getProfileType());

        UserDevice device = userDeviceService.registerDevice(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(device);
    }

    @DeleteMapping("/unregister")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Unregister device", description = "Remove a device from push notifications")
    public ResponseEntity<Void> unregisterDevice(
            @Parameter(description = "Device token") @RequestParam String token) {
        log.debug("REST request to unregister device");

        userDeviceService.unregisterDevice(token);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/language")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Update device language", description = "Update the language preference for a device")
    public ResponseEntity<UserDevice> updateDeviceLanguage(
            @Parameter(description = "Device token") @RequestParam String token,
            @Parameter(description = "Language code (e.g., 'en', 'ar', 'fr')") @RequestParam String lang) {
        log.debug("REST request to update device language to: {}", lang);

        UserDevice device = userDeviceService.updateDeviceLanguage(token, lang);
        return ResponseEntity.ok(device);
    }

    // ========================================================================
    // Device Queries (Admin)
    // ========================================================================

    @GetMapping("/client/{clientId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CLIENT')")
    @Operation(summary = "Get client devices", description = "Get all devices registered for a client")
    public ResponseEntity<List<UserDevice>> getClientDevices(@PathVariable Long clientId) {
        log.debug("REST request to get devices for client: {}", clientId);
        return ResponseEntity.ok(userDeviceService.getClientDevices(clientId));
    }

    @GetMapping("/pro/{proId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PRO')")
    @Operation(summary = "Get pro devices", description = "Get all devices registered for a pro")
    public ResponseEntity<List<UserDevice>> getProDevices(@PathVariable Long proId) {
        log.debug("REST request to get devices for pro: {}", proId);
        return ResponseEntity.ok(userDeviceService.getProDevices(proId));
    }

    @GetMapping("/by-os/{osType}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get devices by OS type", description = "Get all devices by operating system type")
    public ResponseEntity<List<UserDevice>> getDevicesByOsType(@PathVariable OsType osType) {
        log.debug("REST request to get devices by OS type: {}", osType);
        return ResponseEntity.ok(userDeviceService.getDevicesByOsType(osType));
    }

    @GetMapping("/by-profile/{profileType}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get devices by profile type", description = "Get all devices by profile type")
    public ResponseEntity<List<UserDevice>> getDevicesByProfileType(@PathVariable ProfileType profileType) {
        log.debug("REST request to get devices by profile type: {}", profileType);
        return ResponseEntity.ok(userDeviceService.getDevicesByProfileType(profileType));
    }

    @GetMapping("/check")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Check device registration", description = "Check if a device token is registered")
    public ResponseEntity<Boolean> isDeviceRegistered(
            @Parameter(description = "Device token") @RequestParam String token) {
        return ResponseEntity.ok(userDeviceService.isDeviceRegistered(token));
    }

    // ========================================================================
    // Device Cleanup (Admin)
    // ========================================================================

    @DeleteMapping("/client/{clientId}/all")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Remove all client devices", description = "Remove all devices for a client (Admin only)")
    public ResponseEntity<Void> removeAllClientDevices(@PathVariable Long clientId) {
        log.debug("REST request to remove all devices for client: {}", clientId);
        userDeviceService.removeAllClientDevices(clientId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/pro/{proId}/all")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Remove all pro devices", description = "Remove all devices for a pro (Admin only)")
    public ResponseEntity<Void> removeAllProDevices(@PathVariable Long proId) {
        log.debug("REST request to remove all devices for pro: {}", proId);
        userDeviceService.removeAllProDevices(proId);
        return ResponseEntity.noContent().build();
    }
}
