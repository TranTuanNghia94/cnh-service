package com.cnh.ies.service.redis;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;


import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class RedisService {
    
    private final RedisTemplate<String, Object> redisTemplate;

    // Lua script for atomic operations
    private static final String RATE_LIMIT_SCRIPT = 
        "local key = KEYS[1] " +
        "local limit = tonumber(ARGV[1]) " +
        "local window = tonumber(ARGV[2]) " +
        "local current = redis.call('GET', key) " +
        "if current == false then " +
        "  redis.call('SETEX', key, window, 1) " +
        "  return 1 " +
        "else " +
        "  local count = tonumber(current) " +
        "  if count >= limit then " +
        "    return 0 " +
        "  else " +
        "    redis.call('INCR', key) " +
        "    return 1 " +
        "  end " +
        "end";

    /**
     * Set a key-value pair with optional expiration
     */
    public void set(String key, Object value, Duration expiration) {
        try {
            synchronized (this) {
                redisTemplate.opsForValue().set(key, value, expiration);
                log.info("Set key: {} with expiration: {}", key, expiration);
            }
        } catch (Exception e) {
            log.error("Error setting key: {}", key, e);
            throw e;
        }
    }

    /**
     * Set a key-value pair without expiration
     */
    public void set(String key, Object value) {
        try {
            synchronized (this) {
                redisTemplate.opsForValue().set(key, value);
                log.info("Set key: {}", key);
            }
        } catch (Exception e) {
            log.error("Error setting key: {}", key, e);
            throw e;
        }
    }

    /**
     * Get value by key
     */
    public Object get(String key) {
        try {
            Object value;
            synchronized (this) {
                value = redisTemplate.opsForValue().get(key);
            }
            log.info("Get key: {}, found: {}", key, value != null);
            return value;
        } catch (Exception e) {
            log.error("Error getting key: {}", key, e);
            throw e;
        }
    }

    /**
     * Get value by key with type casting
     */
    @SuppressWarnings("unchecked")
    public <T> T get(String key, Class<T> clazz) {
        Object value = get(key);
        if (value != null && clazz.isInstance(value)) {
            return (T) value;
        }
        return null;
    }

    /**
     * Delete a key
     */
    public Boolean delete(String key) {
        try {
            Boolean result;
            synchronized (this) {
                result = redisTemplate.delete(key);
            }
            log.info("Delete key: {}, result: {}", key, result);
            return result;
        } catch (Exception e) {
            log.error("Error deleting key: {}", key, e);
            throw e;
        }
    }

    /**
     * Delete multiple keys
     */
    public Long delete(List<String> keys) {
        try {
            Long result;
            synchronized (this) {
                result = redisTemplate.delete(keys);
            }
            log.info("Delete keys: {}, result: {}", keys, result);
            return result;
        } catch (Exception e) {
            log.error("Error deleting keys: {}", keys, e);
            throw e;
        }
    }

    /**
     * Check if key exists
     */
    public Boolean hasKey(String key) {
        try {
            Boolean result;
            synchronized (this) {
                result = redisTemplate.hasKey(key);
            }
            log.info("Has key: {}, result: {}", key, result);
            return result;
        } catch (Exception e) {
            log.error("Error checking key: {}", key, e);
            throw e;
        }
    }

    /**
     * Set expiration for a key
     */
    public Boolean expire(String key, Duration expiration) {
        try {
            Boolean result;
            synchronized (this) {
                result = redisTemplate.expire(key, expiration);
            }
            log.info("Set expiration for key: {}, expiration: {}, result: {}", key, expiration, result);
            return result;
        } catch (Exception e) {
            log.error("Error setting expiration for key: {}", key, e);
            throw e;
        }
    }

    /**
     * Get time to live for a key
     */
    public Long getExpire(String key) {
        try {
            Long ttl;
            synchronized (this) {
                ttl = redisTemplate.getExpire(key, TimeUnit.SECONDS);
            }
            log.info("Get TTL for key: {}, TTL: {}", key, ttl);
            return ttl;
        } catch (Exception e) {
            log.error("Error getting TTL for key: {}", key, e);
            throw e;
        }
    }

    /**
     * Increment a counter
     */
    public Long increment(String key) {
        try {
            Long result;
            synchronized (this) {
                result = redisTemplate.opsForValue().increment(key);
            }
            log.info("Increment key: {}, result: {}", key, result);
            return result;
        } catch (Exception e) {
            log.error("Error incrementing key: {}", key, e);
            throw e;
        }
    }

    /**
     * Increment a counter by specified amount
     */
    public Long increment(String key, long delta) {
        try {
            Long result;
            synchronized (this) {
                result = redisTemplate.opsForValue().increment(key, delta);
            }
            log.info("Increment key: {} by {}, result: {}", key, delta, result);
            return result;
        } catch (Exception e) {
            log.error("Error incrementing key: {}", key, e);
            throw e;
        }
    }

    /**
     * Hash operations - Set field in hash
     */
    public void hSet(String key, String field, Object value) {
        try {
            synchronized (this) {
                redisTemplate.opsForHash().put(key, field, value);
            }
            log.info("HSet key: {}, field: {}", key, field);
        } catch (Exception e) {
            log.error("Error setting hash field: {}:{}", key, field, e);
            throw e;
        }
    }

    /**
     * Hash operations - Get field from hash
     */
    public Object hGet(String key, String field) {
        try {
            Object value;
            synchronized (this) {
                value = redisTemplate.opsForHash().get(key, field);
            }
            log.info("HGet key: {}, field: {}, found: {}", key, field, value != null);
            return value;
        } catch (Exception e) {
            log.error("Error getting hash field: {}:{}", key, field, e);
            throw e;
        }
    }

    /**
     * Hash operations - Get all fields from hash
     */
    public Map<Object, Object> hGetAll(String key) {
        try {
            Map<Object, Object> result;
            synchronized (this) {
                result = redisTemplate.opsForHash().entries(key);
            }
            log.info("HGetAll key: {}, size: {}", key, result.size());
            return result;
        } catch (Exception e) {
            log.error("Error getting all hash fields: {}", key, e);
            throw e;
        }
    }

    /**
     * Hash operations - Delete field from hash
     */
    public Long hDelete(String key, Object... fields) {
        try {
            Long result;
            synchronized (this) {
                result = redisTemplate.opsForHash().delete(key, fields);
            }
            log.info("HDelete key: {}, fields: {}, result: {}", key, fields, result);
            return result;
        } catch (Exception e) {
            log.error("Error deleting hash fields: {}", key, e);
            throw e;
        }
    }

    /**
     * List operations - Push to left
     */
    public Long lPush(String key, Object... values) {
        try {
            Long result;
            synchronized (this) {
                result = redisTemplate.opsForList().leftPushAll(key, values);
            }
            log.info("LPush key: {}, size: {}", key, result);
            return result;
        } catch (Exception e) {
            log.error("Error pushing to list: {}", key, e);
            throw e;
        }
    }

    /**
     * List operations - Pop from right
     */
    public Object rPop(String key) {
        try {
            Object result;
            synchronized (this) {
                result = redisTemplate.opsForList().rightPop(key);
            }
            log.info("RPop key: {}, result: {}", key, result != null);
            return result;
        } catch (Exception e) {
            log.error("Error popping from list: {}", key, e);
            throw e;
        }
    }

    /**
     * Set operations - Add to set
     */
    public Long sAdd(String key, Object... values) {
        try {
            Long result;
            synchronized (this) {
                result = redisTemplate.opsForSet().add(key, values);
            }
            log.info("SAdd key: {}, size: {}", key, result);
            return result;
        } catch (Exception e) {
            log.error("Error adding to set: {}", key, e);
            throw e;
        }
    }

    /**
     * Set operations - Get all members
     */
    public Set<Object> sMembers(String key) {
        try {
            Set<Object> result;
            synchronized (this) {
                result = redisTemplate.opsForSet().members(key);
            }
            log.info("SMembers key: {}, size: {}", key, result != null ? result.size() : 0);
            return result;
        } catch (Exception e) {
            log.error("Error getting set members: {}", key, e);
            throw e;
        }
    }

    /**
     * Rate limiting using sliding window
     */
    public boolean isRateLimited(String key, int limit, int windowSeconds) {
        try {
            DefaultRedisScript<Long> script = new DefaultRedisScript<>();
            script.setScriptText(RATE_LIMIT_SCRIPT);
            script.setResultType(Long.class);

            Long result;
            synchronized (this) {
                result = redisTemplate.execute(script, 
                    Collections.singletonList(key), 
                    String.valueOf(limit), 
                    String.valueOf(windowSeconds));
            }

            boolean allowed = result != null && result == 1;
            log.info("Rate limit check for key: {}, limit: {}, window: {}s, allowed: {}", 
                key, limit, windowSeconds, allowed);
            return !allowed; // Return true if rate limited
        } catch (Exception e) {
            log.error("Error checking rate limit for key: {}", key, e);
            return false; // Allow on error
        }
    }

    /**
     * Clear all keys (use with caution)
     */
    public void clearAll() {
        try {
            Set<String> keys;
            synchronized (this) {
                keys = redisTemplate.keys("*");
                if (keys != null && !keys.isEmpty()) {
                    redisTemplate.delete(keys);
                }
            }
            log.info("Cleared {} keys from Redis", keys != null ? keys.size() : 0);
        } catch (Exception e) {
            log.error("Error clearing all keys", e);
            throw e;
        }
    }

    /**
     * Get Redis info
     */
    public String getInfo() {
        try {
            String info;
            synchronized (this) {
                info = redisTemplate.getConnectionFactory().getConnection().info("server").toString();
            }
            return info;
        } catch (Exception e) {
            log.error("Error getting Redis info", e);
            throw e;
        }
    }
}
