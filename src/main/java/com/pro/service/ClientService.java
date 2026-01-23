package com.pro.service;

import com.pro.dto.sallahli.ClientDTO;
import com.pro.mapper.ClientMapper;
import com.pro.model.Client;
import com.pro.repository.ClientRepository;
import com.pro.service.crud.AbstractCrudService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class ClientService extends AbstractCrudService<Client, ClientDTO> {

    private final ClientRepository clientRepository;
    private final ClientMapper clientMapper;

    public ClientService(ClientRepository repository, ClientMapper mapper) {
        super(repository, mapper);
        this.clientRepository = repository;
        this.clientMapper = mapper;
    }

    public ClientDTO findByTel(String tel) {
        Client client = clientRepository.findByTel(tel)
                .orElseThrow(() -> new com.pro.exceptions.NotFoundException("Client not found with tel: " + tel));
        return clientMapper.toDto(client);
    }

    public ClientDTO findByCustomerId(String customerId) {
        Client client = clientRepository.findByCustomerId(customerId)
                .orElseThrow(() -> new com.pro.exceptions.NotFoundException("Client not found with customerId: " + customerId));
        return clientMapper.toDto(client);
    }

    public boolean existsByTel(String tel) {
        return clientRepository.existsByTel(tel);
    }
}
