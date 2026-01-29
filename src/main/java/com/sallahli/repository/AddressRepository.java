package com.sallahli.repository;

import com.sallahli.model.Address;
import com.sallahli.repository.generic.GenericRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AddressRepository extends GenericRepository<Address> {

    /**
     * Find all non-archived addresses.
     */
    List<Address> findByArchivedFalse();

    /**
     * Find addresses by list of IDs that are not archived.
     */
    List<Address> findByIdInAndArchivedFalse(List<Long> ids);

    /**
     * Find a single address by ID that is not archived.
     */
    Optional<Address> findByIdAndArchivedFalse(Long id);

    /**
     * Find all addresses for a specific client (non-archived).
     */
    @Query("SELECT a FROM Address a JOIN a.clients c WHERE c.id = :clientId AND a.archived = false")
    List<Address> findByClientIdAndArchivedFalse(@Param("clientId") Long clientId);

    /**
     * Find all addresses by formatted address pattern (for search/autocomplete).
     */
    List<Address> findByFormattedAddressContainingIgnoreCaseAndArchivedFalse(String formattedAddress);

    /**
     * Find addresses within a geographic bounding box.
     */
    @Query("SELECT a FROM Address a WHERE a.archived = false " +
            "AND a.latitude BETWEEN :minLat AND :maxLat " +
            "AND a.longitude BETWEEN :minLon AND :maxLon")
    List<Address> findByBoundingBox(
            @Param("minLat") Double minLat,
            @Param("maxLat") Double maxLat,
            @Param("minLon") Double minLon,
            @Param("maxLon") Double maxLon);

    /**
     * Check if address exists and is not archived.
     */
    boolean existsByIdAndArchivedFalse(Long id);
}
