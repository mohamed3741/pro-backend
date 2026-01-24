package com.sallahli.service;

import com.sallahli.dto.sallahli.ProDTO;
import com.sallahli.dto.sallahli.request.ProOnboardingRequest;
import com.sallahli.exceptions.BadRequestException;
import com.sallahli.mapper.ProMapper;
import com.sallahli.model.Enum.KycStatus;
import com.sallahli.model.Pro;
import com.sallahli.model.ServiceCategory;
import com.sallahli.model.Zone;
import com.sallahli.repository.ProRepository;
import com.sallahli.service.crud.AbstractCrudService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class ProService extends AbstractCrudService<Pro, ProDTO> {

    private final ProRepository proRepository;
    private final ProMapper proMapper;
    private final ServiceCategoryService serviceCategoryService;
    private final ZoneService zoneService;

    public ProService(ProRepository repository, ProMapper mapper,
                      ServiceCategoryService serviceCategoryService, ZoneService zoneService) {
        super(repository, mapper);
        this.proRepository = repository;
        this.proMapper = mapper;
        this.serviceCategoryService = serviceCategoryService;
        this.zoneService = zoneService;
    }

    public ProDTO findByTel(String tel) {
        Pro pro = proRepository.findByTel(tel)
                .orElseThrow(() -> new com.sallahli.exceptions.NotFoundException("Pro not found with tel: " + tel));
        return proMapper.toDto(pro);
    }

    public List<ProDTO> getOnlinePros() {
        List<Pro> pros = proRepository.findByOnlineTrue();
        return proMapper.toDtos(pros);
    }

    public List<ProDTO> getAvailableProsByTrade(Long tradeId) {
        List<Pro> pros = proRepository.findAvailableProsByTrade(tradeId, 50L); // Minimum 50 MRU
        return proMapper.toDtos(pros);
    }

    @Transactional
    public ProDTO onboardPro(ProOnboardingRequest request) {
        // Check if pro already exists
        if (proRepository.findByTel(request.getTel()).isPresent()) {
            throw new com.sallahli.exceptions.ConflictAccountException("Pro already exists with tel: " + request.getTel());
        }

        // Validate trade exists
        ServiceCategory trade = serviceCategoryService.getRepository().findById(request.getTradeId())
                .orElseThrow(() -> new com.sallahli.exceptions.NotFoundException("Service category not found"));

        // Validate zone if provided
        Zone baseZone = null;
        if (request.getBaseZoneId() != null) {
            baseZone = zoneService.getRepository().findById(request.getBaseZoneId())
                    .orElseThrow(() -> new com.sallahli.exceptions.NotFoundException("Zone not found"));
        }

        // Create pro entity
        Pro pro = Pro.builder()
                .tel(request.getTel())
                .fullName(request.getFullName())
                .email(request.getEmail())
                .trade(trade)
                .baseZone(baseZone)
                .kycStatus(KycStatus.PENDING)
                .walletBalanceMru(0L)
                .lowBalanceThresholdMru(50L)
                .isActive(true)
                .isDeleted(false)
                .build();

        // Set KYC media if provided
        if (request.getCniFrontMediaId() != null) {
            // Assuming MediaService exists
            // pro.setCniFrontMedia(mediaService.findEntity(request.getCniFrontMediaId()));
        }
        if (request.getCniBackMediaId() != null) {
            // pro.setCniBackMedia(mediaService.findEntity(request.getCniBackMediaId()));
        }
        if (request.getSelfieMediaId() != null) {
            // pro.setSelfieMedia(mediaService.findEntity(request.getSelfieMediaId()));
        }

        Pro savedPro = proRepository.save(pro);
        log.info("Pro onboarded successfully with ID: {}", savedPro.getId());

        return proMapper.toDto(savedPro);
    }

    @Transactional
    public ProDTO approveProKyc(Long proId, Long approvedBy) {
        Pro pro = findEntity(proId);

        if (pro.getKycStatus() != KycStatus.PENDING) {
            throw new com.sallahli.exceptions.BadRequestException("Pro KYC is not in pending status");
        }

        pro.setKycStatus(KycStatus.APPROVED);
        pro.setApprovedAt(java.time.LocalDateTime.now());
        pro.setApprovedBy(approvedBy);

        Pro savedPro = proRepository.save(pro);
        log.info("Pro KYC approved for ID: {}", proId);

        return proMapper.toDto(savedPro);
    }

    @Transactional
    public ProDTO rejectProKyc(Long proId) {
        Pro pro = findEntity(proId);

        if (pro.getKycStatus() != KycStatus.PENDING) {
            throw new com.sallahli.exceptions.BadRequestException("Pro KYC is not in pending status");
        }

        pro.setKycStatus(KycStatus.REJECTED);

        Pro savedPro = proRepository.save(pro);
        log.info("Pro KYC rejected for ID: {}", proId);

        return proMapper.toDto(savedPro);
    }

    @Transactional
    public ProDTO updateOnlineStatus(Long proId, boolean online) {
        Pro pro = findEntity(proId);
        pro.setOnline(online);

        Pro savedPro = proRepository.save(pro);
        return proMapper.toDto(savedPro);
    }

    public Long countApprovedActivePros() {
        return proRepository.countApprovedActivePros();
    }

    public Double getAverageRating() {
        return proRepository.getAverageRating();
    }

    public ProDTO getConnectedPro() {
        String phoneNumber = getCurrentUserPhoneNumber();
        if (phoneNumber == null) {
            throw new BadRequestException("No authenticated user found");
        }
        return findByTel(phoneNumber);
    }

    public Pro getConnectedProEntity() {
        String phoneNumber = getCurrentUserPhoneNumber();
        if (phoneNumber == null) {
            throw new BadRequestException("No authenticated user found");
        }
        return proRepository.findByTel(phoneNumber)
                .orElseThrow(() -> new NotFoundException("Pro not found with phone: " + phoneNumber));
    }

    private String getCurrentUserPhoneNumber() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication instanceof JwtAuthenticationToken jwtToken) {
                String phoneNumber = jwtToken.getToken().getClaimAsString(KeycloakUtils.PHONE_NUMBER_ATTRIBUTE);
                if (phoneNumber != null && !phoneNumber.trim().isEmpty()) {
                    return phoneNumber.trim();
                }
            }
        } catch (Exception e) {
            log.warn("Failed to extract phone number from JWT token: {}", e.getMessage());
        }
        return null;
    }
}

