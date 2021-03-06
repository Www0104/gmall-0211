package com.atguigu.gmall.pms.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.gmall.common.bean.PageResultVo;
import com.atguigu.gmall.common.bean.PageParamVo;

import com.atguigu.gmall.pms.mapper.CategoryMapper;
import com.atguigu.gmall.pms.entity.CategoryEntity;
import com.atguigu.gmall.pms.service.CategoryService;


@Service("categoryService")
public class CategoryServiceImpl extends ServiceImpl<CategoryMapper, CategoryEntity> implements CategoryService {

    @Autowired
    private CategoryMapper categoryMapper;

    @Override
    public PageResultVo queryPage(PageParamVo paramVo) {
        IPage<CategoryEntity> page = this.page(
                paramVo.getPage(),
                new QueryWrapper<CategoryEntity>()
        );

        return new PageResultVo(page);
    }

    @Override
    public List<CategoryEntity> queryCategoriesWithSubByPid(Long pid) {
        return this.categoryMapper.queryCategoriesWithSubByPid(pid);
    }

    @Override
    public List<CategoryEntity> query123CategoriesByCid3(Long cid) {
        CategoryEntity lvl3categoryEntity = this.categoryMapper.selectById(cid);
        CategoryEntity lvl2categoryEntity = this.categoryMapper.selectById(lvl3categoryEntity.getParentId());
        CategoryEntity lvl1categoryEntity = this.categoryMapper.selectById(lvl2categoryEntity.getParentId());
        return Arrays.asList(lvl1categoryEntity,lvl2categoryEntity,lvl3categoryEntity);
    }

}