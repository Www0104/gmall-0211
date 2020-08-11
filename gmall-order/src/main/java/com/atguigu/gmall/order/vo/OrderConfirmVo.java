package com.atguigu.gmall.order.vo;

import com.atguigu.gmall.oms.vo.OrderItemVo;
import com.atguigu.gmall.ums.entity.UserAddressEntity;
import lombok.Data;

import java.util.List;

@Data
public class OrderConfirmVo {

    //收货地址列表
    private List<UserAddressEntity> addresses;

    private List<OrderItemVo> items;

    private Integer bounds;

    private String orderToken;

}
