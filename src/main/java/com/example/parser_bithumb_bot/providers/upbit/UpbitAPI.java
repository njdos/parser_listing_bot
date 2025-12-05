package com.example.parser_bithumb_bot.providers.upbit;

import com.example.parser_bithumb_bot.providers.upbit.service.UpbitService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class UpbitAPI {

    @Autowired
    private UpbitService upbitService;

    public Set<String> fetchPairs() throws Exception {
        return upbitService.fetchPairs();
    }
}
