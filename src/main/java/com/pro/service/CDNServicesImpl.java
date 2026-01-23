package com.pro.service;

import com.pro.config.MediaProperties;
import com.pro.model.Enum.MediaEnum;
import com.pro.model.Media;
import com.pro.model.util.CDNMedia;
import com.pro.repository.MediaRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.matrixlab.webp4j.WebPCodec;
import lombok.extern.slf4j.Slf4j;
import org.imgscalr.Scalr;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

@Service
@Slf4j
public class CDNServicesImpl {

    @Autowired
    private MediaRepository mediaRepository;
    @Autowired
    private MediaProperties properties;

    public Media uploadFile(String keyName, ByteArrayResource bytes, MediaEnum type, Long mediaId) throws IOException {
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        RestTemplate restTemplate = new RestTemplate();

        // First upload the original as-is
        body.add("file", bytes);
        HttpHeaders requestHeaders = new HttpHeaders();
        requestHeaders.setContentType(MediaType.MULTIPART_FORM_DATA);

        String token = properties.getMediaConfig().getToken().trim();
        requestHeaders.setBearerAuth(token);

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, requestHeaders);
        ResponseEntity<String> response = restTemplate.exchange(
                properties.getMediaConfig().getUrl(),
                HttpMethod.POST,
                requestEntity,
                String.class
        );

        CDNMedia cdnMedia = new ObjectMapper().readValue(response.getBody(), CDNMedia.class);

        String thumbnail = null;
        String link = null;
        if (cdnMedia != null && cdnMedia.getResult() != null && cdnMedia.getResult().getVariants() != null) {
            if (cdnMedia.getResult().getVariants().size() > 0) {
                thumbnail = cdnMedia.getResult().getVariants().get(0);
            }
            if (cdnMedia.getResult().getVariants().size() > 1) {
                link = cdnMedia.getResult().getVariants().get(1);
            }
        }

        return mediaRepository.save(Media.builder()
                .id(mediaId)
                .type(type)
                .thumbnail(thumbnail)
                .link(link)
                .keyName(keyName)
                .build());
    }

    /**
     * Process and upload optimized image version based on image type
     *
     * @return
     */
    private CDNMedia optimizeAndUploadImage(String keyName, ByteArrayResource originalBytes, MediaEnum type) {
        try {
            // Read original image
            BufferedImage originalImage = ImageIO.read(new ByteArrayInputStream(originalBytes.getByteArray()));

            // Define target dimensions based on image type
            ImageDimensions targetDimensions = getTargetDimensions(type);

            // Resize image
            BufferedImage resizedImage = Scalr.resize(
                    originalImage,
                    Scalr.Method.QUALITY,
                    Scalr.Mode.AUTOMATIC,
                    targetDimensions.width,
                    targetDimensions.height,
                    Scalr.OP_ANTIALIAS
            );

            // Convert to WebP with compression
            byte[] optimizedBytes = convertToWebP(resizedImage, targetDimensions.quality);

            // Create optimized resource
            ByteArrayResource optimizedResource = new ByteArrayResource(optimizedBytes) {
                @Override
                public String getFilename() {
                    return keyName + "_opt.webp";
                }
            };

            // Upload optimized version
            MultiValueMap<String, Object> optimizedBody = new LinkedMultiValueMap<>();
            optimizedBody.add("file", optimizedResource);

            HttpHeaders optimizedHeaders = new HttpHeaders();
            optimizedHeaders.setContentType(MediaType.MULTIPART_FORM_DATA);
            optimizedHeaders.add("Authorization", "Bearer " + properties.getMediaConfig().getToken());

            HttpEntity<MultiValueMap<String, Object>> optimizedEntity =
                    new HttpEntity<>(optimizedBody, optimizedHeaders);

            RestTemplate restTemplate = new RestTemplate();
            ResponseEntity<String> response = restTemplate.exchange(
                    properties.getMediaConfig().getUrl(),
                    HttpMethod.POST,
                    optimizedEntity,
                    String.class
            );

            return new ObjectMapper().readValue(response.getBody(), CDNMedia.class);

        } catch (Exception e) {
            // Log error but don't fail the main upload
            log.error("Image optimization failed: " + e.getMessage(), e);
            return null;
        }
    }

    /**
     * Get target dimensions and quality based on image type
     */
    private ImageDimensions getTargetDimensions(MediaEnum type) {
        return switch (type) {
            case LOGO -> new ImageDimensions(512, 512, 0.75f); // Square logos
            case PRODUCT -> new ImageDimensions(800, 800, 0.7f); // Square product images
            case IMAGE -> new ImageDimensions(1200, 800, 0.8f); // Landscape images
            case CATEGORY -> new ImageDimensions(800, 800, 0.7f); // Square category images
            case ITEM -> new ImageDimensions(800, 800, 0.7f); // Square item images
            case CONFIG -> new ImageDimensions(1024, 1024, 0.8f); // Config images
            default -> new ImageDimensions(1024, 1024, 0.8f); // Default size
        };
    }

    /**
     * Convert BufferedImage to WebP format
     *
     * @return
     */
    public byte[] convertToWebP(BufferedImage image, float quality) throws IOException {
        // Encode the BufferedImage into WebP byte array
        byte[] encodedWebP = WebPCodec.encodeImage(image, quality);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        // Write the encoded bytes to the output file
        outputStream.write(encodedWebP);

        // Close the stream to release system resources
        outputStream.close();

        return encodedWebP;
    }

    /**
     * Helper class to store image dimensions and quality
     */
    private record ImageDimensions(int width, int height, float quality) {
    }

    public void deleteFile(String keyName) {
        // TODO: Implement CDN file deletion if needed
        log.info("Delete file from CDN not implemented yet for keyName: {}", keyName);
    }
}

