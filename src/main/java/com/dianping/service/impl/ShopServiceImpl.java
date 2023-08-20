package com.dianping.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.dianping.dto.Result;
import com.dianping.entity.Shop;
import com.dianping.mapper.ShopMapper;
import com.dianping.service.IShopService;
import com.dianping.utils.CacheClient;
import com.dianping.utils.RedisCache;
import com.dianping.utils.SystemConstants;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.GeoResult;
import org.springframework.data.geo.GeoResults;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
public class ShopServiceImpl extends ServiceImpl<ShopMapper, Shop> implements IShopService {
    @Autowired
    private RedisCache redisCache;
    @Autowired
    private CacheClient cacheClient;

    @Override
    public Result queryShopById(Long id) {
        //解决缓存穿透
        Shop shop = cacheClient.queryWithPassThrough("cache:shop:", id, Shop.class, this::getById, 30L, TimeUnit.MINUTES);
        // 互斥锁解决缓存击穿
        // Shop shop = cacheClient
        //         .queryWithMutex("cache:shop:", id, Shop.class, this::getById, 30L, TimeUnit.MINUTES);

        // 逻辑过期解决缓存击穿
        // Shop shop = cacheClient
        //         .queryWithLogicalExpire("cache:shop:", id, Shop.class, this::getById, 20L, TimeUnit.SECONDS);

        //返回
        return Result.ok(shop);
    }

    @Override
    @Transactional
    public Result update(Shop shop) {
        Long id = shop.getId();
        if (id == null){
            return Result.error("店铺id不能为空");
        }
        //更新数据库
        updateById(shop);
        //删除缓存
        redisCache.deleteObject("cache:shop:"+id);
        return Result.ok();
    }

    @Override
    public Result queryShopByType(Integer typeId, Integer current, Double x, Double y) {
        //1.判断是否需要根据坐标查询
        if(x == null || y == null){
            //不需要坐标查询，按数据库查询
            Page<Shop> page = query()
                    .eq("type_id",typeId)
                    .page(new Page<>(current, SystemConstants.DEFAULT_PAGE_SIZE));
            //返回数据
            return Result.ok(page.getRecords());
        }
        //2.计算分页参数
        int from = (current - 1)*SystemConstants.DEFAULT_PAGE_SIZE;
        int end = current*SystemConstants.DEFAULT_PAGE_SIZE;
        //3.查询redis、按照距离排序、分页。结果：shopId、distance
        String key = "shop:geo:"+typeId;
        GeoResults<RedisGeoCommands.GeoLocation<String>> results =
                redisCache.search(key,x,y,5000,end);
        //解析出id
        if (results == null){
            return Result.ok(Collections.emptyList());
        }
        List<GeoResult<RedisGeoCommands.GeoLocation<String>>> list =
                results.getContent();
        if (list.size()<=from){
            //没有下一页了，结束
            return Result.ok(Collections.emptyList());
        }
        //截取 from ~ end 的部分
        List<Long> ids = new ArrayList<>(list.size());
        Map<String, Distance> distanceMap = new HashMap<>(list.size());
        list.stream().skip(from).forEach(result -> {
            //获取店铺id
            String shopIdStr =result.getContent().getName();
            ids.add(Long.valueOf(shopIdStr));
            //获取距离
            Distance distance = result.getDistance();
            distanceMap.put(shopIdStr,distance);
        });
        //根据id查询shop
        String idStr = StrUtil.join(",", ids);
        List<Shop> shops = query().in("id",ids).last("ORDER BY FIELD(id,"+idStr+")").list();
        for (Shop shop:shops){
            shop.setDistance(distanceMap.get(shop.getId().toString()).getValue());
        }
        //返回
        return Result.ok(shops);
    }
}
