package com.atguigu.gmall.ums.service.impl;

import org.apache.commons.codec.digest.DigestUtils;
import org.omg.PortableInterceptor.USER_EXCEPTION;
import org.springframework.stereotype.Service;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.gmall.common.bean.PageResultVo;
import com.atguigu.gmall.common.bean.PageParamVo;

import com.atguigu.gmall.ums.mapper.UserMapper;
import com.atguigu.gmall.ums.entity.UserEntity;
import com.atguigu.gmall.ums.service.UserService;

import java.util.Date;
import java.util.UUID;


@Service("userService")
public class UserServiceImpl extends ServiceImpl<UserMapper, UserEntity> implements UserService {

    @Override
    public PageResultVo queryPage(PageParamVo paramVo) {
        IPage<UserEntity> page = this.page(
                paramVo.getPage(),
                new QueryWrapper<UserEntity>()
        );

        return new PageResultVo(page);
    }

    @Override
    public Boolean checkData(String data, Integer type) {
        QueryWrapper<UserEntity> wrapper = new QueryWrapper<>();

        switch (type) {
            case 1 : wrapper.eq("username",data); break;
            case 2 : wrapper.eq("phone",data); break;
            case 3 : wrapper.eq("email",data); break;
            default: return null;
        }

        return this.count(wrapper) == 0 ;
    }

    @Override
    public void register(UserEntity user, String code) {
        //短信验证码

        //生成盐
        String salt = UUID.randomUUID().toString().substring(0, 6);
        user.setSalt(salt);
        //加盐加米
        DigestUtils.md5Hex(user.getPassword()+ salt);
        //用户注册
        user.setLevelId(1L);
        user.setCreateTime(new Date());
        user.setSourceType(1);
        user.setIntegration(1000);
        user.setGrowth(1000);
        user.setStatus(1);
        this.save(user);
        //删除短信验证码


    }

}