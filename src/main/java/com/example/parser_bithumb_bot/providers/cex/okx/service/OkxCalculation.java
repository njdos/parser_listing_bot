package com.example.parser_bithumb_bot.providers.cex.okx.service;

import com.example.parser_bithumb_bot.providers.cex.okx.dto.Instrument;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Slf4j
@Service
public class OkxCalculation {

    private final ObjectMapper objectMapper = new ObjectMapper();

    public Instrument extractInstrument(String response) {
        try {
            JsonNode jsonNode = objectMapper.readTree(response);
            var node = jsonNode.path("data").get(0);
            if (node == null) {
                log.info("Data is empty: {}", jsonNode.get("msg"));
                return null;
            }

            Instrument instrument = new Instrument();

            instrument.setCtVal(new BigDecimal(node.path("ctVal").asText()));
            instrument.setMinSz(new BigDecimal(node.path("minSz").asText()));
            instrument.setMaxMktSz(new BigDecimal(node.path("maxMktSz").asText()));
            instrument.setLotSz(new BigDecimal(node.path("lotSz").asText()));

            String longQuota = node.path("longPosRemainingQuota").asText(null);
            String shortQuota = node.path("shortPosRemainingQuota").asText(null);
            if (longQuota != null && !longQuota.isBlank()) {
                instrument.setLongQuota(new BigDecimal(longQuota));
            }
            if (shortQuota != null && !shortQuota.isBlank()) {
                instrument.setShortQuota(new BigDecimal(shortQuota));
            }

            instrument.setMaxLeverage(new BigDecimal(Integer.parseInt(node.path("lever").asText("1"))));

            return instrument;

        } catch (Exception e) {
            throw new RuntimeException("Failed to parse instrument", e);
        }
    }

    public BigDecimal calculateSz(BigDecimal usdt, BigDecimal price, BigDecimal ctVal, int leverage) {

        BigDecimal sz = usdt
                .multiply(BigDecimal.valueOf(leverage))
                .divide(price.multiply(ctVal), 8, RoundingMode.DOWN);

        return sz;
    }

    public BigDecimal normalizeSz(BigDecimal sz, Instrument inst) {

        if (sz.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        // округлення до lotSz
        sz = sz.divide(inst.getLotSz(), 0, RoundingMode.DOWN)
                .multiply(inst.getLotSz());

        // min
        if (sz.compareTo(inst.getMinSz()) < 0) {
            return BigDecimal.ZERO;
        }

        // max market
        if (sz.compareTo(inst.getMaxMktSz()) > 0) {
            sz = inst.getMaxMktSz();
        }

        // long quota
        if (inst.getLongQuota() != null) {
            sz = sz.min(inst.getLongQuota());
        }

        // short quota
        if (inst.getShortQuota() != null) {
            sz = sz.min(inst.getShortQuota());
        }

        return sz;
    }

    public int normalizeLeverage(int desiredLeverage, Instrument inst) {
        BigDecimal lev = BigDecimal.valueOf(desiredLeverage);
        BigDecimal max = inst.getMaxLeverage();

        if (lev.compareTo(max) > 0) {
            return max.intValue();
        }

        return desiredLeverage;
    }

}
