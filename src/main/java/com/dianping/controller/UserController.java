package com.dianping.controller;

import cn.hutool.core.bean.BeanUtil;
import com.dianping.dto.UserDTO;
import com.dianping.entity.User;
import com.dianping.entity.UserInfo;
import com.dianping.dto.LoginFormDTO;
import com.dianping.dto.Result;
import com.dianping.service.IUserInfoService;
import com.dianping.service.IUserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;



/**
 * 用户控制器
 */
@Slf4j
@RestController
@RequestMapping("/user")
public class UserController {
    @Autowired
    private IUserService userService;
    @Autowired
    private IUserInfoService userInfoService;

    /**
     * 发送手机验证码
     * @param phone
     * @return
     */
    @PostMapping("/code")
    public Result sendCode(@RequestParam("phone") String phone){
        //发送短信验证码并保存验证码
        return userService.sendCode(phone);
    }

    /**
     * 登录功能
     * @param loginForm
     * @return
     */
    @PostMapping("/login")
    public Result login(@RequestBody LoginFormDTO loginForm){
        //实现登录功能
        return userService.login(loginForm);
    }

    /**
     * 登出功能
     * @return
     */
    @PostMapping("/logout")
    public Result logout(){
        //实现登出功能
        return userService.logout();
    }

    /**
     * 获取当前登录的用户信息并返回
     * @return
     */
    @GetMapping("/me")
    public Result me(){
        //从ThreadLocal获取当前登录的用户并返回
        UserDTO user = (UserDTO) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return Result.ok(user);
    }

    /**
     * 通过用户id查询用户信息
     * @param userId
     * @return
     */
    @GetMapping("/info/{id}")
    public Result info(@PathVariable("id") Long userId){
        //查询详情
        UserInfo info = userInfoService.getById(userId);
        if (info==null){
            //没有信息，应该是第一次查询
            return Result.ok();
        }
//        info.setCreateTime(null);
//        info.setUpdateTime(null);
        //返回
        return Result.ok(info);
    }

    /**
     * 通过用户id查询用户并返回
     * @param userId
     * @return
     */
    @GetMapping("/{id}")
    public Result queryUserById(@PathVariable("id") Long userId){
        //查询详情
        User user = userService.getById(userId);
        if (user==null){
            return Result.ok();
        }
        UserDTO userDTO = BeanUtil.copyProperties(user,UserDTO.class);
        //返回
        return Result.ok(userDTO);

    }
    @PostMapping("/sign")
    public Result sign(){
        return  userService.sign();
    }
    @GetMapping("/sign/count")
    public Result signCount(){
        return userService.signCount();
    }
}
