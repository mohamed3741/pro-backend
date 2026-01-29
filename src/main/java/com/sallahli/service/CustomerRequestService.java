package com.sallahli.service;

import com.sallahli.dto.sallahli.CustomerRequestDTO;
import com.sallahli.exceptions.BadRequestException;
import com.sallahli.exceptions.NotFoundException;
import com.sallahli.mapper.CustomerRequestMapper;
import com.sallahli.model.*;
import com.sallahli.model.Enum.RequestStatus;
import com.sallahli.model.Enum.WorkflowType;
import com.sallahli.repository.*;
import com.sallahli.service.crud.AbstractCrudService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
public class CustomerRequestService extends AbstractCrudService<CustomerRequest, CustomerRequestDTO> {

    private final CustomerRequestRepository customerRequestRepository;
    private final CustomerRequestMapper customerRequestMapper;
    private final ClientRepository clientRepository;
    private final CategoryRepository categoryRepository;
    private final LeadOfferService leadOfferService;

    public CustomerRequestService(CustomerRequestRepository customerRequestRepository,
            CustomerRequestMapper customerRequestMapper,
            ClientRepository clientRepository,
            CategoryRepository categoryRepository,
            LeadOfferService leadOfferService) {
        super(customerRequestRepository, customerRequestMapper);
        this.customerRequestRepository = customerRequestRepository;
        this.customerRequestMapper = customerRequestMapper;
        this.clientRepository = clientRepository;
        this.categoryRepository = categoryRepository;
        this.leadOfferService = leadOfferService;
    }

    // ========================================================================
    // Core CRUD overrides
    // ========================================================================

    @Override
    @Transactional(readOnly = true)
    public List<CustomerRequestDTO> findAll() {
        return getMapper().toDtos(customerRequestRepository.findAll());
    }

    @Override
    @Transactional(readOnly = true)
    public CustomerRequestDTO findById(Long id) {
        CustomerRequest request = customerRequestRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("CustomerRequest not found with id: " + id));
        return getMapper().toDto(request);
    }

    // ========================================================================
    // Request Creation & Lifecycle
    // ========================================================================

    /**
     * Create a new customer request.
     * This will set up the request with OPEN status and link to client/category.
     */
    @Override
    @Transactional
    public CustomerRequestDTO create(CustomerRequestDTO dto) {
        CustomerRequest request = getMapper().toModel(dto);

        // Apply relationships
        applyRelationships(request, dto);

        // Set defaults
        request.setStatus(RequestStatus.OPEN);
        request.setArchived(false);

        CustomerRequest saved = customerRequestRepository.save(request);
        log.info("Created customer request {} for client {}", saved.getId(),
                saved.getClient() != null ? saved.getClient().getId() : "anonymous");

        return getMapper().toDto(saved);
    }

    /**
     * Broadcast a request to nearby available pros.
     * The workflow type (LEAD_OFFER or FIRST_CLICK) is determined by the category.
     */
    @Transactional
    public CustomerRequestDTO broadcastRequest(Long requestId) {
        CustomerRequest request = findCRById(requestId);

        if (request.getStatus() != RequestStatus.OPEN) {
            throw new BadRequestException(
                    "Request must be in OPEN status to broadcast. Current: " + request.getStatus());
        }

        // Get category to determine workflow type
        Category category = request.getCategory();
        WorkflowType workflowType = category.getWorkflowType();

        request.setStatus(RequestStatus.BROADCASTED);
        request.setBroadcastedAt(LocalDateTime.now());

        // Set expiration based on workflow type
        // FIRST_CLICK has shorter expiration (faster response expected)
        int expirationMinutes = workflowType == WorkflowType.FIRST_CLICK ? 5 : 30;
        request.setExpiresAt(LocalDateTime.now().plusMinutes(expirationMinutes));

        CustomerRequest saved = customerRequestRepository.save(request);

        // Trigger lead offers to available pros
        leadOfferService.createLeadOffersForRequest(saved);

        log.info("Broadcasted request {} with workflow type {}", requestId, workflowType);

        return getMapper().toDto(saved);
    }

    /**
     * Cancel a customer request.
     */
    @Transactional
    public CustomerRequestDTO cancelRequest(Long requestId, String reason) {
        CustomerRequest request = findCRById(requestId);

        if (request.getStatus() == RequestStatus.DONE || request.getStatus() == RequestStatus.CANCELLED) {
            throw new BadRequestException("Cannot cancel request with status: " + request.getStatus());
        }

        request.setStatus(RequestStatus.CANCELLED);
        CustomerRequest saved = customerRequestRepository.save(request);

        // Cancel any pending lead offers
        leadOfferService.cancelOffersForRequest(requestId);

        log.info("Cancelled request {} with reason: {}", requestId, reason);

        return getMapper().toDto(saved);
    }

    /**
     * Mark request as assigned when a pro accepts the lead.
     */
    @Transactional
    public CustomerRequestDTO assignRequest(Long requestId) {
        CustomerRequest request = findCRById(requestId);

        if (request.getStatus() != RequestStatus.BROADCASTED) {
            throw new BadRequestException(
                    "Request must be in BROADCASTED status to assign. Current: " + request.getStatus());
        }

        request.setStatus(RequestStatus.ASSIGNED);
        CustomerRequest saved = customerRequestRepository.save(request);

        log.info("Assigned request {}", requestId);

        return getMapper().toDto(saved);
    }

    /**
     * Mark request as done when job is completed.
     */
    @Transactional
    public CustomerRequestDTO completeRequest(Long requestId) {
        CustomerRequest request = findCRById(requestId);

        request.setStatus(RequestStatus.DONE);
        CustomerRequest saved = customerRequestRepository.save(request);

        log.info("Completed request {}", requestId);

        return getMapper().toDto(saved);
    }

    // ========================================================================
    // Query Methods
    // ========================================================================

    /**
     * Get requests by client.
     */
    @Transactional(readOnly = true)
    public List<CustomerRequestDTO> findByClientId(Long clientId) {
        List<CustomerRequest> requests = customerRequestRepository.findByClientIdOrderByCreatedAtDesc(clientId);
        return getMapper().toDtos(requests);
    }

    /**
     * Get requests by status.
     */
    @Transactional(readOnly = true)
    public List<CustomerRequestDTO> findByStatus(RequestStatus status) {
        List<CustomerRequest> requests = customerRequestRepository.findByStatus(status);
        return getMapper().toDtos(requests);
    }

    /**
     * Get requests by category and status.
     */
    @Transactional(readOnly = true)
    public List<CustomerRequestDTO> findByCategoryIdAndStatus(Long categoryId, RequestStatus status) {
        List<CustomerRequest> requests = customerRequestRepository.findByCategoryIdAndStatus(categoryId, status);
        return getMapper().toDtos(requests);
    }

    /**
     * Get active (broadcasted and not expired) requests.
     */
    @Transactional(readOnly = true)
    public List<CustomerRequestDTO> findActiveBroadcastedRequests() {
        List<CustomerRequest> requests = customerRequestRepository.findActiveBroadcastedRequests(LocalDateTime.now());
        return getMapper().toDtos(requests);
    }

    /**
     * Find requests within a geographic bounding box.
     */
    @Transactional(readOnly = true)
    public List<CustomerRequestDTO> findRequestsInBoundingBox(Double minLat, Double maxLat, Double minLng,
            Double maxLng) {
        List<CustomerRequest> requests = customerRequestRepository.findRequestsInBoundingBox(minLat, maxLat, minLng,
                maxLng);
        return getMapper().toDtos(requests);
    }

    // ========================================================================
    // Expiration handling
    // ========================================================================

    /**
     * Find and expire old open requests.
     */
    @Transactional
    public int expireOldRequests(int hoursOld) {
        LocalDateTime cutoff = LocalDateTime.now().minusHours(hoursOld);
        List<CustomerRequest> expired = customerRequestRepository.findExpiredOpenRequests(cutoff);

        for (CustomerRequest request : expired) {
            request.setStatus(RequestStatus.EXPIRED);
            customerRequestRepository.save(request);
            log.info("Expired request {}", request.getId());
        }

        return expired.size();
    }

    // ========================================================================
    // Helper methods
    // ========================================================================

    @Override
    protected void applyRelationships(CustomerRequest entity, CustomerRequestDTO dto) {
        if (dto == null)
            return;

        // Resolve client
        if (dto.getClient() != null && dto.getClient().getId() != null) {
            Client client = clientRepository.findById(dto.getClient().getId())
                    .orElseThrow(() -> new NotFoundException("Client not found with id: " + dto.getClient().getId()));
            entity.setClient(client);
        }

        // Resolve category (required)
        if (dto.getCategory() != null && dto.getCategory().getId() != null) {
            Category category = categoryRepository.findById(dto.getCategory().getId())
                    .orElseThrow(
                            () -> new NotFoundException("Category not found with id: " + dto.getCategory().getId()));
            entity.setCategory(category);
        }
    }

    private CustomerRequest findCRById(Long id) {
        return customerRequestRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("CustomerRequest not found with id: " + id));
    }
}
