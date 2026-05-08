package com.manju.platform;

import com.manju.platform.service.AIService;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@SpringBootApplication
public class ManjuPlatformApplication {

    /**
     * 密码加密器：供用户注册、登录时加密/校验密码
     */
    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    public static void main(String[] args) {
//		AIService.enableTestMode();   // 启用测试模式（供Jmeter使用）
        SpringApplication.run(ManjuPlatformApplication.class, args);
    }
}
