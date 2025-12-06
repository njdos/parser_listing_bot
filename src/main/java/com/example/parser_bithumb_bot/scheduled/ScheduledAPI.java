package com.example.parser_bithumb_bot.scheduled;

import com.example.parser_bithumb_bot.app.checkers.BithumbChecker;
import com.example.parser_bithumb_bot.app.checkers.UpbitChecker;
import com.example.parser_bithumb_bot.providers.gate.GateAPI;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

@Service
public class ScheduledAPI {

    @Autowired
    private BithumbChecker bithumbChecker;

    @Autowired
    private UpbitChecker upbitChecker;

    @Autowired
    private GateAPI gateAPI;

//    @Scheduled(fixedDelay = 30000)
//    public void runChecksBithumbChecker() {
//        bithumbChecker.check();
//    }
//
//    @Scheduled(fixedDelay = 30000)
//    public void runChecksUpbitChecker() {
//        upbitChecker.check();
//    }

    @Scheduled(fixedDelay = 30000)
    public void runPlaceOrder() {
        gateAPI.placePosition(Set.of("BOB"), true);
    }

}
