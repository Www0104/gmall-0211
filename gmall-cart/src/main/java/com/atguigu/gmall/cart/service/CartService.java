package com.atguigu.gmall.cart.service;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.cart.Interceptor.LoginInterceptor;
import com.atguigu.gmall.cart.feign.GmallPmsClient;
import com.atguigu.gmall.cart.feign.GmallSmsClient;
import com.atguigu.gmall.cart.feign.GmallWmsClient;
import com.atguigu.gmall.cart.mapper.CartMapper;
import com.atguigu.gmall.cart.pojo.Cart;
import com.atguigu.gmall.cart.pojo.UserInfo;
import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.pms.entity.SkuAttrValueEntity;
import com.atguigu.gmall.pms.entity.SkuEntity;
import com.atguigu.gmall.sms.vo.ItemSaleVo;
import com.atguigu.gmall.wms.entity.WareSkuEntity;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import org.apache.commons.configuration.resolver.CatalogResolver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import springfox.documentation.spring.web.json.Json;

import java.math.BigDecimal;
import java.util.List;

@Service
public class CartService {

    @Autowired
    private CartMapper cartMapper;
    @Autowired
    private GmallPmsClient pmsClient;
    @Autowired
    private GmallSmsClient smsClient;
    @Autowired
    private GmallWmsClient wmsClient;
    @Autowired
    private StringRedisTemplate redisTemplate;

    private static final String KEY_PREFIX = "cart:info:";

    public void saveCart(Cart cart) {
        //用户登陆信息
        UserInfo userInfo = LoginInterceptor.getUserInfo();
       String userId = null;
        if (userInfo.getUserId() != null) {
            userId = userInfo.getUserId().toString();
        } else {
            userId = userInfo.getUserKey();
        }
        String key = KEY_PREFIX +userId;

        //获取内层的map操作(该用户所有购物车的集合)
        BoundHashOperations<String, Object, Object> hashOps = this.redisTemplate.boundHashOps(key);

        //判断该用户购物车中是否存在该商品
        String skuId = cart.getSkuId().toString();
        BigDecimal count = cart.getCount();
        if (hashOps.hasKey(skuId)){
            //有更新数量
            String cartJson = hashOps.get(skuId).toString();
            cart = JSON.parseObject(cartJson, Cart.class);
            cart.setCount(cart.getCount().add(count));

            //放入mysql及redis
//            this,cartMapper.updateCartByUserIdAndSkuId
            this.cartMapper.update(cart,new UpdateWrapper<Cart>().eq("user_id",userId).eq("sku_id",cart.getSkuId()));
        }else {
            //无则新增记录
            cart.setUserId(userId);
            cart.setCheck(true);

            ResponseVo<SkuEntity> skuEntityResponseVo = this.pmsClient.querySkuById(cart.getSkuId());
            SkuEntity skuEntity = skuEntityResponseVo.getData();
            if (skuEntity != null) {
                cart.setTitle(skuEntity.getTitle());
                cart.setPrice(skuEntity.getPrice());
                cart.setDefaultImage(skuEntity.getDefaultImage());
            }

            ResponseVo<List<WareSkuEntity>> wareResponseVo = this.wmsClient.queryWareSkusBySkuId(cart.getSkuId());
            List<WareSkuEntity> wareSkuEntities = wareResponseVo.getData();
            if (!CollectionUtils.isEmpty(wareSkuEntities)){
                cart.setStore(wareSkuEntities.stream().anyMatch(wareSkuEntity -> wareSkuEntity.getStock() - wareSkuEntity.getStockLocked() >0));

            }

            ResponseVo<List<SkuAttrValueEntity>> saleAttrResponseVo = this.pmsClient.querySaleAttrValueBySkuId(cart.getSkuId());
            List<SkuAttrValueEntity> skuAttrValueEntity = saleAttrResponseVo.getData();
            cart.setSaleAttrs(JSON.toJSONString(skuAttrValueEntity));

            ResponseVo<List<ItemSaleVo>> listResponseVo = this.smsClient.querySalesBySkuId(cart.getSkuId());
            List<ItemSaleVo> itemSaleVos = listResponseVo.getData();
            cart.setSales(JSON.toJSONString(itemSaleVos));

            //新增mysql数据库
            this.cartMapper.insert(cart);

        }

        hashOps.put(skuId,JSON.toJSONString(cart));


    }

    public Cart queryCartBySkuId(Long skuId) {

        String key = KEY_PREFIX;
        UserInfo userInfo = LoginInterceptor.getUserInfo();
        if (userInfo.getUserId() != null){
            key += userInfo.getUserId();
        }else {
            key += userInfo.getUserKey();
        }
        BoundHashOperations<String, Object, Object> hashOps = this.redisTemplate.boundHashOps(key);
        if (hashOps.hasKey(skuId.toString())){
            String cartJson = hashOps.get(skuId.toString()).toString();
            return JSON.parseObject(cartJson,Cart.class);
        }else {
            throw new RuntimeException("不存在对应商品购物车记录");
        }
    }
}
