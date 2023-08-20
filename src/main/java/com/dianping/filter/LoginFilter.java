package com.dianping.filter;

import cn.hutool.core.bean.BeanUtil;
import com.alibaba.fastjson.JSON;
import com.dianping.dto.Result;
import com.dianping.dto.UserDTO;
import com.dianping.utils.JwtUtil;
import com.dianping.utils.RedisCache;
import com.dianping.utils.WebUtils;
import io.jsonwebtoken.Claims;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;


import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

@Component
public class LoginFilter extends OncePerRequestFilter {
    @Autowired
    private RedisCache redisCache;
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        //获取请求头中的token
        String token = request.getHeader("token");
        if (!StringUtils.hasText(token)){
            //直接放行让后面的过滤器链判断是否需要登录
            filterChain.doFilter(request,response);
            return;
        }
        //解析token获取userid
        Claims claims = null;
        try {
            claims = JwtUtil.parseJWT(token);
        } catch (Exception e) {
            e.printStackTrace();
            //token超时 或者 token非法
            //响应告诉前端重新登录：约定code==401
            Result result = Result.error("登录过期，请重新登录");
            WebUtils.renderString(response, JSON.toJSONString(result));
            return;
        }
        String userId=claims.getSubject();
        //从redis判断获取用户信息
        Map<String,Object> userMap = redisCache.getCacheMap("login:token:" + userId);
        if (Objects.isNull(userMap)){
            //用户信息为空说明登录过期,要重新登录
            Result result = Result.error("登录过期，请重新登录");
            WebUtils.renderString(response, JSON.toJSONString(result));
            return;
        }  // 5.将查询到的hash数据转为UserDTO
        UserDTO userDTO = BeanUtil.fillBeanWithMap(userMap, new UserDTO(), false);
        //TODO 刷新token过期时间

        //存入SecurityContextHolder中方便以后使用
        UsernamePasswordAuthenticationToken authenticationToken =
                new UsernamePasswordAuthenticationToken(userDTO,null,null);
        SecurityContextHolder.getContext().setAuthentication(authenticationToken);
        filterChain.doFilter(request,response);
    }

}
