package com.richard.paylite.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Formatter;

@Component
public class SignatureUtil {

    @Value("${paylite.security.webhook-secret}")
    private String secret;

    private static final String HMAC_SHA256 = "HmacSHA256";

    public String calculateHmacSha256(String payload) {
        byte[] hmacSha256;
        try {
            Mac mac = Mac.getInstance(HMAC_SHA256);
            SecretKeySpec secretKeySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_SHA256);
            mac.init(secretKeySpec);
            hmacSha256 = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new RuntimeException("Failed to calculate HMAC-SHA256", e);
        }
        return toHexString(hmacSha256);
    }

    private String toHexString(byte[] bytes) {
        Formatter formatter = new Formatter();
        for (byte b : bytes) {
            formatter.format("%02x", b);
        }
        return formatter.toString();
    }

    public boolean isValidSignature(String signature, String payload) {
        String calculatedSignature = calculateHmacSha256(payload);
        return calculatedSignature.equals(signature);
    }
}
