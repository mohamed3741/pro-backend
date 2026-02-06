package com.sallahli.service;

import kong.unirest.json.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Collection;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class WhatsappService {

    private static final String INSTANCE_URL = "https://wasenderapi.com/api";
    private static final String SEND_MESSAGE_PATH = "/send-message";
    private static final String UPLOAD_PATH = "/upload";

    private static final String ALT_SEND_BASE = "https://www.wasenderapi.com/api";

    private static final long DEFAULT_BULK_DELAY_SECONDS = 10;

    @Value("${whatsapp.bearer-token}")
    private String bearerToken;

    @Value("${whatsapp.default-country-code:222}")
    private String defaultCountryCode;

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public void sendText(String to, String message) {
        Objects.requireNonNull(to, "to must not be null");
        Objects.requireNonNull(message, "message must not be null");

        String msisdn = normalizeMsisdn(to, defaultCountryCode);

        JSONObject payload = new JSONObject()
                .put("to", msisdn)
                .put("text", message);

        sendJson(INSTANCE_URL + SEND_MESSAGE_PATH, payload, bearerToken);
    }

    public void sendOTP(String code, String phoneNumber) {
        Objects.requireNonNull(code, "code must not be null");
        Objects.requireNonNull(phoneNumber, "phoneNumber must not be null");

        String message = "Your Sallahli verification code is: " + code;
        sendText(phoneNumber, message);
    }

    public void sendTmpPassword(String temporaryPassword, String phoneNumber) {
        Objects.requireNonNull(temporaryPassword, "temporaryPassword must not be null");
        Objects.requireNonNull(phoneNumber, "phoneNumber must not be null");

        String message = "Your Sallahli temporary password is: " + temporaryPassword
                + "\n\nPlease change this password after login.";
        sendText(phoneNumber, message);
    }

    public void sendTextToGroup(String groupJid, String message) {
        sendText(groupJid, message);
    }

    public void sendDocument(String to,
            String base64File,
            String fileName,
            String caption) {
        sendDocumentWithMime(to, base64File, fileName, caption, "application/pdf");
    }

    public void sendDocumentWithMime(String to,
            String base64File,
            String fileName,
            String caption,
            String mimeType) {
        Objects.requireNonNull(to, "to must not be null");
        Objects.requireNonNull(base64File, "base64File must not be null");
        Objects.requireNonNull(fileName, "fileName must not be null");
        Objects.requireNonNull(mimeType, "mimeType must not be null");

        String msisdn = normalizeMsisdn(to, defaultCountryCode);
        String dataUri = buildDataUri(base64File, mimeType);

        String publicUrl = uploadPublicUrl(dataUri);

        String safeFileName = ensureExtension(fileName, mimeType);

        JSONObject payload = new JSONObject()
                .put("to", msisdn)
                .put("text", caption == null ? "" : caption)
                .put("documentUrl", publicUrl)
                .put("fileName", safeFileName);

        sendJson(ALT_SEND_BASE + SEND_MESSAGE_PATH, payload, bearerToken);
    }

    public int sendBulkText(Collection<String> recipients,
            String message,
            Long delaySeconds) {
        if (recipients == null || recipients.isEmpty())
            return 0;
        long sleep = delaySeconds != null ? Math.max(0, delaySeconds) : DEFAULT_BULK_DELAY_SECONDS;

        int success = 0;
        int i = 0;
        for (String to : recipients) {
            try {
                sendText(to, message);
                success++;
            } catch (Exception ex) {
                log.warn("WhatsApp text failed for {}: {}", to, ex.toString());
            }
            if (++i < recipients.size() && sleep > 0)
                safeSleepSeconds(sleep);
        }
        return success;
    }

    public int sendBulkDocument(Collection<String> recipients,
            String base64File,
            String fileName,
            String caption,
            Long delaySeconds) {
        if (recipients == null || recipients.isEmpty())
            return 0;
        long sleep = delaySeconds != null ? Math.max(0, delaySeconds) : DEFAULT_BULK_DELAY_SECONDS;

        int success = 0;
        int i = 0;
        for (String to : recipients) {
            try {
                sendDocument(to, base64File, fileName, caption);
                success++;
            } catch (Exception ex) {
                log.warn("WhatsApp doc failed for {}: {}", to, ex.toString());
            }
            if (++i < recipients.size() && sleep > 0)
                safeSleepSeconds(sleep);
        }
        return success;
    }

    @Async
    public void sendBulkTextAsync(Collection<String> recipients,
            String message,
            Long delaySeconds) {
        sendBulkText(recipients, message, delaySeconds);
    }

    @Async
    public void sendBulkDocumentAsync(Collection<String> recipients,
            String base64File,
            String fileName,
            String caption,
            Long delaySeconds) {
        sendBulkDocument(recipients, base64File, fileName, caption, delaySeconds);
    }

    public String uploadPublicUrl(String dataUriBase64) {
        Objects.requireNonNull(dataUriBase64, "dataUriBase64 must not be null");

        JSONObject payload = new JSONObject().put("base64", dataUriBase64);

        HttpRequest req = HttpRequest.newBuilder()
                .uri(java.net.URI.create(INSTANCE_URL + UPLOAD_PATH))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + bearerToken)
                .POST(HttpRequest.BodyPublishers.ofString(payload.toString(), StandardCharsets.UTF_8))
                .build();

        try {
            HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (res.statusCode() != 200 && res.statusCode() != 201) {
                throw new WhatsappApiException("Upload failed: " + res.statusCode() + " " + res.body());
            }
            return new JSONObject(res.body()).getString("publicUrl");
        } catch (Exception e) {
            throw new WhatsappApiException("Upload failed: " + e.getMessage(), e);
        }
    }

    public String normalizeMsisdn(String raw, String defaultCountryCode) {
        if (raw == null || raw.isBlank())
            return raw;
        String r = raw.trim();

        r = r.replaceAll("[\\s\\-()]", "");

        if (r.startsWith("+"))
            r = r.substring(1);

        if (r.startsWith("00"))
            r = r.substring(2);

        if (r.startsWith("0") && defaultCountryCode != null && !defaultCountryCode.isBlank()) {
            r = defaultCountryCode + r.substring(1);
        }

        if (defaultCountryCode != null
                && !defaultCountryCode.isBlank()
                && !r.startsWith(defaultCountryCode)) {
            if (r.length() >= 7 && r.length() <= 10) {
                r = defaultCountryCode + r;
            }
        }
        return r;
    }

    public String buildDataUri(String base64Content, String mimeType) {
        String clean = base64Content.startsWith("data:")
                ? base64Content.substring(base64Content.indexOf(",") + 1)
                : base64Content;
        return "data:" + mimeType + ";base64," + clean;
    }

    private String ensureExtension(String fileName, String mimeType) {
        String fn = fileName;
        if (!fn.contains(".")) {
            String ext = switch (mimeType) {
                case "application/pdf" -> "pdf";
                case "image/png" -> "png";
                case "image/jpeg" -> "jpg";
                case "application/vnd.ms-excel" -> "xls";
                case "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" -> "xlsx";
                case "application/msword" -> "doc";
                case "application/vnd.openxmlformats-officedocument.wordprocessingml.document" -> "docx";
                default -> "bin";
            };
            fn = fn + "." + ext;
        }
        return fn;
    }

    private void sendJson(String url, JSONObject payload, String bearerToken) {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(java.net.URI.create(url))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + bearerToken)
                .POST(HttpRequest.BodyPublishers.ofString(payload.toString(), StandardCharsets.UTF_8))
                .build();

        try {
            HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (res.statusCode() != 200 && res.statusCode() != 201) {
                throw new WhatsappApiException("WhatsApp API failed. Code: "
                        + res.statusCode() + ", Response: " + res.body());
            }
        } catch (Exception e) {
            throw new WhatsappApiException("HTTP error calling WhatsApp API: " + e.getMessage(), e);
        }
    }

    private void safeSleepSeconds(long s) {
        try {
            TimeUnit.SECONDS.sleep(s);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }

    public static class WhatsappApiException extends RuntimeException {
        public WhatsappApiException(String message) {
            super(message);
        }

        public WhatsappApiException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
