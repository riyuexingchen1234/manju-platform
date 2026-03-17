package com.manju.platform.config;
// 配置类

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

// Spring Boot 不会自动帮你创建 RestTemplate 的 Bean，必须自己手动配置
@Configuration
public class AppConfig {
    // 把 RestTemplate 交给 Spring 容器管理，后面直接 @Autowired 用
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
/**
 * Spring 官方封装的 HTTP 请求工具类，专门用来调用「RESTful API」
 * 不用自己写复杂的 HttpClient、OkHttp 代码，Spring 已经给你封装好了
 * 可以调用第三方接口（比如天气接口、支付接口），也可以调用自己的微服务接口
 * 支持 GET/POST/PUT/DELETE 所有请求方式，完美适配 RESTful 风格
 */

