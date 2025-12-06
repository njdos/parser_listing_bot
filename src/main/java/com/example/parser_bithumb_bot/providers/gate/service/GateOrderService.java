package com.example.parser_bithumb_bot.providers.gate.service;

import com.example.parser_bithumb_bot.providers.gate.dto.GateOrderRequest;
import com.example.parser_bithumb_bot.providers.gate.utils.GateRequestUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class GateOrderService {

    @Value("${gate.api.key:}")
    private String API_KEY;

    @Value("${gate.api.secret:}")
    private String SECRET_KEY;

    private static final String GATE_URL = "https://fx-api.gateio.ws/api/v4/futures/usdt/";
    private static final String ORDER_URL = GATE_URL + "orders";
    private static final String POS_LEVERAGE_PATH = "/api/v4/futures/usdt/positions/%s/leverage";

    private final ObjectMapper objectMapper = new ObjectMapper();

    public String setLeverage(String contract, int leverage) throws Exception {
        String path = String.format(POS_LEVERAGE_PATH, contract);
        String query = "leverage=" + leverage;
        String body = "{}";
        String bodyHash = GateRequestUtils.sha512Hex(body);
        String timestamp = String.valueOf(System.currentTimeMillis() / 1000L);

        String preSign = "POST\n" + path + "\n" + query + "\n" + bodyHash + "\n" + timestamp;
        String sign = GateRequestUtils.hmacSHA512(preSign, SECRET_KEY);

        String url = GATE_URL + "positions/" + contract + "/leverage?" + query;
        return GateRequestUtils.postJson(url, body, API_KEY, sign, timestamp);
    }

    public String placeOrder(GateOrderRequest order) throws Exception {
        String body = objectMapper.writeValueAsString(order);
        String bodyHash = GateRequestUtils.sha512Hex(body);
        String timestamp = String.valueOf(System.currentTimeMillis() / 1000L);
        String preSign = "POST\n/api/v4/futures/usdt/orders\n\n" + bodyHash + "\n" + timestamp;
        String sign = GateRequestUtils.hmacSHA512(preSign, SECRET_KEY);
        return GateRequestUtils.postJson(ORDER_URL, body, API_KEY, sign, timestamp);
    }

    public long calculateContracts(double usdtAmount, int leverage, double entryPrice,
                                   int quantoMultiplier, double step, double minSize) {

        double positionValue = usdtAmount * leverage;
        double rawContracts = positionValue / (entryPrice * quantoMultiplier);

        if (step <= 0) step = 1;
        double floored = Math.floor(rawContracts / step) * step;

        if (floored < minSize) {
            return 0L;
        }
        return (long) floored;
    }
}
