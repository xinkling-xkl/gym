package org.example.aichat.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.ChatClient;
import org.springframework.ai.chat.ChatResponse;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 基于 Spring AI 的 DeepSeek 对话服务。
 * 通过 OpenAI 兼容接口调用 DeepSeek，ChatClient 由 SpringAiConfig 手动创建。
 */
@Service
public class DeepSeekService {

    private static final Logger log = LoggerFactory.getLogger(DeepSeekService.class);

    private final ChatClient chatClient;

    @Autowired
    public DeepSeekService(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    /**
     * 调用大模型对话，兼容原有 List<Map> 消息格式。
     *
     * @param messages 消息列表，每条含 role（system/user/assistant）和 content
     * @return 模型回复内容
     */
    public String chat(List<Map<String, String>> messages) {
        List<Message> springMessages = new ArrayList<>();
        for (Map<String, String> m : messages) {
            String role = m.get("role");
            String content = m.get("content");
            if (content == null) content = "";
            switch (role) {
                case "system" -> springMessages.add(new SystemMessage(content));
                case "assistant" -> springMessages.add(new AssistantMessage(content));
                default -> springMessages.add(new UserMessage(content));
            }
        }

        try {
            ChatResponse response = chatClient.call(new Prompt(springMessages));
            return response.getResult().getOutput().getContent();
        } catch (Exception e) {
            log.error("Spring AI ChatClient 调用失败: {}", e.getMessage(), e);
            throw new IllegalStateException("AI 服务暂时不可用，请稍后重试：" + e.getMessage());
        }
    }
}
