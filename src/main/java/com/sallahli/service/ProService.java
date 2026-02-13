package com.sallahli.service;

import com.sallahli.dto.sallahli.ProDTO;
import com.sallahli.exceptions.BadRequestException;
import com.sallahli.exceptions.NotFoundException;
import com.sallahli.mapper.ProMapper;
import com.sallahli.model.Admin;
import com.sallahli.model.Category;
import com.sallahli.model.Enum.KycStatus;
import com.sallahli.model.Media;
import com.sallahli.model.Pro;
import com.sallahli.model.Zone;
import com.sallahli.repository.AdminRepository;
import com.sallahli.repository.CategoryRepository;
import com.sallahli.repository.MediaRepository;
import com.sallahli.repository.ProRepository;
import com.sallahli.repository.ZoneRepository;
import com.sallahli.service.crud.AbstractCrudService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sallahli.dto.sallahli.CategoryDTO;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ProService extends AbstractCrudService<Pro, ProDTO> {

    private final ProRepository proRepository;
    private final CategoryRepository categoryRepository;
    private final ZoneRepository zoneRepository;
    private final MediaRepository mediaRepository;
    private final AdminRepository adminRepository;

    public ProService(ProRepository proRepository,
            ProMapper proMapper,
            CategoryRepository categoryRepository,
            ZoneRepository zoneRepository,
            MediaRepository mediaRepository,
            AdminRepository adminRepository) {
        super(proRepository, proMapper);
        this.proRepository = proRepository;
        this.categoryRepository = categoryRepository;
        this.zoneRepository = zoneRepository;
        this.mediaRepository = mediaRepository;
        this.adminRepository = adminRepository;
    }

    // ========================================================================
    // Core CRUD overrides
    // ========================================================================

    @Override
    @Transactional(readOnly = true)
    public List<ProDTO> findAll() {
        return getMapper().toDtos(proRepository.findAll());
    }

    @Override
    @Transactional(readOnly = true)
    public ProDTO findById(Long id) {
        Pro pro = proRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Pro not found with id: " + id));
        return getMapper().toDto(pro);
    }

    // ========================================================================
    // Pro Registration & Profile
    // ========================================================================

    @Transactional(readOnly = true)
    public ProDTO findByTel(String tel) {
        Pro pro = proRepository.findByTel(tel)
                .orElseThrow(() -> new NotFoundException("Pro not found with tel: " + tel));
        return getMapper().toDto(pro);
    }

    @Transactional(readOnly = true)
    public boolean existsByTel(String tel) {
        return proRepository.findByTel(tel).isPresent();
    }

    @Transactional
    public ProDTO initProFromToken(org.springframework.security.core.Authentication authentication) {
        String username = authentication.getName();
        org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken jwt = (org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken) authentication;
        java.util.Map<String, Object> attrs = jwt.getTokenAttributes();
        // attribute sub is available if needed: String sub =
        // java.util.Objects.toString(attrs.get("sub"));

        Pro pro = new Pro();
        pro.setUsername(username);
        pro.setTel(java.util.Objects.toString(attrs.get("phone_number"), null));
        pro.setFirstName(java.util.Objects.toString(attrs.get("given_name"), null));
        pro.setLastName(java.util.Objects.toString(attrs.get("family_name"), null)); // Keycloak often uses family_name
        pro.setEmail(java.util.Objects.toString(attrs.get("email"), null));

        // administrative fields
        pro.setIsActive(false); // Default inactive until KYC
        pro.setArchived(false);
        pro.setOnline(false);
        pro.setKycStatus(KycStatus.PENDING);
        pro.setIsTelVerified(false);

        // defaults
        pro.setWalletBalance(0L);
        pro.setRatingAvg(5.0);
        pro.setRatingCount(0L);
        pro.setJobsCompleted(0L);
        pro.setLowBalanceThreshold(50L);

        Pro saved = proRepository.save(pro);
        return getMapper().toDto(saved);
    }

    // ========================================================================
    // Search
    // ========================================================================

    @Transactional(readOnly = true)
    public List<ProDTO> searchPros(String query) {
        List<Pro> pros = proRepository.searchByNameOrTel(query);
        return getMapper().toDtos(pros);
    }

    @Transactional
    public ProDTO updateProfile(Long proId, ProDTO dto) {
        Pro pro = findProById(proId);

        // Update allowed profile fields
        if (dto.getFirstName() != null) {
            pro.setFirstName(dto.getFirstName());
        }
        if (dto.getLastName() != null) {
            pro.setLastName(dto.getLastName());
        }
        if (dto.getProfilePhoto() != null) {
            pro.setProfilePhoto(dto.getProfilePhoto());
        }

        // Handle trade update (category)
        if (dto.getTrade() != null && dto.getTrade().getId() != null) {
            Category trade = categoryRepository.findById(dto.getTrade().getId())
                    .orElseThrow(
                            () -> new NotFoundException("Trade/Category not found with id: " + dto.getTrade().getId()));
            pro.setTrade(trade);
        }

        // Handle base zone update
        if (dto.getBaseZone() != null && dto.getBaseZone().getId() != null) {
            Zone zone = zoneRepository.findById(dto.getBaseZone().getId())
                    .orElseThrow(() -> new NotFoundException("Zone not found with id: " + dto.getBaseZone().getId()));
            pro.setBaseZone(zone);
        }

        Pro saved = proRepository.save(pro);
        log.info("Pro {} updated their profile", proId);
        return getMapper().toDto(saved);
    }

    @Transactional
    public ProDTO submitKycDocuments(Long proId, Long cniFrontMediaId, Long cniBackMediaId, Long selfieMediaId,
            Long tradeDocMediaId) {
        Pro pro = findProById(proId);

        // Can only submit if not already pending or approved
        if (pro.getKycStatus() == KycStatus.PENDING) {
            throw new BadRequestException("KYC documents are already pending review");
        }
        if (pro.getKycStatus() == KycStatus.APPROVED) {
            throw new BadRequestException("KYC is already approved");
        }

        // Load and assign media entities
        if (cniFrontMediaId != null) {
            Media cniFront = mediaRepository.findById(cniFrontMediaId)
                    .orElseThrow(() -> new NotFoundException("CNI front media not found with id: " + cniFrontMediaId));
            pro.setCniFrontMedia(cniFront);
        }
        if (cniBackMediaId != null) {
            Media cniBack = mediaRepository.findById(cniBackMediaId)
                    .orElseThrow(() -> new NotFoundException("CNI back media not found with id: " + cniBackMediaId));
            pro.setCniBackMedia(cniBack);
        }
        if (selfieMediaId != null) {
            Media selfie = mediaRepository.findById(selfieMediaId)
                    .orElseThrow(() -> new NotFoundException("Selfie media not found with id: " + selfieMediaId));
            pro.setSelfieMedia(selfie);
        }
        if (tradeDocMediaId != null) {
            Media tradeDoc = mediaRepository.findById(tradeDocMediaId)
                    .orElseThrow(
                            () -> new NotFoundException("Trade document media not found with id: " + tradeDocMediaId));
            pro.setTradeDocMedia(tradeDoc);
        }

        pro.setKycStatus(KycStatus.PENDING);
        pro.setKycSubmittedAt(LocalDateTime.now());

        Pro saved = proRepository.save(pro);
        log.info("Pro {} submitted KYC documents", proId);
        return getMapper().toDto(saved);
    }

    @Transactional
    public ProDTO updateLocation(Long proId, Double latitude, Double longitude) {
        Pro pro = findProById(proId);

        pro.setCurrentLatitude(latitude);
        pro.setCurrentLongitude(longitude);
        pro.setLocationUpdatedAt(LocalDateTime.now());

        Pro saved = proRepository.save(pro);
        log.debug("Pro {} updated location to ({}, {})", proId, latitude, longitude);
        return getMapper().toDto(saved);
    }

    @Transactional
    public ProDTO goOnline(Long proId) {
        return setOnlineStatus(proId, true);
    }

    @Transactional
    public ProDTO goOffline(Long proId) {
        return setOnlineStatus(proId, false);
    }

    @Transactional
    public ProDTO updateMyLowBalanceThreshold(Long proId, Long threshold) {
        return updateLowBalanceThreshold(proId, threshold);
    }

    @Transactional(readOnly = true)
    public ProDTO getMyProfile(Long proId) {
        return findById(proId);
    }

    @Transactional(readOnly = true)
    public KycStatus getMyKycStatus(Long proId) {
        Pro pro = findProById(proId);
        return pro.getKycStatus();
    }

    // ========================================================================
    // KYC Management (Admin)
    // ========================================================================

    @Transactional(readOnly = true)
    public List<ProDTO> findByKycStatus(KycStatus kycStatus) {
        List<Pro> pros = proRepository.findByKycStatus(kycStatus);
        return getMapper().toDtos(pros);
    }

    @Transactional(readOnly = true)
    public List<ProDTO> findPendingKycApplications() {
        return findByKycStatus(KycStatus.PENDING);
    }

    @Transactional
    public ProDTO approveKyc(Long proId, Long adminId) {
        Pro pro = findProById(proId);

        if (pro.getKycStatus() != KycStatus.PENDING) {
            throw new BadRequestException("Pro KYC is not pending. Current status: " + pro.getKycStatus());
        }

        Admin admin = adminRepository.findById(adminId)
                .orElseThrow(() -> new NotFoundException("Admin not found with id: " + adminId));

        pro.setKycStatus(KycStatus.APPROVED);
        pro.setApprovedAt(LocalDateTime.now());
        pro.setApprovedByAdmin(admin);
        pro.setIsActive(true);

        Pro saved = proRepository.save(pro);
        log.info("Approved KYC for pro {} by admin {}", proId, adminId);

        return getMapper().toDto(saved);
    }

    @Transactional
    public ProDTO rejectKyc(Long proId, String reason) {
        Pro pro = findProById(proId);

        if (pro.getKycStatus() != KycStatus.PENDING) {
            throw new BadRequestException("Pro KYC is not pending. Current status: " + pro.getKycStatus());
        }

        pro.setKycStatus(KycStatus.REJECTED);

        Pro saved = proRepository.save(pro);
        log.info("Rejected KYC for pro {} with reason: {}", proId, reason);

        return getMapper().toDto(saved);
    }

    @Transactional
    public ProDTO resetKyc(Long proId) {
        Pro pro = findProById(proId);
        pro.setKycStatus(KycStatus.PENDING);
        pro.setApprovedAt(null);
        pro.setApprovedByAdmin(null);
        pro.setKycSubmittedAt(null);
        Pro saved = proRepository.save(pro);
        log.info("Reset KYC for pro {}", proId);
        return getMapper().toDto(saved);
    }

    // ========================================================================
    // Online/Availability Status
    // ========================================================================

    @Transactional
    public ProDTO setOnlineStatus(Long proId, boolean online) {
        Pro pro = findProById(proId);

        // Only approved pros can go online
        if (online && pro.getKycStatus() != KycStatus.APPROVED) {
            throw new BadRequestException("Pro must have approved KYC to go online");
        }

        // Only active pros can go online
        if (online && !Boolean.TRUE.equals(pro.getIsActive())) {
            throw new BadRequestException("Pro account is not active");
        }

        pro.setOnline(online);
        Pro saved = proRepository.save(pro);

        log.info("Pro {} is now {}", proId, online ? "online" : "offline");

        return getMapper().toDto(saved);
    }

    @Transactional(readOnly = true)
    public List<ProDTO> findOnlinePros() {
        List<Pro> pros = proRepository.findByOnlineTrue();
        return getMapper().toDtos(pros);
    }

    @Transactional(readOnly = true)
    public List<ProDTO> findAvailablePros(Long minBalance) {
        List<Pro> pros = proRepository.findAvailablePros(minBalance);
        return getMapper().toDtos(pros);
    }

    @Transactional(readOnly = true)
    public List<ProDTO> findAvailableProsByTrade(Long tradeId, Long minBalance) {
        List<Pro> pros = proRepository.findAvailableProsByTrade(tradeId, minBalance);
        return getMapper().toDtos(pros);
    }

    // ========================================================================
    // Account Management
    // ========================================================================

    @Transactional
    public ProDTO activateAccount(Long proId) {
        Pro pro = findProById(proId);
        pro.setIsActive(true);
        Pro saved = proRepository.save(pro);
        log.info("Activated pro account {}", proId);
        return getMapper().toDto(saved);
    }

    @Transactional
    public ProDTO deactivateAccount(Long proId) {
        Pro pro = findProById(proId);
        pro.setIsActive(false);
        pro.setOnline(false); // Also set offline
        Pro saved = proRepository.save(pro);
        log.info("Deactivated pro account {}", proId);
        return getMapper().toDto(saved);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Pro pro = findProById(id);
        pro.setArchived(true);
        pro.setIsActive(false);
        pro.setOnline(false);
        proRepository.save(pro);
        log.info("Archived pro {}", id);
    }

    @Transactional(readOnly = true)
    public List<ProDTO> findArchived() {
        List<Pro> archived = proRepository.findByArchivedTrue();
        return getMapper().toDtos(archived);
    }

    @Transactional
    public ProDTO restoreAccount(Long proId) {
        Pro pro = findProById(proId);
        pro.setArchived(false);
        Pro saved = proRepository.save(pro);
        log.info("Restored pro account {}", proId);
        return getMapper().toDto(saved);
    }

    @Transactional
    public ProDTO adminUpdate(Long proId, ProDTO dto) {
        Pro pro = findProById(proId);

        if (dto.getFirstName() != null)
            pro.setFirstName(dto.getFirstName());
        if (dto.getLastName() != null)
            pro.setLastName(dto.getLastName());
        if (dto.getEmail() != null)
            pro.setEmail(dto.getEmail());
        if (dto.getProfilePhoto() != null)
            pro.setProfilePhoto(dto.getProfilePhoto());
        if (dto.getIsActive() != null)
            pro.setIsActive(dto.getIsActive());
        if (dto.getOnline() != null)
            pro.setOnline(dto.getOnline());

        if (dto.getTrade() != null && dto.getTrade().getId() != null) {
            Category trade = categoryRepository.findById(dto.getTrade().getId())
                    .orElseThrow(() -> new NotFoundException("Trade not found with id: " + dto.getTrade().getId()));
            pro.setTrade(trade);
        }
        if (dto.getBaseZone() != null && dto.getBaseZone().getId() != null) {
            Zone zone = zoneRepository.findById(dto.getBaseZone().getId())
                    .orElseThrow(() -> new NotFoundException("Zone not found with id: " + dto.getBaseZone().getId()));
            pro.setBaseZone(zone);
        }

        Pro saved = proRepository.save(pro);
        log.info("Admin updated pro {}", proId);
        return getMapper().toDto(saved);
    }

    // ========================================================================
    // Low Balance Threshold
    // ========================================================================

    @Transactional
    public ProDTO updateLowBalanceThreshold(Long proId, Long threshold) {
        if (threshold < 0) {
            throw new BadRequestException("Threshold must be non-negative");
        }

        Pro pro = findProById(proId);
        pro.setLowBalanceThreshold(threshold);
        Pro saved = proRepository.save(pro);

        log.info("Updated low balance threshold for pro {} to {}", proId, threshold);

        return getMapper().toDto(saved);
    }

    // ========================================================================
    // Categories Management
    // ========================================================================

    // ========================================================================
    // Statistics
    // ========================================================================

    @Transactional(readOnly = true)
    public Long countApprovedActivePros() {
        return proRepository.countApprovedActivePros();
    }

    @Transactional(readOnly = true)
    public Double getAverageRating() {
        return proRepository.getAverageRating();
    }

    // ========================================================================
    // Relationship handling
    // ========================================================================

    @Override
    protected void applyRelationships(Pro entity, ProDTO dto) {
        if (dto == null)
            return;

        // Resolve trade (category)
        if (dto.getTrade() != null && dto.getTrade().getId() != null) {
            Category trade = categoryRepository.findById(dto.getTrade().getId())
                    .orElseThrow(
                            () -> new NotFoundException("Trade/Category not found with id: " + dto.getTrade().getId()));
            entity.setTrade(trade);
        }

        // Resolve base zone
        if (dto.getBaseZone() != null && dto.getBaseZone().getId() != null) {
            Zone zone = zoneRepository.findById(dto.getBaseZone().getId())
                    .orElseThrow(() -> new NotFoundException("Zone not found with id: " + dto.getBaseZone().getId()));
            entity.setBaseZone(zone);
        }

    }

    @Override
    protected void beforePersist(Pro entity, ProDTO dto, boolean isNew) {
        // Set defaults for new pros
        if (isNew) {
            if (entity.getKycStatus() == null) {
                entity.setKycStatus(KycStatus.PENDING);
            }
            if (entity.getOnline() == null) {
                entity.setOnline(false);
            }
            if (entity.getIsActive() == null) {
                entity.setIsActive(false); // Inactive until KYC approved
            }
            if (entity.getWalletBalance() == null) {
                entity.setWalletBalance(0L);
            }
            if (entity.getRatingAvg() == null) {
                entity.setRatingAvg(5.0);
            }
            if (entity.getRatingCount() == null) {
                entity.setRatingCount(0L);
            }
            if (entity.getJobsCompleted() == null) {
                entity.setJobsCompleted(0L);
            }
            if (entity.getLowBalanceThreshold() == null) {
                entity.setLowBalanceThreshold(50L);
            }
            if (entity.getArchived() == null) {
                entity.setArchived(false);
            }
        }
    }

    // ========================================================================
    // Helper methods
    // ========================================================================

    private Pro findProById(Long id) {
        return proRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Pro not found with id: " + id));
    }
}
