package com.challenge.ratelimiter.record;

import java.util.List;

public record WindowCounterRecord(Long windowStart, List<String> ipAddrList) {
}
