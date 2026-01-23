package com.pro.repository;

import com.pro.model.LeadAcceptance;
import com.pro.repository.generic.GenericRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface LeadAcceptanceRepository extends GenericRepository<LeadAcceptance> {

    Optional<LeadAcceptance> findByRequestId(Long requestId);

    Optional<LeadAcceptance> findByLeadOfferId(Long leadOfferId);

    boolean existsByRequestId(Long requestId);
}
