package com.ray.atten.middle.module.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

// 该配置用于解决 Spring Boot 2.6+ 启动 Swagger 时的路径匹配报错问题
@Configuration
public class SpringfoxPathMatchingConfig implements WebMvcConfigurer {

    // 解决 Swagger UI 资源文件找不到的问题
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("swagger-ui/**")
                .addResourceLocations("classpath:/META-INF/resources/webjars/springfox-swagger-ui/")
                .resourceChain(false);
    }

}
