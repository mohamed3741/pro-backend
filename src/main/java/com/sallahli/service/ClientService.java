package com.sallahli.service;

import com.sallahli.dto.sallahli.ClientDTO;
import com.sallahli.exceptions.BadRequestException;
import com.sallahli.exceptions.NotFoundException;
import com.sallahli.mapper.ClientMapper;
import com.sallahli.model.Client;
import com.sallahli.repository.ClientRepository;
import com.sallahli.service.crud.AbstractCrudService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Slf4j
public class ClientService extends AbstractCrudService<Client, ClientDTO> {

    private final ClientRepository clientRepository;

    public ClientService(ClientRepository clientRepository, ClientMapper clientMapper) {
        super(clientRepository, clientMapper);
        this.clientRepository = clientRepository;
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
    public ClientDTO signup(ClientDTO dto) {
        // Validate required fields
        if (dto.getTel() == null || dto.getTel().isBlank()) {
            throw new BadRequestException("Telephone number is required");
        }

        // Check if telephone already exists
        if (existsByTel(dto.getTel())) {
            throw new BadRequestException("A client with this telephone number already exists");
        }

        // Create the client
        ClientDTO created = create(dto);
        log.info("New client signup with telephone: {}", dto.getTel());
        return created;
    }

    
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
