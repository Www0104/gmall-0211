package com.atguigu.gmall.pms.service.impl;

import com.atguigu.gmall.pms.entity.*;
import com.atguigu.gmall.pms.feign.GmallSmsClient;
import com.atguigu.gmall.pms.mapper.SkuMapper;
import com.atguigu.gmall.pms.mapper.SpuDescMapper;
import com.atguigu.gmall.pms.service.*;
import com.atguigu.gmall.pms.vo.BaseAttrValueVo;
import com.atguigu.gmall.pms.vo.SkuVo;
import com.atguigu.gmall.pms.vo.SpuVo;
import com.atguigu.gmall.sms.vo.SkuSaleVo;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.gmall.common.bean.PageResultVo;
import com.atguigu.gmall.common.bean.PageParamVo;

import com.atguigu.gmall.pms.mapper.SpuMapper;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;


@Service("spuService")
public class SpuServiceImpl extends ServiceImpl<SpuMapper, SpuEntity> implements SpuService {

    @Autowired
    private SpuDescMapper descMapper;
    @Autowired
    private SpuAttrValueService spuAttrValueService;

    @Autowired
    private SkuMapper skuMapper;
    @Autowired
    private SkuImagesService imagesService;
    @Autowired
    private SkuAttrValueService attrValueService;
    @Autowired
    private GmallSmsClient smsClient;


    @Override
    public PageResultVo queryPage(PageParamVo paramVo) {
        IPage<SpuEntity> page = this.page(
                paramVo.getPage(),
                new QueryWrapper<SpuEntity>()
        );

        return new PageResultVo(page);
    }

    @Override
    public PageResultVo querySpuByPageAndCid3(Long cid, PageParamVo paramVo) {
        QueryWrapper<SpuEntity> wrapper = new QueryWrapper<>();

        if (cid != 0){
            wrapper.eq("category_id",cid);
        }
        String key = paramVo.getKey();
        if (StringUtils.isNotBlank(key)){
            wrapper.and(t ->  t.like("name",key).or().eq("id",key));
        }


        IPage<SpuEntity> page = this.page(
                paramVo.getPage(),wrapper
        );

        return new PageResultVo(page);
    }

    @Override
    @Transactional
    public void bigSave(SpuVo spuVo) {
        //保存spu相关信息
        spuVo.setCreateTime(new Date());
        spuVo.setUpdateTime(spuVo.getCreateTime());
        this.save(spuVo);
        Long spuId = spuVo.getId();

        //保存spu描述相关信息
        List<String> spuImages = spuVo.getSpuImages();
        if (!CollectionUtils.isEmpty(spuImages)){
            SpuDescEntity descEntity = new SpuDescEntity();
            descEntity.setSpuId(spuId);
            descEntity.setDecript(StringUtils.join(spuImages,","));
            this.descMapper.insert(descEntity);
        }

        //保存spu基本属性
        List<BaseAttrValueVo> baseAttrs = spuVo.getBaseAttrs();
        if (!CollectionUtils.isEmpty(baseAttrs)){
            List<SpuAttrValueEntity> spuAttrValueEntities = baseAttrs.stream().map(baseAttrValueVo -> {
                SpuAttrValueEntity spuAttrValueEntity = new SpuAttrValueEntity();
                BeanUtils.copyProperties(baseAttrValueVo, spuAttrValueEntity);
                spuAttrValueEntity.setSpuId(spuId);
                return spuAttrValueEntity;
            }).collect(Collectors.toList());
            this.spuAttrValueService.saveBatch(spuAttrValueEntities);
        }


        //保存sku相关信息
        List<SkuVo> skus = spuVo.getSkus();
        if (CollectionUtils.isEmpty(skus)){
            return;
        }

        skus.forEach(skuVo -> {
            //保存sku表
            skuVo.setSpuId(spuId);
            skuVo.setBrandId(spuVo.getBrandId());
            skuVo.setCatagoryId(spuVo.getCategoryId());
            List<String> images = skuVo.getImages();
            if (!CollectionUtils.isEmpty(images)){
                skuVo.setDefaultImage (StringUtils.isNotBlank(skuVo.getDefaultImage()) ? skuVo.getDefaultImage() : images.get(0));
            }
            this.skuMapper.insert(skuVo);
            Long skuId = skuVo.getId();
            //保存图片
             if (!CollectionUtils.isEmpty(images)){
                 List<SkuImagesEntity> imagesEntities = images.stream().map(image -> {
                     SkuImagesEntity skuImagesEntity = new SkuImagesEntity();
                     skuImagesEntity.setSkuId(skuId);
                     skuImagesEntity.setUrl(image);
                     skuImagesEntity.setDefaultStatus(StringUtils.equals(image, skuVo.getDefaultImage()) ? 1 : 0);
                     return skuImagesEntity;
                 }).collect(Collectors.toList());
                 this.imagesService.saveBatch(imagesEntities);
             }


            //保存销售属性
            List<SkuAttrValueEntity> saleAttrs = skuVo.getSaleAttrs();
             if (!CollectionUtils.isEmpty(saleAttrs)){
                 saleAttrs.forEach(saleAttr -> saleAttr.setSkuId(skuId));
                 this.attrValueService.saveBatch(saleAttrs);
             }

            //营销信息
            SkuSaleVo skuSaleVo = new SkuSaleVo();
            BeanUtils.copyProperties(skuVo,skuSaleVo);
            skuSaleVo.setSkuId(skuId);
            this.smsClient.saveSkuSales(skuSaleVo);
            //积分信息

            //满减信息

            //打折信息
        });



    }

}