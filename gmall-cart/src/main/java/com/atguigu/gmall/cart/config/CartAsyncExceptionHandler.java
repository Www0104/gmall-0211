package com.atguigu.gmall.cart.config;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundListOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
@Slf4j
@Component
public class CartAsyncExceptionHandler implements AsyncUncaughtExceptionHandler {

    @Autowired
    private StringRedisTemplate redisTemplate;

    private static final String KEY = "cart:async:exception";
    @Override
    public void handleUncaughtException(Throwable ex, Method method, Object... params) {
       log.error("有一个子任务出现了异常,信息为:{},异常方法为:{},方法参数:{}",ex.getMessage(),method,params);

       String userId = params[0].toString();
        if (StringUtils.isNotBlank(userId)){
            BoundListOperations<String, String> listOps = this.redisTemplate.boundListOps(KEY);
            listOps.leftPush(userId);
        }else {
            throw new RuntimeException("执行失败,未传递用户购物车信息");
        }



    }
}
