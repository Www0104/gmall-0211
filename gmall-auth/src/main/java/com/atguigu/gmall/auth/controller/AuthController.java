package com.atguigu.gmall.auth.controller;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class AuthController {

    @Autowired
    private com.atguigu.gmall.auth.service.AuthService authService;


    @GetMapping("toLogin")
    public String toLogin(@RequestParam(value = "returnUrl",required = false)String returnUrl,Model model){
        model.addAttribute("returnUrl",returnUrl);
        return "login";
    }

    @PostMapping("login")
    public String login(@RequestParam(value = "returnUrl",required = false)String returnUrl,
                        @RequestParam(value = "loginName")String loginName,
                        @RequestParam(value = "password")String password,
                        HttpServletRequest request,
                        HttpServletResponse response) throws Exception {
        this.authService.accredit(loginName,password,request,response);
        if (StringUtils.isNotBlank(returnUrl)){
            return "redirect:"+returnUrl;
        }
        return "redirect:http://gmall.com";
    }

}
