package com.atguigu.gmall.ums.mapper;

import com.atguigu.gmall.ums.entity.UserEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户表
 * 
 * @author www
 * @email 1711196043@qq.com
 * @date 2020-08-03 23:26:09
 */
@Mapper
public interface UserMapper extends BaseMapper<UserEntity> {
	
}
