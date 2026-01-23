package com.pro.service;

import com.pro.dto.sallahli.LeadOfferDTO;
import com.pro.dto.sallahli.request.LeadAcceptanceRequest;
import com.pro.mapper.LeadOfferMapper;
import com.pro.model.*;
import com.pro.model.Enum.LeadOfferStatus;
import com.pro.model.Enum.RequestStatus;
import com.pro.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class LeadService {

    private final LeadOfferRepository leadOfferRepository;
    private final LeadAcceptanceRepository leadAcceptanceRepository;
    private final JobRepository jobRepository;
    private final CustomerRequestRepository customerRequestRepository;
    private final ProRepository proRepository;
    private final LeadOfferMapper leadOfferMapper;
    private final WalletService walletService;
    private final NotificationService notificationService;
    private final WebSocketService webSocketService;

    @Transactional
    public void broadcastRequestToPros(CustomerRequest request) {
        // Update request status
        request.setStatus(RequestStatus.BROADCASTED);
        request.setBroadcastedAt(LocalDateTime.now());
        request.setExpiresAt(LocalDateTime.now().plusMinutes(10)); // 10 minutes to respond
        customerRequestRepository.save(request);

        // Find nearby available pros
        List<Pro> nearbyPros = findNearbyPros(request);

        // Create lead offers for each pro
        for (Pro pro : nearbyPros) {
            LeadOffer offer = LeadOffer.builder()
                    .request(request)
                    .pro(pro)
                    .distanceKm(calculateDistance(request, pro))
                    .priceMru(50L) // Default price
                    .status(LeadOfferStatus.OFFERED)
                    .offeredAt(LocalDateTime.now())
                    .expiresAt(request.getExpiresAt())
                    .build();

            leadOfferRepository.save(offer);

            // Send push notification to pro
            notificationService.sendLeadOfferNotification(pro, offer);

            // Send real-time WebSocket notification
            webSocketService.sendJobUpdateToPro(pro.getId(), Map.of(
                    "type", "LEAD_OFFER",
                    "offer", offer,
                    "request", request
            ));
        }

        log.info("Broadcasted request {} to {} pros", request.getId(), nearbyPros.size());
    }

    @Transactional
    public LeadAcceptanceDTO acceptLeadOffer(LeadAcceptanceRequest acceptanceRequest, Long proId) {
        // Validate pro
        Pro pro = proRepository.findById(proId)
                .orElseThrow(() -> new com.pro.exceptions.NotFoundException("Pro not found"));

        // Find and validate lead offer
        LeadOffer leadOffer = leadOfferRepository.findById(acceptanceRequest.getLeadOfferId())
                .orElseThrow(() -> new com.pro.exceptions.NotFoundException("Lead offer not found"));

        if (!leadOffer.getPro().getId().equals(proId)) {
            throw new com.pro.exceptions.AccessDeniedException("Pro can only accept their own offers");
        }

        if (leadOffer.getStatus() != LeadOfferStatus.OFFERED) {
            throw new com.pro.exceptions.BadRequestException("Lead offer is not available for acceptance");
        }

        if (leadOffer.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new com.pro.exceptions.BadRequestException("Lead offer has expired");
        }

        // Check wallet balance
        if (!walletService.deductLeadCost(proId, leadOffer.getPriceMru(), leadOffer.getRequest().getId())) {
            throw new com.pro.exceptions.BadRequestException("Insufficient wallet balance");
        }

        // Update lead offer status
        leadOffer.setStatus(LeadOfferStatus.ACCEPTED);
        leadOfferRepository.save(leadOffer);

        // Create lead acceptance
        LeadAcceptance acceptance = LeadAcceptance.builder()
                .leadOffer(leadOffer)
                .request(leadOffer.getRequest())
                .pro(pro)
                .priceMru(leadOffer.getPriceMru())
                .acceptedAt(LocalDateTime.now())
                .build();

        LeadAcceptance savedAcceptance = leadAcceptanceRepository.save(acceptance);

        // Update request status
        CustomerRequest request = leadOffer.getRequest();
        request.setStatus(RequestStatus.ASSIGNED);
        customerRequestRepository.save(request);

        // Expire other offers for this request
        expireOtherOffersForRequest(request.getId(), leadOffer.getId());

        // Create job
        createJob(savedAcceptance);

        // Send notifications
        notificationService.sendLeadAcceptedNotification(pro, acceptance);
        notificationService.sendRequestAssignedNotification(request.getClient(), acceptance);

        log.info("Lead accepted: proId={}, requestId={}, price={}", proId, request.getId(), leadOffer.getPriceMru());

        return null; // Would return mapped DTO
    }

    @Transactional
    public void expireOldOffers() {
        List<LeadOffer> expiredOffers = leadOfferRepository.findExpiredOffers(LocalDateTime.now());

        for (LeadOffer offer : expiredOffers) {
            offer.setStatus(LeadOfferStatus.EXPIRED);
            leadOfferRepository.save(offer);

            // Notify pro that offer expired
            notificationService.sendLeadExpiredNotification(offer.getPro(), offer);
        }

        log.info("Expired {} old lead offers", expiredOffers.size());
    }

    @Transactional(readOnly = true)
    public List<LeadOfferDTO> getProLeadOffers(Long proId, LeadOfferStatus status) {
        List<LeadOffer> offers = leadOfferRepository.findByProIdAndStatus(proId, status);
        return leadOfferMapper.toDtos(offers);
    }

    private List<Pro> findNearbyPros(CustomerRequest request) {
        // Simplified: find all online pros in the same trade
        // In real implementation, you'd use geospatial queries
        List<Pro> availablePros = proRepository.findAvailableProsByTrade(request.getCategory().getId(), 50L);

        // Filter by distance (simplified - would use actual distance calculation)
        return availablePros.stream()
                .filter(pro -> isWithinDistance(request, pro, 3.0)) // 3km radius
                .limit(10) // Max 10 pros
                .collect(Collectors.toList());
    }

    private Double calculateDistance(CustomerRequest request, Pro pro) {
        // Simplified distance calculation
        // In real implementation, use proper geospatial distance calculation
        return 1.5; // Mock distance
    }

    private boolean isWithinDistance(CustomerRequest request, Pro pro, double maxDistanceKm) {
        return calculateDistance(request, pro) <= maxDistanceKm;
    }

    private void expireOtherOffersForRequest(Long requestId, Long acceptedOfferId) {
        List<LeadOffer> otherOffers = leadOfferRepository.findByRequestId(requestId).stream()
                .filter(offer -> !offer.getId().equals(acceptedOfferId))
                .filter(offer -> offer.getStatus() == LeadOfferStatus.OFFERED)
                .collect(Collectors.toList());

        for (LeadOffer offer : otherOffers) {
            offer.setStatus(LeadOfferStatus.MISSED);
            leadOfferRepository.save(offer);
        }
    }

    private void createJob(LeadAcceptance acceptance) {
        Job job = Job.builder()
                .request(acceptance.getRequest())
                .acceptance(acceptance)
                .pro(acceptance.getPro())
                .client(acceptance.getRequest().getClient())
                .status(com.pro.model.Enum.JobStatus.IN_PROGRESS)
                .startedAt(LocalDateTime.now())
                .build();

        jobRepository.save(job);
        log.info("Job created for acceptance: {}", acceptance.getId());
    }
}
