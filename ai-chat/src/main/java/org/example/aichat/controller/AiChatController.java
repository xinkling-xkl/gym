package org.example.aichat.controller;

import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.example.aichat.common.Result;
import org.example.aichat.service.ConversationHistoryService;
import org.example.aichat.service.DeepSeekService;
import org.example.aichat.service.PromptBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@RestController
@RequestMapping("/api/ai")
@CrossOrigin(origins = "*")
public class AiChatController {

    @Autowired
    private DeepSeekService deepSeekService;

    @Autowired
    private ConversationHistoryService historyService;

    @Autowired
    private PromptBuilder promptBuilder;

    private final RestTemplate restTemplate = new RestTemplate();

    @PostMapping("/chat")
    @SentinelResource(value = "ai-chat", blockHandler = "handleBlock")
    public Result<Map<String, Object>> chat(@RequestBody Map<String, Object> body,
                                            @RequestHeader(value = "X-User-Account", required = false) String accountHeader,
                                            @RequestHeader(value = "X-User-Role", required = false) String roleHeader,
                                            @RequestHeader(value = "X-User-Name", required = false) String nameHeader) {
        String message = (String) body.get("message");
        Object userIdObj = body.get("userId");
        Integer userId = userIdObj instanceof Number
                ? ((Number) userIdObj).intValue()
                : Integer.parseInt(userIdObj.toString());

        if (message == null || message.trim().isEmpty()) {
            return Result.error(400, "消息不能为空");
        }

        // 从 Gateway 注入的 Header 获取角色，前端也可传 role 作为兜底
        String role = roleHeader != null ? roleHeader : (String) body.getOrDefault("role", "MEMBER");

        historyService.saveMessage(role, userId, "user", message);
        List<Map<String, String>> history = historyService.getHistory(role, userId);
        List<Map<String, String>> messages = promptBuilder.buildMessages(role, message, history);

        try {
            String reply = deepSeekService.chat(messages);

            // 管理员命令解析：提取 [CMD:xxx] 并执行
            if ("ADMIN".equals(role) && reply.contains("[CMD:")) {
                reply = executeAdminCommand(reply);
            }

            historyService.saveMessage(role, userId, "assistant", reply);

            Map<String, Object> data = new HashMap<>();
            data.put("reply", reply);
            data.put("messageCount", history.size() + 2);
            return Result.success(data);
        } catch (Exception e) {
            return Result.error(500, "AI 服务异常: " + e.getMessage());
        }
    }

    /**
     * 解析管理员命令并执行，返回执行结果
     */
    private String executeAdminCommand(String reply) {
        try {
            int cmdStart = reply.indexOf("[CMD:");
            int cmdEnd = reply.indexOf("]", cmdStart);
            if (cmdStart == -1 || cmdEnd == -1) return reply;

            String cmdPart = reply.substring(cmdStart + 5, cmdEnd);  // 如 ADD_MEMBER
            String[] parts = cmdPart.split("\\{", 2);
            String action = parts[0].trim();
            String jsonStr = parts.length > 1 ? "{" + parts[1] : "{}";
            JSONObject params = JSON.parseObject(jsonStr);

            return executeAction(action, params);
        } catch (Exception e) {
            return "命令执行失败：" + e.getMessage();
        }
    }

    private String executeAction(String action, JSONObject p) {
        return switch (action) {
            case "QUERY" -> {
                String type = p.getString("type");
                yield switch (type) {
                    case "member" -> callMainServer("/api/member/list", "GET", null);
                    case "employee" -> callMainServer("/api/employee/list", "GET", null);
                    case "equipment" -> callMainServer("/api/equipment/list", "GET", null);
                    case "class" -> callMainServer("/api/class/list", "GET", null);
                    case "order" -> callMainServer("/api/order/list", "GET", null);
                    default -> "未知查询类型：" + type;
                };
            }
            case "ADD_MEMBER" -> callMainServer("/api/member/add", "POST", p);
            case "ADD_EMPLOYEE" -> callMainServer("/api/employee/add", "POST", p);
            case "ADD_EQUIPMENT" -> callMainServer("/api/equipment/add", "POST", p);
            case "ADD_CLASS" -> callMainServer("/api/class/add", "POST", p);
            case "DEL_MEMBER" -> callMainServer("/api/member/delete/" + p.get("memberAccount"), "DELETE", null);
            case "DEL_EMPLOYEE" -> callMainServer("/api/employee/delete/" + p.get("employeeAccount"), "DELETE", null);
            case "DEL_EQUIPMENT" -> callMainServer("/api/equipment/delete/" + p.get("equipmentId"), "DELETE", null);
            case "DEL_CLASS" -> callMainServer("/api/class/delete/" + p.get("classId"), "DELETE", null);
            case "DEL_ORDER" -> callMainServer("/api/order/delete/" + p.get("classOrderId"), "DELETE", null);
            case "FIX_EQUIPMENT" -> callMainServer("/api/equipment/update", "PUT", p);
            default -> "未知命令：" + action;
        };
    }

    private String callMainServer(String path, String method, JSONObject body) {
        try {
            String url = "http://main-server" + path;
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            ResponseEntity<String> response;
            if ("GET".equals(method)) {
                response = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers), String.class);
            } else if ("DELETE".equals(method)) {
                response = restTemplate.exchange(url, HttpMethod.DELETE, new HttpEntity<>(headers), String.class);
            } else {
                HttpEntity<String> request = new HttpEntity<>(body != null ? body.toJSONString() : "{}", headers);
                response = restTemplate.exchange(url, method.equals("PUT") ? HttpMethod.PUT : HttpMethod.POST,
                        request, String.class);
            }

            JSONObject result = JSON.parseObject(response.getBody());
            if ((Integer) result.get("code") == 200) {
                return "操作成功！" + (result.get("message") != null ? result.get("message") : "");
            }
            return "操作失败：" + result.get("message");
        } catch (Exception e) {
            return "调用失败：" + e.getMessage();
        }
    }

    @PostMapping("/clear")
    @SentinelResource(value = "ai-clear", blockHandler = "handleBlock")
    public Result<Void> clearHistory(@RequestBody Map<String, Object> body,
                                     @RequestHeader(value = "X-User-Role", required = false) String roleHeader) {
        Object userIdObj = body.get("userId");
        Integer userId = userIdObj instanceof Number
                ? ((Number) userIdObj).intValue()
                : Integer.parseInt(userIdObj.toString());
        String role = roleHeader != null ? roleHeader : (String) body.getOrDefault("role", "MEMBER");
        historyService.clearHistory(role, userId);
        return Result.success("对话历史已清空", null);
    }

    public Result<Void> handleBlock(BlockException e) {
        return Result.error(429, "AI 请求过于频繁，请稍后再试");
    }
}
