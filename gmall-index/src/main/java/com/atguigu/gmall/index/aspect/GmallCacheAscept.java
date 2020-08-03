package com.atguigu.gmall.index.aspect;

import com.alibaba.fastjson.JSON;
import org.apache.commons.lang.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.TimeUnit;

@Aspect
@Component
public class GmallCacheAscept {

    @Autowired
    private StringRedisTemplate redisTemplate;
    @Autowired
    private RedissonClient redissonClient;

//    @Around("execution( * com.atguigu.gmall.index.service.*.*(..))")
    @Around("@annotation(com.atguigu.gmall.index.aspect.GmallCache)")
    public Object arround(ProceedingJoinPoint joinPoint)throws Throwable{
        //获取目标方法以及注解
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        GmallCache gmallCache = method.getAnnotation(GmallCache.class);
        String prefix = gmallCache.prefix();
        //获取目标方法形参
        Object[] args = joinPoint.getArgs();
        String key = prefix + Arrays.asList(args);
        //获取方法返回值类型
        Class returnType = signature.getReturnType();

        String json = this.redisTemplate.opsForValue().get(key);
        if (!StringUtils.isNotBlank(json)){
            return JSON.parseObject(json,returnType);
        }

        String lock = gmallCache.lock();
        RLock fairLock= this.redissonClient.getFairLock(lock+args);
        fairLock.lock();


        String json2 = this.redisTemplate.opsForValue().get(key);
        if (!StringUtils.isNotBlank(json2)){
            fairLock.unlock();
            return JSON.parseObject(json2,returnType);
        }

        Object result = joinPoint.proceed(joinPoint.getArgs());

        int timeout = gmallCache.timeout();
        int random = gmallCache.random();
        this.redisTemplate.opsForValue().set(key,JSON.toJSONString(result),timeout+new Random().nextInt(random), TimeUnit.MINUTES);

        fairLock.unlock();

        return result;
    }
}
