package com.atguigu.gmall.order.service;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.cart.pojo.Cart;
import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.common.bean.UserInfo;
import com.atguigu.gmall.common.exception.OrderException;
import com.atguigu.gmall.oms.entity.OrderEntity;
import com.atguigu.gmall.oms.vo.OrderItemVo;
import com.atguigu.gmall.oms.vo.OrderSubmitVo;
import com.atguigu.gmall.order.Interceptor.LoginInterceptor;
import com.atguigu.gmall.order.feign.*;
import com.atguigu.gmall.order.vo.OrderConfirmVo;
import com.atguigu.gmall.pms.entity.SkuAttrValueEntity;
import com.atguigu.gmall.pms.entity.SkuEntity;
import com.atguigu.gmall.sms.vo.ItemSaleVo;
import com.atguigu.gmall.ums.entity.UserAddressEntity;
import com.atguigu.gmall.ums.entity.UserEntity;
import com.atguigu.gmall.wms.entity.WareSkuEntity;
import com.atguigu.gmall.wms.vo.SkuLockVo;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class OrderService {
    @Autowired
    private GmallOmsClient omsClient;
    @Autowired
    private GmallPmsClient pmsClient;
    @Autowired
    private GmallWmsClient wmsClient;
    @Autowired
    private GmallUmsClient umsClient;
    @Autowired
    private GmallSmsClient smsClient;
    @Autowired
    private GmallCartClient cartClient;
    @Autowired
    private StringRedisTemplate redisTemplate;
    @Autowired
    private RabbitTemplate rabbitTemplate;

    private static final String KEY_PREFIX = "order:token:";

    public OrderConfirmVo confirm(){
        OrderConfirmVo confirmVo = new OrderConfirmVo();
        UserInfo userInfo = LoginInterceptor.getUserInfo();
        Long userId = userInfo.getUserId();

        //收货地址列表
        ResponseVo<List<UserAddressEntity>> addressResponseVo = this.umsClient.queryAddressByUserId(userId);
        List<UserAddressEntity> address = addressResponseVo.getData();
        confirmVo.setAddresses(address);
        //商品列表
        ResponseVo<List<Cart>> cartResponseVo = this.cartClient.queryCheckedCartByUserId(userId);
        List<Cart> carts = cartResponseVo.getData();
        if (CollectionUtils.isEmpty(carts)){
            throw new RuntimeException("您的购物车为空,无购买记录");
        }
        List<OrderItemVo> items = carts.stream().map(cart -> {
            OrderItemVo orderItemVo = new OrderItemVo();
            orderItemVo.setSkuId(cart.getSkuId());
            orderItemVo.setCount(cart.getCount());

            ResponseVo<SkuEntity> skuEntityResponseVo = this.pmsClient.querySkuById(cart.getSkuId());
            SkuEntity skuEntity = skuEntityResponseVo.getData();
            orderItemVo.setTitle(skuEntity.getTitle());
            orderItemVo.setPrice(skuEntity.getPrice());
            orderItemVo.setDefaultImage(skuEntity.getDefaultImage());
            orderItemVo.setWeight(new BigDecimal(skuEntity.getWeight()));

            ResponseVo<List<WareSkuEntity>> wareResponseVo = this.wmsClient.queryWareSkusBySkuId(cart.getSkuId());
            List<WareSkuEntity> wareSkuEntities = wareResponseVo.getData();
            if (!CollectionUtils.isEmpty(wareSkuEntities)){
                 orderItemVo.setStore(wareSkuEntities.stream().anyMatch(wareSkuEntity -> wareSkuEntity.getStock() - wareSkuEntity.getStockLocked() > 0));
            }
            ResponseVo<List<ItemSaleVo>> salesResponseVo = this.smsClient.querySalesBySkuId(cart.getSkuId());
            orderItemVo.setSales(salesResponseVo.getData());

            ResponseVo<List<SkuAttrValueEntity>> saleAttrResponseVo = this.pmsClient.querySaleAttrValueBySkuId(cart.getSkuId());
            List<SkuAttrValueEntity> skuAttrValueEntities = saleAttrResponseVo.getData();
            orderItemVo.setSaleAttrs(skuAttrValueEntities);
            return orderItemVo;
        }).collect(Collectors.toList());
        //查询用户信息及积分
        ResponseVo<UserEntity> userEntityResponseVo = this.umsClient.queryUserById(userId);
        UserEntity userEntity = userEntityResponseVo.getData();
        confirmVo.setBounds(userEntity.getIntegration());

        String orderToken = IdWorker.getTimeId();
        confirmVo.setOrderToken(orderToken);
        this.redisTemplate.opsForValue().set(KEY_PREFIX + orderToken , orderToken);

        return confirmVo;
    }

    public OrderEntity submit(OrderSubmitVo submitVo){
        //防重
        String orderToken = submitVo.getOrderToken();
        if (StringUtils.isBlank(orderToken)){
            throw new OrderException("无");
        }
        String script = "if redis.call('get', KEYS[1]) == ARGV[1] " +
                "then return redis.call('del', KEYS[1]) " +
                "else return 0 end";
        Boolean flag = this.redisTemplate.execute(new DefaultRedisScript<>(script, Boolean.class), Arrays.asList(KEY_PREFIX + orderToken), orderToken);
        if (!flag) {
            throw new OrderException("多次提交过快，请稍后再试！");
        }
        //验价格

        BigDecimal totalPrice = submitVo.getTotalPrice();
        List<OrderItemVo> items = submitVo.getItems();
        if (CollectionUtils.isEmpty(items)){
            throw new OrderException("选择需要购买的物品");
        }
        BigDecimal currentTotalPrice = items.stream().map(item -> {
            ResponseVo<SkuEntity> skuEntityResponseVo = this.pmsClient.querySkuById(item.getSkuId());
            SkuEntity skuEntity = skuEntityResponseVo.getData();
            if (skuEntity == null) {
                return new BigDecimal(0);
            }
            return skuEntity.getPrice().multiply(item.getCount());
        }).reduce((a, b) -> a.add(b)).get();

        if (totalPrice.compareTo(currentTotalPrice) != 0){
            throw new OrderException("页面已经过期");
        }
        //验库存并锁定库存
        List<SkuLockVo> lockVos = items.stream().map(item -> {
            SkuLockVo lockVo = new SkuLockVo();
            lockVo.setSkuId(item.getSkuId());
            lockVo.setCount(item.getCount().intValue());
            return lockVo;
        }).collect(Collectors.toList());
        ResponseVo<List<SkuLockVo>> skuLockVoResponseVo = this.wmsClient.checkAndLock(lockVos,orderToken);
        List<SkuLockVo> skuLockVos = skuLockVoResponseVo.getData();
        if (!CollectionUtils.isEmpty(skuLockVos)){
            throw new OrderException(JSON.toJSONString(skuLockVos));
        }
        Long userId = null;
        OrderEntity orderEntity = null;

        try {
            //下单
            UserInfo userInfo = LoginInterceptor.getUserInfo();
            userId = userInfo.getUserId();
            ResponseVo<OrderEntity> orderEntityResponseVo = this.omsClient.saveOrder(submitVo,userId);
            orderEntity = orderEntityResponseVo.getData();
        } catch (Exception e) {
            e.printStackTrace();
            //发送消息解锁库存
            this.rabbitTemplate.convertAndSend("ORDER-EXCHANGE","order.fail",orderToken);
        }

        //发送消息给购物车,删除购物车
        try {
            Map<String,Object> map = new HashMap<>();
            map.put("userId",userId);
            List<Long> skuIds = items.stream().map(OrderItemVo::getSkuId).collect(Collectors.toList());
            map.put("skuIds",JSON.toJSONString(skuIds));
            this.rabbitTemplate.convertAndSend("ORDER-EXCHANGE","cart.delete",map);
        } catch (AmqpException e) {
            e.printStackTrace();
        }

        return orderEntity;
    }
}
