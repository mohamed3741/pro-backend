package com.pro.service;

import com.pro.model.Enum.MediaEnum;
import com.pro.model.Media;
import com.pro.repository.MediaRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class S3ServicesImpl {

    @Autowired
    private MediaRepository mediaRepository;

    public Media uploadFile(String keyName, ByteArrayResource bytes, MediaEnum type, Long mediaId) {
        // TODO: Implement S3 upload logic
        log.warn("S3 upload not implemented yet for keyName: {}", keyName);
        return Media.builder()
                .id(mediaId)
                .type(type)
                .keyName(keyName)
                .build();
    }

    public void deleteFile(String keyName) {
        // TODO: Implement S3 delete logic
        log.info("Delete file from S3 not implemented yet for keyName: {}", keyName);
    }
}


