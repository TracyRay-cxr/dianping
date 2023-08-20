package com.dianping.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.dianping.dto.LoginFormDTO;
import com.dianping.dto.Result;
import com.dianping.entity.User;


public interface IUserService extends IService<User> {

    Result sendCode(String phone);

    Result login(LoginFormDTO loginForm);

    Result logout();

    Result sign();

    Result signCount();
}
