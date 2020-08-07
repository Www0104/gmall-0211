package com.atguigu.gmall.cart.controller;

import com.atguigu.gmall.cart.Interceptor.LoginInterceptor;
import com.atguigu.gmall.cart.pojo.Cart;
import com.atguigu.gmall.cart.pojo.UserInfo;
import com.atguigu.gmall.cart.service.CartService;
import com.atguigu.gmall.common.bean.ResponseVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

@Controller
public class CartController {

    @Autowired
    private CartService cartService;

    @GetMapping("cart.html")
    public String queryCarts(Model model){
        List<Cart> carts = this.cartService.queryCarts();
        model.addAttribute("carts",carts);
        return "cart";
    }

    @PostMapping("deleteCart")
    @ResponseBody
    public ResponseVo<Object> delectCart(@RequestParam("skuId") Long skuId){
        this.cartService.delectCart(skuId);
        return ResponseVo.ok();
    }


    @PostMapping("updateNum")
    @ResponseBody
    public ResponseVo<Object> updateNum(@RequestBody Cart cart){
        this.cartService.updateNum(cart);
        return ResponseVo.ok();
    }


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
