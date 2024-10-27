
package com.alibaba.cloud.ai.autoconfigure.redis;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import java.util.concurrent.TimeUnit;

/**
 * Redis client<br>
 * @author HeYQ
 * @version
 */
@Component
public class RedisClientService {

    @Autowired
    RedisTemplate<String, Object> redisTemplate;

    /**
     * @param key
     * @param releaseTime
     * @return
     */
    public boolean lock(String key, long releaseTime) {
        // 尝试获取锁
        Boolean boo = redisTemplate.opsForValue().setIfAbsent(key, "0", releaseTime, TimeUnit.SECONDS);
        // 判断结果
        return boo != null && boo;
    }

    /**
     * @param key
     */
    public void deleteLock(String key) {
        // 删除key即可释放锁
        redisTemplate.delete(key);
    }

    public void setKeyValue(String key, String value, int timeout) {
        // Set the cache key to indicate the collection exists
        redisTemplate.opsForValue().set(key, value, timeout, TimeUnit.SECONDS);
    }

    public String getValueByKey(String key) {
        return (String) redisTemplate.opsForValue().get(key);
    }

}
