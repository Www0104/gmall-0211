package com.atguigu.gmall.oms.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.atguigu.gmall.common.bean.PageResultVo;
import com.atguigu.gmall.common.bean.PageParamVo;
import com.atguigu.gmall.oms.entity.OrderSettingEntity;

import java.util.Map;

/**
 * 订单配置信息
 *
 * @author www
 * @email 1711196043@qq.com
 * @date 2020-08-11 09:08:34
 */
public interface OrderSettingService extends IService<OrderSettingEntity> {

    PageResultVo queryPage(PageParamVo paramVo);
}

