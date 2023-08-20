package com.dianping.utils;

import com.dianping.entity.LoginUser;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public class SecutiryUtils {
    /**
     * 获取用户
     * @return
     */
    public static LoginUser getLoginUser(){
        return  (LoginUser) getAuthentication().getPrincipal();
    }

    /**
     * 获取Authentication
     * @return
     */
    public static Authentication getAuthentication(){
        return SecurityContextHolder.getContext().getAuthentication();
    }

    /**
     * 判断管理员
     * @return
     */
    public static Boolean isAdmin(){
        //TODO 判断管理员
        return false;
    }

    /**
     * 获取用户id
     * @return
     */
    public static Long getUserId() {
        return getLoginUser().getUser().getId();
    }
}
