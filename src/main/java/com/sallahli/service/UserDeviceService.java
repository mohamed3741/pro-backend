package com.sallahli.service;

import com.sallahli.dto.sallahli.request.UserDeviceRegistrationRequest;
import com.sallahli.exceptions.BadRequestException;
import com.sallahli.exceptions.NotFoundException;
import com.sallahli.model.Client;
import com.sallahli.model.Enum.OsType;
import com.sallahli.model.Enum.ProfileType;
import com.sallahli.model.Pro;
import com.sallahli.model.UserDevice;
import com.sallahli.repository.ClientRepository;
import com.sallahli.repository.ProRepository;
import com.sallahli.repository.UserDeviceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Service for managing user devices (push notification tokens).
 * Supports iOS, Android, and Web push notification registrations.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class UserDeviceService {

    private final UserDeviceRepository userDeviceRepository;
    private final ClientRepository clientRepository;
    private final ProRepository proRepository;

    // ========================================================================
    // Device Registration
    // ========================================================================

    /**
     * Register or update a device for push notifications.
     * If the token already exists, updates the existing device.
     * If not, creates a new device registration.
     */
    @Transactional
    public UserDevice registerDevice(UserDeviceRegistrationRequest request) {
        validateRegistrationRequest(request);

        // Check if device token already exists
        Optional<UserDevice> existingDevice = userDeviceRepository.findByToken(request.getToken());

        UserDevice device;
        if (existingDevice.isPresent()) {
            device = existingDevice.get();
            log.info("Updating existing device registration for token: {}...",
                    truncateToken(request.getToken()));
        } else {
            device = new UserDevice();
            device.setToken(request.getToken());
            log.info("Creating new device registration for token: {}...",
                    truncateToken(request.getToken()));
        }

        // Update device properties
        device.setOsType(request.getOsType());
        device.setLang(request.getLang() != null ? request.getLang() : "en");
        device.setProfileType(request.getProfileType());

        // Associate with client or pro
        if (request.getClientId() != null) {
            Client client = clientRepository.findById(request.getClientId())
                    .orElseThrow(() -> new NotFoundException("Client not found: " + request.getClientId()));
            device.setClient(client);
            device.setPro(null); // Clear pro association
        } else if (request.getProId() != null) {
            Pro pro = proRepository.findById(request.getProId())
                    .orElseThrow(() -> new NotFoundException("Pro not found: " + request.getProId()));
            device.setPro(pro);
            device.setClient(null); // Clear client association
        }

        UserDevice saved = userDeviceRepository.save(device);
        log.info("Device registered successfully: OS={}, Profile={}, User={}",
                device.getOsType(),
                device.getProfileType(),
                request.getClientId() != null ? "Client:" + request.getClientId() : "Pro:" + request.getProId());

        return saved;
    }

    /**
     * Unregister a device (remove push notification token).
     */
    @Transactional
    public void unregisterDevice(String token) {
        if (token == null || token.isBlank()) {
            throw new BadRequestException("Token is required");
        }

        Optional<UserDevice> device = userDeviceRepository.findByToken(token);
        if (device.isPresent()) {
            userDeviceRepository.delete(device.get());
            log.info("Device unregistered: {}...", truncateToken(token));
        } else {
            log.warn("Attempted to unregister non-existent device: {}...", truncateToken(token));
        }
    }

    /**
     * Update device language preference.
     */
    @Transactional
    public UserDevice updateDeviceLanguage(String token, String lang) {
        UserDevice device = userDeviceRepository.findByToken(token)
                .orElseThrow(() -> new NotFoundException("Device not found"));

        device.setLang(lang);
        return userDeviceRepository.save(device);
    }

    // ========================================================================
    // Device Queries
    // ========================================================================

    /**
     * Get all devices for a specific client.
     */
    @Transactional(readOnly = true)
    public List<UserDevice> getClientDevices(Long clientId) {
        return userDeviceRepository.findByClientId(clientId);
    }

    /**
     * Get all devices for a specific pro.
     */
    @Transactional(readOnly = true)
    public List<UserDevice> getProDevices(Long proId) {
        return userDeviceRepository.findByProId(proId);
    }

    /**
     * Get all devices by OS type.
     */
    @Transactional(readOnly = true)
    public List<UserDevice> getDevicesByOsType(OsType osType) {
        return userDeviceRepository.findByOsType(osType);
    }

    /**
     * Get all devices by profile type.
     */
    @Transactional(readOnly = true)
    public List<UserDevice> getDevicesByProfileType(ProfileType profileType) {
        return userDeviceRepository.findByProfileType(profileType);
    }

    /**
     * Get device by token.
     */
    @Transactional(readOnly = true)
    public Optional<UserDevice> getDeviceByToken(String token) {
        return userDeviceRepository.findByToken(token);
    }

    /**
     * Check if a device is registered.
     */
    @Transactional(readOnly = true)
    public boolean isDeviceRegistered(String token) {
        return userDeviceRepository.findByToken(token).isPresent();
    }

    // ========================================================================
    // Device Cleanup
    // ========================================================================

    /**
     * Remove all devices for a client (e.g., on logout or account deletion).
     */
    @Transactional
    public void removeAllClientDevices(Long clientId) {
        List<UserDevice> devices = userDeviceRepository.findByClientId(clientId);
        userDeviceRepository.deleteAll(devices);
        log.info("Removed {} devices for client {}", devices.size(), clientId);
    }

    /**
     * Remove all devices for a pro (e.g., on logout or account deletion).
     */
    @Transactional
    public void removeAllProDevices(Long proId) {
        List<UserDevice> devices = userDeviceRepository.findByProId(proId);
        userDeviceRepository.deleteAll(devices);
        log.info("Removed {} devices for pro {}", devices.size(), proId);
    }

    /**
     * Remove invalid/expired device tokens.
     * Called when push notification service reports invalid tokens.
     */
    @Transactional
    public void removeInvalidToken(String token) {
        userDeviceRepository.findByToken(token).ifPresent(device -> {
            userDeviceRepository.delete(device);
            log.info("Removed invalid device token: {}...", truncateToken(token));
        });
    }

    // ========================================================================
    // Helpers
    // ========================================================================

    private void validateRegistrationRequest(UserDeviceRegistrationRequest request) {
        if (request.getToken() == null || request.getToken().isBlank()) {
            throw new BadRequestException("Device token is required");
        }
        if (request.getOsType() == null) {
            throw new BadRequestException("OS type is required");
        }
        if (request.getProfileType() == null) {
            throw new BadRequestException("Profile type is required");
        }
        if (request.getClientId() == null && request.getProId() == null) {
            throw new BadRequestException("Either clientId or proId must be provided");
        }
        if (request.getClientId() != null && request.getProId() != null) {
            throw new BadRequestException("Only one of clientId or proId should be provided");
        }
    }

    private String truncateToken(String token) {
        if (token == null || token.length() < 10) {
            return token;
        }
        return token.substring(0, 10);
    }
}
