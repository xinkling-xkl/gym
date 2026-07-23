package org.example.login;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients
public class LoginApplication {

    public static void main(String[] args) {
        // Sentinel 1.8.6 JVM 级别配置（绕过 Spring Cloud Alibaba 自动配置潜在的 SPI 问题）
        System.setProperty("csp.sentinel.dashboard.server", "localhost:8858");
        System.setProperty("project.name", "login-server");
        SpringApplication.run(LoginApplication.class, args);
    }

}
