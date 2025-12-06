package com.example.parser_bithumb_bot.providers.gate.utils;

import org.apache.commons.codec.binary.Hex;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.security.MessageDigest;
import java.util.Collections;

public class GateRequestUtils {

    private static final RestTemplate rest = new RestTemplate();

    public static String sha512Hex(String data) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-512");
        byte[] digest = md.digest(data.getBytes("UTF-8"));
        return Hex.encodeHexString(digest);
    }

    public static String hmacSHA512(String data, String key) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA512");
        SecretKeySpec secretKeySpec = new SecretKeySpec(key.getBytes("UTF-8"), "HmacSHA512");
        mac.init(secretKeySpec);
        byte[] hash = mac.doFinal(data.getBytes("UTF-8"));
        return Hex.encodeHexString(hash);
    }

    public static ResponseEntity<String> postJson(String url, String body, String apiKey, String sign, String timestamp) throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add("KEY", apiKey);
        headers.add("SIGN", sign);
        headers.add("Timestamp", timestamp);
        HttpEntity<String> entity = new HttpEntity<>(body, headers);
        return rest.exchange(url, HttpMethod.POST, entity, String.class);
    }

    public static String get(String url) {
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        HttpEntity<Void> entity = new HttpEntity<>(headers);
        ResponseEntity<String> resp = rest.exchange(url, HttpMethod.GET, entity, String.class);
        return resp.getBody();
    }
}

