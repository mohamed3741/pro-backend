package com.pro.service;

import com.pro.dto.UserDTO;
import com.pro.exceptions.BadRequestException;
import com.pro.exceptions.ConflictAccountException;
import com.pro.exceptions.NotFoundException;
import com.pro.mapper.UserMapper;
import com.pro.model.Enum.UserRoleEnum;
import com.pro.model.User;
import com.pro.security.KeycloakProvider;
import com.pro.security.constant.KeycloakUtils;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.AccessTokenResponse;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;

import javax.ws.rs.core.Response;
import java.security.SecureRandom;
import java.util.*;

@Service
@Slf4j
public class UserService {

    @Value("${keycloak.auth-server-url}")
    private String serverURL;
    
    @Value("${keycloak.realm}")
    private String realm;
    
    @Value("${keycloak.client-id}")
    private String clientID;
    
    @Value("${keycloak.credentials.secret}")
    private String clientSecret;

    private final Keycloak keycloak;
    private final KeycloakProvider keycloakProvider;
    private final UserMapper mapper;

    public UserService(Keycloak keycloak, KeycloakProvider keycloakProvider, UserMapper mapper) {
        this.keycloak = keycloak;
        this.keycloakProvider = keycloakProvider;
        this.mapper = mapper;
    }

    /**
     * Create a new user in Keycloak with clinic_id attribute.
     * 
     * @param userDTO User data
     * @param clinicId The clinic ID to associate with the user
     * @param role The role to assign (ADMIN or AGENT)
     * @param password Optional password (if null, generates temporary password)
     * @return The created user's Keycloak ID
     */
    public String createUser(UserDTO userDTO, Long clinicId, UserRoleEnum role, String password) {
        UsersResource usersResource = getUsersResource();

        // Check if user already exists
        List<UserRepresentation> existingUsers = usersResource.searchByEmail(userDTO.getEmail(), true);
        if (existingUsers != null && !existingUsers.isEmpty()) {
            throw new ConflictAccountException("User with email " + userDTO.getEmail() + " already exists");
        }

        existingUsers = usersResource.searchByUsername(userDTO.getUsername(), true);
        if (existingUsers != null && !existingUsers.isEmpty()) {
            throw new ConflictAccountException("User with username " + userDTO.getUsername() + " already exists");
        }

        // Create user representation
        UserRepresentation user = new UserRepresentation();
        user.setUsername(userDTO.getUsername());
        user.setEmail(userDTO.getEmail());
        user.setFirstName(userDTO.getFirstName());
        user.setLastName(userDTO.getLastName());
        user.setEnabled(true);
        user.setEmailVerified(role == UserRoleEnum.ADMIN || role == UserRoleEnum.AGENT);

        Map<String, List<String>> attributes = new HashMap<>();
        attributes.put(KeycloakUtils.CLINIC_ID_ATTRIBUTE, List.of(String.valueOf(clinicId)));
        attributes.put(KeycloakUtils.IS_VERIFIED_ATTRIBUTE, List.of("true"));
        if (userDTO.getTel() != null) {
            attributes.put(KeycloakUtils.PHONE_NUMBER_ATTRIBUTE, List.of(userDTO.getTel()));
        }
        user.setAttributes(attributes);

        // Create user in Keycloak
        Response response = usersResource.create(user);
        
        if (response.getStatus() != 201) {
            String errorMessage = response.readEntity(String.class);
            log.error("Failed to create Keycloak user: {} - {}", response.getStatus(), errorMessage);
            throw new BadRequestException("Failed to create user in Keycloak: " + errorMessage);
        }

        // Extract user ID from location header
        String userId = extractUserIdFromResponse(response);
        log.info("Created Keycloak user with ID: {}", userId);

        UserResource userResource = usersResource.get(userId);

        // Set password
        String userPassword = password != null ? password : generateTemporaryPassword();
        CredentialRepresentation credential = createPasswordCredentials(userPassword);
        credential.setTemporary(password == null); // Temporary if auto-generated
        userResource.resetPassword(credential);

        // Assign role
        assignRole(userId, role);

        // Send email with password reset if password was auto-generated
        if (password == null) {
            try {
                List<String> actions = new ArrayList<>();
                actions.add("UPDATE_PASSWORD");
                actions.add("VERIFY_EMAIL");
                userResource.executeActionsEmail(actions);
                log.info("Sent welcome email to user: {}", userDTO.getEmail());
            } catch (Exception e) {
                log.warn("Failed to send welcome email to {}: {}", userDTO.getEmail(), e.getMessage());
            }
        }

        return userId;
    }

    /**
     * Update user's clinic_id attribute in Keycloak.
     */
    public void updateUserClinicId(String username, Long clinicId) {
        UsersResource usersResource = getUsersResource();
        List<UserRepresentation> users = usersResource.searchByUsername(username, true);
        
        if (users == null || users.isEmpty()) {
            throw new NotFoundException("User not found: " + username);
        }

        UserRepresentation user = users.get(0);
        UserResource userResource = usersResource.get(user.getId());

        Map<String, List<String>> attributes = user.getAttributes();
        if (attributes == null) {
            attributes = new HashMap<>();
        }
        attributes.put(KeycloakUtils.CLINIC_ID_ATTRIBUTE, List.of(String.valueOf(clinicId)));
        user.setAttributes(attributes);

        userResource.update(user);
        log.info("Updated clinic_id to {} for user: {}", clinicId, username);
    }

    /**
     * Assign a role to a user.
     */
    public void assignRole(String userId, UserRoleEnum role) {
        try {
            RoleRepresentation roleRepresentation = keycloak.realm(realm).roles()
                    .get(role.name()).toRepresentation();
            
            getUsersResource().get(userId)
                    .roles().realmLevel()
                    .add(List.of(roleRepresentation));
            
            log.info("Assigned role {} to user {}", role.name(), userId);
        } catch (Exception e) {
            log.error("Failed to assign role {} to user {}: {}", role.name(), userId, e.getMessage());
            throw new BadRequestException("Failed to assign role: " + role.name());
        }
    }

    public UserDTO initUserFromToken(Authentication authentication) {
        String username = authentication.getName();
        JwtAuthenticationToken jwt = (JwtAuthenticationToken) authentication;
        Map<String, Object> attrs = jwt.getTokenAttributes();
        User user = User.builder()
                .username(username)
                .tel(Objects.toString(attrs.get("phone_number"), null))
                .firstName(Objects.toString(attrs.get("given_name"), null))
                .lastName(Objects.toString(attrs.get("family_name"), null))
                .email(Objects.toString(attrs.get("email"), null))
                .build();
        return mapper.toDto(user);
    }

    private UsersResource getUsersResource() {
        return keycloak.realm(realm).users();
    }

    public void forgetPassword(String username) {
        UsersResource usersResource = getUsersResource();
        List<UserRepresentation> users = usersResource.searchByUsername(username, true);
        if (users == null || users.isEmpty()) {
            throw new BadRequestException("No user found with username: " + username);
        }
        final UserRepresentation kcUser = users.get(0);
        UserResource userResource = usersResource.get(kcUser.getId());
        List<String> actions = new ArrayList<>();
        actions.add("UPDATE_PASSWORD");
        userResource.executeActionsEmail(actions);
    }

    public void updatePassword(String userId) {
        UsersResource usersResource = getUsersResource();
        UserResource userResource = usersResource.get(userId);
        List<String> actions = new ArrayList<>();
        actions.add("UPDATE_PASSWORD");
        userResource.executeActionsEmail(actions);
    }

    public UserDTO updateUser(UserDTO user) {
        UsersResource usersResource = getUsersResource();

        List<UserRepresentation> users = usersResource.searchByUsername(user.getUsername(), true);
        if (users == null || users.isEmpty()) {
            throw new NotFoundException("User not found: " + user.getUsername());
        }
        
        UserRepresentation existingUser = users.get(0);
        UserResource userResource = usersResource.get(existingUser.getId());
        existingUser.setFirstName(user.getFirstName());
        existingUser.setLastName(user.getLastName());
        existingUser.setEmail(user.getEmail());
        existingUser.setEnabled(true);
        
        if (user.getTel() != null) {
            existingUser.singleAttribute(KeycloakUtils.PHONE_NUMBER_ATTRIBUTE, user.getTel());
        }

        userResource.update(existingUser);
        return user;
    }

    public boolean changePassword(String username, String newPassword) {
        UsersResource usersResource = keycloak.realm(realm).users();
        List<UserRepresentation> users = usersResource.search(username, true);

        if (users == null || users.isEmpty()) {
            throw new NotFoundException("User not found");
        }

        UserRepresentation user = users.get(0);
        UserResource userResource = usersResource.get(user.getId());

        CredentialRepresentation newCredential = new CredentialRepresentation();
        newCredential.setType(CredentialRepresentation.PASSWORD);
        newCredential.setValue(newPassword);
        newCredential.setTemporary(false);

        try {
            userResource.resetPassword(newCredential);
            return true;
        } catch (Exception e) {
            log.error("Error changing password for user: " + username, e);
            return false;
        }
    }

    public boolean verifyOldPassword(String username, String oldPassword) {
        try (Keycloak tempKeycloak = KeycloakBuilder.builder()
                .serverUrl(serverURL)
                .realm(realm)
                .username(username)
                .password(oldPassword)
                .clientId(clientID)
                .clientSecret(clientSecret)
                .build()) {
            AccessTokenResponse response = tempKeycloak.tokenManager().getAccessToken();
            return response != null && response.getToken() != null;
        } catch (Exception e) {
            log.debug("Old password verification failed for user: {}", username);
            return false;
        }
    }

    public List<UserRepresentation> searchByUsername(String username) {
        return getUsersResource().searchByUsername(username, true);
    }

    public void deleteUser(String username) {
        UsersResource usersResource = keycloak.realm(realm).users();
        UserRepresentation kcUser = usersResource.search(username, true)
                .stream()
                .findFirst()
                .orElseThrow(() -> new NotFoundException("Keycloak user not found: " + username));
        usersResource.get(kcUser.getId()).remove();
        log.info("Deleted Keycloak user: {}", username);
    }

    public void saveRole(String username, UserRoleEnum roleEnum) {
        RoleRepresentation userRole = keycloak.realm(realm).roles()
                .get(roleEnum.name()).toRepresentation();
        final UsersResource usersResource = keycloak.realm(realm).users();
        UserRepresentation kcUser = usersResource.search(username, true)
                .stream()
                .findFirst()
                .orElseThrow(() -> new NotFoundException("User not found: " + username));
        usersResource.get(kcUser.getId())
                .roles().realmLevel().add(List.of(userRole));
    }

    public void removeRole(String username, UserRoleEnum roleEnum) {
        RoleRepresentation userRole = keycloak.realm(realm).roles()
                .get(roleEnum.name()).toRepresentation();
        final UsersResource usersResource = keycloak.realm(realm).users();
        UserRepresentation kcUser = usersResource.search(username, true)
                .stream()
                .findFirst()
                .orElseThrow(() -> new NotFoundException("User not found: " + username));
        usersResource.get(kcUser.getId())
                .roles().realmLevel().remove(List.of(userRole));
    }

    private static CredentialRepresentation createPasswordCredentials(String password) {
        CredentialRepresentation passwordCredentials = new CredentialRepresentation();
        passwordCredentials.setTemporary(false);
        passwordCredentials.setType(CredentialRepresentation.PASSWORD);
        passwordCredentials.setValue(password);
        return passwordCredentials;
    }

    private String extractUserIdFromResponse(Response response) {
        String location = response.getHeaderString("Location");
        if (location != null) {
            String[] parts = location.split("/");
            return parts[parts.length - 1];
        }
        throw new BadRequestException("Failed to extract user ID from Keycloak response");
    }

    private String generateTemporaryPassword() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%";
        SecureRandom random = new SecureRandom();
        StringBuilder password = new StringBuilder(12);
        for (int i = 0; i < 12; i++) {
            password.append(chars.charAt(random.nextInt(chars.length())));
        }
        return password.toString();
    }
}
