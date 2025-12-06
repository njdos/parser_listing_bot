package com.example.parser_bithumb_bot.providers.gate.utils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

public class GateRequestUtils {

    public static String sha512Hex(String data) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-512");
        byte[] digest = md.digest(data.getBytes());
        StringBuilder hex = new StringBuilder();
        for (byte b : digest) {
            String h = Integer.toHexString(0xff & b);
            if (h.length() == 1) hex.append('0');
            hex.append(h);
        }
        return hex.toString();
    }

    public static String hmacSHA512(String data, String key) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA512");
        SecretKeySpec secretKeySpec = new SecretKeySpec(key.getBytes(), "HmacSHA512");
        mac.init(secretKeySpec);
        byte[] hash = mac.doFinal(data.getBytes());
        StringBuilder hex = new StringBuilder();
        for (byte b : hash) {
            String h = Integer.toHexString(0xff & b);
            if (h.length() == 1) hex.append('0');
            hex.append(h);
        }
        return hex.toString();
    }

//    public static String post(String urlStr, String body, String apiKey, String sign, String timestamp) throws Exception {
//        URL url = new URL(urlStr);
//        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
//        conn.setRequestMethod("POST");
//        conn.setRequestProperty("Content-Type", "application/json");
//        conn.setRequestProperty("KEY", apiKey);
//        conn.setRequestProperty("SIGN", sign);
//        conn.setRequestProperty("Timestamp", timestamp);
//        conn.setDoOutput(true);
//
//        OutputStream os = conn.getOutputStream();
//        os.write(body.getBytes());
//        os.close();
//
//        BufferedReader br = new BufferedReader(new InputStreamReader(
//                conn.getResponseCode() == 200 ? conn.getInputStream() : conn.getErrorStream()
//        ));
//
//        StringBuilder sb = new StringBuilder();
//        String line;
//        while ((line = br.readLine()) != null) sb.append(line);
//        br.close();
//        return sb.toString();
//    }

    public static String post(String urlStr, String body, String apiKey, String sign, String timestamp) throws Exception {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("KEY", apiKey);
        conn.setRequestProperty("SIGN", sign);
        conn.setRequestProperty("Timestamp", timestamp);
        conn.setDoOutput(true);

        try (OutputStream os = conn.getOutputStream()) {
            os.write(body.getBytes(StandardCharsets.UTF_8));
        }

        InputStream is;
        if (conn.getResponseCode() >= 400) {
            is = conn.getErrorStream();
        } else {
            is = conn.getInputStream();
        }

        if (is == null) {
            throw new IOException("No response from server");
        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            return sb.toString();
        }
    }


}
