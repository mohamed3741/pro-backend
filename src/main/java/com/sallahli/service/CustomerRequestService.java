package com.sallahli.service;

import com.sallahli.dto.sallahli.CustomerRequestDTO;
import com.sallahli.exceptions.BadRequestException;
import com.sallahli.exceptions.NotFoundException;
import com.sallahli.mapper.AddressMapper;
import com.sallahli.mapper.CustomerRequestMapper;
import com.sallahli.model.*;
import com.sallahli.model.Enum.RequestStatus;
import com.sallahli.model.Enum.WorkflowType;
import com.sallahli.repository.*;
import com.sallahli.service.crud.AbstractCrudService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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
    private final AddressRepository addressRepository;
    private final AddressMapper addressMapper;
    private final NotificationService notificationService;

    public CustomerRequestService(CustomerRequestRepository customerRequestRepository,
            CustomerRequestMapper customerRequestMapper,
            ClientRepository clientRepository,
            CategoryRepository categoryRepository,
            LeadOfferService leadOfferService,
            AddressRepository addressRepository,
            AddressMapper addressMapper,
            NotificationService notificationService) {
        super(customerRequestRepository, customerRequestMapper);
        this.customerRequestRepository = customerRequestRepository;
        this.customerRequestMapper = customerRequestMapper;
        this.clientRepository = clientRepository;
        this.categoryRepository = categoryRepository;
        this.leadOfferService = leadOfferService;
        this.addressRepository = addressRepository;
        this.addressMapper = addressMapper;
        this.notificationService = notificationService;
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

        if (saved.getClient() != null) {
            notificationService.sendRequestCreatedNotification(saved.getClient(), saved);
        }

        return getMapper().toDto(saved);
    }

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

    @Transactional(readOnly = true)
    public List<CustomerRequestDTO> findByClientId(Long clientId) {
        List<CustomerRequest> requests = customerRequestRepository.findByClientIdOrderByCreatedAtDesc(clientId);
        return getMapper().toDtos(requests);
    }

    @Transactional(readOnly = true)
    public List<CustomerRequestDTO> findByStatus(RequestStatus status) {
        List<CustomerRequest> requests = customerRequestRepository.findByStatus(status);
        return getMapper().toDtos(requests);
    }

    @Transactional(readOnly = true)
    public List<CustomerRequestDTO> findByCategoryIdAndStatus(Long categoryId, RequestStatus status) {
        List<CustomerRequest> requests = customerRequestRepository.findByCategoryIdAndStatus(categoryId, status);
        return getMapper().toDtos(requests);
    }

    @Transactional(readOnly = true)
    public List<CustomerRequestDTO> findActiveBroadcastedRequests() {
        List<CustomerRequest> requests = customerRequestRepository.findActiveBroadcastedRequests(LocalDateTime.now());
        return getMapper().toDtos(requests);
    }

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
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean isAdmin = authentication != null && authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")); // Adjust role name if needed ("ADMIN" or
                                                                       // "ROLE_ADMIN")

        if (isAdmin) {
            // ADMIN must provide client ID
            if (dto.getClient() == null || dto.getClient().getId() == null) {
                throw new BadRequestException("Admin must provide a client ID to create a request.");
            }
            Client client = clientRepository.findById(dto.getClient().getId())
                    .orElseThrow(() -> new NotFoundException("Client not found with id: " + dto.getClient().getId()));
            entity.setClient(client);
        } else {
            // CLIENT (or PRO acting as client? Assume CLIENT role for request creation)
            // Always resolve from token, ignore DTO client ID for security
            if (authentication != null && authentication.isAuthenticated()
                    && !authentication.getName().equals("anonymousUser")) {
                String username = authentication.getName();
                Client client = clientRepository.findByUsername(username);
                if (client == null) {
                    throw new NotFoundException("Authenticated client profile not found for user: " + username);
                }
                entity.setClient(client);
            } else {
                // Should not happen if endpoint is secured, but as a fallback/guard
                throw new BadRequestException("Cannot create request without authentication.");
            }
        }

        // Resolve category (required)
        if (dto.getCategory() != null && dto.getCategory().getId() != null) {
            Category category = categoryRepository.findById(dto.getCategory().getId())
                    .orElseThrow(
                            () -> new NotFoundException("Category not found with id: " + dto.getCategory().getId()));
            entity.setCategory(category);
        }

        // Resolve Address
        resolveAddress(entity, dto);
    }

    private void resolveAddress(CustomerRequest entity, CustomerRequestDTO dto) {
        if (dto.getAddress() != null && dto.getAddress().getId() != null) {
            // Case 1: Existing Address ID provided
            Address address = addressRepository.findById(dto.getAddress().getId())
                    .orElseThrow(() -> new NotFoundException("Address not found with id: " + dto.getAddress().getId()));
            entity.setAddress(address);

            // Override local coordinates with address coordinates if not explicitly set?
            // For now, let's assume the request coordinates take precedence if set,
            // otherwise use address
            if (entity.getLatitude() == null)
                entity.setLatitude(address.getLatitude());
            if (entity.getLongitude() == null)
                entity.setLongitude(address.getLongitude());
            if (entity.getAddressText() == null)
                entity.setAddressText(address.getFormattedAddress());

        } else if (dto.getLatitude() != null && dto.getLongitude() != null) {
            // Case 2: No Address ID, but coordinates provided -> Create new Address for
            // client

            Address newAddress = Address.builder()
                    .latitude(dto.getLatitude())
                    .longitude(dto.getLongitude())
                    .formattedAddress(dto.getAddressText())
                    .name("Request Address") // Default name, maybe refine later
                    // .description(dto.getLandmark()) // Maybe map landmark to description?
                    .build();

            // Link to client if exists
            if (entity.getClient() != null) {
                // We're adding this address to the client's address list
                // However, Client <-> Address is ManyToMany.
                // For simplicity here, we just save the address. Linking to client might
                // require helper in Client entity or manual relation save.
                // Let's just save the address first.
                // If we want to persist it as "Saved Address" for the client, we'd need to add
                // it to client.getAddresses().add(newAddress) and save client/address.
            }

            Address savedAddress = addressRepository.save(newAddress);
            entity.setAddress(savedAddress);
        }
    }

    private CustomerRequest findCRById(Long id) {
        return customerRequestRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("CustomerRequest not found with id: " + id));
    }
}
