package com.example.parser_bithumb_bot.providers.gate.service;

import com.example.parser_bithumb_bot.providers.gate.dto.GateOrderRequest;
import com.example.parser_bithumb_bot.providers.gate.utils.GateRequestUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

@Service
public class GateOrderService {

    private static final String API_KEY = "4b6f18f4cf9e430f181d2e9b5626e726";
    private static final String SECRET_KEY = "044fa044a5e3f0e5b60906678854bdb93c31509452866d35126a611e1a064308";

    private static final String GATE_URL = "https://fx-api.gateio.ws/api/v4/futures/usdt/";
    private static final String ORDER_URL = GATE_URL + "orders";
    private static final String SET_MARGIN_URL = GATE_URL + "positions/";

    private final ObjectMapper objectMapper = new ObjectMapper();

    public void setLeverage(String contract, int leverage) throws Exception {
        String path = "/api/v4/futures/usdt/positions/" + contract + "/leverage";
        String query = "leverage=" + leverage;
        String body = "{}";
        String bodyHash = GateRequestUtils.sha512Hex(body);
        String timestamp = String.valueOf(System.currentTimeMillis() / 1000L);

        String preSign = "POST\n" + path + "\n" + query + "\n" + bodyHash + "\n" + timestamp;
        String sign = GateRequestUtils.hmacSHA512(preSign, SECRET_KEY);

        String url = SET_MARGIN_URL + contract + "/leverage?" + query;

        String resp = GateRequestUtils.post(url, body, API_KEY, sign, timestamp);

        System.out.println(">>> Leverage set response: " + resp);
    }

    public String placeOrder(GateOrderRequest order) throws Exception {
        String body = objectMapper.writeValueAsString(order);
        String bodyHash = GateRequestUtils.sha512Hex(body);
        String timestamp = String.valueOf(System.currentTimeMillis() / 1000L);
        String method = "POST";
        String path = "/api/v4/futures/usdt/orders";

        String preSign = method + "\n" + path + "\n\n" + bodyHash + "\n" + timestamp;
        String sign = GateRequestUtils.hmacSHA512(preSign, SECRET_KEY);

        return GateRequestUtils.post(ORDER_URL, body, API_KEY, sign, timestamp);
    }

}
