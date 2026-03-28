package com.example.parser_bithumb_bot.scheduled;

import com.example.parser_bithumb_bot.app.checkers.BithumbChecker;
import com.example.parser_bithumb_bot.app.checkers.UpbitChecker;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class ScheduledAPI {

    @Autowired
    private BithumbChecker bithumbChecker;

    @Autowired
    private UpbitChecker upbitChecker;

//    @Scheduled(fixedDelay = 30000)
    public void runChecksBithumbChecker() {
        bithumbChecker.check();
    }

//    @Scheduled(fixedDelay = 30000)
    public void runChecksUpbitChecker() {
        upbitChecker.check();
    }

}
