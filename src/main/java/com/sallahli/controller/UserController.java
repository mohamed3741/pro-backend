package com.sallahli.controller;

import com.sallahli.dto.*;
import com.sallahli.exceptions.AccessDeniedException;
import com.sallahli.exceptions.BadRequestException;
import com.sallahli.exceptions.NotFoundException;
import com.sallahli.security.KeycloakProvider;
import com.sallahli.security.constant.RefreshTokenDto;
import com.sallahli.service.UserService;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.keycloak.representations.AccessTokenResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import javax.ws.rs.NotAuthorizedException;
import java.util.Collections;

@RestController
@RequestMapping("/users")
@Slf4j
public class UserController {

    @Autowired
    private KeycloakProvider keycloakProvider;

    @Autowired
    private UserService userService;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody UserDTO userDTO) {

        if (userDTO.getUsername() == null || userDTO.getPassword() == null) {
            log.warn("Login attempt with missing username or password");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse("Username and password are required"));
        }

        try {
            AccessTokenResponse accessTokenResponse = keycloakProvider
                    .newKeycloakBuilderWithPasswordCredentials(userDTO.getUsername(), userDTO.getPassword())
                    .build()
                    .tokenManager()
                    .getAccessToken();

            return ResponseEntity.ok(accessTokenResponse);

        } catch (NotAuthorizedException ex) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse("Invalid username or password"));

        } catch (BadRequestException ex) {
            log.warn("Bad request during login for user: {}", userDTO.getUsername(), ex);
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ErrorResponse("Account is not verified or is disabled"));

        } catch (Exception ex) {
            log.error("Unexpected error during login for user: {}", userDTO.getUsername(), ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("An error occurred during login. Please try again."));
        }
    }

    @lombok.Data
    @lombok.AllArgsConstructor
    private static class ErrorResponse {
        private String message;
    }

    @PostMapping("/exchange-token")
    public ResponseEntity<AccessTokenResponse> exchangeToken(@RequestBody ExchangeableTokenDto exchangeableToken) {
        try {
            AccessTokenResponse accessTokenResponse = keycloakProvider.exchangeToken(
                    exchangeableToken.getExternalToken(), 
                    exchangeableToken.getLoginProvider()
            );
            return ResponseEntity.status(HttpStatus.OK).body(accessTokenResponse);
        } catch (JsonProcessingException e) {
            log.error("Error exchanging token", e);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(null);
        }
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<AccessTokenResponse> refreshToken(@RequestBody RefreshTokenDto refreshToken) {
        try {
            AccessTokenResponse accessTokenResponse = keycloakProvider.refreshToken(refreshToken);
            return ResponseEntity.status(HttpStatus.OK).body(accessTokenResponse);
        } catch (JsonProcessingException e) {
            log.error("Error refreshing token", e);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(null);
        }
    }

    @GetMapping("/me")
    public ResponseEntity<?> findMe(Authentication authentication) {
        try {
            UserDTO user = userService.initUserFromToken(authentication);
            if (user == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new ErrorResponse("User not found"));
            }
            return ResponseEntity.ok(user);
        } catch (Exception e) {
            log.error("Error getting user info", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Error retrieving user information"));
        }
    }

    @PostMapping("/change-password")
    public ResponseEntity<?> changePassword(@RequestBody PasswordChangeRequest passwordChangeRequest, Authentication authentication) {
        if (passwordChangeRequest == null || 
                StringUtils.isEmpty(passwordChangeRequest.getNewPassword()) ||
                StringUtils.isEmpty(passwordChangeRequest.getOldPassword())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Collections.singletonMap("message", "Invalid request data"));
        }

        String username = authentication.getName();

        if (!userService.verifyOldPassword(username, passwordChangeRequest.getOldPassword())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Collections.singletonMap("message", "Ancien mot de passe incorrect"));
        }

        try {
            if (userService.changePassword(username, passwordChangeRequest.getNewPassword())) {
                return ResponseEntity.status(HttpStatus.OK)
                        .body(Collections.singletonMap("message", "Le mot de passe a été modifié avec succès"));
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Collections.singletonMap("message", "Échec de la modification du mot de passe"));
            }
        } catch (NotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Collections.singletonMap("message", "User not found"));
        } catch (Exception e) {
            log.error("Error changing password for user: " + username, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Collections.singletonMap("message", "Error during password change"));
        }
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody UserDTO userDTO) {
        try {
            if (StringUtils.isEmpty(userDTO.getUsername())) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Collections.singletonMap("message", "Username is required"));
            }

            userService.forgetPassword(userDTO.getUsername());
            return ResponseEntity.ok()
                    .body(Collections.singletonMap("message", "Password reset email sent successfully"));
        } catch (BadRequestException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Collections.singletonMap("message", e.getMessage()));
        } catch (Exception e) {
            log.error("Error during forgot password for user: " + userDTO.getUsername(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Collections.singletonMap("message", "Error sending password reset email"));
        }
    }

    @PutMapping("/update")
    public ResponseEntity<?> updateUser(@RequestBody UserDTO userDTO, Authentication authentication) {
        try {
            String username = authentication.getName();
            
            // Ensure user can only update their own profile
            if (!username.equals(userDTO.getUsername())) {
                throw new AccessDeniedException("You can only update your own profile");
            }

            UserDTO updatedUser = userService.updateUser(userDTO);
            return ResponseEntity.ok(updatedUser);
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Collections.singletonMap("message", e.getMessage()));
        } catch (NotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Collections.singletonMap("message", "User not found"));
        } catch (Exception e) {
            log.error("Error updating user", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Collections.singletonMap("message", "Error updating user"));
        }
    }





}

