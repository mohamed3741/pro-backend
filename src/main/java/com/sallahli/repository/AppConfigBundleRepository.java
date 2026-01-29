package com.sallahli.repository;

import com.sallahli.model.AppConfigBundle;
import com.sallahli.model.Enum.AppType;
import com.sallahli.model.Enum.PlatformType;
import com.sallahli.repository.generic.GenericRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AppConfigBundleRepository extends GenericRepository<AppConfigBundle> {

    @Query("SELECT a FROM AppConfigBundle a WHERE " +
            "a.app = :app AND " +
            "a.platform = :platform AND " +
            "a.active = true AND " +
            "(:country IS NULL OR a.country IS NULL OR a.country = :country) " +
            "ORDER BY " +
            "CASE WHEN a.country = :country THEN 1 ELSE 2 END, " +
            "a.createdAt DESC")
    List<AppConfigBundle> findMatchingConfigs(
            @Param("app") AppType app,
            @Param("platform") PlatformType platform,
            @Param("country") String country
    );


    boolean existsByHashAndActiveTrue(String hash);

    List<AppConfigBundle> findByAppAndPlatformOrderByCreatedAtDesc(AppType app, PlatformType platform);

    List<AppConfigBundle> findAllByOrderByCreatedAtDesc();

    List<AppConfigBundle> findByActiveTrueOrderByCreatedAtDesc();
}

