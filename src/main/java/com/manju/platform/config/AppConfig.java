package com.manju.platform.config;
// 配置类 全局工具配置中心

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;
// 创建一些整个项目都要用的工具对象，交给 Spring 容器统一管理，
// 这样在任何地方都能直接@Autowired注入使用，不用每次都 new 一个新的。
// @Configuration 注解告诉 Spring：这是一个配置类，启动时自动执行里面的所有@Bean方法。
// @Bean = 告诉 Spring，把这个方法返回的对象放到 Spring 容器里，这样整个项目都能直接注入使用。
@Configuration
public class AppConfig {
    // ObjectMapper JSON解析工具 把 Java 对象 → 转成 JSON 字符串 把 JSON 字符串 → 转成 Java 对象
    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        // 默认情况下，如果 ObjectMapper 要序列化一个空对象（所有字段都是 null 的对象），它会直接抛出异常！
        // 加了如下配置后，即使对象是空的，也不会报错，会正常返回{}
        mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        return mapper;
    }
    // 把 RestTemplate 交给 Spring 容器管理，后面直接 @Autowired 用
    // Spring Boot 不会自动创建 RestTemplate 的 Bean！ 需要手动配置
    // 专门用来调用 AI 接口，并且设置了适合 AI 生成的长超时时间
    @Bean
    public RestTemplate restTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setReadTimeout(120000);  // 读取超时 120秒
        factory.setConnectTimeout(10000);   // 链接超时 10秒
        return new RestTemplate(factory);
    }
}