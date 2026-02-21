package com.sallahli.repository;

import com.sallahli.model.CustomerRequest;
import com.sallahli.model.Enum.RequestStatus;
import com.sallahli.repository.generic.GenericRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface CustomerRequestRepository extends GenericRepository<CustomerRequest> {

    List<CustomerRequest> findByStatus(RequestStatus status);

    List<CustomerRequest> findByCategoryIdAndStatus(Long categoryId, RequestStatus status);

    List<CustomerRequest> findByClientIdOrderByCreatedAtDesc(Long clientId);

    @Query("SELECT r FROM CustomerRequest r WHERE r.status = 'BROADCASTED' AND r.expiresAt > :now")
    List<CustomerRequest> findActiveBroadcastedRequests(@Param("now") LocalDateTime now);

    @Query("SELECT r FROM CustomerRequest r WHERE r.status = 'BROADCASTED' AND r.createdAt < :cutoffTime")
    List<CustomerRequest> findExpiredBroadcastedRequests(@Param("cutoffTime") LocalDateTime cutoffTime);

    @Query("SELECT COUNT(r) FROM CustomerRequest r WHERE r.status = 'DONE' AND r.createdAt BETWEEN :startDate AND :endDate")
    Long countCompletedRequestsInDateRange(@Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    // Geospatial query to find requests near a location (simplified - you might
    // want to use PostGIS for real geospatial)
    @Query("SELECT r FROM CustomerRequest r WHERE " +
            "r.latitude BETWEEN :minLat AND :maxLat AND " +
            "r.longitude BETWEEN :minLng AND :maxLng AND " +
            "r.status = 'BROADCASTED'")
    List<CustomerRequest> findRequestsInBoundingBox(
            @Param("minLat") Double minLat,
            @Param("maxLat") Double maxLat,
            @Param("minLng") Double minLng,
            @Param("maxLng") Double maxLng);

    Long countByStatus(RequestStatus status);

    @Query("SELECT r FROM CustomerRequest r ORDER BY r.createdAt DESC")
    List<CustomerRequest> findRecentRequests(Pageable pageable);
}
