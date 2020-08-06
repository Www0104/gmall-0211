package com.atguigu.gmall.index.service;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.index.aspect.GmallCache;
import com.atguigu.gmall.index.feign.GmallPmsClient;
import com.atguigu.gmall.index.utils.DistributeLock;
import com.atguigu.gmall.pms.entity.CategoryEntity;
import com.google.common.collect.Lists;
import com.sun.org.apache.bcel.internal.generic.NEW;
import org.apache.commons.lang3.StringUtils;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.PublicKey;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class IndexService {

    @Autowired
    private GmallPmsClient pmsClient;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    private static final String KEY_PREFIX ="index:category:";
    @Autowired
    private DistributeLock distributeLock;
    @Autowired
    private RedissonClient redissonClient;

    public List<CategoryEntity> queryLv1lCategories() {
        ResponseVo<List<CategoryEntity>> listResponseVo = this.pmsClient.queryCategoriesByPid(0L);
        List<CategoryEntity> categoryEntities = listResponseVo.getData();
        return categoryEntities;
    }

    @GmallCache(prefix = KEY_PREFIX,lock = "lock",timeout = 43200,random = 10080)
    public List<CategoryEntity> queryCategoriesWithSubByPid(Long pid) {
//        //判断缓存
//        String json = this.stringRedisTemplate.opsForValue().get(KEY_PREFIX + pid);
//        if (StringUtils.isNotBlank(json)){
//            return JSON.parseArray(json,CategoryEntity.class);
//        }
//
//        RLock lock = this.redissonClient.getFairLock("lock");
//        lock.lock();
//        String json2 = this.stringRedisTemplate.opsForValue().get(KEY_PREFIX + pid);
//        if (StringUtils.isNotBlank(json2)){
//            lock.unlock();
//            return JSON.parseArray(json2,CategoryEntity.class);
//        }
//
//
//        ResponseVo<List<CategoryEntity>> listResponseVo = this.pmsClient.queryCategoriesWithSubByPid(pid);
//        List<CategoryEntity> categoryEntities = listResponseVo.getData();
//        //解决缓存穿透问题
//        this.stringRedisTemplate.opsForValue().set(KEY_PREFIX+pid,JSON.toJSONString(categoryEntities),30+new Random().nextInt(10), TimeUnit.DAYS);
//
//        lock.unlock();
        ResponseVo<List<CategoryEntity>> listResponseVo = this.pmsClient.queryCategoriesWithSubByPid(pid);
        List<CategoryEntity> categoryEntities = listResponseVo.getData();
        return categoryEntities;
    }


//    public void testLock() {
//        RLock lock = this.redissonClient.getLock("lock");
//        lock.lock(50, TimeUnit.SECONDS);
//        String numString = this.stringRedisTemplate.opsForValue().get("num");
//        if (StringUtils.isBlank(numString)) {
//            return;
//        }
//        Integer num = Integer.parseInt(numString);
//        this.stringRedisTemplate.opsForValue().set("num", String.valueOf(++num));
//
//        testSubLock();
//
//        lock.unlock();
//    }
//
//    private void testSubLock() {
//        RLock lock = this.redissonClient.getLock("lock");
//        lock.lock(50, TimeUnit.SECONDS);
//        System.out.println("这是一个子方法，也需要获取锁。。。。。。");
//        lock.unlock();
//    }
//
//    public void testLock3() {
//        String uuid = UUID.randomUUID().toString();
//        this.distributeLock.tryLock("lock", uuid, 30L);
//        String numString = this.stringRedisTemplate.opsForValue().get("num");
//        if (StringUtils.isBlank(numString)) {
//            return;
//        }
//        try {
//            TimeUnit.SECONDS.sleep(300);
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
//        Integer num = Integer.parseInt(numString);
//        this.stringRedisTemplate.opsForValue().set("num", String.valueOf(++num));
//
//        this.testSubLock3("lock", uuid);
//
//        this.distributeLock.unLock("lock", uuid);
//    }
//
//    private void testSubLock3(String lockName, String uuid) {
//
//        this.distributeLock.tryLock(lockName, uuid, 30L);
//
//        System.out.println("这是一个子方法，也需要获取锁。。。。。。");
//
//        this.distributeLock.unLock(lockName, uuid);
//    }
//
//    public void testLock2() throws InterruptedException {
//        String uuid = UUID.randomUUID().toString();
//
//        Boolean lock = this.stringRedisTemplate.opsForValue().setIfAbsent("lock", uuid,3,TimeUnit.SECONDS);
//        if (!lock){
//            Thread.sleep(300);
//            testLock();
//        }else {
//            this.stringRedisTemplate.expire("lock", 30, TimeUnit.SECONDS);
//            String numStr = this.stringRedisTemplate.opsForValue().get("num");
//            if (StringUtils.isBlank(numStr)){
//                return;
//            }
//            Integer anInt = Integer.parseInt(numStr);
//            this.stringRedisTemplate.opsForValue().set("num", String.valueOf(++anInt));
//
//            this.testSubLock();
//            //释放
//
//
//            String script = "if redis.call('get',KEYS[1]) ==ARGV[1] then return redis.call('del',KEYS[1]) else return 0 end;";
//            this.stringRedisTemplate.execute(new DefaultRedisScript<>(script,Long.class), Arrays.asList("lock"),uuid);
//
//            //            if (StringUtils.equals(uuid,this.stringRedisTemplate.opsForValue().get("lock"))){
////                this.stringRedisTemplate.delete("lock");
////            }
//        }
//
//    }
//
//    private void testSubLock2(){
//        String uuid = UUID.randomUUID().toString();
//        Boolean lock = this.stringRedisTemplate.opsForValue().setIfAbsent("lock",uuid,300,TimeUnit.SECONDS);
//        if (!lock){
//            try {
//                Thread.sleep(200);
//                testSubLock();
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
//        }else {
//            // 2.获取到锁执行业务逻辑
//            System.out.println("这是一个子方法，也需要获取锁。。。。。。");
//            // 释放锁
//            String script = "if redis.call('get', KEYS[1]) == ARGV[1] " +
//                    "then return redis.call('del', KEYS[1]) " +
//                    "else return 0 end";
//            this.stringRedisTemplate.execute(new DefaultRedisScript<>(script, Long.class), Arrays.asList("lock"), uuid);
//        }
//    }
}
