package com.pro.repository;

import com.pro.model.Zone;
import com.pro.repository.generic.GenericRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ZoneRepository extends GenericRepository<Zone> {

    Optional<Zone> findByName(String name);

    List<Zone> findByActiveTrue();

    List<Zone> findByCity(String city);
}
