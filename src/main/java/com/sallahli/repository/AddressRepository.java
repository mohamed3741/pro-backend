package com.sallahli.repository;

import com.sallahli.model.Address;
import com.sallahli.repository.generic.GenericRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

import java.math.BigDecimal;

@Repository
public interface AddressRepository extends GenericRepository<Address> {

        
        List<Address> findByArchivedFalse();

        
        List<Address> findByIdInAndArchivedFalse(List<Long> ids);

        
        Optional<Address> findByIdAndArchivedFalse(Long id);

        
        @Query("SELECT a FROM Address a JOIN a.clients c WHERE c.id = :clientId AND a.archived = false")
        List<Address> findByClientIdAndArchivedFalse(@Param("clientId") Long clientId);

        
        List<Address> findByFormattedAddressContainingIgnoreCaseAndArchivedFalse(String formattedAddress);

        
        @Query("SELECT a FROM Address a WHERE a.archived = false " +
                        "AND a.latitude BETWEEN :minLat AND :maxLat " +
                        "AND a.longitude BETWEEN :minLon AND :maxLon")
        List<Address> findByBoundingBox(
                        @Param("minLat") BigDecimal minLat,
                        @Param("maxLat") BigDecimal maxLat,
                        @Param("minLon") BigDecimal minLon,
                        @Param("maxLon") BigDecimal maxLon);

        
        boolean existsByIdAndArchivedFalse(Long id);
}
