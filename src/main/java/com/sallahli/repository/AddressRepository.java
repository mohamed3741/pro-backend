package com.sallahli.repository;

import com.sallahli.model.Address;
import com.sallahli.repository.generic.GenericRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AddressRepository extends GenericRepository<Address> {

    List<Address> findByArchivedFalse();

    List<Address> findByIdInAndArchivedFalse(List<Long> ids);
}

