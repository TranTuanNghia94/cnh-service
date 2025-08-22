package com.cnh.ies.constant;

import java.time.Duration;

public class RedisKey {

    public static final Duration REFRESH_TOKEN_DURATION = Duration.ofHours(8); // 8 hours
    public static final Duration ACCESS_TOKEN_DURATION = Duration.ofHours(4); // 4 hours
    public static final Duration USER_INFO_DURATION = Duration.ofHours(8); // 8 hours
    
    public static final String REFRESH_TOKEN_PREFIX = "refresh_token_";
    public static final String ACCESS_TOKEN_PREFIX = "access_token_";
}
