package com.dianping.handler;


import com.alibaba.fastjson.JSON;
import com.dianping.dto.Result;
import com.dianping.utils.WebUtils;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 自定义认证异常处理器
 */
@Component
public class AuthenticationEntryPointImpl implements AuthenticationEntryPoint {
    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) throws IOException, ServletException {
        //打印异常信息
        authException.printStackTrace();
        //InsufficientAuthenticationException
        //BadCredentialsException
        //判断一下是什么错误并返回对应的错误信息
        Result result=null;
        if (authException instanceof BadCredentialsException){
            result=Result.error(authException.getMessage());
        }else if (authException instanceof InsufficientAuthenticationException){
            result=Result.error("需要登录后操作");
        }else{
            //其他错误先这样处理:D
            result=Result.error("认证或授权失败");
        }
        //响应给前端
        WebUtils.renderString(response, JSON.toJSONString(result));
    }
}
