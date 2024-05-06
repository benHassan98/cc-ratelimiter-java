package com.challenge.ratelimiter.pojo;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class TokenRateLimiter extends RateLimiter{

    private final ConcurrentHashMap<String, Long> userTokens = new ConcurrentHashMap<>();
    private final Long N;
    private static final Logger logger = LoggerFactory.getLogger(TokenRateLimiter.class);
    public TokenRateLimiter(@Value("${token.bucket.N}") Long N,
                            @Value("${token.bucket.add_interval_sec}") Long addInterval){
        this.N = N;
        new ScheduledThreadPoolExecutor(1).
                scheduleWithFixedDelay(this::addTokens,
                        addInterval*1000,
                        addInterval*1000,
                        TimeUnit.MILLISECONDS);
    }
    @Override
    public String acceptOrReject(String ipAddr) {

        userTokens.putIfAbsent(ipAddr, N);
        var val = userTokens.get(ipAddr);
        if(val>0){
            userTokens.put(ipAddr, val-1);
        }

        logger.info("TokenBucket: Request from {} served and {} tokens left", ipAddr, val);

        return val>0 ?"Accepted":"Rejected";
    }

    private void addTokens(){
        logger.info("TokenBucket: Token added to all users");
        userTokens.replaceAll((k, v)->Long.min(v+1, N));
    }

}
