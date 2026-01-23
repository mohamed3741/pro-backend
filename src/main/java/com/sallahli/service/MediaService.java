package com.sallahli.service;

import com.sallahli.config.MediaProperties;
import com.sallahli.dto.MediaDTO;
import com.sallahli.exceptions.BadRequestException;
import com.sallahli.exceptions.NotFoundException;
import com.sallahli.mapper.MediaMapper;
import com.sallahli.model.Enum.MediaEnum;
import com.sallahli.model.Enum.MediaStorageType;
import com.sallahli.model.Media;
import com.sallahli.model.util.Helper;
import com.sallahli.repository.MediaRepository;
import jakarta.transaction.Transactional;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Base64;
import org.apache.tika.Tika;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@Transactional
public class MediaService {

    @Autowired
    private MediaRepository mediaRepository;
    @Autowired
    private S3ServicesImpl s3Services;
    @Autowired
    private MediaMapper mapper;
    @Autowired
    private MediaProperties properties;
    @Autowired
    private CDNServicesImpl cdnServices;
    @Autowired
    private Helper helper;

    public MediaDTO save(Media media) {
        return mapper.toDto(mediaRepository.save(media));
    }

    public MediaDTO cleanMediaDelete(Long id, boolean onlyFromS3) {
        Media media = mediaRepository.findById(id).orElseThrow(() -> new NotFoundException("Media not found with id " + id));

        try {
            if (MediaStorageType.CDN.equals(properties.getMediaConfig().getType())) {
                cdnServices.deleteFile(media.getKeyName());
            } else {
                s3Services.deleteFile(media.getKeyName());
            }
        } catch (Exception e) {
            log.info("Cannot Delete File -> Keyname = " + media.getKeyName());
            return mapper.toDto(media);
        }
        if (!onlyFromS3) {
            mediaRepository.delete(media);
        }
        return mapper.toDto(media);
    }

    @SneakyThrows
    public Media create(byte[] bytes, MediaEnum type, Long mediaId) {
        if (bytes == null || bytes.length == 0) {
            throw new BadRequestException("Media file data cannot be null or empty");
        }
        if (type == null) {
            throw new BadRequestException("Media type cannot be null");
        }
        
        String contentType = new Tika().detect(bytes);
        String fileName = helper.generateFileName("").concat(".").concat(contentType.split("/")[1]);
        
        // Add folder prefix if configured to organize images by project
        String folder = properties.getMediaConfig().getFolder();
        String keyName = (folder != null && !folder.trim().isEmpty()) 
            ? folder.trim().replaceAll("^/+|/+$", "") + "/" + fileName  // Remove leading/trailing slashes and add folder prefix
            : fileName;

        ByteArrayResource fileAsResource = new ByteArrayResource(bytes) {
            @Override
            public String getFilename() {
                return keyName;
            }
        };

        if (MediaStorageType.CDN.equals(properties.getMediaConfig().getType())) {
            return cdnServices.uploadFile(keyName, fileAsResource, type, mediaId);
        } else {
            return s3Services.uploadFile(keyName, fileAsResource, type, mediaId);
        }
    }

    public MediaDTO createDto(byte[] file, MediaEnum type, Long mediaId) {
        return mapper.toDto(create(file, type, mediaId));
    }

    public MediaDTO saveMedia(MediaDTO mediaDTO) {
        if (mediaDTO == null) {
            throw new BadRequestException("MediaDTO cannot be null");
        }
        
        if (mediaDTO.getId() == null) {
            // Check if thumbnail field has the base64 encoded data
            String base64Data = mediaDTO.getThumbnail();
            if (base64Data == null || base64Data.trim().isEmpty()) {
                // Try link field as fallback
                base64Data = mediaDTO.getLink();
            }
            
            if (base64Data == null || base64Data.trim().isEmpty()) {
                throw new BadRequestException("Media file data (thumbnail or link) is required and cannot be empty");
            }
            
            if (mediaDTO.getType() == null) {
                throw new BadRequestException("Media type is required");
            }
            
            // Clean base64 data (remove data:image/...;base64, prefix if present)
            String cleanBase64 = base64Data;
            if (cleanBase64.contains(",")) {
                cleanBase64 = cleanBase64.substring(cleanBase64.indexOf(",") + 1);
            }
            
            byte[] mediaBytes = Base64.decodeBase64(cleanBase64);
            if (mediaBytes == null || mediaBytes.length == 0) {
                throw new BadRequestException("Failed to decode base64 media data");
            }
            
            return createDto(mediaBytes, mediaDTO.getType(), mediaDTO.getId());
        } else {
            return mediaDTO;
        }
    }

    public MediaDTO findById(Long id) {
        return mediaRepository.findById(id).map(mapper::toDto).orElseThrow(() -> new NotFoundException("Media not found with id " + id));
    }
}

