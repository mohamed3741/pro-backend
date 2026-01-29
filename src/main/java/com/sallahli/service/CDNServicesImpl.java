package com.sallahli.service;

import com.sallahli.config.MediaProperties;
import com.sallahli.model.Enum.MediaEnum;
import com.sallahli.model.Media;
import com.sallahli.model.util.CDNMedia;
import com.sallahli.repository.MediaRepository;
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
            if (!cdnMedia.getResult().getVariants().isEmpty()) {
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

    
    private record ImageDimensions(int width, int height, float quality) {
    }

    
    public void deleteFile(String keyName) {
        if (keyName == null || keyName.trim().isEmpty()) {
            log.warn("Cannot delete file: keyName is null or empty");
            return;
        }

        try {
            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders requestHeaders = new HttpHeaders();
            
            // Set Bearer token for authentication (same as upload)
            String token = properties.getMediaConfig().getToken().trim();
            requestHeaders.setBearerAuth(token);
            requestHeaders.setContentType(MediaType.APPLICATION_JSON);

            // Construct delete URL based on upload URL pattern
            String baseUrl = properties.getMediaConfig().getUrl();
            String deleteUrl = buildDeleteUrl(baseUrl, keyName);

            // Prepare request body with keyName
            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("keyName", keyName);

            HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<>(body, requestHeaders);

            // Try DELETE method first (most common for file deletion APIs)
            try {
                ResponseEntity<String> response = restTemplate.exchange(
                        deleteUrl,
                        HttpMethod.DELETE,
                        requestEntity,
                        String.class
                );

                if (response.getStatusCode().is2xxSuccessful()) {
                    log.info("Successfully deleted file from CDN: {}", keyName);
                    return;
                } else {
                    log.warn("Failed to delete file from CDN using DELETE. Status: {}, keyName: {}", 
                            response.getStatusCode(), keyName);
                }
            } catch (org.springframework.web.client.HttpClientErrorException | 
                     org.springframework.web.client.HttpServerErrorException e) {
                // If DELETE fails with HTTP error, try POST method (some CDN APIs use POST for deletion)
                log.debug("DELETE method failed with status {}, trying POST method: {}", 
                        e.getStatusCode(), e.getMessage());
                
                // Prepare POST request with delete action
                body.clear();
                body.add("keyName", keyName);
                body.add("action", "delete");
                
                HttpEntity<MultiValueMap<String, String>> postRequestEntity = 
                        new HttpEntity<>(body, requestHeaders);
                
                try {
                    ResponseEntity<String> response = restTemplate.exchange(
                            deleteUrl,
                            HttpMethod.POST,
                            postRequestEntity,
                            String.class
                    );

                    if (response.getStatusCode().is2xxSuccessful()) {
                        log.info("Successfully deleted file from CDN using POST: {}", keyName);
                    } else {
                        log.warn("Failed to delete file from CDN using POST. Status: {}, keyName: {}", 
                                response.getStatusCode(), keyName);
                    }
                } catch (Exception postException) {
                    log.error("Both DELETE and POST methods failed for file deletion. keyName: {}, error: {}", 
                            keyName, postException.getMessage());
                }
            }

        } catch (Exception e) {
            // Log error but don't throw - allow the application to continue
            // This matches the behavior in MediaService.cleanMediaDelete which catches exceptions
            log.error("Error deleting file from CDN. keyName: {}, error: {}", keyName, e.getMessage(), e);
        }
    }

    
    private String buildDeleteUrl(String baseUrl, String keyName) {
        if (baseUrl == null || baseUrl.trim().isEmpty()) {
            throw new IllegalArgumentException("Base URL cannot be null or empty");
        }

        // Common CDN API patterns:
        // 1. /upload -> /delete
        // 2. /upload -> /delete/{keyName}
        // 3. Base URL + /delete
        // 4. Base URL + /delete/{keyName}
        
        String deleteUrl = baseUrl.trim();
        
        // Replace /upload with /delete if present
        if (deleteUrl.contains("/upload")) {
            deleteUrl = deleteUrl.replace("/upload", "/delete");
        } else {
            // Append /delete if not present
            if (!deleteUrl.endsWith("/delete") && !deleteUrl.endsWith("/delete/")) {
                deleteUrl = deleteUrl.endsWith("/") ? deleteUrl + "delete" : deleteUrl + "/delete";
            }
        }
        
        // Some APIs require keyName as path parameter: /delete/{keyName}
        // Uncomment the line below if your CDN API requires this pattern:
        // deleteUrl = deleteUrl.endsWith("/") ? deleteUrl + keyName : deleteUrl + "/" + keyName;
        
        return deleteUrl;
    }
}

