package com.pro.repository;

import com.pro.model.Client;
import com.pro.repository.generic.GenericRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ClientRepository extends GenericRepository<Client> {

    Optional<Client> findByTel(String tel);

    Optional<Client> findByCustomerId(String customerId);

    boolean existsByTel(String tel);
}
