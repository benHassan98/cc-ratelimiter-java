package com.challenge.ratelimiter.component;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;


import java.time.Instant;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class SlidingWindowLogRateLimiter extends RateLimiter{

    private final ConcurrentHashMap<String, TreeSet<Long>> userLogs = new ConcurrentHashMap<>();

    @Value("${window_log.size_sec}")
    private Long size;
    @Value("${window_log.request_cnt}")
    private Long requestCnt;
    private final static Logger logger = LoggerFactory.getLogger(SlidingWindowLogRateLimiter.class);

    @Override
    public String acceptOrReject(String ipAddr) {

        Long currTime = Instant.now().getEpochSecond();
        userLogs.putIfAbsent(ipAddr, new TreeSet<>());
        var itr = userLogs.get(ipAddr).descendingIterator();
        long cnt = 1;

        while(itr.hasNext()){
            Long val = itr.next();
            if(currTime-val > size)
                break;
            cnt++;
        }
        if(cnt<=requestCnt){
            userLogs.compute(ipAddr, (k,v)->{
                v.add(currTime);
                return v;
            });
        }
        String res = cnt<=requestCnt?"Accepted":"Rejected";
        logger.info("SlidingWindowLog: Request from {} served with {}", ipAddr, res);
        return res;
    }





}
