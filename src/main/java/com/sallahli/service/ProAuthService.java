package com.sallahli.service;

import com.sallahli.dto.UserCode;
import com.sallahli.dto.sallahli.ProDTO;
import com.sallahli.exceptions.BadRequestException;
import com.sallahli.exceptions.ConflictAccountException;
import com.sallahli.exceptions.NotFoundException;
import com.sallahli.mapper.ProMapper;
import com.sallahli.model.Enum.DeliveryMethod;
import com.sallahli.model.Pro;
import com.sallahli.repository.ProRepository;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.ws.rs.core.Response;
import java.time.LocalDateTime;
import java.util.*;

@RequiredArgsConstructor
@Service
@Slf4j
public class ProAuthService {

    @Value("${keycloak.realm}")
    private String realm;

    private final Keycloak keycloak;
    private final ProRepository proRepository;
    private final OtpDispatchService otpDispatchService;
    private final ProMapper proMapper;
    private final KeycloakProvider keycloakProvider;

    @Transactional
    public ProDTO signup(ProDTO proDTO) {

        String tel = proDTO.getTel();
        if (tel == null || tel.trim().isEmpty()) {
            throw new BadRequestException("tel must not be empty");
        }
        tel = tel.trim();
        proDTO.setUsername(tel);

        String email = proDTO.getEmail();
        boolean hasEmail = email != null && !email.trim().isEmpty();

        if (hasEmail) {
            email = email.trim();
            if (!email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$")) {
                throw new BadRequestException("email format not valid");
            }
            proDTO.setEmail(email);
        }

        UsersResource usersResource = keycloak.realm(realm).users();

        // Check local DB first
        Optional<Pro> localExistingOpt = proRepository.findByUsername(proDTO.getUsername());
        if (localExistingOpt.isPresent()) {
            Pro localExisting = localExistingOpt.get();
            if (Boolean.TRUE.equals(localExisting.getIsActive())) {
                throw new ConflictAccountException("User already exists");
            }

            if (hasEmail) {
                Optional<Pro> emailOwnerOpt = proRepository.findByEmail(proDTO.getEmail());
                if (emailOwnerOpt.isPresent()
                        && !emailOwnerOpt.get().getUsername().equalsIgnoreCase(proDTO.getUsername())) {
                    throw new ConflictAccountException("The email is already used by another user");
                }

                List<UserRepresentation> kcByEmail = usersResource.searchByEmail(proDTO.getEmail(), true);
                if (kcByEmail != null && !kcByEmail.isEmpty()) {
                    boolean emailUsedByOther = kcByEmail.stream().anyMatch(
                            u -> u.getUsername() == null || !u.getUsername().equalsIgnoreCase(proDTO.getUsername()));
                    if (emailUsedByOther) {
                        throw new ConflictAccountException("The email is already used by another user");
                    }
                }
            }

            String kcUserId = ensureKeycloakUserExists(proDTO, usersResource);
            ensureProRole(kcUserId);
            return saveProAndGenerateVerificationCode(proDTO, kcUserId);
        }

        // Check Keycloak for existing username
        List<UserRepresentation> byUsername = usersResource.searchByUsername(proDTO.getUsername(), true);
        if (byUsername != null && !byUsername.isEmpty()) {
            throw new ConflictAccountException("User already exists");
        }

        if (hasEmail) {
            List<UserRepresentation> byEmail = usersResource.searchByEmail(proDTO.getEmail(), true);
            if (byEmail != null && !byEmail.isEmpty()) {
                throw new ConflictAccountException("The email is already used by another user");
            }
        }

        // Create in Keycloak, assign role, save locally
        try (Response response = saveNewKeycloakUser(proDTO, usersResource)) {

            if (response.getStatus() == HttpStatus.SC_CREATED) {
                String kcUserId = CreatedResponseUtil.getCreatedId(response);

                ensureProRole(kcUserId);
                return saveProAndGenerateVerificationCode(proDTO, kcUserId);

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
                        "Error happened while registering pro to keycloak codeStatus=%d body=%s",
                        response.getStatus(), body));
            }

        } catch (BadRequestException | ConflictAccountException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(String.format(
                    "Unknown exception happened while registering pro to keycloak: %s",
                    e.getMessage()), e);
        }
    }

    private String ensureKeycloakUserExists(ProDTO proDTO, UsersResource usersResource) {
        UserRepresentation existing = usersResource
                .searchByUsername(proDTO.getUsername(), true)
                .stream()
                .findFirst()
                .orElse(null);

        if (existing != null) {
            return existing.getId();
        }

        try (Response response = saveNewKeycloakUser(proDTO, usersResource)) {
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

    private void ensureProRole(String kcUserId) {
        RoleRepresentation proRole = keycloak.realm(realm)
                .roles()
                .get(KeycloakUtils.PRO_ROLE)
                .toRepresentation();

        keycloak.realm(realm)
                .users()
                .get(kcUserId)
                .roles()
                .realmLevel()
                .add(List.of(proRole));
    }

    private Response saveNewKeycloakUser(ProDTO proDTO, UsersResource usersResource) {
        CredentialRepresentation credentialRepresentation = createPasswordCredentials(proDTO.getPassword());

        UserRepresentation kcUser = new UserRepresentation();
        kcUser.setUsername(proDTO.getUsername());
        kcUser.setCredentials(Collections.singletonList(credentialRepresentation));
        kcUser.setFirstName(proDTO.getFirstName());
        kcUser.setLastName(proDTO.getLastName());
        kcUser.setEmail(proDTO.getEmail());
        kcUser.setEnabled(true);
        kcUser.setEmailVerified(true);
        kcUser.singleAttribute(KeycloakUtils.PHONE_NUMBER_ATTRIBUTE, proDTO.getUsername());
        kcUser.singleAttribute(KeycloakUtils.IS_VERIFIED_ATTRIBUTE, "false");
        return usersResource.create(kcUser);
    }

    private ProDTO saveProAndGenerateVerificationCode(ProDTO proDTO, String kcUserId) {

        Optional<Pro> existingOpt = proRepository.findByUsername(proDTO.getUsername());
        Pro pro;
        if (existingOpt.isPresent()) {
            pro = existingOpt.get();
        } else {
            pro = new Pro();
            pro.setUsername(proDTO.getUsername());
        }

        pro.setFirstName(proDTO.getFirstName());
        pro.setLastName(proDTO.getLastName());
        pro.setEmail(proDTO.getEmail());
        pro.setTel(proDTO.getUsername());

        pro.setIsActive(false); // Inactive until KYC approved
        pro.setArchived(false);
        pro.setOnline(false);

        // Set defaults for new pro
        if (pro.getKycStatus() == null) {
            pro.setKycStatus(com.sallahli.model.Enum.KycStatus.PENDING);
        }
        if (pro.getWalletBalance() == null) {
            pro.setWalletBalance(0L);
        }
        if (pro.getRatingAvg() == null) {
            pro.setRatingAvg(5.0);
        }
        if (pro.getRatingCount() == null) {
            pro.setRatingCount(0L);
        }
        if (pro.getJobsCompleted() == null) {
            pro.setJobsCompleted(0L);
        }
        if (pro.getLowBalanceThreshold() == null) {
            pro.setLowBalanceThreshold(50L);
        }

        pro = proRepository.save(pro);
        return proMapper.toDto(pro);
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

            // Update local pro
            Optional<Pro> proOpt = proRepository.findByUsername(userCode.getUsername());
            if (proOpt.isPresent()) {
                Pro pro = proOpt.get();
                pro.setIsActive(true);
                proRepository.save(pro);
            }
        }

        return valid;
    }

    private boolean localCodeVerification(UserRepresentation user, String code) {
        String expirationDateStr = user.firstAttribute(KeycloakUtils.VERIFICATION_CODE_EXPIRATION_ATTRIBUTE);

        if (expirationDateStr == null) {
            throw new BadRequestException("No previous code generated to be checked");
        }

        LocalDateTime expirationDate = LocalDateTime.parse(expirationDateStr);
        if (LocalDateTime.now().isAfter(expirationDate)) {
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
        List<UserRepresentation> users = usersResource.search(username, true);
        if (users.isEmpty()) {
            throw new BadRequestException("User not found in Keycloak: " + username);
        }
        UserRepresentation user = users.get(0);

        String retryNumber = user.firstAttribute(KeycloakUtils.VERIFICATION_CODE_RETRY_NUMBER);
        int nbRetry = retryNumber != null ? Integer.parseInt(retryNumber) : 0;

        if (nbRetry > 10) {
            throw new BadRequestException("Too many retries detected");
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
            deliveryMethod = DeliveryMethod.WHATSAPP;
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

    public ProDTO updateUserNames(String username, ProDTO proDTO) {
        UsersResource usersResource = keycloak.realm(realm).users();
        List<UserRepresentation> users = usersResource.searchByUsername(username, true);
        if (users.isEmpty()) {
            throw new BadRequestException("No user found with username: " + username);
        }

        UserRepresentation kcUser = users.get(0);
        kcUser.setFirstName(proDTO.getFirstName());
        kcUser.setLastName(proDTO.getLastName());
        usersResource.get(kcUser.getId()).update(kcUser);

        Pro pro = proRepository.findByUsername(username)
                .orElseThrow(() -> new NotFoundException("Pro not found with username: " + username));
        pro.setFirstName(proDTO.getFirstName());
        pro.setLastName(proDTO.getLastName());
        proRepository.save(pro);
        generateVerificationCode(username, DeliveryMethod.WHATSAPP);
        return proMapper.toDto(pro);
    }

    public ProDTO registerPhoneNumber(String tel, String username) {

        UsersResource usersResource = keycloak.realm(realm).users();
        List<UserRepresentation> users = usersResource.searchByUsername(username, true);
        if (users == null || users.isEmpty()) {
            throw new BadRequestException("No user found with username: " + username);
        }
        final UserRepresentation kcUser = users.get(0);

        kcUser.singleAttribute(KeycloakUtils.PHONE_NUMBER_ATTRIBUTE, tel);
        kcUser.singleAttribute(KeycloakUtils.IS_VERIFIED_ATTRIBUTE, "false");
        usersResource.get(kcUser.getId()).update(kcUser);

        Pro pro = proRepository.findByUsername(username)
                .orElseThrow(() -> new NotFoundException("Pro not found with username: " + username));
        pro.setTel(tel);
        proRepository.save(pro);
        return proMapper.toDto(pro);
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

        Pro pro = proRepository.findByUsername(username)
                .orElseThrow(() -> new NotFoundException("Pro not found"));

        String newUsername = username + '_' + UUID.randomUUID();
        pro.setArchived(true);
        pro.setIsActive(false);
        pro.setOnline(false);
        pro.setUsername(newUsername);
        pro.setEmail(null);
        pro.setTel(null);
        proRepository.save(pro);

        UsersResource usersResource = keycloak.realm(realm).users();
        UserRepresentation kcUser = usersResource.search(username, true)
                .stream().findFirst()
                .orElseThrow(() -> new NotFoundException("Keycloak user not found"));
        usersResource.get(kcUser.getId()).remove();
    }

    @Transactional
    public void deleteUser(Authentication authentication, String password) {
        String username = authentication.getName();
        Pro pro = proRepository.findByUsername(username)
                .orElseThrow(() -> new NotFoundException("Pro not found"));

        if (!verifyPassword(username, password)) {
            throw new BadRequestException("Invalid password");
        }

        String newUsername = username + '_' + UUID.randomUUID();
        pro.setArchived(true);
        pro.setIsActive(false);
        pro.setOnline(false);
        pro.setUsername(newUsername);
        pro.setTel(null);
        proRepository.save(pro);

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
        return proRepository.findByUsername(username).isPresent() ||
                proRepository.findByTel(username).isPresent();
    }
}
