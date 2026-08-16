package org.example.aichat.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Service
public class DeepSeekService {

    private static final Logger log = LoggerFactory.getLogger(DeepSeekService.class);

    @Value("${deepseek.api-key}")
    private String apiKey;

    @Value("${deepseek.base-url}")
    private String baseUrl;

    @Value("${deepseek.model}")
    private String model;

    /**
     * 带超时配置的 RestTemplate：连接 10 秒，读取 60 秒（AI 生成较慢）
     */
    private final RestTemplate restTemplate;

    public DeepSeekService() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(10_000);   // 10 秒
        factory.setReadTimeout(60_000);      // 60 秒
        this.restTemplate = new RestTemplate(factory);
    }

    public String chat(List<Map<String, String>> messages) {
        // 校验 API Key 是否已配置
        if (apiKey == null || apiKey.isEmpty() || "sk-your-key-here".equals(apiKey)) {
            throw new IllegalStateException("DeepSeek API Key 未配置，请在 application.yml 或 Nacos 中设置 deepseek.api-key");
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        JSONObject body = new JSONObject();
        body.put("model", model);
        body.put("messages", messages);
        body.put("temperature", 0.7);
        body.put("max_tokens", 800);
        body.put("stream", false);

        HttpEntity<String> request = new HttpEntity<>(body.toJSONString(), headers);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(
                    baseUrl + "/chat/completions", request, String.class);

            JSONObject result = JSON.parseObject(response.getBody());
            return result.getJSONArray("choices")
                    .getJSONObject(0)
                    .getJSONObject("message")
                    .getString("content");
        } catch (HttpClientErrorException.Unauthorized e) {
            log.error("DeepSeek API Key 无效: {}", e.getMessage());
            throw new IllegalStateException("API Key 无效或已过期，请检查 deepseek.api-key 配置");
        } catch (HttpClientErrorException.Forbidden e) {
            log.error("DeepSeek API 访问被拒（可能余额不足）: {}", e.getMessage());
            throw new IllegalStateException("API 访问被拒，可能余额不足，请检查 DeepSeek 账户");
        } catch (ResourceAccessException e) {
            log.error("DeepSeek API 网络不可达: {}", e.getMessage());
            throw new IllegalStateException("无法连接 DeepSeek 服务，请检查网络或稍后重试");
        } catch (HttpServerErrorException e) {
            log.error("DeepSeek 服务端异常: {} {}", e.getStatusCode(), e.getMessage());
            throw new IllegalStateException("DeepSeek 服务暂时不可用，请稍后重试");
        }
    }
}
