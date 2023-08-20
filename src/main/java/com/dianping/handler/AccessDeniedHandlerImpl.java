package com.dianping.handler;


import com.alibaba.fastjson.JSON;
import com.dianping.dto.Result;
import com.dianping.utils.WebUtils;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 自定义授权异常处理器
 */
@Component
public class AccessDeniedHandlerImpl implements AccessDeniedHandler {
    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException accessDeniedException) throws IOException, ServletException {
        //打印异常信息
        accessDeniedException.printStackTrace();
        Result result=Result.error("无权限操作");
        //响应给前端
        WebUtils.renderString(response, JSON.toJSONString(result));
    }
}
