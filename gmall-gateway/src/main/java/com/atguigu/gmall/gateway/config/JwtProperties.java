package com.atguigu.gmall.gateway.config;

import com.atguigu.gmall.common.utils.RsaUtils;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;

import javax.annotation.PostConstruct;
import java.security.PublicKey;

@ConfigurationProperties(prefix = "auth.jwt")
@Data
@Slf4j
public class JwtProperties {
    private PublicKey publicKey;


    private String pubKeyPath;
    private String cookieName;

    @PostConstruct
    public void init(){
        try {
            this.publicKey = RsaUtils.getPublicKey(pubKeyPath);
        } catch (Exception e) {
            e.printStackTrace();
            log.error("生成失败"+e.getMessage());
        }
    }
}
