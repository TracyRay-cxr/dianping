package com.dianping.utils;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

@Slf4j
@Component
public class CacheClient {

    private final StringRedisTemplate stringRedisTemplate;

    private static final ExecutorService CACHE_REBUILD_EXECUTOR = Executors.newFixedThreadPool(10);

    public CacheClient(StringRedisTemplate stringRedisTemplate){
        this.stringRedisTemplate = stringRedisTemplate;
    }
    public void set(String key , Object value, Long time, TimeUnit unit){
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(value),time,unit);
    }
    public void setWithLogicalExpire(String key,Object value,Long time,TimeUnit unit){   // 设置逻辑过期
        RedisData redisData = new RedisData();
        redisData.setData(value);
        redisData.setExpireTime(LocalDateTime.now().plusSeconds(unit.toSeconds(time)));
        // 写入Redis
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(redisData));
    }

    /**
     * 解决缓存穿透
     * @param keyPrefix
     * @param id
     * @param type
     * @param dbFallback
     * @param time
     * @param unit
     * @param <R>
     * @param <ID>
     * @return
     */
    public <R,ID> R queryWithPassThrough(String keyPrefix, ID id , Class<R> type, Function<ID,R> dbFallback,Long time, TimeUnit unit){
        String key = keyPrefix + id;
        //从reids中查询商铺缓存
        String json = stringRedisTemplate.opsForValue().get(key);
        //判断是否存在
        if (StrUtil.isNotBlank(json)) {
            //存在直接返回
            return JSONUtil.toBean(json,type);
        }
        ///判断是否为空
        if (json!=null){
            //返回一个错误信息
            return null;
        }
        //不存在，根据id查询数据库
        R r = dbFallback.apply(id);
        //不存在，返回错误
        if (r==null) {
            //将空值写入redis
            stringRedisTemplate.opsForValue().set(key,"",30,TimeUnit.MINUTES);
            //返回错误信息
            return null;
        }
        //存在，写入redis并返回
        stringRedisTemplate.opsForValue().set(key,JSONUtil.toJsonStr(r),time,unit);
        return r;
    }

    /**
     * 逻辑过期方式解决缓存击穿
     * @param keyPrefix
     * @param id
     * @param type
     * @param dbFallback
     * @param time
     * @param unit
     * @param <R>
     * @param <ID>
     * @return
     */
    public <R,ID> R queryWithLogicalExpire(
            String keyPrefix,ID id,Class<R> type,Function<ID,R> dbFallback,Long time, TimeUnit unit
    ){
        String key = keyPrefix+id;
        //从redis中查询商铺缓存
        String json = stringRedisTemplate.opsForValue().get(key);
        //判断是否存在
        if (StrUtil.isBlank(json)){
            //不存在，直接返回
            return null;
        }
        //由于逻辑过期，所以不为空总会
        //命中，需要先把json反序列化为对象
        RedisData redisData=JSONUtil.toBean(json,RedisData.class);
        R r =JSONUtil.toBean((JSONObject) redisData.getData(),type);
        LocalDateTime expireTime =redisData.getExpireTime();
        //判断是否过期
        if (expireTime.isAfter(LocalDateTime.now())){
            //未过期，直接返回店铺信息
            return r;
        }
        //已过期，需要重建缓存
        //重建缓存
        //获取互斥锁
        String lockKey = "lock:shop:"+id;
        boolean isLock = tryLock(lockKey);
        //判断是否获取锁成功
        if (isLock){
            //成功，开启新的独立线程，实现缓存重建
            CACHE_REBUILD_EXECUTOR.submit(()->{
                try {
                    //查询数据库
                    R newR =dbFallback.apply(id);
                    //重建缓存
                    this.setWithLogicalExpire(key,newR,time,unit);
                }catch (Exception e){
                    throw new RuntimeException(e);
                }
                finally {
                    //释放锁
                    unlock(lockKey);
                }

            });
        }
        //主线程，或其他线程（未获取锁的）返回过期的商铺信息
        return r;
    }

    /**
     * 通过互斥锁来解决缓存击穿
     * @param keyPrefix
     * @param id
     * @param type
     * @param dbFallback
     * @param time
     * @param unit
     * @param <R>
     * @param <ID>
     * @return
     */
    public <R,ID> R queryWithMutex(
            String keyPrefix,ID id,Class<R> type,Function<ID,R> dbFallback,Long time,TimeUnit unit
    ){
        String key=keyPrefix+id;
        //从redis中查询商铺信息
        String shopJson = stringRedisTemplate.opsForValue().get(key);
        //判断是否存在
        if (StrUtil.isNotBlank(shopJson)){
            //存在直接返回
            return JSONUtil.toBean(shopJson,type);
        }
        //不存在，判断是否为空
        if (shopJson!=null){
            //返回错误信息
            return null;
        }
        //如果为null
        //实现缓存重建
        //1.获取互斥锁
        String lockKey="lock:shop:"+id;
        R r =null;
        try{
            boolean isLock =tryLock(lockKey);
            //2.判断是否获取成功
            while (!isLock){
                //3.获取失败，休眠并重试
                //利用自选锁来重试
                Thread.sleep(50);
                isLock=tryLock(lockKey);
            }
            //4.获取锁成功
            r=dbFallback.apply(id);
            //5.不存在，返回错误
            if (r == null){
                //将空值写入reids
                stringRedisTemplate.opsForValue().set(key,null,30L,TimeUnit.MINUTES);
                //返回错误信息
                return null;
        }
            //6.存在，写入redis
        this.set(key,r,time,unit);

    } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }finally{
            //7.释放锁
            unlock(lockKey);
        }
        //返回查询结果
        return r;
    }

    /**
     * 获取互斥锁
     * @param key
     * @return
     */
    private boolean tryLock(String key){
        Boolean flag = stringRedisTemplate.opsForValue().setIfAbsent(key,"1",10L,TimeUnit.SECONDS);
        //拆箱可能会出现null
        return BooleanUtil.isTrue(flag);
    }

    /**
     * 释放锁
     * @param key
     */
    private void unlock(String key){
        stringRedisTemplate.delete(key);
    }

}
