package com.example.parser_bithumb_bot.providers.cex.gate.utils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

public class GateSignatureUtils {

    public static String sign(
            String method,
            String path,
            String query,
            String body,
            long timestamp,
            String secret
    ) {
        try {

            String fullPath = "/api/v4" + path;

            String bodyHash = "";
            if (body != null && !body.isEmpty()) {
                MessageDigest digest = MessageDigest.getInstance("SHA-512");
                byte[] hash = digest.digest(body.getBytes(StandardCharsets.UTF_8));
                bodyHash = bytesToHex(hash);
            }

            String payload = method + "\n" +
                    fullPath + "\n" +
                    (query == null ? "" : query) + "\n" +
                    bodyHash + "\n" +
                    timestamp;

            Mac mac = Mac.getInstance("HmacSHA512");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA512"));

            return bytesToHex(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}