package com.challenge.ratelimiter.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.BoundListOperations;
import org.springframework.data.redis.core.StringRedisTemplate;


@Configuration
public class RedisConfig {

    @Bean
    BoundListOperations<String, String> template(RedisConnectionFactory connectionFactory,
                                                 @Value("${window_counter.redis_key}") String redisKey) {
        return new StringRedisTemplate(connectionFactory).boundListOps(redisKey);
    }




}
