package com.manju.platform.config;
// 配置类

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

// Spring Boot 不会自动帮你创建 RestTemplate 的 Bean，必须自己手动配置
@Configuration
public class AppConfig {
    // 把 RestTemplate 交给 Spring 容器管理，后面直接 @Autowired 用
    @Bean
    public RestTemplate restTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setReadTimeout(120000);  // 读取超时 120秒
        factory.setConnectTimeout(10000);   // 链接超时 10秒
        return new RestTemplate(factory);
    }
}


