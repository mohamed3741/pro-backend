package com.sallahli.repository;

import com.sallahli.model.UserOtpExpiration;
import com.sallahli.repository.generic.GenericRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface UserOtpExpirationRepository extends GenericRepository<UserOtpExpiration> {

    Optional<UserOtpExpiration> findByUsernameAndIsUsedFalse(String username);

    Optional<UserOtpExpiration> findByOtpAndIsUsedFalse(String otp);

    @Query("SELECT u FROM UserOtpExpiration u WHERE u.expirationTime < :now AND u.isUsed = false")
    java.util.List<UserOtpExpiration> findExpiredOtps(@Param("now") LocalDateTime now);

    boolean existsByUsernameAndExpirationTimeAfterAndIsUsedFalse(String username, LocalDateTime time);
}

