package com.atguigu.gmall.oms.vo;

import com.atguigu.gmall.ums.entity.UserAddressEntity;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
@Data
public class OrderSubmitVo {


    private String orderToken; //防重
    private BigDecimal totalPrice; //验价
    private UserAddressEntity address;//收货地址
    private Integer payType;//支付方式
    private String deliveryCompany;//快递方式

    private List<OrderItemVo> items;
    private Integer bounds;
    private BigDecimal postFee;

}