package com.pro.service;

import com.pro.dto.sallahli.ProDTO;
import com.pro.dto.sallahli.request.ProOnboardingRequest;
import com.pro.mapper.ProMapper;
import com.pro.model.Enum.KycStatus;
import com.pro.model.Pro;
import com.pro.model.ServiceCategory;
import com.pro.model.Zone;
import com.pro.repository.ProRepository;
import com.pro.service.crud.AbstractCrudService;
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
                .orElseThrow(() -> new com.pro.exceptions.NotFoundException("Pro not found with tel: " + tel));
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
            throw new com.pro.exceptions.ConflictAccountException("Pro already exists with tel: " + request.getTel());
        }

        // Validate trade exists
        ServiceCategory trade = serviceCategoryService.getRepository().findById(request.getTradeId())
                .orElseThrow(() -> new com.pro.exceptions.NotFoundException("Service category not found"));

        // Validate zone if provided
        Zone baseZone = null;
        if (request.getBaseZoneId() != null) {
            baseZone = zoneService.getRepository().findById(request.getBaseZoneId())
                    .orElseThrow(() -> new com.pro.exceptions.NotFoundException("Zone not found"));
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
            throw new com.pro.exceptions.BadRequestException("Pro KYC is not in pending status");
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
            throw new com.pro.exceptions.BadRequestException("Pro KYC is not in pending status");
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
}
