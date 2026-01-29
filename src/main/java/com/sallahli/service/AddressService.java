package com.sallahli.service;

import com.sallahli.dto.sallahli.AddressDTO;
import com.sallahli.exceptions.NotFoundException;
import com.sallahli.mapper.AddressMapper;
import com.sallahli.model.Address;
import com.sallahli.model.Client;
import com.sallahli.repository.AddressRepository;
import com.sallahli.repository.ClientRepository;
import com.sallahli.service.crud.AbstractCrudService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@Slf4j
public class AddressService extends AbstractCrudService<Address, AddressDTO> {

    private final AddressRepository addressRepository;
    private final ClientRepository clientRepository;
    private final AddressMapper addressMapper;

    public AddressService(AddressRepository addressRepository,
            ClientRepository clientRepository,
            AddressMapper addressMapper) {
        super(addressRepository, addressMapper);
        this.addressRepository = addressRepository;
        this.clientRepository = clientRepository;
        this.addressMapper = addressMapper;
    }

    // ========================================================================
    // Override base methods to filter by archived status
    // ========================================================================

    
    @Override
    @Transactional(readOnly = true)
    public List<AddressDTO> findAll() {
        return addressMapper.toDtos(addressRepository.findByArchivedFalse());
    }

    
    @Override
    @Transactional(readOnly = true)
    public AddressDTO findById(Long id) {
        Address address = addressRepository.findByIdAndArchivedFalse(id)
                .orElseThrow(() -> new NotFoundException("Address not found with id: " + id));
        return addressMapper.toDto(address);
    }

    // ========================================================================
    // Client-Address relationship methods
    // ========================================================================

    
    @Transactional(readOnly = true)
    public List<AddressDTO> findByClientId(Long clientId) {
        // Verify client exists
        if (!clientRepository.existsById(clientId)) {
            throw new NotFoundException("Client not found with id: " + clientId);
        }
        List<Address> addresses = addressRepository.findByClientIdAndArchivedFalse(clientId);
        return addressMapper.toDtos(addresses);
    }

    
    @Transactional
    public AddressDTO addAddressToClient(Long clientId, AddressDTO addressDTO) {
        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new NotFoundException("Client not found with id: " + clientId));

        // Create the address
        Address address = addressMapper.toModel(addressDTO);
        address.setArchived(false);
        Address savedAddress = addressRepository.save(address);

        // Link to client
        client.getAddresses().add(savedAddress);
        clientRepository.save(client);

        log.info("Added address {} to client {}", savedAddress.getId(), clientId);
        return addressMapper.toDto(savedAddress);
    }

    
    @Transactional
    public void linkAddressToClient(Long clientId, Long addressId) {
        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new NotFoundException("Client not found with id: " + clientId));

        Address address = addressRepository.findByIdAndArchivedFalse(addressId)
                .orElseThrow(() -> new NotFoundException("Address not found with id: " + addressId));

        // Check if already linked
        if (client.getAddresses().stream().anyMatch(a -> a.getId().equals(addressId))) {
            log.debug("Address {} is already linked to client {}", addressId, clientId);
            return;
        }

        client.getAddresses().add(address);
        clientRepository.save(client);
        log.info("Linked address {} to client {}", addressId, clientId);
    }

    
    @Transactional
    public void removeAddressFromClient(Long clientId, Long addressId) {
        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new NotFoundException("Client not found with id: " + clientId));

        boolean removed = client.getAddresses().removeIf(a -> a.getId().equals(addressId));
        if (!removed) {
            throw new NotFoundException("Address " + addressId + " is not linked to client " + clientId);
        }

        clientRepository.save(client);
        log.info("Removed address {} from client {}", addressId, clientId);
    }

    // ========================================================================
    // Soft delete (archive) operations
    // ========================================================================

    
    @Override
    @Transactional
    public void delete(Long id) {
        Address address = addressRepository.findByIdAndArchivedFalse(id)
                .orElseThrow(() -> new NotFoundException("Address not found with id: " + id));

        address.setArchived(true);
        addressRepository.save(address);
        log.info("Archived address {}", id);
    }

    
    @Transactional
    public AddressDTO restore(Long id) {
        Address address = addressRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Address not found with id: " + id));

        if (!Boolean.TRUE.equals(address.getArchived())) {
            log.debug("Address {} is not archived", id);
            return addressMapper.toDto(address);
        }

        address.setArchived(false);
        Address restored = addressRepository.save(address);
        log.info("Restored address {}", id);
        return addressMapper.toDto(restored);
    }

    // ========================================================================
    // Search and query methods
    // ========================================================================

    
    @Transactional(readOnly = true)
    public List<AddressDTO> searchByFormattedAddress(String query) {
        List<Address> addresses = addressRepository.findByFormattedAddressContainingIgnoreCaseAndArchivedFalse(query);
        return addressMapper.toDtos(addresses);
    }


    @Transactional(readOnly = true)
    public List<AddressDTO> findByBoundingBox(Double minLat, Double maxLat, Double minLon, Double maxLon) {
        List<Address> addresses = addressRepository.findByBoundingBox(
                BigDecimal.valueOf(minLat), 
                BigDecimal.valueOf(maxLat), 
                BigDecimal.valueOf(minLon), 
                BigDecimal.valueOf(maxLon));
        return addressMapper.toDtos(addresses);
    }

    
    @Transactional(readOnly = true)
    public List<AddressDTO> findByIds(List<Long> ids) {
        List<Address> addresses = addressRepository.findByIdInAndArchivedFalse(ids);
        return addressMapper.toDtos(addresses);
    }

    // ========================================================================
    // Validation hooks
    // ========================================================================

    @Override
    protected void beforePersist(Address entity, AddressDTO dto, boolean isNew) {
        if (isNew && entity.getArchived() == null) {
            entity.setArchived(false);
        }
        if (entity.getLatitude() != null) {
            if (entity.getLatitude().compareTo(BigDecimal.valueOf(-90)) < 0 || entity.getLatitude().compareTo(BigDecimal.valueOf(90)) > 0) {
                throw new IllegalArgumentException("Latitude must be between -90 and 90");
            }
        }
        if (entity.getLongitude() != null) {
            if (entity.getLongitude().compareTo(BigDecimal.valueOf(-180)) < 0 || entity.getLongitude().compareTo(BigDecimal.valueOf(180)) > 0) {
                throw new IllegalArgumentException("Longitude must be between -180 and 180");
            }
        }
    }
}
