package com.sallahli.service;

import com.sallahli.dto.UserDTO;
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
    private final UserService userService;

    @Transactional
    public UserDTO signup(UserDTO userDTO) {
        // Validate admin input
        validateAdminInput(userDTO);

        UsersResource usersResource = keycloak.realm(realm).users();

        // Check if user exists
        List<UserRepresentation> existingUsers = usersResource.searchByUsername(userDTO.getUsername(), true);
        if (existingUsers != null && !existingUsers.isEmpty()) {
            throw new com.sallahli.exceptions.ConflictAccountException("Admin account already exists with this username");
        }

        return createNewAdmin(usersResource, userDTO);
    }

    private void validateAdminInput(UserDTO userDTO) {
        if (userDTO.getUsername() == null || userDTO.getUsername().trim().isEmpty()) {
            throw new com.sallahli.exceptions.BadRequestException("Username is required");
        }
        if (userDTO.getEmail() == null || userDTO.getEmail().trim().isEmpty()) {
            throw new com.sallahli.exceptions.BadRequestException("Email is required");
        }
        if (userDTO.getFirstName() == null || userDTO.getFirstName().trim().isEmpty()) {
            throw new com.sallahli.exceptions.BadRequestException("First name is required");
        }
        if (userDTO.getLastName() == null || userDTO.getLastName().trim().isEmpty()) {
            throw new com.sallahli.exceptions.BadRequestException("Last name is required");
        }
        if (userDTO.getPassword() == null || userDTO.getPassword().trim().isEmpty()) {
            throw new com.sallahli.exceptions.BadRequestException("Password is required");
        }
    }

    private UserDTO createNewAdmin(UsersResource usersResource, UserDTO userDTO) {
        Response response = null;
        try {
            response = createKeycloakAdminUser(userDTO, usersResource);

            if (response.getStatus() == 201) { // HttpStatus.SC_CREATED
                // Assign admin role
                assignAdminRole(userDTO.getUsername());

                // Save admin user using UserService (for admin management)
                return userService.signup(userDTO);
            } else if (response.getStatus() == 409) { // HttpStatus.SC_CONFLICT
                throw new com.sallahli.exceptions.BadRequestException("The email or username is already used by another user");
            } else {
                throw new RuntimeException(String.format("Error creating admin in Keycloak: status=%d", response.getStatus()));
            }
        } catch (Exception e) {
            log.error("Error during admin signup for username: {}", userDTO.getUsername(), e);
            throw new RuntimeException("Error during admin signup: " + e.getMessage());
        } finally {
            if (response != null) {
                response.close();
            }
        }
    }

    private Response createKeycloakAdminUser(UserDTO userDTO, UsersResource usersResource) {
        UserRepresentation kcUser = new UserRepresentation();
        kcUser.setUsername(userDTO.getUsername());
        kcUser.setCredentials(Collections.singletonList(createPasswordCredentials(userDTO.getPassword())));
        kcUser.setFirstName(userDTO.getFirstName());
        kcUser.setLastName(userDTO.getLastName());
        kcUser.setEmail(userDTO.getEmail());
        kcUser.setEnabled(true);
        kcUser.setEmailVerified(true);

        // Set phone number if provided
        if (userDTO.getTel() != null && !userDTO.getTel().trim().isEmpty()) {
            kcUser.singleAttribute(KeycloakUtils.PHONE_NUMBER_ATTRIBUTE, userDTO.getTel());
        }

        // Admins are automatically verified
        kcUser.singleAttribute(KeycloakUtils.IS_VERIFIED_ATTRIBUTE, "true");

        return usersResource.create(kcUser);
    }

    private void assignAdminRole(String username) {
        RoleRepresentation adminRole = keycloak.realm(realm).roles().get(KeycloakUtils.ADMIN_ROLE).toRepresentation();
        UsersResource usersResource = keycloak.realm(realm).users();
        UserRepresentation kcUser = usersResource.search(username, true).stream().findFirst().orElseThrow();
        usersResource.get(kcUser.getId()).roles().realmLevel().add(List.of(adminRole));
    }

    private static CredentialRepresentation createPasswordCredentials(String password) {
        CredentialRepresentation passwordCredentials = new CredentialRepresentation();
        passwordCredentials.setTemporary(false);
        passwordCredentials.setType(CredentialRepresentation.PASSWORD);
        passwordCredentials.setValue(password);
        return passwordCredentials;
    }
}
