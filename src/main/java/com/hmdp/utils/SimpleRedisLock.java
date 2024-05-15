package com.hmdp.utils;

import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.BooleanUtil;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

public class SimpleRedisLock implements ILock{

    private String name;
     private StringRedisTemplate stringRedisTemplate;
     private static final String KEY_PREFIX = "lock:";
    private static final String ID_PREFIX = UUID.randomUUID().toString(true)+"-";

    private static final DefaultRedisScript<Long> UNLOCK_SCRIPT;

    static {
        UNLOCK_SCRIPT = new DefaultRedisScript<>();
        UNLOCK_SCRIPT.setLocation(new ClassPathResource("unlock.lua"));
        UNLOCK_SCRIPT.setResultType(Long.class);
    }

    public SimpleRedisLock(String name, StringRedisTemplate stringRedisTemplate) {
        this.name = name;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    public boolean tryLock(long timeoutSec) {
        String key = KEY_PREFIX + name;
        String threadId = ID_PREFIX + Thread.currentThread().getId();
        Boolean isSuccess = stringRedisTemplate.opsForValue().setIfAbsent(key, threadId +"", timeoutSec, TimeUnit.SECONDS);

        return Boolean.TRUE.equals(isSuccess);
    }

    @Override
    public void unlock() {
        //TODO 以下代码 不具有原子性  可能在判断完毕后 刚准备释放的瞬间  出现阻塞
        //TODO 因此 用Lua脚本来修改
/*        String threadId = ID_PREFIX + Thread.currentThread().getName();
        String lockId = stringRedisTemplate.opsForValue().get(KEY_PREFIX + name);
        if(threadId.equals(lockId)){
            stringRedisTemplate.delete(KEY_PREFIX + name);

        }*/
        stringRedisTemplate.execute(UNLOCK_SCRIPT,
                Collections.singletonList(KEY_PREFIX + name),
                ID_PREFIX + Thread.currentThread().getId());
    }
}
