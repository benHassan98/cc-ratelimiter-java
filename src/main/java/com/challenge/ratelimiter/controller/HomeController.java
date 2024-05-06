package com.challenge.ratelimiter.controller;

import com.challenge.ratelimiter.pojo.FixedWindowRateLimiter;
import com.challenge.ratelimiter.pojo.SlidingWindowCounterRateLimiter;
import com.challenge.ratelimiter.pojo.SlidingWindowLogRateLimiter;
import com.challenge.ratelimiter.pojo.TokenRateLimiter;
import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HomeController {

    private final TokenRateLimiter tokenRateLimiter;
    private final FixedWindowRateLimiter fixedWindowRateLimiter;
    private final SlidingWindowLogRateLimiter slidingWindowLogRateLimiter;
    private final SlidingWindowCounterRateLimiter slidingWindowCounterRateLimiter;

    @Autowired
    public HomeController(TokenRateLimiter tokenRateLimiter,
                          FixedWindowRateLimiter fixedWindowRateLimiter,
                          SlidingWindowLogRateLimiter slidingWindowLogRateLimiter,
                          SlidingWindowCounterRateLimiter slidingWindowCounterRateLimiter) {
        this.tokenRateLimiter = tokenRateLimiter;
        this.fixedWindowRateLimiter = fixedWindowRateLimiter;
        this.slidingWindowLogRateLimiter = slidingWindowLogRateLimiter;
        this.slidingWindowCounterRateLimiter = slidingWindowCounterRateLimiter;
    }

    @GetMapping("/token")
    public ResponseEntity<?> token(HttpServletRequest req){
        return tokenRateLimiter.acceptOrReject(req.getRemoteAddr()).equals("Accepted")?
                ResponseEntity.ok("Accepted"):ResponseEntity.status(429).build();
    }

    @GetMapping("/window")
    public ResponseEntity<?> fixedWindow(HttpServletRequest req){
        return fixedWindowRateLimiter.acceptOrReject(req.getRemoteAddr()).equals("Accepted")?
                ResponseEntity.ok("Accepted"):ResponseEntity.status(429).build();
    }

    @GetMapping("/window_log")
    public ResponseEntity<?> slidingWindowLog(HttpServletRequest req){
        return slidingWindowLogRateLimiter.acceptOrReject(req.getRemoteAddr()).equals("Accepted")?
                ResponseEntity.ok("Accepted"):ResponseEntity.status(429).build();
    }

    @GetMapping("/window_counter")
    public ResponseEntity<?> slidingWindowCounter(HttpServletRequest req) throws Exception {
        return slidingWindowCounterRateLimiter.acceptOrReject(req.getRemoteAddr()).equals("Accepted")?
                ResponseEntity.ok("Accepted"):ResponseEntity.status(429).build();
    }

    @ExceptionHandler(value = JsonProcessingException.class)
    public ResponseEntity<?> exceptionHandler(JsonProcessingException exception){
        return ResponseEntity.badRequest().body(exception.getMessage());
    }

}
