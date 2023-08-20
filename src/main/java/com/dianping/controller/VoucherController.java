package com.dianping.controller;


import com.dianping.dto.Result;
import com.dianping.entity.Voucher;
import com.dianping.service.IVoucherService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/voucher")
public class VoucherController {

    @Autowired
    private IVoucherService voucherService;

    /**
     * 新增秒杀券
     * @param voucher
     * @return
     */
    @PostMapping("seckill")
    public Result addSeckillVoucher(@RequestBody Voucher voucher){
        voucherService.addSeckillVoucher(voucher);
        return Result.ok(voucher.getId());
    }

    /**
     * 新增普通优惠券
     * @param voucher
     * @return
     */
    @PostMapping
    public Result addVoucher(@RequestBody Voucher voucher){
        voucherService.save(voucher);
        return Result.ok(voucher.getId());
    }

    /**
     * 查询店铺优惠券列表
     * @param shopId
     * @return
     */
    @GetMapping("/list/{shopId}")
    public Result queryVoucherOfShop(@PathVariable("shopId") Long shopId)
    {
        return voucherService.queryVoucherOfShop(shopId);
    }


}
