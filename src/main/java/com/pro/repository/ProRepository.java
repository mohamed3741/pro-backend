package com.pro.repository;

import com.pro.model.Enum.KycStatus;
import com.pro.model.Pro;
import com.pro.repository.generic.GenericRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProRepository extends GenericRepository<Pro> {

    Optional<Pro> findByTel(String tel);

    List<Pro> findByOnlineTrue();

    List<Pro> findByTradeIdAndOnlineTrue(Long tradeId);

    List<Pro> findByKycStatus(KycStatus kycStatus);

    @Query("SELECT p FROM Pro p WHERE p.online = true AND p.isActive = true AND p.kycStatus = 'APPROVED' " +
           "AND p.walletBalanceMru >= :minBalance ORDER BY p.ratingAvg DESC, p.ratingCount DESC")
    List<Pro> findAvailablePros(@Param("minBalance") Long minBalance);

    @Query("SELECT p FROM Pro p WHERE p.online = true AND p.isActive = true AND p.kycStatus = 'APPROVED' " +
           "AND p.trade.id = :tradeId AND p.walletBalanceMru >= :minBalance " +
           "ORDER BY p.ratingAvg DESC, p.ratingCount DESC")
    List<Pro> findAvailableProsByTrade(@Param("tradeId") Long tradeId, @Param("minBalance") Long minBalance);

    @Query("SELECT COUNT(p) FROM Pro p WHERE p.kycStatus = 'APPROVED' AND p.isActive = true")
    Long countApprovedActivePros();

    @Query("SELECT AVG(p.ratingAvg) FROM Pro p WHERE p.ratingCount > 0")
    Double getAverageRating();
}
