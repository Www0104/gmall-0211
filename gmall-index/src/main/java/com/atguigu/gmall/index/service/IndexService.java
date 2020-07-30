package com.atguigu.gmall.index.service;

import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.index.feign.GmallPmsClient;
import com.atguigu.gmall.pms.entity.CategoryEntity;
import org.bouncycastle.math.raw.Mod;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class IndexService {

    @Autowired
    private GmallPmsClient pmsClient;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    public List<CategoryEntity> queryLv1lCategories() {
        ResponseVo<List<CategoryEntity>> listResponseVo = this.pmsClient.queryCategoriesByPid(0L);
        List<CategoryEntity> categoryEntities = listResponseVo.getData();
        return categoryEntities;
    }

    public List<CategoryEntity> queryCategoriesWithSubByPid(Long pid) {
        ResponseVo<List<CategoryEntity>> listResponseVo = this.pmsClient.queryCategoriseWithSubByPid(pid);
        List<CategoryEntity> categoryEntities = listResponseVo.getData();
        return categoryEntities;
    }
}
