package com.manju.platform.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;


/**
 * 跨域资源共享（CORS）配置类
 * 作用：解决前后端分离架构中，浏览器同源策略导致的跨域请求被拦截问题
 * 背景：
 * - 前端通常运行在 http://localhost:5173（Vite开发服务器）
 * - 后端通常运行在 http://localhost:8080
 * - 浏览器默认不允许不同源（协议/域名/端口不同）的请求，需要后端显式配置允许
 */
// Spring注解：标记该类为配置类，Spring容器启动时会自动扫描并加载该类的配置
@Configuration
public class CorsConfig implements WebMvcConfigurer {
    // 实现WebMvcConfigurer接口：Spring MVC提供的Web配置定制接口
    // 重写addCorsMappings方法：添加跨域映射规则
    @Override
    // registry:CORS注册器，用于注册跨域规则
    public void addCorsMappings(CorsRegistry registry) {
        // 1. 匹配所有接口路径：表示后端所有接口都允许跨域访问
        registry.addMapping("/**")
        // 2. 允许的前端源地址：只允许来自Vite开发服务器（默认端口5173）的请求
                .allowedOrigins("http://localhost:5173")
        // 3. 允许的HTTP请求方法：常见的增删改查方法都允许
                .allowedMethods("GET","POST","PUT","DELETE")
        // 4. 允许携带凭证：允许前端请求携带Cookie、Session等认证信息（维持登录状态必须开启）
                .allowCredentials(true)
        // 5. 预检请求缓存时间：单位秒，3600表示1小时内同一请求不再发送预检请求（OPTIONS），提高性能
                .maxAge(3600);
    }

}
