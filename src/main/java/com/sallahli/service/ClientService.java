package com.sallahli.service;

import com.sallahli.dto.MediaDTO;
import com.sallahli.dto.sallahli.ClientDTO;
import com.sallahli.exceptions.NotFoundException;
import com.sallahli.mapper.ClientMapper;
import com.sallahli.mapper.MediaMapper;
import com.sallahli.model.Client;
import com.sallahli.model.Enum.MediaEnum;
import com.sallahli.model.Media;
import com.sallahli.repository.ClientRepository;
import com.sallahli.service.crud.AbstractCrudService;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Service
@Slf4j
public class ClientService extends AbstractCrudService<Client, ClientDTO> {

    private final ClientRepository clientRepository;
    private final MediaService mediaService;
    private final MediaMapper mediaMapper;
    private final Keycloak keycloak;

    @Value("${keycloak.realm}")
    private String realm;

    public ClientService(ClientRepository clientRepository,
            ClientMapper clientMapper,
            MediaService mediaService,
            MediaMapper mediaMapper,
            Keycloak keycloak) {
        super(clientRepository, clientMapper);
        this.clientRepository = clientRepository;
        this.mediaService = mediaService;
        this.mediaMapper = mediaMapper;
        this.keycloak = keycloak;
    }

    // ========================================================================
    // Core CRUD overrides
    // ========================================================================

    @Override
    @Transactional(readOnly = true)
    public List<ClientDTO> findAll() {
        return getMapper().toDtos(clientRepository.findAll());
    }

    @Override
    @Transactional(readOnly = true)
    public ClientDTO findById(Long id) {
        Client client = clientRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Client not found with id: " + id));
        return getMapper().toDto(client);
    }

    // ========================================================================
    // Self-Service: Registration
    // ========================================================================

    @Transactional
    public ClientDTO updateProfile(Long clientId, ClientDTO dto) {
        Client client = findClientById(clientId);

        // Update allowed profile fields
        if (dto.getFirstName() != null) {
            client.setFirstName(dto.getFirstName());
        }
        if (dto.getLastName() != null) {
            client.setLastName(dto.getLastName());
        }
        if (dto.getEmail() != null) {
            client.setEmail(dto.getEmail());
        }
        if (dto.getNationality() != null) {
            client.setNationality(dto.getNationality());
        }
        if (dto.getBirthDate() != null) {
            client.setBirthDate(dto.getBirthDate());
        }
        if (dto.getGender() != null) {
            client.setGender(dto.getGender());
        }

        Client saved = clientRepository.save(client);
        log.info("Client {} updated their profile", clientId);
        return getMapper().toDto(saved);
    }

    public ResponseEntity<String> updateClientProfileImage(Authentication authentication, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest().body("File cannot be null or empty.");
        }

        try {
            byte[] fileBytes = file.getBytes();
            if (fileBytes.length == 0) {
                return ResponseEntity.badRequest().body("File data cannot be empty.");
            }

            // Get the client to check if they have an existing logo (for mediaId)
            Client client = clientRepository.findByUsername(authentication.getName())
                    .orElseThrow(() -> new NotFoundException("Client not found."));

            Long mediaId = (client.getLogo() != null && client.getLogo().getId() != null)
                    ? client.getLogo().getId()
                    : null;

            MediaDTO profileImg = mediaService.createDto(fileBytes, MediaEnum.LOGO, mediaId);
            Media media = mediaMapper.toModel(profileImg);

            client.setLogo(media);
            clientRepository.save(client);
            return ResponseEntity.ok("Client profile image updated successfully.");
        } catch (IOException e) {
            return ResponseEntity.badRequest().body("Error reading file: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error updating profile image: " + e.getMessage());
        }
    }

    @Transactional
    public ClientDTO updateUser(Authentication authentication, ClientDTO clientDTO) {
        String username = authentication.getName();
        Client client = clientRepository.findByUsername(username)
                .orElseThrow(() -> new NotFoundException("User not found"));

        UsersResource usersResource = keycloak.realm(realm).users();
        UserRepresentation kcUser = usersResource.search(username, true).stream().findFirst()
                .orElseThrow(() -> new NotFoundException("Keycloak user not found"));

        kcUser.setFirstName(clientDTO.getFirstName());
        kcUser.setLastName(clientDTO.getLastName());
        kcUser.setEmail(clientDTO.getEmail());
        usersResource.get(kcUser.getId()).update(kcUser);

        client.setFirstName(clientDTO.getFirstName());
        client.setLastName(clientDTO.getLastName());
        client.setEmail(clientDTO.getEmail());
        client = clientRepository.save(client);

        return getMapper().toDto(client);
    }

    @Transactional(readOnly = true)
    public ClientDTO getMyProfile(Long clientId) {
        return findById(clientId);
    }

    // ========================================================================
    // Client Lookup
    // ========================================================================

    @Transactional(readOnly = true)
    public ClientDTO findByTel(String tel) {
        Client client = clientRepository.findByTel(tel)
                .orElseThrow(() -> new NotFoundException("Client not found with tel: " + tel));
        return getMapper().toDto(client);
    }

    @Transactional(readOnly = true)
    public boolean existsByTel(String tel) {
        return clientRepository.findByTel(tel).isPresent();
    }

    @Transactional(readOnly = true)
    public ClientDTO findByCustomerId(String customerId) {
        Client client = clientRepository.findByCustomerId(customerId)
                .orElseThrow(() -> new NotFoundException("Client not found with customerId: " + customerId));
        return getMapper().toDto(client);
    }

    @Transactional(readOnly = true)
    public ClientDTO findByUsername(String username) {
        Client client = clientRepository.findByUsername(username)
                .orElseThrow(() -> new NotFoundException("Client not found with username: " + username));
        return getMapper().toDto(client);
    }

    // ========================================================================
    // Validation hooks
    // ========================================================================

    @Override
    protected void beforePersist(Client entity, ClientDTO dto, boolean isNew) {
        if (isNew) {
            // Set defaults for new clients
            if (entity.getArchived() == null) {
                entity.setArchived(false);
            }
            if (entity.getIsActive() == null) {
                entity.setIsActive(true);
            }
            if (entity.getIsTelVerified() == null) {
                entity.setIsTelVerified(false);
            }
            if (entity.getIsDeleted() == null) {
                entity.setIsDeleted(false);
            }
        }
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Client client = findClientById(id);
        client.setArchived(true);
        clientRepository.save(client);
        log.info("Archived client {}", id);
    }

    // ========================================================================
    // Helper methods
    // ========================================================================

    private Client findClientById(Long id) {
        return clientRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Client not found with id: " + id));
    }
}
