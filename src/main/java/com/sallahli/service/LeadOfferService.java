package com.sallahli.service;

import com.sallahli.dto.sallahli.LeadOfferDTO;
import com.sallahli.exceptions.BadRequestException;
import com.sallahli.exceptions.NotFoundException;
import com.sallahli.mapper.LeadOfferMapper;
import com.sallahli.model.*;
import com.sallahli.model.Enum.LeadOfferStatus;
import com.sallahli.model.Enum.WorkflowType;
import com.sallahli.repository.*;
import com.sallahli.service.crud.AbstractCrudService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
public class LeadOfferService extends AbstractCrudService<LeadOffer, LeadOfferDTO> {

    private final LeadOfferRepository leadOfferRepository;
    private final LeadOfferMapper leadOfferMapper;
    private final ProRepository proRepository;
    private final CustomerRequestRepository customerRequestRepository;
    private final ProWalletService proWalletService;
    private final JobService jobService;

    public LeadOfferService(LeadOfferRepository leadOfferRepository,
            LeadOfferMapper leadOfferMapper,
            ProRepository proRepository,
            CustomerRequestRepository customerRequestRepository,
            ProWalletService proWalletService,
            @Lazy JobService jobService) {
        super(leadOfferRepository, leadOfferMapper);
        this.leadOfferRepository = leadOfferRepository;
        this.leadOfferMapper = leadOfferMapper;
        this.proRepository = proRepository;
        this.customerRequestRepository = customerRequestRepository;
        this.proWalletService = proWalletService;
        this.jobService = jobService;
    }

    // ========================================================================
    // Lead Offer Creation
    // ========================================================================

    /**
     * Create lead offers for a customer request.
     * Finds available pros matching the category and creates offers.
     */
    @Transactional
    public List<LeadOfferDTO> createLeadOffersForRequest(CustomerRequest request) {
        Category category = request.getCategory();
        Long leadCost = category.getLeadCost().longValue();
        WorkflowType workflowType = category.getWorkflowType();
        Integer matchLimit = category.getMatchLimit();

        // Find available pros with sufficient wallet balance
        List<Pro> availablePros = proRepository.findAvailableProsByTrade(category.getId(), leadCost);

        // Limit the number of offers based on category setting
        List<Pro> selectedPros = availablePros.stream()
                .limit(matchLimit)
                .toList();

        // Set offer expiration based on workflow type
        int expirationMinutes = workflowType == WorkflowType.FIRST_CLICK ? 2 : 15;
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(expirationMinutes);

        List<LeadOffer> offers = selectedPros.stream()
                .map(pro -> {
                    LeadOffer offer = LeadOffer.builder()
                            .request(request)
                            .pro(pro)
                            .price(leadCost)
                            .status(LeadOfferStatus.OFFERED)
                            .offeredAt(LocalDateTime.now())
                            .expiresAt(expiresAt)
                            .build();
                    return leadOfferRepository.save(offer);
                })
                .toList();

        log.info("Created {} lead offers for request {} with workflow type {}",
                offers.size(), request.getId(), workflowType);

        return getMapper().toDtos(offers);
    }

    /**
     * Create a single lead offer for a specific pro.
     */
    @Transactional
    public LeadOfferDTO createOffer(Long requestId, Long proId) {
        CustomerRequest request = customerRequestRepository.findById(requestId)
                .orElseThrow(() -> new NotFoundException("CustomerRequest not found with id: " + requestId));

        Pro pro = proRepository.findById(proId)
                .orElseThrow(() -> new NotFoundException("Pro not found with id: " + proId));

        // Check if offer already exists
        if (leadOfferRepository.findByRequestIdAndProId(requestId, proId).isPresent()) {
            throw new BadRequestException("Lead offer already exists for this request and pro");
        }

        Category category = request.getCategory();
        Long leadCost = category.getLeadCost().longValue();

        LeadOffer offer = LeadOffer.builder()
                .request(request)
                .pro(pro)
                .price(leadCost)
                .status(LeadOfferStatus.OFFERED)
                .offeredAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusMinutes(15))
                .build();

        LeadOffer saved = leadOfferRepository.save(offer);
        log.info("Created lead offer {} for request {} and pro {}", saved.getId(), requestId, proId);

        return getMapper().toDto(saved);
    }

    // ========================================================================
    // Lead Offer Actions
    // ========================================================================

    /**
     * Accept a lead offer.
     * This deducts the lead cost from pro's wallet and creates a job.
     * For FIRST_CLICK workflow, this also cancels other pending offers.
     */
    @Transactional
    public LeadOfferDTO acceptOffer(Long offerId) {
        LeadOffer offer = findEntityById(offerId);

        if (offer.getStatus() != LeadOfferStatus.OFFERED) {
            throw new BadRequestException("Offer is not in OFFERED status. Current: " + offer.getStatus());
        }

        if (offer.getExpiresAt() != null && offer.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("Offer has expired");
        }

        Pro pro = offer.getPro();
        Long leadCost = offer.getPrice();

        // Check wallet balance
        if (pro.getWalletBalance() < leadCost) {
            throw new BadRequestException("Insufficient wallet balance. Required: " + leadCost +
                    ", Available: " + pro.getWalletBalance());
        }

        // Deduct lead cost from wallet
        proWalletService.deductForLeadPurchase(pro.getId(), leadCost, offer.getRequest().getId());

        // Update offer status
        offer.setStatus(LeadOfferStatus.ACCEPTED);
        LeadOffer saved = leadOfferRepository.save(offer);

        // Create job
        jobService.createJobFromLeadOffer(saved);

        // Cancel other offers for this request (for both workflow types after
        // acceptance)
        cancelOtherOffersForRequest(offer.getRequest().getId(), offerId);

        log.info("Lead offer {} accepted by pro {}", offerId, pro.getId());

        return getMapper().toDto(saved);
    }

    /**
     * Reject/miss a lead offer.
     */
    @Transactional
    public LeadOfferDTO missOffer(Long offerId) {
        LeadOffer offer = findEntityById(offerId);

        if (offer.getStatus() != LeadOfferStatus.OFFERED) {
            throw new BadRequestException("Offer is not in OFFERED status. Current: " + offer.getStatus());
        }

        offer.setStatus(LeadOfferStatus.MISSED);
        LeadOffer saved = leadOfferRepository.save(offer);

        log.info("Lead offer {} missed by pro {}", offerId, offer.getPro().getId());

        return getMapper().toDto(saved);
    }

    /**
     * Cancel a specific offer.
     */
    @Transactional
    public LeadOfferDTO cancelOffer(Long offerId) {
        LeadOffer offer = findEntityById(offerId);

        if (offer.getStatus() != LeadOfferStatus.OFFERED) {
            throw new BadRequestException("Can only cancel offers in OFFERED status. Current: " + offer.getStatus());
        }

        offer.setStatus(LeadOfferStatus.CANCELLED);
        LeadOffer saved = leadOfferRepository.save(offer);

        log.info("Lead offer {} cancelled", offerId);

        return getMapper().toDto(saved);
    }

    /**
     * Cancel all pending offers for a request.
     */
    @Transactional
    public void cancelOffersForRequest(Long requestId) {
        List<LeadOffer> offers = leadOfferRepository.findByRequestId(requestId);

        for (LeadOffer offer : offers) {
            if (offer.getStatus() == LeadOfferStatus.OFFERED) {
                offer.setStatus(LeadOfferStatus.CANCELLED);
                leadOfferRepository.save(offer);
            }
        }

        log.info("Cancelled all pending offers for request {}", requestId);
    }

    /**
     * Cancel other offers for a request (except the accepted one).
     */
    private void cancelOtherOffersForRequest(Long requestId, Long exceptOfferId) {
        List<LeadOffer> offers = leadOfferRepository.findByRequestId(requestId);

        for (LeadOffer offer : offers) {
            if (!offer.getId().equals(exceptOfferId) && offer.getStatus() == LeadOfferStatus.OFFERED) {
                offer.setStatus(LeadOfferStatus.CANCELLED);
                leadOfferRepository.save(offer);
            }
        }
    }

    // ========================================================================
    // Query Methods
    // ========================================================================

    /**
     * Get offers for a request.
     */
    @Transactional(readOnly = true)
    public List<LeadOfferDTO> findByRequestId(Long requestId) {
        List<LeadOffer> offers = leadOfferRepository.findByRequestId(requestId);
        return getMapper().toDtos(offers);
    }

    /**
     * Get pending offers for a pro.
     */
    @Transactional(readOnly = true)
    public List<LeadOfferDTO> findPendingOffersByProId(Long proId) {
        List<LeadOffer> offers = leadOfferRepository.findByProIdAndStatus(proId, LeadOfferStatus.OFFERED);
        return getMapper().toDtos(offers);
    }

    /**
     * Get accepted offers for a pro.
     */
    @Transactional(readOnly = true)
    public List<LeadOfferDTO> findAcceptedOffersByProId(Long proId) {
        List<LeadOffer> offers = leadOfferRepository.findByProIdAndStatus(proId, LeadOfferStatus.ACCEPTED);
        return getMapper().toDtos(offers);
    }

    // ========================================================================
    // Expiration handling
    // ========================================================================

    /**
     * Expire old offers that have passed their expiration time.
     */
    @Transactional
    public int expireOldOffers() {
        List<LeadOffer> expired = leadOfferRepository.findExpiredOffers(LocalDateTime.now());

        for (LeadOffer offer : expired) {
            offer.setStatus(LeadOfferStatus.EXPIRED);
            leadOfferRepository.save(offer);
            log.info("Expired lead offer {}", offer.getId());
        }

        return expired.size();
    }

    // ========================================================================
    // Helper methods
    // ========================================================================

    private LeadOffer findEntityById(Long id) {
        return leadOfferRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("LeadOffer not found with id: " + id));
    }
}
