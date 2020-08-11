package com.atguigu.gmall.scheduled.jobhandler;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.scheduled.mapper.CartMapper;
import com.atguigu.gmall.scheduled.pojo.Cart;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.xxl.job.core.biz.model.ReturnT;
import com.xxl.job.core.handler.annotation.XxlJob;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.BoundListOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.List;

@Component
public class CartJobHandler {

    @Autowired
    private StringRedisTemplate redisTemplate;
    @Autowired
    private CartMapper cartMapper;

    private static final String KEY = "cart:async:exception";

    private static final String KEY_PREFIX = "cart:info:";


    @XxlJob("cartJobHandler")
    public ReturnT<String> executor(String param){
        BoundListOperations<String, String> listOps = this.redisTemplate.boundListOps(KEY);
        String userId = listOps.rightPop();

        while (StringUtils.isNotBlank(userId)){

            this.cartMapper.delete(new QueryWrapper<Cart>().eq("user_id",userId));

            BoundHashOperations<String, Object, Object> hashOps = this.redisTemplate.boundHashOps(KEY_PREFIX + userId);
            List<Object> cartJsons = hashOps.values();
            if (CollectionUtils.isEmpty(cartJsons)){
                userId = listOps.rightPop();
                continue;
            }

            cartJsons.forEach(cartJson ->{
                Cart cart = JSON.parseObject(cartJson.toString(),Cart.class);
                this.cartMapper.insert(cart);
            });

            userId = listOps.rightPop();
        }
        return ReturnT.SUCCESS;
    }

}
