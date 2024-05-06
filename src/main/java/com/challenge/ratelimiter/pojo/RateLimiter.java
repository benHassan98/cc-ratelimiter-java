package com.challenge.ratelimiter.pojo;



public abstract class RateLimiter {

    public abstract String acceptOrReject(String ipAddr);

}
