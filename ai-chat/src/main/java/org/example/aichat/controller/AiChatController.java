package org.example.aichat.controller;

import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import org.example.aichat.client.AiPythonClient;
import org.example.aichat.common.Result;
import org.example.aichat.service.ConversationHistoryService;
import org.example.aichat.service.PromptBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/ai")
@CrossOrigin(origins = "*")
public class AiChatController {

    @Autowired
    private AiPythonClient aiPythonClient;

    @Autowired
    private ConversationHistoryService historyService;

    @Autowired
    private PromptBuilder promptBuilder;

    @PostMapping("/chat")
    @SentinelResource(value = "ai-chat", blockHandler = "handleBlock")
    public Result<Map<String, Object>> chat(@RequestBody Map<String, Object> body) {
        String message = (String) body.get("message");
        Object userIdObj = body.get("userId");
        Integer userId = userIdObj instanceof Number
                ? ((Number) userIdObj).intValue()
                : Integer.parseInt(userIdObj.toString());

        if (message == null || message.trim().isEmpty()) {
            return Result.error(400, "消息不能为空");
        }

        // 1. 保存用户消息到 Redis
        historyService.saveMessage(userId, "user", message);

        // 2. 构建 RAG 增强的完整消息列表
        List<Map<String, String>> history = historyService.getHistory(userId);
        List<Map<String, String>> messages = promptBuilder.buildMessages(userId, message, history);

        // 3. 调用 Python 服务
        Map<String, Object> pythonRequest = new HashMap<>();
        pythonRequest.put("messages", messages);
        pythonRequest.put("stream", false);

        try {
            Map<String, Object> result = aiPythonClient.chat(pythonRequest);

            // 4. 提取 AI 回复
            String reply = (String) result.getOrDefault("reply", "AI 服务暂时不可用，请稍后重试。");

            // 5. 保存 AI 回复到 Redis
            historyService.saveMessage(userId, "assistant", reply);

            Map<String, Object> data = new HashMap<>();
            data.put("reply", reply);
            data.put("messageCount", history.size() + 2);
            return Result.success(data);
        } catch (Exception e) {
            return Result.error(500, "AI 服务异常: " + e.getMessage());
        }
    }

    @PostMapping("/clear")
    @SentinelResource(value = "ai-clear", blockHandler = "handleBlock")
    public Result<Void> clearHistory(@RequestBody Map<String, Object> body) {
        Object userIdObj = body.get("userId");
        Integer userId = userIdObj instanceof Number
                ? ((Number) userIdObj).intValue()
                : Integer.parseInt(userIdObj.toString());
        historyService.clearHistory(userId);
        return Result.success("对话历史已清空", null);
    }

    public Result<Void> handleBlock(BlockException e) {
        return Result.error(429, "AI 请求过于频繁，请稍后再试");
    }
}
