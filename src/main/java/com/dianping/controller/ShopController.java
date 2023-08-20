package com.dianping.controller;

import cn.hutool.core.util.StrUtil;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dianping.dto.Result;
import com.dianping.entity.Shop;
import com.dianping.service.IShopService;
import com.dianping.utils.SystemConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/shop")
public class ShopController {

    @Autowired
    public IShopService shopService;

    /**
     * 根据id查询商铺信息
     * @param id
     * @return
     */
    @GetMapping("/{id}")
    public Result queryShopById(@PathVariable("id") Long id){
        return shopService.queryShopById(id);
    }

    /**
     * 新增商铺信息
     * @param shop
     * @return
     */
    @PostMapping
    public Result saveShop(@RequestBody Shop shop){
        //写入数据库
        shopService.save(shop);
        //返回店铺id
        return Result.ok(shop.getId());
    }

    /**
     * 修改商铺信息
     * @param shop
     * @return
     */
    @PutMapping
    public Result updateShop(@RequestBody Shop shop){
        //写入数据库
        return shopService.update(shop);
    }

    /**
     * 通过类型分页查询商铺信息
     * @param typeId
     * @param current
     * @param x
     * @param y
     * @return
     */
    @GetMapping("/of/type")
    public Result queryShopByType( @RequestParam("typeId") Integer typeId,
                                   @RequestParam(value = "current", defaultValue = "1") Integer current,
                                   @RequestParam(value = "x", required = false) Double x,
                                   @RequestParam(value = "y", required = false) Double y){
        return shopService.queryShopByType(typeId,current,x,y);
    }

    /**
     * 根据商铺名称关键字分页查询商铺信息
     * @param name
     * @param current
     * @return
     */
    @GetMapping("/of/name")
    public Result queryShopByName(
            @RequestParam(value = "name", required = false) String name,
            @RequestParam(value = "current", defaultValue = "1") Integer current
    ){
        Page<Shop> page = shopService.query()
                .like(StrUtil.isNotBlank(name),"name",name)
                .page(new Page<>(current, SystemConstants.MAX_PAGE_SIZE));
        return Result.ok(page.getRecords());
    }

}
