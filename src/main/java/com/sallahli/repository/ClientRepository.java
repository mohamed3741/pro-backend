package com.sallahli.repository;

import com.sallahli.model.Client;
import com.sallahli.repository.generic.GenericRepository;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface ClientRepository extends GenericRepository<Client> {

    Optional<Client> findByTel(String tel);

    Optional<Client> findByCustomerId(String customerId);

    boolean existsByTel(String tel);

    Client findByUsername(String username);

    Optional<Client> findByUsernameOrTel(String usernameOrTel, String tel);

    Optional<Client> findByEmail(
            @Email(message = "Invalid email format") @Size(max = 255, message = "Email must not exceed 255 characters") String email);

    @Query("SELECT COUNT(c) FROM Client c WHERE c.createdAt >= :startDate")
    Long countNewClientsSince(@Param("startDate") LocalDateTime startDate);
}
