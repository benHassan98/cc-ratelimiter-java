package com.challenge.ratelimiter.pojo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class FixedWindowRateLimiter extends RateLimiter{

    private final Long REQUEST_CNT;
    private final AtomicLong requestCnt;
    private static final Logger logger = LoggerFactory.getLogger(FixedWindowRateLimiter.class);
    public FixedWindowRateLimiter(@Value("${window.size_sec}") Long size,
                                  @Value("${window.request_cnt}") Long request_cnt){

        this.REQUEST_CNT = request_cnt;
        this.requestCnt = new AtomicLong(request_cnt);
        new ScheduledThreadPoolExecutor(1).
                scheduleWithFixedDelay(this::resetCnt,
                        size*1000,
                        size*1000,
                        TimeUnit.MILLISECONDS);

    }
    @Override
    public String acceptOrReject(String ipAddr) {
        var val = requestCnt.getAndUpdate((curr)->Long.max(curr-1, 0));
        logger.info("FixedWindow: Request from {} served and {} requests left", ipAddr, val);
        return val>0? "Accepted": "Rejected";
    }

    public void resetCnt(){
        logger.info("FixedWindow: Resetting Requests Count to {}", this.REQUEST_CNT);
        this.requestCnt.set(this.REQUEST_CNT);
    }

}
