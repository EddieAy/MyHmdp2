package com.hmdp.utils;


import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.hmdp.entity.Shop;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import static com.hmdp.utils.RedisConstants.*;

@Component
@Slf4j
public class CacheClient {

    @Resource
    private StringRedisTemplate stringRedisTemplate;


    public void set(String key,Object value,Long expireTTL,TimeUnit timeUnit){
        stringRedisTemplate.opsForValue().set(key,JSONUtil.toJsonStr(value),expireTTL,timeUnit);
    }
    public void setLogicalExpire(String key,Object value,Long expireTime,TimeUnit timeUnit){
        RedisData redisData = new RedisData();
        redisData.setData(value);
        redisData.setExpireTime(LocalDateTime.now().plusSeconds(timeUnit.toSeconds(expireTime)));
        stringRedisTemplate.opsForValue().set(key,JSONUtil.toJsonStr(redisData));
    }

    public <R,ID> R queryWithPassThrough(ID id, String prefix,Class<R> type,
                                         Function<ID,R> dbFallback,Long time,TimeUnit timeUnit){
        String key = prefix + id;
        String json = stringRedisTemplate.opsForValue().get(key);

        if(StrUtil.isNotBlank(json)){
            return JSONUtil.toBean(json, type);
        }

        if(json != null){
            //  防止缓存穿透  直接返回空值
            return null;
        }

        R r = dbFallback.apply(id);

        if(r == null){
            stringRedisTemplate.opsForValue().set(key, "",CACHE_NULL_TTL,TimeUnit.MINUTES);
            return null;
        }
        this.set(key,JSONUtil.toJsonStr(r),time,timeUnit);
        return r;
    }

    private  static  final ExecutorService CACHE_REBUILD_EXECUTOR = Executors.newFixedThreadPool(10);
    public <R,ID> R queryByIdWithLogicalExpire(String keyPrefix,ID id,Class<R> type,
                                            Function<ID,R> dbFallback,Long logicalExpireTime,TimeUnit timeUnit) {
        String key = keyPrefix + id;
        String json = stringRedisTemplate.opsForValue().get(key);

        if(StrUtil.isBlank(json)){
            return null;
        }

        RedisData redisData = JSONUtil.toBean(json, RedisData.class);
        JSONObject data = (JSONObject)redisData.getData();
        R r = JSONUtil.toBean(data, type);
        LocalDateTime expireTime = redisData.getExpireTime();

        if(expireTime.isAfter(LocalDateTime.now())){
            return r;
        }

        String lockKey = LOCK_SHOP_KEY + id;
        boolean isLock = tryLock(lockKey);
        if(isLock){
            CACHE_REBUILD_EXECUTOR.submit(() ->{
                try {
                    R r1 = dbFallback.apply(id);
                    this.setLogicalExpire(key,r1,logicalExpireTime,timeUnit);
                } finally {
                    unLock(lockKey);
                }
            });
        }

        return r;
    }

    private boolean tryLock(String key){
        Boolean lock = stringRedisTemplate.opsForValue().setIfAbsent(key, "lock", 10L, TimeUnit.SECONDS);
        return BooleanUtil.isTrue(lock);
    }

    private void unLock(String key){
        stringRedisTemplate.delete(key);
    }


}
