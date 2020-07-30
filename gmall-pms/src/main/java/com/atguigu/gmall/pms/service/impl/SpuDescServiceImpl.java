package com.atguigu.gmall.pms.service.impl;

import com.atguigu.gmall.pms.vo.SpuVo;
import com.sun.deploy.util.StringUtils;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.gmall.common.bean.PageResultVo;
import com.atguigu.gmall.common.bean.PageParamVo;

import com.atguigu.gmall.pms.mapper.SpuDescMapper;
import com.atguigu.gmall.pms.entity.SpuDescEntity;
import com.atguigu.gmall.pms.service.SpuDescService;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;


@Service("spuDescService")
public class SpuDescServiceImpl extends ServiceImpl<SpuDescMapper, SpuDescEntity> implements SpuDescService {

    @Override
    public PageResultVo queryPage(PageParamVo paramVo) {
        IPage<SpuDescEntity> page = this.page(
                paramVo.getPage(),
                new QueryWrapper<SpuDescEntity>()
        );

        return new PageResultVo(page);
    }

    @Override
    @Transactional
    public void saveSpuDesc(SpuVo spuVo, Long spuId) {
        List<String> spuImages = spuVo.getSpuImages();
        if (!CollectionUtils.isEmpty(spuImages)){
            SpuDescEntity descEntity = new SpuDescEntity();
            descEntity.setSpuId(spuId);
            descEntity.setDecript(StringUtils.join(spuImages, ","));
            this.save(descEntity);
        }
    }

}