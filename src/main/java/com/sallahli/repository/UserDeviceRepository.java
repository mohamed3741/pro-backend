package com.sallahli.repository;

import com.sallahli.model.Enum.OsType;
import com.sallahli.model.Enum.ProfileType;
import com.sallahli.model.UserDevice;
import com.sallahli.repository.generic.GenericRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserDeviceRepository extends GenericRepository<UserDevice> {

    List<UserDevice> findByClientId(Long clientId);

    List<UserDevice> findByProId(Long proId);

    List<UserDevice> findByOsType(OsType osType);

    List<UserDevice> findByProfileType(ProfileType profileType);

    Optional<UserDevice> findByToken(String token);

    void deleteByToken(String token);
}

