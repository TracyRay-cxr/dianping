package com.dianping.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.dianping.dto.Result;
import com.dianping.entity.Voucher;
import com.dianping.mapper.VoucherMapper;
import com.dianping.service.IVoucherService;
import org.springframework.stereotype.Service;

import java.util.List;


@Service
public class VoucherServiceImpl extends ServiceImpl<VoucherMapper, Voucher> implements IVoucherService {

    @Override
    public Result addSeckillVoucher(Voucher voucher) {
      //保存优惠券
        save(voucher);
        //保存秒杀信息
        return null;
    }

    @Override
    public Result queryVoucherOfShop(Long shopId) {
        //查询优惠券信息
        List<Voucher> vouchers = getBaseMapper().queryVoucherOfShop(shopId);
        //返回结果
        return Result.ok(vouchers);
    }
}
