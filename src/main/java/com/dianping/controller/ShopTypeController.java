package com.dianping.controller;

import com.dianping.dto.Result;
import com.dianping.entity.ShopType;
import com.dianping.service.IShopService;
import com.dianping.service.IShopTypeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/shop-type")
public class ShopTypeController {
    @Autowired
    private IShopTypeService typeService;
    @GetMapping("/list")
    public Result queryTypeList(){
        List<ShopType> typeList=typeService.query().orderByAsc("sort").list();
        return Result.ok(typeList);
    }

}
