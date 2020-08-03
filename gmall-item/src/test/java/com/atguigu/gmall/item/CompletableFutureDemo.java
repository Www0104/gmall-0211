package com.atguigu.gmall.item;

import java.util.concurrent.CompletableFuture;

public class CompletableFutureDemo {

    public static void main(String[] args) {
        CompletableFuture.supplyAsync(() -> {
            System.out.println("处理子任务....supplyAsync方法");
            int i = 1/0;
            return "hello CompletableFuture";
        }).whenCompleteAsync((t,u) -> {
            System.out.println("上一个任务处理完成,开始执行新任务");
            System.out.println("t = " + t);
            System.out.println("u = " + u);
        }).exceptionally(throwable -> {
            System.out.println("上一个任务出现异常: "+throwable);
            return "hello exceptionally";
        });
    }

}
