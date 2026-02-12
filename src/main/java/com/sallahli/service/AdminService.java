package com.sallahli.service;

import com.sallahli.dto.sallahli.AdminDTO;
import com.sallahli.exceptions.NotFoundException;
import com.sallahli.mapper.AdminMapper;
import com.sallahli.model.Admin;
import com.sallahli.model.Enum.AdminRole;
import com.sallahli.repository.AdminRepository;
import com.sallahli.service.crud.AbstractCrudService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Slf4j
public class AdminService extends AbstractCrudService<Admin, AdminDTO> {

    private final AdminRepository adminRepository;

    public AdminService(AdminRepository adminRepository, AdminMapper adminMapper) {
        super(adminRepository, adminMapper);
        this.adminRepository = adminRepository;
    }

    // ========================================================================
    // Lookups
    // ========================================================================

    @Transactional(readOnly = true)
    public AdminDTO findByUsername(String username) {
        Admin admin = adminRepository.findByUsername(username)
                .orElseThrow(() -> new NotFoundException("Admin not found with username: " + username));
        return getMapper().toDto(admin);
    }

    @Transactional(readOnly = true)
    public Admin findAdminEntityByUsername(String username) {
        return adminRepository.findByUsername(username)
                .orElseThrow(() -> new NotFoundException("Admin not found with username: " + username));
    }

    @Transactional(readOnly = true)
    public Admin findAdminEntityById(Long id) {
        return adminRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Admin not found with id: " + id));
    }

    // ========================================================================
    // Search & Filter
    // ========================================================================

    @Transactional(readOnly = true)
    public List<AdminDTO> searchAdmins(String query) {
        List<Admin> admins = adminRepository.searchAdmins(query);
        return getMapper().toDtos(admins);
    }

    @Transactional(readOnly = true)
    public List<AdminDTO> findByRole(AdminRole role) {
        List<Admin> admins = adminRepository.findByRole(role);
        return getMapper().toDtos(admins);
    }

    @Transactional(readOnly = true)
    public List<AdminDTO> findActive() {
        List<Admin> admins = adminRepository.findByArchivedFalse();
        return getMapper().toDtos(admins);
    }

    // ========================================================================
    // Archive / Restore
    // ========================================================================

    @Transactional(readOnly = true)
    public List<AdminDTO> findArchived() {
        List<Admin> admins = adminRepository.findByArchivedTrue();
        return getMapper().toDtos(admins);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Admin admin = adminRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Admin not found with id: " + id));
        admin.setArchived(true);
        admin.setIsActive(false);
        adminRepository.save(admin);
        log.info("Archived admin {}", id);
    }

    @Transactional
    public AdminDTO restoreAccount(Long id) {
        Admin admin = adminRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Admin not found with id: " + id));
        admin.setArchived(false);
        admin.setIsActive(true);
        Admin saved = adminRepository.save(admin);
        log.info("Restored admin account {}", id);
        return getMapper().toDto(saved);
    }

    // ========================================================================
    // Activate / Deactivate
    // ========================================================================

    @Transactional
    public AdminDTO activateAccount(Long id) {
        Admin admin = adminRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Admin not found with id: " + id));
        admin.setIsActive(true);
        Admin saved = adminRepository.save(admin);
        log.info("Activated admin account {}", id);
        return getMapper().toDto(saved);
    }

    @Transactional
    public AdminDTO deactivateAccount(Long id) {
        Admin admin = adminRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Admin not found with id: " + id));
        admin.setIsActive(false);
        Admin saved = adminRepository.save(admin);
        log.info("Deactivated admin account {}", id);
        return getMapper().toDto(saved);
    }

    // ========================================================================
    // Lifecycle hooks
    // ========================================================================

    @Override
    protected void beforePersist(Admin entity, AdminDTO dto, boolean isNew) {
        if (isNew) {
            if (entity.getRole() == null) {
                entity.setRole(AdminRole.ADMIN);
            }
            if (entity.getIsActive() == null) {
                entity.setIsActive(true);
            }
            if (entity.getArchived() == null) {
                entity.setArchived(false);
            }
        }
    }
}
