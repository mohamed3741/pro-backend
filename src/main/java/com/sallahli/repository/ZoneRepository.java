package com.sallahli.repository;

import com.sallahli.model.Zone;
import com.sallahli.repository.generic.GenericRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ZoneRepository extends GenericRepository<Zone> {

    Optional<Zone> findByName(String name);

    List<Zone> findByActiveTrue();

    List<Zone> findByCity(String city);
}

