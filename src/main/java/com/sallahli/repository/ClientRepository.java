package com.sallahli.repository;

import com.sallahli.model.Client;
import com.sallahli.repository.generic.GenericRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ClientRepository extends GenericRepository<Client> {

    Optional<Client> findByTel(String tel);

    Optional<Client> findByCustomerId(String customerId);

    boolean existsByTel(String tel);

    Optional<Client> findByUsername(String username);
}
