package com.sallahli.repository;

import com.sallahli.model.LeadAcceptance;
import com.sallahli.repository.generic.GenericRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface LeadAcceptanceRepository extends GenericRepository<LeadAcceptance> {

    Optional<LeadAcceptance> findByRequestId(Long requestId);

    Optional<LeadAcceptance> findByLeadOfferId(Long leadOfferId);

    boolean existsByRequestId(Long requestId);
}

