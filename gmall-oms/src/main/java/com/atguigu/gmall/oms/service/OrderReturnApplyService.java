package com.atguigu.gmall.oms.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.atguigu.gmall.common.bean.PageResultVo;
import com.atguigu.gmall.common.bean.PageParamVo;
import com.atguigu.gmall.oms.entity.OrderReturnApplyEntity;

import java.util.Map;

/**
 * 订单退货申请
 *
 * @author www
 * @email 1711196043@qq.com
 * @date 2020-08-11 09:08:34
 */
public interface OrderReturnApplyService extends IService<OrderReturnApplyEntity> {

    PageResultVo queryPage(PageParamVo paramVo);
}

