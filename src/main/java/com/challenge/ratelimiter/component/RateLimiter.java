package com.challenge.ratelimiter.component;



public abstract class RateLimiter {

    public abstract String acceptOrReject(String ipAddr) throws Exception;

}
