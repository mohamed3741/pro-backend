package com.sallahli.service;

import com.sallahli.dto.UserCode;
import com.sallahli.dto.UserDTO;
import com.sallahli.model.Client;
import com.sallahli.model.Enum.DeliveryMethod;
import com.sallahli.repository.ClientRepository;
import com.sallahli.security.constant.KeycloakUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RoleScopeResource;
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
import java.util.Random;

@Service
@RequiredArgsConstructor
@Slf4j
public class ClientSignupService {

    @Value("${keycloak.realm}")
    private String realm;

    private final Keycloak keycloak;
    private final ClientRepository clientRepository;
    private final OtpDispatchService otpDispatchService;

    @Transactional
    public UserDTO signup(UserDTO userDTO) {
        // Validate input
        validateUserInput(userDTO);

        UsersResource usersResource = keycloak.realm(realm).users();

        // Check if user exists
        List<UserRepresentation> existingUsers = usersResource.searchByUsername(userDTO.getUsername(), true);
        if (existingUsers != null && !existingUsers.isEmpty()) {
            UserRepresentation existingUser = existingUsers.get(0);
            return handleExistingClient(usersResource, userDTO, existingUser);
        } else {
            return createNewClient(usersResource, userDTO);
        }
    }

    private void validateUserInput(UserDTO userDTO) {
        if (userDTO.getUsername() == null || userDTO.getUsername().trim().isEmpty()) {
            throw new com.sallahli.exceptions.BadRequestException("Username is required");
        }
        if (userDTO.getTel() == null || userDTO.getTel().trim().isEmpty()) {
            throw new com.sallahli.exceptions.BadRequestException("Phone number is required");
        }
        if (userDTO.getFirstName() == null || userDTO.getFirstName().trim().isEmpty()) {
            throw new com.sallahli.exceptions.BadRequestException("First name is required");
        }
        if (userDTO.getLastName() == null || userDTO.getLastName().trim().isEmpty()) {
            throw new com.sallahli.exceptions.BadRequestException("Last name is required");
        }
    }

    private UserDTO createNewClient(UsersResource usersResource, UserDTO userDTO) {
        Response response = null;
        try {
            response = createKeycloakUser(userDTO, usersResource);

            if (response.getStatus() == 201) { // HttpStatus.SC_CREATED
                // Assign client role
                assignClientRole(userDTO.getUsername());

                // Save to local database
                return saveClient(userDTO);
            } else if (response.getStatus() == 409) { // HttpStatus.SC_CONFLICT
                throw new com.sallahli.exceptions.BadRequestException("The email is already used by another user");
            } else {
                throw new RuntimeException(
                        String.format("Error creating user in Keycloak: status=%d", response.getStatus()));
            }
        } catch (Exception e) {
            log.error("Error during client signup for username: {}", userDTO.getUsername(), e);
            throw new RuntimeException("Error during signup: " + e.getMessage());
        } finally {
            if (response != null) {
                response.close();
            }
        }
    }

    private Response createKeycloakUser(UserDTO userDTO, UsersResource usersResource) {
        UserRepresentation kcUser = new UserRepresentation();
        kcUser.setUsername(userDTO.getUsername());
        kcUser.setCredentials(Collections.singletonList(createPasswordCredentials(userDTO.getPassword())));
        kcUser.setFirstName(userDTO.getFirstName());
        kcUser.setLastName(userDTO.getLastName());
        kcUser.setEmail(userDTO.getEmail());
        kcUser.setEnabled(true);
        kcUser.setEmailVerified(true);

        // Set phone number and verification status
        kcUser.singleAttribute(KeycloakUtils.PHONE_NUMBER_ATTRIBUTE, userDTO.getUsername());
        kcUser.singleAttribute(KeycloakUtils.IS_VERIFIED_ATTRIBUTE, "false");

        return usersResource.create(kcUser);
    }

    private void assignClientRole(String username) {
        RoleRepresentation clientRole = keycloak.realm(realm).roles().get(KeycloakUtils.CLIENT_ROLE).toRepresentation();
        UsersResource usersResource = keycloak.realm(realm).users();
        UserRepresentation kcUser = usersResource.search(username, true).stream().findFirst().orElseThrow();
        usersResource.get(kcUser.getId()).roles().realmLevel().add(List.of(clientRole));
    }

    private UserDTO handleExistingClient(UsersResource usersResource, UserDTO userDTO,
            UserRepresentation kcUser) {
        UserRepresentation existingUser = usersResource.get(kcUser.getId()).toRepresentation();
        RoleScopeResource roleScopes = usersResource.get(kcUser.getId()).roles().realmLevel();

        if (Boolean.parseBoolean(existingUser.firstAttribute(KeycloakUtils.IS_VERIFIED_ATTRIBUTE))) {
            throw new com.sallahli.exceptions.ConflictAccountException("Account already exists");
        } else {
            // Assign client role if not already assigned
            RoleRepresentation clientRole = keycloak.realm(realm).roles().get(KeycloakUtils.CLIENT_ROLE)
                    .toRepresentation();
            if (!roleScopes.listEffective().contains(clientRole)) {
                usersResource.get(kcUser.getId()).roles().realmLevel().add(List.of(clientRole));
            }

            // Update credentials and user info
            existingUser.setCredentials(Collections.singletonList(createPasswordCredentials(userDTO.getPassword())));
            existingUser.setFirstName(userDTO.getFirstName());
            existingUser.setLastName(userDTO.getLastName());
            existingUser.setEmail(userDTO.getEmail());
            usersResource.get(existingUser.getId()).update(existingUser);

            return saveClient(userDTO);
        }
    }

    private UserDTO saveClient(UserDTO userDTO) {
        Client localUser = clientRepository.findByUsername(userDTO.getUsername()).orElse(null);
        if (localUser == null) {
            localUser = new Client();
            localUser.setUsername(userDTO.getUsername());
        }

        // Get Keycloak user ID
        UsersResource usersResource = keycloak.realm(realm).users();
        List<UserRepresentation> users = usersResource.searchByUsername(userDTO.getUsername(), true);
        if (!users.isEmpty()) {
            localUser.setCustomerId(users.get(0).getId());
        }

        localUser.setFirstName(userDTO.getFirstName());
        localUser.setLastName(userDTO.getLastName());
        localUser.setEmail(userDTO.getEmail());
        localUser.setTel(userDTO.getUsername());
        localUser.setIsActive(true);
        localUser.setArchived(false);
        localUser.setIsTelVerified(false);

        clientRepository.save(localUser);

        return userDTO;
    }

    public Boolean checkCode(UserCode userCode) {
        if (userCode == null || userCode.getCode() == null || userCode.getUsername() == null) {
            throw new com.sallahli.exceptions.BadRequestException("Invalid data");
        }

        UsersResource usersResource = keycloak.realm(realm).users();
        List<UserRepresentation> users = usersResource.searchByUsername(userCode.getUsername(), true);
        if (users == null || users.isEmpty()) {
            throw new com.sallahli.exceptions.BadRequestException(
                    String.format("No user found with phone number %s", userCode.getUsername()));
        }

        UserRepresentation user = usersResource.get(users.get(0).getId()).toRepresentation();

        boolean valid = localCodeVerification(user, userCode.getCode());

        if (valid) {
            // Clear verification attributes and mark as verified
            user.singleAttribute(KeycloakUtils.VERIFICATION_CODE_ATTRIBUTE, null);
            user.singleAttribute(KeycloakUtils.VERIFICATION_CODE_EXPIRATION_ATTRIBUTE, null);
            user.singleAttribute(KeycloakUtils.IS_VERIFIED_ATTRIBUTE, "true");
            usersResource.get(user.getId()).update(user);

            // Update local client
            Client client = clientRepository.findByUsername(userCode.getUsername()).orElse(null);
            if (client != null) {
                client.setIsTelVerified(true);
                clientRepository.save(client);
            }
        }

        return valid;
    }

    private boolean localCodeVerification(UserRepresentation user, String code) {
        String expirationDateStr = user.firstAttribute(KeycloakUtils.VERIFICATION_CODE_EXPIRATION_ATTRIBUTE);

        if (expirationDateStr == null) {
            throw new com.sallahli.exceptions.BadRequestException("No previous code generated to be checked");
        }

        java.time.LocalDateTime expirationDate = java.time.LocalDateTime.parse(expirationDateStr);
        if (java.time.LocalDateTime.now().isAfter(expirationDate)) {
            throw new com.sallahli.exceptions.BadRequestException(
                    "Verification code expired, generate new one and try again");
        }

        String storedCode = user.firstAttribute(KeycloakUtils.VERIFICATION_CODE_ATTRIBUTE);
        if (!code.equals(storedCode)) {
            throw new com.sallahli.exceptions.BadRequestException("Invalid verification code");
        }

        return true;
    }

    public Boolean generateVerificationCode(String username, DeliveryMethod deliveryMethod) {
        UsersResource usersResource = keycloak.realm(realm).users();
        UserRepresentation user = usersResource.search(username, true).get(0);

        Random random = new Random();
        String code = String.format("%06d", random.nextInt(1000000));

        user.singleAttribute(KeycloakUtils.VERIFICATION_CODE_ATTRIBUTE, code);
        user.singleAttribute(KeycloakUtils.VERIFICATION_CODE_EXPIRATION_ATTRIBUTE,
                java.time.LocalDateTime.now().plusMinutes(5).toString());
        usersResource.get(user.getId()).update(user);

        sendVerificationCode(user, deliveryMethod);
        return true;
    }

    private void sendVerificationCode(UserRepresentation user, DeliveryMethod deliveryMethod) {
        String verificationCode = user.firstAttribute(KeycloakUtils.VERIFICATION_CODE_ATTRIBUTE);
        String phoneNumber = user.firstAttribute(KeycloakUtils.PHONE_NUMBER_ATTRIBUTE);

        if (deliveryMethod == null) {
            deliveryMethod = DeliveryMethod.WHATSAPP; // Default method
        }

        otpDispatchService.sendVerificationCode(verificationCode, phoneNumber, deliveryMethod);
    }

    private static CredentialRepresentation createPasswordCredentials(String password) {
        CredentialRepresentation passwordCredentials = new CredentialRepresentation();
        passwordCredentials.setTemporary(false);
        passwordCredentials.setType(CredentialRepresentation.PASSWORD);
        passwordCredentials.setValue(password);
        return passwordCredentials;
    }
}
