package com.sallahli.repository;

import com.sallahli.model.Admin;
import com.sallahli.model.Enum.AdminRole;
import com.sallahli.repository.generic.GenericRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AdminRepository extends GenericRepository<Admin> {

    Optional<Admin> findByUsername(String username);

    Optional<Admin> findByEmail(String email);

    List<Admin> findByRole(AdminRole role);

    List<Admin> findByIsActiveTrue();

    List<Admin> findByArchivedFalse();

    List<Admin> findByArchivedTrue();

    @Query("SELECT a FROM Admin a WHERE " +
            "LOWER(a.firstName) LIKE LOWER(CONCAT('%', :query, '%')) " +
            "OR LOWER(a.lastName) LIKE LOWER(CONCAT('%', :query, '%')) " +
            "OR LOWER(a.username) LIKE LOWER(CONCAT('%', :query, '%')) " +
            "OR LOWER(a.email) LIKE LOWER(CONCAT('%', :query, '%')) " +
            "OR a.tel LIKE CONCAT('%', :query, '%')")
    List<Admin> searchAdmins(@Param("query") String query);
}
