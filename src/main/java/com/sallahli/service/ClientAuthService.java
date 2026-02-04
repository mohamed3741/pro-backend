package com.sallahli.service;

import com.sallahli.dto.UserCode;
import com.sallahli.dto.UserDTO;
import com.sallahli.dto.sallahli.ClientDTO;
import com.sallahli.exceptions.BadRequestException;
import com.sallahli.exceptions.ConflictAccountException;
import com.sallahli.exceptions.NotFoundException;
import com.sallahli.mapper.ClientMapper;
import com.sallahli.model.Client;
import com.sallahli.model.Enum.DeliveryMethod;
import com.sallahli.repository.ClientRepository;
import com.sallahli.security.KeycloakProvider;
import com.sallahli.security.constant.KeycloakUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpStatus;
import org.keycloak.admin.client.CreatedResponseUtil;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.AccessTokenResponse;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.ws.rs.core.Response;
import java.time.LocalDateTime;
import java.util.*;

@RequiredArgsConstructor
@Service
@Slf4j
public class ClientAuthService {

    @Value("${keycloak.realm}")
    private String realm;

    private final Keycloak keycloak;
    private final ClientRepository clientRepository;
    private final OtpDispatchService otpDispatchService;
    private final ClientMapper clientMapper;
    private final KeycloakProvider keycloakProvider;

    @Transactional
    public ClientDTO signup(UserDTO userDTO) {

        String tel = userDTO.getTel();
        if (tel == null || tel.trim().isEmpty()) {
            throw new BadRequestException("tel must not be empty");
        }
        tel = tel.trim();
        userDTO.setUsername(tel);

        String email = userDTO.getEmail();
        boolean hasEmail = email != null && !email.trim().isEmpty();

        if (hasEmail) {
            email = email.trim();
            if (!email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$")) {
                throw new BadRequestException("emailformat not valid");
            }
            userDTO.setEmail(email);
        }

        UsersResource usersResource = keycloak.realm(realm).users();

        Client localExisting = clientRepository.findByUsername(userDTO.getUsername());
        if (localExisting != null) {
            if (Boolean.TRUE.equals(localExisting.getIsTelVerified())) {
                throw new ConflictAccountException("User already exists");
            }

            if (hasEmail) {
                Optional<Client> emailOwnerOpt = clientRepository.findByEmail(userDTO.getEmail());
                if (emailOwnerOpt.isPresent()
                        && !emailOwnerOpt.get().getUsername().equalsIgnoreCase(userDTO.getUsername())) {
                    throw new ConflictAccountException("The email is already used by another user");
                }

                List<UserRepresentation> kcByEmail = usersResource.searchByEmail(userDTO.getEmail(), true);
                if (kcByEmail != null && !kcByEmail.isEmpty()) {
                    boolean emailUsedByOther = kcByEmail.stream().anyMatch(
                            u -> u.getUsername() == null || !u.getUsername().equalsIgnoreCase(userDTO.getUsername()));
                    if (emailUsedByOther) {
                        throw new ConflictAccountException("The email is already used by another user");
                    }
                }
            }

            String kcUserId = ensureKeycloakUserExists(userDTO, usersResource);
            ensureClientRole(kcUserId);
            return saveClientAndGenerationVerificationCode(userDTO, kcUserId);
        }

        List<UserRepresentation> byUsername = usersResource.searchByUsername(userDTO.getUsername(), true);
        if (byUsername != null && !byUsername.isEmpty()) {
            throw new ConflictAccountException("User already exists");
        }

        if (hasEmail) {
            List<UserRepresentation> byEmail = usersResource.searchByEmail(userDTO.getEmail(), true);
            if (byEmail != null && !byEmail.isEmpty()) {
                throw new ConflictAccountException("The email is already used by another user");
            }
        }

        // ---------------------------------------
        // 4) Create in Keycloak, assign role, save
        // ---------------------------------------
        try (Response response = saveNewKeycloakUser(userDTO, usersResource)) {

            if (response.getStatus() == HttpStatus.SC_CREATED) {
                String kcUserId = CreatedResponseUtil.getCreatedId(response); // robust: no search needed

                ensureClientRole(kcUserId);
                return saveClientAndGenerationVerificationCode(userDTO, kcUserId);

            } else if (response.getStatus() == HttpStatus.SC_CONFLICT) {
                throw new ConflictAccountException("User/email already exists");

            } else {
                String body;
                try {
                    body = response.readEntity(String.class);
                } catch (Exception ignore) {
                    body = "";
                }

                throw new RuntimeException(String.format(
                        "Error happened while registering user to keycloak codeStatus=%d body=%s",
                        response.getStatus(), body));
            }

        } catch (BadRequestException | ConflictAccountException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(String.format(
                    "Unknown exception happened while registering user to keycloak: %s",
                    e.getMessage()), e);
        }
    }

    private String ensureKeycloakUserExists(UserDTO userDTO, UsersResource usersResource) {
        UserRepresentation existing = usersResource
                .searchByUsername(userDTO.getUsername(), true)
                .stream()
                .findFirst()
                .orElse(null);

        if (existing != null) {
            return existing.getId();
        }

        try (Response response = saveNewKeycloakUser(userDTO, usersResource)) {
            if (response.getStatus() == HttpStatus.SC_CREATED) {
                return CreatedResponseUtil.getCreatedId(response);
            } else if (response.getStatus() == HttpStatus.SC_CONFLICT) {
                throw new ConflictAccountException("User/email already exists");
            } else {
                String body;
                try {
                    body = response.readEntity(String.class);
                } catch (Exception ignore) {
                    body = "";
                }
                throw new RuntimeException("Keycloak create failed status=" + response.getStatus() + " body=" + body);
            }
        }
    }

    private void ensureClientRole(String kcUserId) {
        RoleRepresentation clientRole = keycloak.realm(realm)
                .roles()
                .get(KeycloakUtils.CLIENT_ROLE)
                .toRepresentation();

        keycloak.realm(realm)
                .users()
                .get(kcUserId)
                .roles()
                .realmLevel()
                .add(List.of(clientRole));
    }

    private Response saveNewKeycloakUser(UserDTO user, UsersResource usersResource) {
        UserRepresentation kcUser;
        CredentialRepresentation credentialRepresentation = createPasswordCredentials(user.getPassword());

        kcUser = new UserRepresentation();
        kcUser.setUsername(user.getUsername());
        kcUser.setCredentials(Collections.singletonList(credentialRepresentation));
        kcUser.setFirstName(user.getFirstName());
        kcUser.setLastName(user.getLastName());
        kcUser.setEmail(user.getEmail());
        kcUser.setEnabled(true);
        kcUser.setEmailVerified(true);
        kcUser.singleAttribute(KeycloakUtils.PHONE_NUMBER_ATTRIBUTE, user.getUsername());
        kcUser.singleAttribute(KeycloakUtils.IS_VERIFIED_ATTRIBUTE, "false");
        return usersResource.create(kcUser);
    }

    private ClientDTO saveClientAndGenerationVerificationCode(UserDTO user, String kcUserId) {

        Client localUser = clientRepository.findByUsername(user.getUsername());
        if (localUser == null) {
            localUser = new Client();
            localUser.setUsername(user.getUsername());
        }

        localUser.setCustomerId(kcUserId);
        localUser.setFirstName(user.getFirstName());
        localUser.setLastName(user.getLastName());
        localUser.setEmail(user.getEmail());
        localUser.setTel(localUser.getUsername());

        localUser.setIsActive(true);
        localUser.setArchived(false);
        localUser.setIsTelVerified(false);

        localUser = clientRepository.save(localUser);
        return clientMapper.toLightDto(localUser);
    }

    public Boolean checkCode(UserCode userCode) {
        if (userCode == null || userCode.getCode() == null || userCode.getUsername() == null) {
            throw new BadRequestException("Invalid data");
        }

        UsersResource usersResource = keycloak.realm(realm).users();
        List<UserRepresentation> users = usersResource.searchByUsername(userCode.getUsername(), true);
        if (users == null || users.isEmpty()) {
            throw new BadRequestException(
                    String.format("No user found with phone number %s", userCode.getUsername()));
        }

        UserRepresentation user = usersResource.get(users.get(0).getId()).toRepresentation();

        boolean valid = localCodeVerification(user, userCode.getCode());

        if (valid) {
            // Clear verification attributes and mark as verified
            user.singleAttribute(KeycloakUtils.VERIFICATION_CODE_ATTRIBUTE, null);
            user.singleAttribute(KeycloakUtils.VERIFICATION_CODE_EXPIRATION_ATTRIBUTE, null);
            user.singleAttribute(KeycloakUtils.VERIFICATION_CODE_RETRY_NUMBER, null);
            user.singleAttribute(KeycloakUtils.IS_VERIFIED_ATTRIBUTE, "true");
            usersResource.get(user.getId()).update(user);

            // Update local client
            Client client = clientRepository.findByUsername(userCode.getUsername());
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
            throw new BadRequestException("No previous code generated to be checked");
        }

        java.time.LocalDateTime expirationDate = java.time.LocalDateTime.parse(expirationDateStr);
        if (java.time.LocalDateTime.now().isAfter(expirationDate)) {
            throw new BadRequestException(
                    "Verification code expired, generate new one and try again");
        }

        String storedCode = user.firstAttribute(KeycloakUtils.VERIFICATION_CODE_ATTRIBUTE);
        if (!code.equals(storedCode)) {
            throw new BadRequestException("Invalid verification code");
        }

        return true;
    }

    public Boolean generateVerificationCode(String username, DeliveryMethod deliveryMethod) {
        UsersResource usersResource = keycloak.realm(realm).users();
        UserRepresentation user = usersResource.search(username, true).get(0);

        String retryNumber = user.firstAttribute(KeycloakUtils.VERIFICATION_CODE_RETRY_NUMBER);
        int nbRetry = retryNumber != null ? Integer.parseInt(retryNumber) : 0;

        // TODO max nb retries allowed 10 add to config
        if (nbRetry > 10) {
            throw new BadRequestException("To many retries detected");
        }

        Random random = new Random();
        String code = String.format("%06d", random.nextInt(1000000));
        user.singleAttribute(KeycloakUtils.VERIFICATION_CODE_ATTRIBUTE, code);

        user.singleAttribute(KeycloakUtils.VERIFICATION_CODE_RETRY_NUMBER, String.valueOf(nbRetry + 1));
        user.singleAttribute(KeycloakUtils.VERIFICATION_CODE_EXPIRATION_ATTRIBUTE,
                LocalDateTime.now().plusMinutes(5).toString());
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
        if (password == null || password.isBlank()) {
            throw new BadRequestException("password must not be empty");
        }
        CredentialRepresentation passwordCredentials = new CredentialRepresentation();
        passwordCredentials.setTemporary(false);
        passwordCredentials.setType(CredentialRepresentation.PASSWORD);
        passwordCredentials.setValue(password);
        return passwordCredentials;
    }

    public ClientDTO updateUserNames(String username, UserDTO userDTO) {
        UsersResource usersResource = keycloak.realm(realm).users();
        List<UserRepresentation> users = usersResource.searchByUsername(username, true);
        if (users.isEmpty()) {
            throw new BadRequestException("No user found with username: " + username);
        }

        UserRepresentation kcUser = users.get(0);
        kcUser.setFirstName(userDTO.getFirstName());
        kcUser.setLastName(userDTO.getLastName());
        usersResource.get(kcUser.getId()).update(kcUser);

        Client client = clientRepository.findByUsername(username);
        if (client == null) {
            throw new NotFoundException("Client not found with username: " + username);
        }
        client.setFirstName(userDTO.getFirstName());
        client.setLastName(userDTO.getLastName());
        clientRepository.save(client);
        generateVerificationCode(username, DeliveryMethod.WHATSAPP);
        return clientMapper.toDto(client);
    }

    public ClientDTO initClientFromToken(Authentication authentication) {
        String username = authentication.getName();
        JwtAuthenticationToken jwt = (JwtAuthenticationToken) authentication;
        Map<String, Object> attrs = jwt.getTokenAttributes();
        String sub = Objects.toString(attrs.get("sub"));

        ensureClientRole(sub);

        Client client = Client.builder()
                .username(username)
                .isActive(true)
                .isTelVerified(false)
                .tel(Objects.toString(attrs.get("phone_number"), null))
                .lastName(Objects.toString(attrs.get("given_name"), null))
                .firstName(Objects.toString(attrs.get("given_name"), null))
                .email(Objects.toString(attrs.get("email"), null))
                .customerId(sub)
                .build();
        return clientMapper.toDto(clientRepository.save(client));
    }

    public ClientDTO registerPhoneNumber(String tel, String username) {

        UsersResource usersResource = keycloak.realm(realm).users();
        List<UserRepresentation> users = usersResource.searchByUsername(username, true);
        if (users == null || users.isEmpty()) {
            throw new BadRequestException("No user phone with username : " + username);
        }
        final UserRepresentation kcUser = users.get(0);

        kcUser.singleAttribute(KeycloakUtils.PHONE_NUMBER_ATTRIBUTE, tel);
        kcUser.singleAttribute(KeycloakUtils.IS_VERIFIED_ATTRIBUTE, "false");
        usersResource.get(kcUser.getId()).update(kcUser);

        Client client = clientRepository.findByUsername(username);
        if (client == null) {
            throw new NotFoundException("Client not found with username: " + username);
        }
        client.setTel(tel);
        client.setIsTelVerified(false);
        clientRepository.save(client);
        return clientMapper.toDto(client);
    }

    @Transactional
    public void deleteUserWithOtp(Authentication authentication, String otp) {
        String username = authentication.getName();

        UserCode userCode = new UserCode();
        userCode.username = username;
        userCode.code = otp;
        if (!Boolean.TRUE.equals(checkCode(userCode))) {
            throw new BadRequestException("Invalid or expired OTP");
        }

        Client client = clientRepository.findByUsername(username);
        if (client == null) {
            throw new NotFoundException("Client not found");
        }

        String newUsername = username + '_' + UUID.randomUUID();
        client.setArchived(true);
        client.setIsDeleted(true);
        client.setUsername(newUsername);
        client.setEmail(null);
        client.setTel(null);
        clientRepository.save(client);

        UsersResource usersResource = keycloak.realm(realm).users();
        UserRepresentation kcUser = usersResource.search(username, true)
                .stream().findFirst()
                .orElseThrow(() -> new NotFoundException("Keycloak user not found"));
        usersResource.get(kcUser.getId()).remove();
    }

    @Transactional
    public void deleteUser(Authentication authentication, String password) {
        String username = authentication.getName();
        Client client = clientRepository.findByUsername(username);
        if (client == null) {
            throw new NotFoundException("Client not found");
        }

        if (!verifyPassword(username, password)) {
            throw new BadRequestException("Invalid password");
        }

        String newUsername = username + '_' + UUID.randomUUID();
        client.setArchived(true);
        client.setIsDeleted(true);
        client.setUsername(newUsername);
        client.setTel(null);
        clientRepository.save(client);

        UsersResource usersResource = keycloak.realm(realm).users();
        UserRepresentation kcUser = usersResource.search(username, true).stream().findFirst()
                .orElseThrow(() -> new NotFoundException("Keycloak user not found"));

        usersResource.get(kcUser.getId()).remove();
    }

    public boolean verifyPassword(String username, String password) {
        try {
            Keycloak keycloak = keycloakProvider.newKeycloakBuilderWithPasswordCredentials(username, password).build();
            AccessTokenResponse accessTokenResponse = keycloak.tokenManager().getAccessToken();
            return accessTokenResponse != null;
        } catch (BadRequestException ex) {
            log.warn("Invalid password for user {}", username, ex);
            return false;
        }
    }

    public boolean isUserExists(String username) {
        return clientRepository.findByUsername(username) != null ||
                clientRepository.findByTel(username).isPresent();
    }
}
