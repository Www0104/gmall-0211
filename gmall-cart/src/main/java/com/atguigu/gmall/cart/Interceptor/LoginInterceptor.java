 package com.atguigu.gmall.cart.Interceptor;

import com.atguigu.gmall.cart.config.JwtProperties;
import com.atguigu.gmall.cart.pojo.UserInfo;
import com.atguigu.gmall.common.utils.CookieUtils;
import com.atguigu.gmall.common.utils.JwtUtils;
import io.swagger.annotations.Scope;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;
import java.util.UUID;

@Component
@EnableConfigurationProperties(JwtProperties.class)
public class LoginInterceptor implements HandlerInterceptor {

    @Autowired
    private JwtProperties properties;

    private static final ThreadLocal<UserInfo> THREAD_LOCAL =new ThreadLocal<>();


    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        //获取cookie中的token和userkey
        String token = CookieUtils.getCookieValue(request,this.properties.getCookieName());
        String userKey = CookieUtils.getCookieValue(request,this.properties.getUserKeyName());
        if (StringUtils.isBlank(userKey)){
            userKey = UUID.randomUUID().toString();
            CookieUtils.setCookie(request,response,this.properties.getUserKeyName(),userKey,180*24*3600);
        }
        //token不为空则解析出userid
        Long userId = null;
        if (StringUtils.isNotBlank(token)){
            Map<String, Object> map = JwtUtils.getInfoFromToken(token, this.properties.getPublicKey());
            userId = new Long(map.get("userId").toString());
        }
        UserInfo userInfo = new UserInfo();
        userInfo.setUserId(userId);
        userInfo.setUserKey(userKey);
        THREAD_LOCAL.set(userInfo);
//        request.setAttribute("userId",userId);
//        request.setAttribute("userKey",userKey);
        return true;
    }

    public static UserInfo getUserInfo(){
        return THREAD_LOCAL.get();
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        THREAD_LOCAL.remove();
    }
}
