package com.pro.service;

import com.pro.dto.sallahli.request.UserDeviceRegistrationRequest;
import com.pro.model.Client;
import com.pro.model.Pro;
import com.pro.model.UserDevice;
import com.pro.repository.UserDeviceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserDeviceService {

    private final UserDeviceRepository userDeviceRepository;
    private final ClientService clientService;
    private final ProService proService;

    @Transactional
    public void registerDevice(UserDeviceRegistrationRequest request) {
        // Check if device already exists
        userDeviceRepository.findByToken(request.getToken()).ifPresent(existingDevice -> {
            userDeviceRepository.delete(existingDevice);
            log.info("Removed existing device registration for token: {}", request.getToken());
        });

        // Validate that only one of clientId or proId is provided
        if ((request.getClientId() != null && request.getProId() != null) ||
            (request.getClientId() == null && request.getProId() == null)) {
            throw new com.pro.exceptions.BadRequestException("Either clientId or proId must be provided, but not both");
        }

        // Validate the user exists
        if (request.getClientId() != null) {
            clientService.getRepository().findById(request.getClientId())
                    .orElseThrow(() -> new com.pro.exceptions.NotFoundException("Client not found"));
        } else if (request.getProId() != null) {
            proService.getRepository().findById(request.getProId())
                    .orElseThrow(() -> new com.pro.exceptions.NotFoundException("Pro not found"));
        }

        UserDevice device = UserDevice.builder()
                .token(request.getToken())
                .osType(request.getOsType())
                .lang(request.getLang())
                .profileType(request.getProfileType())
                .client(request.getClientId() != null ? Client.builder().id(request.getClientId()).build() : null)
                .pro(request.getProId() != null ? Pro.builder().id(request.getProId()).build() : null)
                .build();

        userDeviceRepository.save(device);
        log.info("Device registered for {}: {}", request.getProfileType(),
                request.getClientId() != null ? "client-" + request.getClientId() : "pro-" + request.getProId());
    }

    @Transactional
    public void unregisterDevice(String token) {
        UserDevice device = userDeviceRepository.findByToken(token)
                .orElseThrow(() -> new com.pro.exceptions.NotFoundException("Device not found with token: " + token));

        userDeviceRepository.delete(device);
        log.info("Device unregistered: {}", token);
    }
}
