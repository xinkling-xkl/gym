package org.example.aichat.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.ChatClient;
import org.springframework.ai.openai.OpenAiChatClient;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring AI 手动配置：创建 ChatClient Bean 指向 DeepSeek API。
 * 从 deepseek.* 配置读取（支持 Nacos 热刷新）。
 */
@Configuration
public class SpringAiConfig {

    private static final Logger log = LoggerFactory.getLogger(SpringAiConfig.class);

    @Value("${deepseek.api-key:}")
    private String apiKey;

    @Value("${deepseek.base-url:https://api.deepseek.com}")
    private String baseUrl;

    @Value("${deepseek.model:deepseek-chat}")
    private String model;

    @Value("${deepseek.temperature:0.7}")
    private float temperature;

    @Value("${deepseek.max-tokens:800}")
    private int maxTokens;

    @Bean
    public ChatClient chatClient() {
        // Spring AI 的 OpenAiApi 会自动拼接 /v1/chat/completions，
        // 如果 baseUrl 以 /v1 结尾则去掉，避免 /v1/v1 重复
        String cleanBaseUrl = baseUrl;
        if (cleanBaseUrl.endsWith("/v1")) {
            cleanBaseUrl = cleanBaseUrl.substring(0, cleanBaseUrl.length() - 3);
        }

        log.info("初始化 Spring AI ChatClient: baseUrl={} model={}", cleanBaseUrl, model);

        if (apiKey == null || apiKey.isBlank()) {
            log.warn("DeepSeek API Key 未配置（deepseek.api-key 为空），AI 功能将不可用");
        }

        OpenAiApi openAiApi = new OpenAiApi(cleanBaseUrl, apiKey);

        OpenAiChatOptions options = OpenAiChatOptions.builder()
                .withModel(model)
                .withTemperature(temperature)
                .withMaxTokens(maxTokens)
                .build();

        return new OpenAiChatClient(openAiApi, options);
    }
}
