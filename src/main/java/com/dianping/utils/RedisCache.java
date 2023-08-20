package com.dianping.utils;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.GeoResults;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.BoundSetOperations;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.domain.geo.GeoReference;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.TimeUnit;

@Component
public class RedisCache {
    @Autowired
    public RedisTemplate redisTemplate;

    /**
     * 缓存对象
     * @param key
     * @param value
     * @param <T>
     */
    public <T> void setCacheObject(final String key,final T value){
        redisTemplate.opsForValue().set(key,value);
    }

    /**
     * 缓存对象+过期时间
     * @param key
     * @param value
     * @param timeOut
     * @param timeUnit
     * @param <T>
     */
    public <T> void setCacheObject(final String key, final T value, final Integer timeOut, final TimeUnit timeUnit){
        redisTemplate.opsForValue().set(key,value,timeOut,timeUnit);
    }

    /**
     * 设置有效时间
     * @param key
     * @param timeout
     * @return
     */
    public boolean expire(final String key,final long timeout){
        return redisTemplate.expire(key,timeout,TimeUnit.SECONDS);
    }

    /**
     * 设置有效时间
     * @param key
     * @param timeout
     * @param util
     * @return
     */
    public boolean expire(final String key,final long timeout,final TimeUnit util){
        return redisTemplate.expire(key,timeout,util);
    }

    /**
     * 获取对应的key的值
     * @param key
     * @param <T>
     * @return
     */
    public <T> T getCacheObject(final String key){
        ValueOperations<String,T> operations = redisTemplate.opsForValue();
        return operations.get(key);
    }

    /**
     * 单个删除对应key和value
     * @param key
     * @return
     */
    public boolean deleteObject(final String key){
        return redisTemplate.delete(key);
    }

    /**
     * 删除集合对象
     * @param collection
     * @param <T>
     * @return
     */
    public <T> long deleteObject(final Collection collection){
        return redisTemplate.delete(collection);
    }

    /**
     * 缓存List数据
     * @param key
     * @param list
     * @param <T>
     * @return
     */
    public <T> long setCacheList(final String key,final List<T> list){
        Long count = redisTemplate.opsForList().leftPushAll(list);
        return count ==null ? 0: count;
    }
    /**
     * 获取缓存List
     * @param key
     * @param <T>
     * @return
     */
    public <T> List<T> getCacheList(final String key){
        return redisTemplate.opsForList().range(key,0,-1);
    }

    /**
     * 缓存Set
     * @param key
     * @param set
     * @param <T>
     * @return
     */
    public <T> BoundSetOperations<String,T> setCacheSet(final String key,final Set<T> set){
        BoundSetOperations<String ,T> setOperations = redisTemplate.boundSetOps(key);
        Iterator<T> it = set.iterator();
        while(it.hasNext()){
            setOperations.add(it.next());
        }
        return setOperations;
    }

    /**
     * 获取set缓存
     * @param key
     * @param <T>
     * @return
     */
    public <T> Set<T> getCacheSet(final String key){
        return redisTemplate.opsForSet().members(key);
    }

    /**
     * 缓存map
     * @param key
     * @param map
     * @param <T>
     */
    public <T> void setCacheMap(final String key, final Map<String , T> map){
        if (map!=null){
            redisTemplate.opsForHash().putAll(key,map);
        }
    }

    /**
     * 获取Map缓存
     * @param key
     * @param <T>
     * @return
     */
    public <T> Map<String,T> getCacheMap(final String key){
        return redisTemplate.opsForHash().entries(key);
    }

    /**
     * 向已有的map中缓存数据
     * @param key
     * @param hkey
     * @param value
     * @param <T>
     */
    public <T> void setCacheMapValue(final String key,final String hkey ,final T value){
        redisTemplate.opsForHash().put(key,hkey,value);
    }
    /**
     * 获取Hash中的数据
     *
     * @param key Redis键
     * @param hKey Hash键
     * @return Hash中的对象
     */
    public <T> T getCacheMapValue(final String key,final String hKey){
        HashOperations<String,String,T> hashOperations = redisTemplate.opsForHash();
        return hashOperations.get(key,hKey);
    }
    /**
     *对对应表的对应哈希key的value自增V
     */
    public void incrementCacheMapVaule(String key,String hKey,int V){
        redisTemplate.opsForHash().increment(key,hKey,V);
    }
    /**
     * 删除Hash中的数据
     *
     * @param key
     * @param hkey
     */
    public void delCacheMapValue(final String key, final String hkey)
    {
        HashOperations hashOperations = redisTemplate.opsForHash();
        hashOperations.delete(key, hkey);
    }
    /**
     * 获取多个Hash中的数据
     *
     * @param key Redis键
     * @param hKeys Hash键集合
     * @return Hash对象集合
     */
    public <T> List<T> getMultiCacheMapValue(final String key,final Collection<Object> hKeys){
        return redisTemplate.opsForHash().multiGet(key,hKeys);
    }
    /**
     * 通过前缀获得缓存的基本对象列表
     *
     * @param pattern 字符串前缀
     * @return 对象列表
     */
    public Collection<String> keys(final String pattern){
        return redisTemplate.keys(pattern);
    }

    /**
     * 设置有效期
     * @param key
     * @param timeOut
     * @param unit
     */
    public void setExpire(final String key,final long timeOut,final TimeUnit unit){
        redisTemplate.expire(key,timeOut,unit);
    }

    /**
     * 获取距离内的所有商铺按距离排序，分页
     * @param key
     * @param x
     * @param y
     * @param end
     * @return
     */
    public GeoResults<RedisGeoCommands.GeoLocation<String>> search(String key,Double x, Double y,int distance,int end){
       return redisTemplate.opsForGeo().search(key, GeoReference.fromCoordinate(x,y),new Distance(distance), RedisGeoCommands.GeoSearchCommandArgs.newGeoSearchArgs().includeDistance().limit(end));
    }
}
