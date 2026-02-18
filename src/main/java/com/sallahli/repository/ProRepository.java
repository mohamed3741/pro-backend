package com.sallahli.repository;

import com.sallahli.model.Enum.KycStatus;
import com.sallahli.model.Pro;
import com.sallahli.repository.generic.GenericRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProRepository extends GenericRepository<Pro> {

       Optional<Pro> findByTel(String tel);

       Optional<Pro> findByUsername(String username);

       Optional<Pro> findByEmail(String email);

       List<Pro> findByOnlineTrue();

       List<Pro> findByCategoriesIdAndOnlineTrue(Long categoryId);

       List<Pro> findByKycStatus(KycStatus kycStatus);

       List<Pro> findByArchivedTrue();

       @Query("SELECT p FROM Pro p WHERE p.online = true AND p.isActive = true AND p.kycStatus = 'APPROVED' " +
                     "AND p.walletBalance >= :minBalance ORDER BY p.ratingAvg DESC, p.ratingCount DESC")
       List<Pro> findAvailablePros(@Param("minBalance") Long minBalance);

       @Query("SELECT p FROM Pro p JOIN p.categories c WHERE p.online = true AND p.isActive = true AND p.kycStatus = 'APPROVED' "
                     +
                     "AND c.id = :categoryId AND p.walletBalance >= :minBalance " +
                     "ORDER BY p.ratingAvg DESC, p.ratingCount DESC")
       List<Pro> findAvailableProsByCategory(@Param("categoryId") Long categoryId,
                     @Param("minBalance") Long minBalance);

       @Query("SELECT p FROM Pro p WHERE LOWER(p.firstName) LIKE LOWER(CONCAT('%', :query, '%')) " +
                     "OR LOWER(p.lastName) LIKE LOWER(CONCAT('%', :query, '%')) " +
                     "OR p.tel LIKE CONCAT('%', :query, '%') " +
                     "OR LOWER(p.username) LIKE LOWER(CONCAT('%', :query, '%'))")
       List<Pro> searchByNameOrTel(@Param("query") String query);

       @Query("SELECT COUNT(p) FROM Pro p WHERE p.kycStatus = 'APPROVED' AND p.isActive = true")
       Long countApprovedActivePros();

       @Query("SELECT AVG(p.ratingAvg) FROM Pro p WHERE p.ratingCount > 0")
       Double getAverageRating();
}
