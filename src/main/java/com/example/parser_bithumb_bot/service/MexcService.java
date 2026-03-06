package com.example.parser_bithumb_bot.service;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.stereotype.Service;

import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

@Service
public class MexcService {

    private static final String API_URL = "https://api.mexc.com/api/v1/contract/detail";

    public List<String> getTokens() {
        List<String> tokens = new ArrayList<>();

        try {
            HttpURLConnection connection = (HttpURLConnection) new URL(API_URL).openConnection();
            connection.setRequestMethod("GET");

            InputStreamReader reader = new InputStreamReader(connection.getInputStream());
            StringBuilder sb = new StringBuilder();
            int c;
            while ((c = reader.read()) != -1) sb.append((char) c);
            reader.close();

            JSONObject response = new JSONObject(sb.toString());
            JSONArray data = response.getJSONArray("data");

            for (int i = 0; i < data.length(); i++) {
                JSONObject token = data.getJSONObject(i);
                tokens.add(token.getString("baseCoinName"));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return tokens;
    }
}