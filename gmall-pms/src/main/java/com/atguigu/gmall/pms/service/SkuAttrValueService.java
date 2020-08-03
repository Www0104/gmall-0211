package com.atguigu.gmall.pms.service;

import com.atguigu.gmall.pms.vo.SaleAttrValueVo;
import com.baomidou.mybatisplus.extension.service.IService;
import com.atguigu.gmall.common.bean.PageResultVo;
import com.atguigu.gmall.common.bean.PageParamVo;
import com.atguigu.gmall.pms.entity.SkuAttrValueEntity;

import java.util.List;

/**
 * sku销售属性&值
 *
 * @author www
 * @email 1711196043@qq.com
 * @date 2020-07-21 13:52:10
 */
public interface SkuAttrValueService extends IService<SkuAttrValueEntity> {

    PageResultVo queryPage(PageParamVo paramVo);


    List<SkuAttrValueEntity> querySearchAttrValueBySkuId(Long skuId);

    List<SaleAttrValueVo> queryAllSaleAttrValueBySpuId(Long spuId);

    String querySaleAttrMappingSkuIdBySpuId(Long spuId);
}

