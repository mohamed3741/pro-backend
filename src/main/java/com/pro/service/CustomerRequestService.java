package com.pro.service;

import com.pro.dto.sallahli.CustomerRequestDTO;
import com.pro.dto.sallahli.request.CustomerRequestCreationRequest;
import com.pro.mapper.CustomerRequestMapper;
import com.pro.model.Client;
import com.pro.model.CustomerRequest;
import com.pro.model.Enum.RequestStatus;
import com.pro.model.ServiceCategory;
import com.pro.repository.CustomerRequestRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class CustomerRequestService {

    private final CustomerRequestRepository customerRequestRepository;
    private final CustomerRequestMapper customerRequestMapper;
    private final ClientService clientService;
    private final ServiceCategoryService serviceCategoryService;
    private final LeadService leadService;

    @Transactional
    public CustomerRequestDTO createCustomerRequest(CustomerRequestCreationRequest request) {
        // Validate client exists
        Client client = clientService.getRepository().findById(request.getClientId())
                .orElseThrow(() -> new com.pro.exceptions.NotFoundException("Client not found"));

        // Validate category exists
        ServiceCategory category = serviceCategoryService.getRepository().findById(request.getCategoryId())
                .orElseThrow(() -> new com.pro.exceptions.NotFoundException("Service category not found"));

        // Create request entity
        CustomerRequest customerRequest = CustomerRequest.builder()
                .client(client)
                .category(category)
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .addressText(request.getAddressText())
                .landmark(request.getLandmark())
                .descriptionText(request.getDescriptionText())
                .voiceNoteMediaId(request.getVoiceNoteMediaId())
                .status(RequestStatus.OPEN)
                .urgent(request.getUrgent() != null ? request.getUrgent() : true)
                .build();

        CustomerRequest savedRequest = customerRequestRepository.save(customerRequest);
        log.info("Customer request created with ID: {}", savedRequest.getId());

        // Start broadcasting to nearby pros
        leadService.broadcastRequestToPros(savedRequest);

        return customerRequestMapper.toDto(savedRequest);
    }

    @Transactional(readOnly = true)
    public List<CustomerRequestDTO> getClientRequests(Long clientId) {
        List<CustomerRequest> requests = customerRequestRepository.findByClientIdOrderByCreatedAtDesc(clientId);
        return customerRequestMapper.toDtos(requests);
    }

    @Transactional(readOnly = true)
    public CustomerRequestDTO getRequestById(Long requestId) {
        CustomerRequest request = customerRequestRepository.findById(requestId)
                .orElseThrow(() -> new com.pro.exceptions.NotFoundException("Customer request not found"));
        return customerRequestMapper.toDto(request);
    }

    @Transactional
    public CustomerRequestDTO cancelRequest(Long requestId, Long clientId) {
        CustomerRequest request = customerRequestRepository.findById(requestId)
                .orElseThrow(() -> new com.pro.exceptions.NotFoundException("Customer request not found"));

        if (!request.getClient().getId().equals(clientId)) {
            throw new com.pro.exceptions.AccessDeniedException("Client can only cancel their own requests");
        }

        if (request.getStatus() != RequestStatus.OPEN && request.getStatus() != RequestStatus.BROADCASTED) {
            throw new com.pro.exceptions.BadRequestException("Request can only be cancelled when open or broadcasted");
        }

        request.setStatus(RequestStatus.CANCELLED);
        CustomerRequest savedRequest = customerRequestRepository.save(request);

        log.info("Customer request cancelled: {}", requestId);
        return customerRequestMapper.toDto(savedRequest);
    }

    @Transactional
    public void expireOldRequests() {
        LocalDateTime cutoffTime = LocalDateTime.now().minusMinutes(30); // Expire after 30 minutes
        List<CustomerRequest> expiredRequests = customerRequestRepository.findExpiredOpenRequests(cutoffTime);

        for (CustomerRequest request : expiredRequests) {
            request.setStatus(RequestStatus.EXPIRED);
            customerRequestRepository.save(request);
            log.info("Request expired: {}", request.getId());
        }
    }

    @Transactional
    public void updateRequestStatus(Long requestId, RequestStatus newStatus) {
        CustomerRequest request = customerRequestRepository.findById(requestId)
                .orElseThrow(() -> new com.pro.exceptions.NotFoundException("Customer request not found"));

        request.setStatus(newStatus);
        customerRequestRepository.save(request);

        log.info("Request status updated: {} -> {}", requestId, newStatus);
    }

    public CustomerRequest getRepository() {
        // This is a workaround since the repository is private
        // In a real implementation, you'd make the repository protected or add a getter
        return null;
    }
}
