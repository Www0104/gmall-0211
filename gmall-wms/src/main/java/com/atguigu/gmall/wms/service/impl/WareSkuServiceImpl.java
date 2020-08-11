package com.atguigu.gmall.wms.service.impl;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.wms.vo.SkuLockVo;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.gmall.common.bean.PageResultVo;
import com.atguigu.gmall.common.bean.PageParamVo;

import com.atguigu.gmall.wms.mapper.WareSkuMapper;
import com.atguigu.gmall.wms.entity.WareSkuEntity;
import com.atguigu.gmall.wms.service.WareSkuService;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;


@Service("wareSkuService")
public class WareSkuServiceImpl extends ServiceImpl<WareSkuMapper, WareSkuEntity> implements WareSkuService {

    @Autowired
    private RedissonClient redissonClient;
    @Autowired
    private WareSkuMapper wareSkuMapper;
    @Autowired
    private StringRedisTemplate redisTemplate;

    private static final String KEY_PREFIX = "wms:lock:";
    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Override
    public PageResultVo queryPage(PageParamVo paramVo) {
        IPage<WareSkuEntity> page = this.page(
                paramVo.getPage(),
                new QueryWrapper<WareSkuEntity>()
        );

        return new PageResultVo(page);
    }

    @Transactional
    @Override
    public List<SkuLockVo> checkAndLock(List<SkuLockVo> lockVos, String orderToken) {
        //
        if (CollectionUtils.isEmpty(lockVos)){
            return null;
        }
        lockVos.forEach(lockVo -> {
            this.checkLock(lockVo);
        });
        //判断是否有锁定失败的
        if (lockVos.stream().anyMatch(lockVo -> !lockVo.getLock())) {
            List<SkuLockVo> successLockVos = lockVos.stream().filter(SkuLockVo::getLock).collect(Collectors.toList());
            if (!CollectionUtils.isEmpty(successLockVos)){
                successLockVos.forEach(lockVo -> {
                    this.wareSkuMapper.unlock(lockVo.getWareSkuId(),lockVo.getCount());
                });
            }
            return lockVos.stream().filter(SkuLockVo -> !SkuLockVo.getLock()).collect(Collectors.toList());
        }

        //解锁库存,放入redis缓存
        this.redisTemplate.opsForValue().set(KEY_PREFIX + orderToken, JSON.toJSONString(lockVos));
        this.rabbitTemplate.convertAndSend("ORDER-EXCHANGE","stock.ttl",orderToken);
        //锁定成功返回null
        return null;
    }

    /**
     * 验库存并锁定库存
     */
    private void checkLock(SkuLockVo lockVo){
        RLock fairLock = this.redissonClient.getFairLock("lock:" + lockVo.getSkuId());
        fairLock.lock();

        //查询库存
        List<WareSkuEntity> wareSkuEntities = this.wareSkuMapper.check(lockVo.getSkuId(), lockVo.getCount());
        if (CollectionUtils.isEmpty(wareSkuEntities)){
            lockVo.setLock(false);
            fairLock.unlock();
            return;
        }

        //锁库存
        Long id = wareSkuEntities.get(0).getId();
        if (this.wareSkuMapper.lock(id,lockVo.getCount()) == 1){
            lockVo.setLock(true);
            lockVo.setWareSkuId(id);

        }
        fairLock.unlock();

    }

}