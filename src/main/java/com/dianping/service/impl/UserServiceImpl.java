package com.dianping.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.dianping.dto.LoginFormDTO;
import com.dianping.dto.Result;
import com.dianping.dto.UserDTO;
import com.dianping.entity.User;
import com.dianping.mapper.UserMapper;
import com.dianping.service.IUserService;
import com.dianping.utils.JwtUtil;
import com.dianping.utils.RedisCache;
import com.dianping.utils.RegexUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.dianping.utils.JwtUtil.JWT_TTL;

@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {
    @Autowired
    public RedisCache redisCache;

    @Override
    public Result sendCode(String phone) {
        //校验手机号
        if(RegexUtils.isPhoneInvalid(phone)){
            //不符合，返回错误信息
            return Result.error("手机号格式错误");
        }
        //符合，生成验证码(使用hutool工具类)
       String code =  RandomUtil.randomNumbers(6);
        //保存验证码到Redis
        //TODO 存入验证码,设置有效期为5分钟
        redisCache.setCacheObject("login:phone:"+phone,code,300, TimeUnit.SECONDS);
        //发送验证码
        //TODO 购买服务后设置
        log.debug("发送短信验证码成功，验证码:"+code);
        return Result.ok();
    }

    @Override
    public Result login(LoginFormDTO loginForm) {
        //校验手机号
        String phone = loginForm.getPhone();
        if (RegexUtils.isPhoneInvalid(phone)) {
            // 2.如果不符合，返回错误信息
            return Result.error("手机号格式错误！");
        }
        //校验验证码
        String code = loginForm.getCode();
        if (RegexUtils.isCodeInvalid(code)){
            return Result.error("验证码格式错误！");
        }
        String codeCache = redisCache.getCacheObject("login:phone:"+phone);
        if (codeCache==null){
            return Result.error("验证码不存在！");
        }
        if (!code.equals(codeCache)){
            return Result.error("验证码错误!");
        }
        //查询用户
        User user = query().eq("phone", phone).one();
        //判断用户是否存在
        if (user==null){
            //TODO 用户不存在，需要注册
        }
        //通过userId用jwt生成令牌
        String jwt = JwtUtil.createJWT(user.getId().toString());
        //用户信息保存到redis中
        //用hash结构存储
        UserDTO userDTO = BeanUtil.copyProperties(user, UserDTO.class);
        Map<String,Object> userMap = BeanUtil.beanToMap(userDTO);
        String tokenKey = "login:token:"+jwt;
        redisCache.setCacheMap(tokenKey,userMap);
        //设置登录有效期
        redisCache.setExpire(tokenKey,JWT_TTL,TimeUnit.MILLISECONDS);
        //返回给前端
        return Result.ok(jwt);
    }

    @Override
    public Result logout() {
        return null;
    }

    @Override
    public Result sign() {
        return null;
    }

    @Override
    public Result signCount() {
        return null;
    }
}
