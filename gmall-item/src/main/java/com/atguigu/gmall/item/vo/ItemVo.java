package com.atguigu.gmall.item.vo;


import com.atguigu.gmall.pms.entity.CategoryEntity;
import com.atguigu.gmall.pms.vo.ItemGroupVo;
import com.atguigu.gmall.pms.vo.SaleAttrValueVo;
import com.atguigu.gmall.pms.entity.SkuImagesEntity;
import com.atguigu.gmall.sms.vo.ItemSaleVo;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
public class ItemVo {
    //三级信息
    private List<CategoryEntity> categories;

    //品牌信息
    private Long brandId;
    private String brandName;

    //品牌属性
    private Long spuId;
    private String spuName;

    //核心部分
    private Long skuId;
    private String title;
    private String subTitle;
    private BigDecimal price;
    private Integer weight;
    private String defaultImage;

    //sku图片列表
    private List<SkuImagesEntity> images;

    //sku营销信息
    private List<ItemSaleVo> sales;
    //是否有货
    private Boolean store = false;

    //销售属性合集
    private List<SaleAttrValueVo> saleAttrs;

    //当前SKU销售属性
    private Map<Long,String> saleAttr;
    //
    private String skusJson;


    //商品描述信息
    private List<String> spuImages;

    //规格与包装
    private List<ItemGroupVo> groups;

}
