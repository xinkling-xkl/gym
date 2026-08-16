package com.gym.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI 3 接口文档配置
 * 启动后访问 http://localhost:8084/swagger-ui.html 查看在线文档
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI gymOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("健身房管理系统 API 文档")
                        .description("main-server 微服务接口文档：会员、课程、订单、签到、器材、统计等模块")
                        .version("1.0.0")
                        .contact(new Contact().name("gym-system")));
    }
}
