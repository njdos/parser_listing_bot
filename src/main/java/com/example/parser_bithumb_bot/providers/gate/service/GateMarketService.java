package com.example.parser_bithumb_bot.providers.gate.service;

import com.example.parser_bithumb_bot.providers.gate.dto.GateContractInfo;
import com.example.parser_bithumb_bot.providers.gate.dto.GateMarketInfo;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Service
public class GateMarketService {

    private static final String TICKERS_URL = "https://api.gateio.ws/api/v4/futures/usdt/tickers";
    private static final String CONTRACTS_URL = "https://api.gateio.ws/api/v4/futures/usdt/contracts";

    private static final RestTemplate rest = new RestTemplate();
    private static final ObjectMapper mapper = new ObjectMapper();

    public Map<String, GateMarketInfo> getMarketInfo(Set<String> tokens) {
        Map<String, GateMarketInfo> map = new HashMap<>();
        try {
            String resp = rest.getForObject(TICKERS_URL, String.class);
            JsonNode root = mapper.readTree(resp);

            for (JsonNode node : root) {
                String contract = node.get("contract").asText();
                GateMarketInfo info = new GateMarketInfo();
                info.setContract(contract);
                info.setLast(node.path("last").asDouble(0.0));
                info.setMarkPrice(node.path("mark_price").asDouble(info.getLast()));
                info.setIndexPrice(node.path("index_price").asDouble(0.0));
                info.setFundingRateIndicative(node.path("funding_rate_indicative").asDouble(0.0));
                info.setLowestSize(node.path("lowest_size").asInt(1));
                info.setQuantoMultiplier(node.path("quanto_multiplier").asInt(1));
                map.put(contract.split("_")[0].toUpperCase(), info);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return map;
    }

    public Map<String, GateContractInfo> getContractsInfo() {
        Map<String, GateContractInfo> out = new HashMap<>();
        try {
            String resp = rest.getForObject(CONTRACTS_URL, String.class);
            JsonNode root = mapper.readTree(resp);
            for (JsonNode node : root) {
                GateContractInfo c = new GateContractInfo();
                c.setName(node.path("name").asText());
                c.setOrderSizeMin(node.path("order_size_min").asDouble(1));
                c.setOrderSizeStep(node.path("order_size_step").asDouble(1));
                c.setContractMultiplier(node.path("contract_unit").asDouble(1));
                out.put(c.getName().split("_")[0].toUpperCase(), c);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return out;
    }
}
