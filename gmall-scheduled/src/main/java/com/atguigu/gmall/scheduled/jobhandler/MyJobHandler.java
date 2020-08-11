package com.atguigu.gmall.scheduled.jobhandler;

import com.xxl.job.core.biz.model.ReturnT;
import com.xxl.job.core.handler.annotation.XxlJob;
import org.springframework.stereotype.Component;

@Component
public class MyJobHandler {

    @XxlJob("myJobHandler")
    public ReturnT<String> executor(String param){
        System.out.println("这个是第一个定时任务");


        return ReturnT.SUCCESS;
    }

}
