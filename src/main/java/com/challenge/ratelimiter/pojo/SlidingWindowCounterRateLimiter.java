package com.challenge.ratelimiter.pojo;

import com.challenge.ratelimiter.record.WindowCounterRecord;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.BoundListOperations;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@Component
@SuppressWarnings({"DataFlowIssue"})
public class SlidingWindowCounterRateLimiter extends RateLimiter{

    private final BoundListOperations<String, String> boundListOperations;
    private final ReentrantReadWriteLock readWriteLock = new ReentrantReadWriteLock();
    private final ReentrantReadWriteLock.WriteLock writeLock = this.readWriteLock.writeLock();
    private final Long size;
    private final Long requestCnt;
    private final static Logger logger = LoggerFactory.getLogger(SlidingWindowCounterRateLimiter.class);
    @Autowired
    public SlidingWindowCounterRateLimiter(BoundListOperations<String, String> boundListOperations,
                                           @Value("${window_counter.size_sec}") Long size,
                                           @Value("${window_counter.request_cnt}") Long requestCnt){
        this.boundListOperations = boundListOperations;
        this.size = size;
        this.requestCnt = requestCnt;

        new ScheduledThreadPoolExecutor(1).
                scheduleWithFixedDelay(this::addWindow,
                        0,
                        (size * 1000L)/2,
                        TimeUnit.MILLISECONDS);


        new ScheduledThreadPoolExecutor(1).
                scheduleWithFixedDelay(this::removeWindow,
                        size * 1000 * 7,
                        size * 1000 * 7,
                        TimeUnit.MILLISECONDS);

    }
    private Optional<String> object2Json(WindowCounterRecord record){
        try{
            return Optional.of(
                    new ObjectMapper().writeValueAsString(record)
            );
        }catch (JsonProcessingException exception){
            exception.printStackTrace();
        }
        return Optional.empty();
    }
    private Optional<WindowCounterRecord> json2Object(String json){

        try{
            return Optional.of(
                    new ObjectMapper().readValue(json, WindowCounterRecord.class)
            );
        }catch (JsonProcessingException exception){
            exception.printStackTrace();
        }
        return Optional.empty();
    };
    private void addWindow(){

        final AtomicLong startTime = new AtomicLong(Instant.now().getEpochSecond());

        writeLock.lock();

        if(boundListOperations.size() > 0){
            json2Object(this.boundListOperations.rightPop())
                    .flatMap(rec->{
                        startTime.set(rec.windowStart() + size);
                        return object2Json(rec);
                    })
                    .ifPresent(this.boundListOperations::rightPush);


        }
        object2Json(new WindowCounterRecord(
                startTime.get(),
                0L
        ))
                .ifPresent(boundListOperations::rightPush
                );


        writeLock.unlock();
        logger.info("SlidingWindowCounter: Added new window to redis server with start time: {}", startTime);
    }
    private void removeWindow(){

        writeLock.lock();

        boundListOperations.leftPop();

        writeLock.unlock();

        logger.info("SlidingWindowCounter: Removed window from redis server");
    }

    @Override
    public String acceptOrReject(String ipAddr) throws Exception{

        Long requestTime = Instant.now().getEpochSecond();
        Stack<WindowCounterRecord> stack = new Stack<>();
        String res = "Rejected";

        writeLock.lock();

        while(boundListOperations.size() > 0){

            var rec = json2Object(boundListOperations.rightPop())
                    .orElseThrow(()->new Exception("Cannot serialize object from redis"));
            stack.add(rec);
            if(rec.windowStart()<= requestTime && rec.windowStart()+size > requestTime){
                break;
            }

        }

        var requestWindowRec = stack.pop();

        if(boundListOperations.size() == 0){
            boundListOperations
                    .rightPush(object2Json(
                            new WindowCounterRecord(
                                    requestWindowRec.windowStart(),
                                    requestWindowRec.requestCnt() + 1
                            )
                    ).orElseThrow(()->new Exception("Cannot deserialize object from redis")));
            res = "Accepted";
        }else{

            var prevRec = json2Object(boundListOperations.rightPop())
                    .orElseThrow(()->new Exception("Cannot deserialize object from redis"));

            boundListOperations.rightPush(object2Json(prevRec)
                    .orElseThrow(()->new Exception("Cannot serialize object from redis")));

            if(prevRec.requestCnt()*(size-(requestWindowRec.windowStart()-requestTime+size)) + requestWindowRec.requestCnt()*size<= requestCnt*size){
                boundListOperations.rightPush(
                        object2Json(new WindowCounterRecord(
                                requestWindowRec.windowStart(),
                                requestWindowRec.requestCnt() + 1
                        ))
                                .orElseThrow(()->new Exception("Cannot serialize object from redis"))
                );
                res = "Accepted";
            }

        }

        while(!stack.isEmpty()){
            boundListOperations.rightPush(object2Json(stack.pop())
                    .orElseThrow(()->new Exception("Cannot serialize object from redis")));
        }

        writeLock.unlock();
        logger.info("SlidingWindowCounter: Request from {} served with {}", ipAddr, res);
        return res;
    }
}
