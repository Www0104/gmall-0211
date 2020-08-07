package com.atguigu.gmall.cart.controller;

import com.atguigu.gmall.cart.Interceptor.LoginInterceptor;
import com.atguigu.gmall.cart.pojo.Cart;
import com.atguigu.gmall.cart.pojo.UserInfo;
import com.atguigu.gmall.cart.service.CartService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import javax.servlet.http.HttpServletRequest;

@Controller
public class CartController {

    @Autowired
    private CartService cartService;

    @GetMapping
    public String addCart(Cart cart){
        if (cart == null || cart.getSkuId() == null){
            throw new RuntimeException("未选择物品");
        }
        this.cartService.saveCart(cart);
        return "redirect:http://cart.gmall.com/addCart?skuId="+cart.getSkuId();
    }

    @GetMapping("addCart")
    public String queryCartBySkuId(@RequestParam("skuId")Long skuId, Model model){
        Cart cart = this.cartService.queryCartBySkuId(skuId);
        model.addAttribute("cart",cart);
        return "addCart";
    }

    @GetMapping("test")
    @ResponseBody
    public String test(HttpServletRequest request){
//        System.out.println(request.getAttribute("userId"));
//        System.out.println(request.getAttribute("userKey"));
        UserInfo userInfo = LoginInterceptor.getUserInfo();
        System.out.println("userInfo = " + userInfo);
        return "hello";
    }
}
