package org.example.aichat.config;

import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@Configuration
public class RestTemplateConfig {

    /**
     * 被 @LoadBalanced 增强的 RestTemplate
     * 可以通过服务名（如 http://main-server/...）发起调用，
     * Spring Cloud LoadBalancer 会从 Nacos 拉取服务实例并做负载均衡
     */
    @Bean
    @LoadBalanced
    public RestTemplate loadBalancedRestTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(10_000);
        factory.setReadTimeout(30_000);
        return new RestTemplate(factory);
    }
}
