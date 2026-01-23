package com.pro.repository;

import com.pro.model.Enum.OsType;
import com.pro.model.Enum.ProfileType;
import com.pro.model.UserDevice;
import com.pro.repository.generic.GenericRepository;
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
