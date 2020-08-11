 package com.atguigu.gmall.order.Interceptor;


import com.atguigu.gmall.common.bean.UserInfo;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

 @Component
 public class LoginInterceptor implements HandlerInterceptor {

     private static final ThreadLocal<UserInfo> THREAD_LOCAL =new ThreadLocal<>();


     @Override
     public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
         String userId = request.getHeader("userId");
         if (StringUtils.isNotBlank(userId)){
             UserInfo userInfo = new UserInfo();
             userInfo.setUserId(Long.valueOf(userId));
             THREAD_LOCAL.set(userInfo);
         }else {
             response.sendRedirect("http://sso.gmall.com/toLogin?returnUrl="+request.getRequestURL());
         }
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
