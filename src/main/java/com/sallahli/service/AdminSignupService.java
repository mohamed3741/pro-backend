package com.sallahli.service;

import com.sallahli.dto.sallahli.AdminDTO;
import com.sallahli.mapper.AdminMapper;
import com.sallahli.model.Admin;
import com.sallahli.model.Enum.AdminRole;
import com.sallahli.repository.AdminRepository;
import com.sallahli.security.constant.KeycloakUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.ws.rs.core.Response;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminSignupService {

    @Value("${keycloak.realm}")
    private String realm;

    private final Keycloak keycloak;
    private final AdminRepository adminRepository;
    private final AdminMapper adminMapper;

    @Transactional
    public AdminDTO signup(AdminDTO adminDTO) {
        // Validate admin input
        validateAdminInput(adminDTO);

        UsersResource usersResource = keycloak.realm(realm).users();

        // Check if user exists in Keycloak
        List<UserRepresentation> existingUsers = usersResource.searchByUsername(adminDTO.getUsername(), true);
        if (existingUsers != null && !existingUsers.isEmpty()) {
            throw new com.sallahli.exceptions.ConflictAccountException(
                    "Admin account already exists with this username");
        }

        // Check if user exists locally
        if (adminRepository.findByUsername(adminDTO.getUsername()).isPresent()) {
            throw new com.sallahli.exceptions.ConflictAccountException(
                    "Admin account already exists with this username");
        }

        return createNewAdmin(usersResource, adminDTO);
    }

    private void validateAdminInput(AdminDTO adminDTO) {
        if (adminDTO.getUsername() == null || adminDTO.getUsername().trim().isEmpty()) {
            throw new com.sallahli.exceptions.BadRequestException("Username is required");
        }
        if (adminDTO.getEmail() == null || adminDTO.getEmail().trim().isEmpty()) {
            throw new com.sallahli.exceptions.BadRequestException("Email is required");
        }
        if (adminDTO.getFirstName() == null || adminDTO.getFirstName().trim().isEmpty()) {
            throw new com.sallahli.exceptions.BadRequestException("First name is required");
        }
        if (adminDTO.getLastName() == null || adminDTO.getLastName().trim().isEmpty()) {
            throw new com.sallahli.exceptions.BadRequestException("Last name is required");
        }
        if (adminDTO.getPassword() == null || adminDTO.getPassword().trim().isEmpty()) {
            throw new com.sallahli.exceptions.BadRequestException("Password is required");
        }
    }

    private AdminDTO createNewAdmin(UsersResource usersResource, AdminDTO adminDTO) {
        Response response = null;
        try {
            response = createKeycloakAdminUser(adminDTO, usersResource);

            if (response.getStatus() == 201) {
                // Determine the Keycloak role to assign based on AdminRole
                AdminRole role = adminDTO.getRole() != null ? adminDTO.getRole() : AdminRole.ADMIN;
                String keycloakRoleName = mapAdminRoleToKeycloakRole(role);
                assignRole(adminDTO.getUsername(), keycloakRoleName);

                // Save admin entity locally
                return saveAdminLocally(adminDTO, role);
            } else if (response.getStatus() == 409) {
                throw new com.sallahli.exceptions.BadRequestException(
                        "The email or username is already used by another user");
            } else {
                throw new RuntimeException(
                        String.format("Error creating admin in Keycloak: status=%d", response.getStatus()));
            }
        } catch (com.sallahli.exceptions.BadRequestException | com.sallahli.exceptions.ConflictAccountException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error during admin signup for username: {}", adminDTO.getUsername(), e);
            throw new RuntimeException("Error during admin signup: " + e.getMessage());
        } finally {
            if (response != null) {
                response.close();
            }
        }
    }

    private AdminDTO saveAdminLocally(AdminDTO adminDTO, AdminRole role) {
        Admin admin = adminMapper.toModel(adminDTO);
        admin.setRole(role);
        admin.setIsActive(true);
        admin.setArchived(false);
        Admin saved = adminRepository.save(admin);
        log.info("Admin '{}' with role {} created and saved locally with id={}", saved.getUsername(), role,
                saved.getId());
        return adminMapper.toDto(saved);
    }

    private String mapAdminRoleToKeycloakRole(AdminRole role) {
        return switch (role) {
            case SUPER_ADMIN -> KeycloakUtils.SUPER_ADMIN_ROLE;
            case ADMIN -> KeycloakUtils.ADMIN_ROLE;
            case AGENT -> KeycloakUtils.AGENT_ROLE;
            case CUSTOMER_SUPPORT_AGENT -> KeycloakUtils.CUSTOMER_SUPPORT_AGENT_ROLE;
            case LOGISTICS_COORDINATOR -> KeycloakUtils.LOGISTICS_COORDINATOR_ROLE;
            case ACCOUNTANT -> KeycloakUtils.ACCOUNTANT_ROLE;
        };
    }

    private Response createKeycloakAdminUser(AdminDTO adminDTO, UsersResource usersResource) {
        UserRepresentation kcUser = new UserRepresentation();
        kcUser.setUsername(adminDTO.getUsername());
        kcUser.setCredentials(Collections.singletonList(createPasswordCredentials(adminDTO.getPassword())));
        kcUser.setFirstName(adminDTO.getFirstName());
        kcUser.setLastName(adminDTO.getLastName());
        kcUser.setEmail(adminDTO.getEmail());
        kcUser.setEnabled(true);
        kcUser.setEmailVerified(true);

        // Set phone number if provided
        if (adminDTO.getTel() != null && !adminDTO.getTel().trim().isEmpty()) {
            kcUser.singleAttribute(KeycloakUtils.PHONE_NUMBER_ATTRIBUTE, adminDTO.getTel());
        }

        // Admins are automatically verified
        kcUser.singleAttribute(KeycloakUtils.IS_VERIFIED_ATTRIBUTE, "true");

        return usersResource.create(kcUser);
    }

    private void assignRole(String username, String roleName) {
        RoleRepresentation role = keycloak.realm(realm).roles().get(roleName).toRepresentation();
        UsersResource usersResource = keycloak.realm(realm).users();
        UserRepresentation kcUser = usersResource.search(username, true).stream().findFirst().orElseThrow();
        usersResource.get(kcUser.getId()).roles().realmLevel().add(List.of(role));
    }

    private static CredentialRepresentation createPasswordCredentials(String password) {
        CredentialRepresentation passwordCredentials = new CredentialRepresentation();
        passwordCredentials.setTemporary(false);
        passwordCredentials.setType(CredentialRepresentation.PASSWORD);
        passwordCredentials.setValue(password);
        return passwordCredentials;
    }
}
