package com.example.parser_bithumb_bot.providers.bithumb;

import com.example.parser_bithumb_bot.providers.bithumb.service.BithumbService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class BithumbAPI {

    @Autowired
    private BithumbService bithumbService;

    public Set<String> fetchPairs() throws Exception {
        return bithumbService.fetchPairs();
    }
}
