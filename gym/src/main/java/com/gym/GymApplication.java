package com.gym;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients
@EnableScheduling
public class GymApplication {

    public static void main(String[] args) {
        // Sentinel 1.8.6 JVM 级别配置（绕过 Spring Cloud Alibaba 自动配置潜在的 SPI 问题）
        System.setProperty("csp.sentinel.dashboard.server", "localhost:8858");
        System.setProperty("project.name", "main-server");
        SpringApplication.run(GymApplication.class, args);
    }
}
