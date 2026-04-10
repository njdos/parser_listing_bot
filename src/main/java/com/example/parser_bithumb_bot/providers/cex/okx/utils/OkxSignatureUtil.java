package com.example.parser_bithumb_bot.providers.cex.okx.utils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class OkxSignatureUtil {

    public static String sign(
            String timestamp, String method, String requestPath, String body, String secretKey) {

        try {
            String preHash = timestamp + method.toUpperCase() + requestPath + body;

            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec =
                    new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256");

            mac.init(secretKeySpec);

            byte[] rawHmac = mac.doFinal(preHash.getBytes(StandardCharsets.UTF_8));

            return Base64.getEncoder().encodeToString(rawHmac);

        } catch (Exception e) {
            throw new RuntimeException("Error generating OKX signature", e);
        }
    }
}