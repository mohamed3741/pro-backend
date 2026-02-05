package com.sallahli.controller;

import com.sallahli.dto.MediaDTO;
import com.sallahli.service.MediaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.sallahli.model.Enum.MediaEnum;
import org.springframework.http.MediaType;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/media")
@RequiredArgsConstructor
@Tag(name = "Media", description = "Media management APIs")
public class MediaController {

    private final MediaService mediaService;

    @PostMapping(value = "/create", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyAuthority('CLIENT', 'PRO', 'ADMIN')")
    @Operation(summary = "Upload or save media")
    public ResponseEntity<MediaDTO> saveMedia(
            @RequestPart("file") MultipartFile file,
            @RequestParam("type") MediaEnum type) {
        return ResponseEntity.ok(mediaService.saveMedia(file, type));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get media by ID")
    public ResponseEntity<MediaDTO> findById(@PathVariable Long id) {
        return ResponseEntity.ok(mediaService.findById(id));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ADMIN')")
    @Operation(summary = "Delete media")
    public ResponseEntity<MediaDTO> delete(@PathVariable Long id,
            @RequestParam(defaultValue = "false") boolean onlyFromS3) {
        return ResponseEntity.ok(mediaService.cleanMediaDelete(id, onlyFromS3));
    }
}
