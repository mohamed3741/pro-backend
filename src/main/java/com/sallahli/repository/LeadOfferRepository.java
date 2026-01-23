package com.sallahli.repository;

import com.sallahli.model.Enum.LeadOfferStatus;
import com.sallahli.model.LeadOffer;
import com.sallahli.repository.generic.GenericRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface LeadOfferRepository extends GenericRepository<LeadOffer> {

    List<LeadOffer> findByRequestId(Long requestId);

    List<LeadOffer> findByProIdAndStatus(Long proId, LeadOfferStatus status);

    Optional<LeadOffer> findByRequestIdAndProId(Long requestId, Long proId);

    List<LeadOffer> findByStatusAndExpiresAtBefore(LeadOfferStatus status, LocalDateTime now);

    @Query("SELECT lo FROM LeadOffer lo WHERE lo.status = 'OFFERED' AND lo.expiresAt < :now")
    List<LeadOffer> findExpiredOffers(@Param("now") LocalDateTime now);

    @Query("SELECT COUNT(lo) FROM LeadOffer lo WHERE lo.pro.id = :proId AND lo.status = 'ACCEPTED' AND lo.createdAt BETWEEN :startDate AND :endDate")
    Long countAcceptedOffersByProInDateRange(@Param("proId") Long proId, @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    @Query("SELECT AVG(lo.priceMru) FROM LeadOffer lo WHERE lo.status = 'ACCEPTED' AND lo.createdAt BETWEEN :startDate AND :endDate")
    Double getAverageAcceptedPriceInDateRange(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
}

