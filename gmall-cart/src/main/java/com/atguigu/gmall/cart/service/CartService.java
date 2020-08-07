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
import java.util.stream.Collectors;

@Service
public class CartService {


    @Autowired
    private GmallPmsClient pmsClient;
    @Autowired
    private GmallSmsClient smsClient;
    @Autowired
    private GmallWmsClient wmsClient;
    @Autowired
    private StringRedisTemplate redisTemplate;
    @Autowired
    private CartAsyncService cartAsyncService;

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
            this.cartAsyncService.updateCartByUserIdAndSkuId(userId,cart);
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
            this.cartAsyncService.addCart(cart);

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

    public List<Cart>queryCarts() {
        //查询未登录的购物车
        UserInfo userInfo = LoginInterceptor.getUserInfo();
        String userKey = userInfo.getUserKey();
        String unLoginKey = KEY_PREFIX + userKey;
        BoundHashOperations<String, Object, Object> unLoginHashOps = this.redisTemplate.boundHashOps(unLoginKey);
        //未登录购物车集合
        List<Object> unLoginCartJsons = unLoginHashOps.values();
        List<Cart> unLoginCarts = null;
        if (!CollectionUtils.isEmpty(unLoginCartJsons)){
            unLoginCarts = unLoginCartJsons.stream().map(cartJson -> JSON.parseObject(cartJson.toString(), Cart.class)).collect(Collectors.toList());
        }
        //判断登陆状态
        Long userId = userInfo.getUserId();
        //未登录返回未登录购物车
        if (userId == null){
            return unLoginCarts;
        }


        //登录后合并购物车
        String loginKey = KEY_PREFIX + userId;
        BoundHashOperations<String, Object, Object> loginHashOps = this.redisTemplate.boundHashOps(loginKey);
        if (!CollectionUtils.isEmpty(unLoginCarts)){
            unLoginCarts.forEach(cart -> {
                BigDecimal count = cart.getCount();
                if (loginHashOps.hasKey(cart.getSkuId().toString())){
                    String cartJson = loginHashOps.get(cart.getSkuId().toString()).toString();
                    cart = JSON.parseObject(cartJson, Cart.class);
                    cart.setCount(cart.getCount().add(count));
                    //更新redis和mysql中的数量
                    this.cartAsyncService.updateCartByUserIdAndSkuId(userId.toString(),cart);
                } else {
                    cart.setUserId(userId.toString());
                    this.cartAsyncService.addCart(cart);
                }
                loginHashOps.put(cart.getSkuId().toString(),JSON.toJSONString(cart));
//                loginHashOps.delete();
            });
        }
        //删除未登录购物车
        this.cartAsyncService.deleteCartsByUserId(userKey);
        this.redisTemplate.delete(unLoginKey);
        //查询登陆状态购物车
        List<Object> loginCartJsons = loginHashOps.values();
        if (!CollectionUtils.isEmpty(loginCartJsons)){
            return loginCartJsons.stream().map(cartJson -> JSON.parseObject(cartJson.toString(),Cart.class)).collect(Collectors.toList());
        }

        return null;
    }

    public void updateNum(Cart cart) {
        UserInfo userInfo = LoginInterceptor.getUserInfo();
        String userId = null;
        if (userInfo.getUserId() != null){
            userId = userInfo.getUserId().toString();
        } else {
            userId = userInfo.getUserKey();
        }
        String key = KEY_PREFIX + userId;

        BoundHashOperations<String, Object, Object> hashOps = this.redisTemplate.boundHashOps(key);
        BigDecimal count = cart.getCount();
        if (hashOps.hasKey(cart.getSkuId().toString())){
            String cartJson = hashOps.get(cart.getSkuId().toString()).toString();
            cart = JSON.parseObject(cartJson,Cart.class);
            cart.setCount(count);

            this.cartAsyncService.updateCartByUserIdAndSkuId(userId,cart);
            hashOps.put(cart.getSkuId().toString(),JSON.toJSONString(cart));
        }
    }

    public void delectCart(Long skuId) {
        UserInfo userInfo = LoginInterceptor.getUserInfo();
        String userId = null;
        if (userInfo.getUserId() != null){
            userId = userInfo.getUserId().toString();
        } else {
            userId = userInfo.getUserKey();
        }
        String key = KEY_PREFIX + userId;
        BoundHashOperations<String, Object, Object> hashOps = this.redisTemplate.boundHashOps(key);
        if (hashOps.hasKey(skuId.toString())){
            this.cartAsyncService.deleteCartsByUserIdAndSkuId(userId,skuId);
            hashOps.delete(skuId.toString());
        }
    }
}
