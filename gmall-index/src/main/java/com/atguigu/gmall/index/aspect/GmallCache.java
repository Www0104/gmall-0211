package com.atguigu.gmall.index.aspect;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface GmallCache {

    /**
     * 缓存KEY前缀
     * @return
     */
    String prefix() default "";



    /**
     * 缓存过期时间,单位是分钟
     * @return
     */
    int timeout() default 5;

    /**
     * 防止缓存的雪崩指定随机值范围
     * 单位为分钟
     * @return
     */
    int random() default 5;

    /**
     * 防止缓存穿透
     * 通过该属性指定分布式锁的名称
     * @return
     */
    String lock() default "lock";
}
