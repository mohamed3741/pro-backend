package com.pro.controller.sallahli;

import com.pro.dto.sallahli.ZoneDTO;
import com.pro.service.ZoneService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/sallahli/zones")
@RequiredArgsConstructor
@Tag(name = "Zones", description = "APIs for managing geographic zones")
public class ZoneController {

    private final ZoneService zoneService;

    @GetMapping
    @Operation(summary = "Get all active zones")
    public ResponseEntity<List<ZoneDTO>> getActiveZones() {
        List<ZoneDTO> zones = zoneService.getActiveZones();
        return ResponseEntity.ok(zones);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get zone by ID")
    public ResponseEntity<ZoneDTO> getZone(@PathVariable Long id) {
        ZoneDTO zone = zoneService.findById(id);
        return ResponseEntity.ok(zone);
    }

    @GetMapping("/by-name/{name}")
    @Operation(summary = "Get zone by name")
    public ResponseEntity<ZoneDTO> getZoneByName(@PathVariable String name) {
        ZoneDTO zone = zoneService.findByName(name);
        return ResponseEntity.ok(zone);
    }
}
