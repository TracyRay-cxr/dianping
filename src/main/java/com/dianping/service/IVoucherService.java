package com.dianping.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.dianping.dto.Result;
import com.dianping.entity.Voucher;

public interface IVoucherService extends IService<Voucher> {

    Result addSeckillVoucher(Voucher voucher);


    Result queryVoucherOfShop(Long shopId);
}
