package com.pro.service;

import com.pro.dto.sallahli.ZoneDTO;
import com.pro.mapper.ZoneMapper;
import com.pro.model.Zone;
import com.pro.repository.ZoneRepository;
import com.pro.service.crud.AbstractCrudService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class ZoneService extends AbstractCrudService<Zone, ZoneDTO> {

    private final ZoneRepository zoneRepository;
    private final ZoneMapper zoneMapper;

    public ZoneService(ZoneRepository repository, ZoneMapper mapper) {
        super(repository, mapper);
        this.zoneRepository = repository;
        this.zoneMapper = mapper;
    }

    public List<ZoneDTO> getActiveZones() {
        List<Zone> zones = zoneRepository.findByActiveTrue();
        return zoneMapper.toDtos(zones);
    }

    public ZoneDTO findByName(String name) {
        Zone zone = zoneRepository.findByName(name)
                .orElseThrow(() -> new com.pro.exceptions.NotFoundException("Zone not found with name: " + name));
        return zoneMapper.toDto(zone);
    }
}
