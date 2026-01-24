package com.sallahli.service;

import com.sallahli.dto.ManagerDto;
import com.sallahli.dto.TelVerificationResponseDto;
import com.sallahli.dto.UserDTO;
import com.sallahli.exceptions.BadRequestException;
import com.sallahli.exceptions.NotFoundException;
import com.sallahli.mapper.UserMapper;
import com.sallahli.model.Enum.DeliveryMethod;
import com.sallahli.model.Enum.UserRoleEnum;
import com.sallahli.model.User;
import com.sallahli.repository.UserOtpExpirationRepository;
import com.sallahli.security.KeycloakProvider;
import com.sallahli.security.constant.KeycloakUtils;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpStatus;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RolesResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.AccessTokenResponse;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.FederatedIdentityRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.ws.rs.NotAuthorizedException;
import javax.ws.rs.core.Response;
import java.time.LocalDateTime;
import java.util.*;

@Service
@AllArgsConstructor
@NoArgsConstructor
@Slf4j
public class UserService {


    @Value("${keycloak.auth-server-url}")
    public String serverURL;
    @Value("${keycloak.realm}")
    public String realm;
    @Value("${keycloak.client-id}")
    public String clientID;
    @Value("${keycloak.credentials.secret}")
    public String clientSecret;

    @Autowired
    private Keycloak keycloak;

    @Autowired
    private KeycloakProvider keycloakProvider;


    @Autowired
    private SmsService smsService;

    @Autowired
    private WhatsappService whatsappService;


    @Autowired
    private UserMapper mapper;

    @Autowired
    private UserOtpExpirationRepository userOtpExpirationRepository;


    // ==================== HELPER METHODS ====================

    public Slice<UserDTO> findSocialLoginUsers(Pageable pageable, String provider) {
        int size = pageable.getPageSize();
        int page = pageable.getPageNumber();
        int offset = Math.max(0, page * size);

        String providerFilter = StringUtils.isBlank(provider) ? null : provider.trim();

        int scanBatchSize = Math.max(size * 15, 200);

        List<UserDTO> content = new ArrayList<>(size);
        int scanFirst = 0;
        int socialSeen = 0;

        boolean hasNext = false;

        while (true) {
            List<UserRepresentation> batch = getUsersResource().list(scanFirst, scanBatchSize);
            if (batch == null || batch.isEmpty()) break;

            for (UserRepresentation u : batch) {
                List<FederatedIdentityRepresentation> fed;
                try {
                    fed = getUserResource(u.getId()).getFederatedIdentity();
                } catch (Exception e) {
                    continue;
                }

                boolean isSocial = fed != null && !fed.isEmpty();
                if (!isSocial) continue;

                if (providerFilter != null) {
                    boolean matches = fed.stream()
                            .map(FederatedIdentityRepresentation::getIdentityProvider)
                            .filter(Objects::nonNull)
                            .anyMatch(idp -> idp.equalsIgnoreCase(providerFilter));
                    if (!matches) continue;
                }

                if (socialSeen >= offset && content.size() < size) {
                    content.add(toUserDtoFromKeycloak(u, fed));
                }
                socialSeen++;

                if (socialSeen > offset + size) {
                    hasNext = true;
                    break;
                }
            }

            scanFirst += scanBatchSize;
            if (hasNext) break;
            if (batch.size() < scanBatchSize) break;
        }

        return new SliceImpl<>(content, pageable, hasNext);
    }


    private UserDTO toUserDtoFromKeycloak(UserRepresentation u, List<FederatedIdentityRepresentation> fed) {
        UserDTO dto = new UserDTO();
        dto.setUsername(u.getUsername());
        dto.setEmail(u.getEmail());
        dto.setFirstName(u.getFirstName());
        dto.setLastName(u.getLastName());
        dto.setTel(u.firstAttribute(KeycloakUtils.PHONE_NUMBER_ATTRIBUTE));
        List<String> providers = fed.stream()
                .map(FederatedIdentityRepresentation::getIdentityProvider)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        if (providers.isEmpty()) {
            dto.setProvider(null);
            return dto;
        }

        if (providers.size() > 1) {
            log.warn(" expects one provider but user {} has multiple: {}. Using first={}",
                    u.getUsername(), providers, providers.get(0));
        }

        dto.setProvider(providers.get(0));

        return dto;
    }


    private UsersResource getUsersResource() {
        return keycloak.realm(realm).users();
    }

    public UserRepresentation findUserByUsernameOrEmail(String usernameOrEmail) {
        // First, try to find by username
        List<UserRepresentation> usersByUsername = getUsersResource()
                .searchByUsername(usernameOrEmail, true);

        if (!usersByUsername.isEmpty()) {
            return usersByUsername.get(0);
        }

        // If not found by username, try to find by email
        List<UserRepresentation> usersByEmail = getUsersResource()
                .searchByEmail(usernameOrEmail, true);

        if (!usersByEmail.isEmpty()) {
            return usersByEmail.get(0);
        }

        return null;
    }


    public UserRepresentation getUserByUsernameOrEmailOrThrow(String usernameOrEmail) {
        UserRepresentation user = findUserByUsernameOrEmail(usernameOrEmail);
        if (user == null) {
            throw new NotFoundException("User not found with username or email: " + usernameOrEmail);
        }
        return user;
    }


    private UserResource getUserResource(String userId) {
        return getUsersResource().get(userId);
    }

    private boolean verifyPassword(String usernameOrEmail, String password) {
        try {
            Keycloak tempKeycloak = keycloakProvider.newKeycloakBuilderWithPasswordCredentials(
                    usernameOrEmail,
                    password
            ).build();

            AccessTokenResponse response = tempKeycloak.tokenManager().getAccessToken();
            return response != null && response.getToken() != null;
        } catch (NotAuthorizedException ex) {
            log.error("Password verification failed for user: {}", usernameOrEmail, ex);
            return false;
        } catch (Exception ex) {
            return true;
        }
    }

    public AccessTokenResponse authenticateUser(String usernameOrEmail, String password) {
        Keycloak keycloak = keycloakProvider.newKeycloakBuilderWithPasswordCredentials(
                usernameOrEmail,
                password
        ).build();
        return keycloak.tokenManager().getAccessToken();
    }

    private void updateUserAttribute(UserRepresentation user, String key, String value) {
        user.singleAttribute(key, value);
        getUserResource(user.getId()).update(user);
    }

    private void updateUserAttributes(UserRepresentation user, Map<String, String> attributes) {
        attributes.forEach(user::singleAttribute);
        getUserResource(user.getId()).update(user);
    }

    private static String generateRandomPassword() {
        Random random = new Random();
        return String.format("%06d", random.nextInt(1000000));
    }

// ==================== PASSWORD MANAGEMENT ====================

    private static CredentialRepresentation createPasswordCredentials(String password) {
        CredentialRepresentation passwordCredentials = new CredentialRepresentation();
        passwordCredentials.setType(CredentialRepresentation.PASSWORD);

        // Check if password is null to determine if it's temporary
        if (password != null && !password.isEmpty()) {
            passwordCredentials.setTemporary(false);
            passwordCredentials.setValue(password);
        } else {
            passwordCredentials.setTemporary(true);
            passwordCredentials.setValue(generateRandomPassword());
        }
        return passwordCredentials;
    }


    public boolean verifyOldPassword(String usernameOrEmail, String oldPassword) {
        try {
            Keycloak tempKeycloak = keycloakProvider.newKeycloakBuilderWithPasswordCredentials(
                    usernameOrEmail,
                    oldPassword
            ).build();

            AccessTokenResponse response = tempKeycloak.tokenManager().getAccessToken();
            return response != null && response.getToken() != null;
        } catch (Exception e) {
            log.error("Password verification failed for user: {}", usernameOrEmail, e);
            return false;
        }
    }


    @Transactional
    public UserDTO signup(UserDTO user) {
        UsersResource usersResource = getUsersResource();

        if (findUserByUsernameOrEmail(user.getUsername()) != null) {
            throw new BadRequestException("User with username " + user.getUsername() + " already exists");
        }

        try (Response response = saveNewKeycloakUser(user, usersResource)) {
            if (response.getStatus() == HttpStatus.SC_CREATED) {
                return user;
            } else if (response.getStatus() == HttpStatus.SC_CONFLICT) {
                throw new BadRequestException("The email is already used by another user");
            } else {
                throw new BadRequestException(
                        String.format("Error happened while registering user to keycloak codeStatus=%d",
                                response.getStatus())
                );
            }
        } catch (Exception e) {
            throw new BadRequestException(
                    String.format("Error happened while registering user to keycloak: %s", e.getMessage())
            );
        }
    }

    private Response saveNewKeycloakUser(UserDTO user, UsersResource usersResource) {
        CredentialRepresentation credential = createPasswordCredentials(user.getPassword());

        UserRepresentation kcUser = new UserRepresentation();
        kcUser.setUsername(user.getUsername());
        kcUser.setCredentials(Collections.singletonList(credential));
        kcUser.setFirstName(user.getFirstName());
        kcUser.setLastName(user.getLastName());
        kcUser.setEmail(user.getEmail());
        kcUser.setEnabled(true);
        kcUser.setEmailVerified(true);

        String phoneNumber = user.getTel() != null ? user.getTel() : user.getUsername();
        kcUser.singleAttribute(KeycloakUtils.PHONE_NUMBER_ATTRIBUTE, phoneNumber);
        kcUser.singleAttribute(KeycloakUtils.IS_VERIFIED_ATTRIBUTE, "false");

        return usersResource.create(kcUser);
    }

    public UserDTO updateUser(UserDTO user) {
        UserRepresentation existingUser = getUserByUsernameOrEmailOrThrow(user.getUsername());

        existingUser.setUsername(user.getUsername());
        existingUser.setFirstName(user.getFirstName());
        existingUser.setLastName(user.getLastName());
        existingUser.setEmail(user.getEmail());
        existingUser.setEnabled(true);
        existingUser.setEmailVerified(true);
        existingUser.singleAttribute(KeycloakUtils.PHONE_NUMBER_ATTRIBUTE, user.getTel());

        getUserResource(existingUser.getId()).update(existingUser);
        return user;
    }

    public void updateUsername(String oldUsername, String newUsername, String currentPassword) {
        // Check if new username already exists
        if (findUserByUsernameOrEmail(newUsername) != null) {
            throw new BadRequestException("Username " + newUsername + " is already taken");
        }

        // Get current user
        UserRepresentation user = getUserByUsernameOrEmailOrThrow(oldUsername);

        // Verify current password
        if (!verifyPassword(oldUsername, currentPassword)) {
            throw new BadRequestException("Invalid current password");
        }

        // Update username and related attributes
        user.setUsername(newUsername);
        Map<String, String> attributes = Map.of(
                KeycloakUtils.PHONE_NUMBER_ATTRIBUTE, newUsername,
                KeycloakUtils.IS_VERIFIED_ATTRIBUTE, "false"
        );
        updateUserAttributes(user, attributes);

        // Generate and send verification code for the new username
        generateAndSendVerificationCode(newUsername, DeliveryMethod.WHATSAPP);
    }


    public String generateAndSendVerificationCode(String username, DeliveryMethod deliveryMethod) {
        UserRepresentation user = getUserByUsernameOrEmailOrThrow(username);

        Random random = new Random();
        String code = String.format("%06d", random.nextInt(1000000));

        Map<String, String> attributes = Map.of(
                KeycloakUtils.VERIFICATION_CODE_ATTRIBUTE, code,
                KeycloakUtils.VERIFICATION_CODE_EXPIRATION_ATTRIBUTE,
                LocalDateTime.now().plusMinutes(5).toString()
        );
        updateUserAttributes(user, attributes);

        sendVerificationCode(user, deliveryMethod);
        return code;
    }


    @Async
    public void sendVerificationCode(UserRepresentation user, DeliveryMethod method) {

        try {
            String otp = user.getAttributes().get(KeycloakUtils.VERIFICATION_CODE_ATTRIBUTE).get(0);
            StringBuilder sb = new StringBuilder();
            sb.append("Your Almersoul verification code is :  \n ").append(otp);

            log.info("Sending verification code via {}: {}", method, sb);

            String phoneNumber = user.firstAttribute(KeycloakUtils.PHONE_NUMBER_ATTRIBUTE);
            String email = user.getEmail();

            switch (method) {
                case SMS -> smsService.send(sb.toString(), phoneNumber);
                case WHATSAPP -> whatsappService.sendText(phoneNumber, sb.toString());
                default -> throw new BadRequestException("Invalid method");
            }


        } catch (Exception e) {
            log.error("Failed to send otp password", e);

        }

    }

// ==================== PHONE NUMBER MANAGEMENT ====================

    public UserDTO registerPhoneNumber(String tel, String username) {
        UserRepresentation kcUser = getUserByUsernameOrEmailOrThrow(username);

        Map<String, String> attributes = Map.of(
                KeycloakUtils.PHONE_NUMBER_ATTRIBUTE, tel,
                KeycloakUtils.IS_VERIFIED_ATTRIBUTE, "false"
        );

        updateUserAttributes(kcUser, attributes);
        generateAndSendVerificationCode(username, DeliveryMethod.SMS);

        UserDTO userDTO = new UserDTO();
        userDTO.setUsername(username);
        userDTO.setId(Long.valueOf(kcUser.getId()));
        userDTO.setEmail(kcUser.getEmail());
        userDTO.setFirstName(kcUser.getFirstName());
        userDTO.setLastName(kcUser.getLastName());
        userDTO.setUsername(kcUser.getUsername());
        userDTO.setTel(kcUser.getUsername());

        return userDTO;
    }

    public TelVerificationResponseDto checkPhoneNumberExistence(String tel) {
        TelVerificationResponseDto telVerification = new TelVerificationResponseDto();
        telVerification.setIsExist(false);
        telVerification.setIsVerified(false);

        UserRepresentation user = findUserByUsernameOrEmail(tel);
        if (user != null) {
            telVerification.setIsExist(true);

            String isVerified = user.firstAttribute(KeycloakUtils.IS_VERIFIED_ATTRIBUTE);
            telVerification.setIsVerified("true".equals(isVerified));
        }

        return telVerification;
    }

// ==================== ROLE MANAGEMENT ====================

    public void saveRole(String username, UserRoleEnum roleEnum) {
        RoleRepresentation userRole = getOrCreateRole(roleEnum);

        UserRepresentation user = getUserByUsernameOrEmailOrThrow(username);
        getUserResource(user.getId())
                .roles()
                .realmLevel()
                .add(List.of(userRole));
    }

    public void removeRole(String username, UserRoleEnum roleEnum) {
        RoleRepresentation userRole = keycloak.realm(realm)
                .roles()
                .get(roleEnum.name())
                .toRepresentation();

        UserRepresentation user = getUserByUsernameOrEmailOrThrow(username);
        getUserResource(user.getId())
                .roles()
                .realmLevel()
                .remove(List.of(userRole));
    }

    private RoleRepresentation getOrCreateRole(UserRoleEnum roleEnum) {
        RolesResource rolesResource = keycloak.realm(realm).roles();
        try {
            return rolesResource.get(roleEnum.name()).toRepresentation();
        } catch (javax.ws.rs.NotFoundException e) {
            RoleRepresentation newRole = new RoleRepresentation();
            newRole.setName(roleEnum.name());
            newRole.setDescription("Auto-created role for " + roleEnum.name());
            rolesResource.create(newRole);
            return rolesResource.get(roleEnum.name()).toRepresentation();
        }
    }

// ==================== USER QUERIES ====================

    public List<UserRepresentation> searchByUsername(String username) {
        return getUsersResource().searchByUsername(username, true);
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

    public ManagerDto findById(long id) {
        return null; // TODO: Implement if needed
    }
}
