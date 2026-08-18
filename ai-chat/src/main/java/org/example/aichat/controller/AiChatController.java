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
import org.springframework.beans.factory.annotation.Qualifier;
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

    /**
     * 被 @LoadBalanced 增强的 RestTemplate，
     * 可以通过服务名（main-server/plan-server）发起调用
     */
    @Autowired
    @Qualifier("loadBalancedRestTemplate")
    private RestTemplate restTemplate;

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
        // 当前登录用户账号（会员命令执行时必须用此账号，防止越权）
        String currentUserAccount = accountHeader != null ? accountHeader : String.valueOf(userId);

        historyService.saveMessage(role, userId, "user", message);
        List<Map<String, String>> history = historyService.getHistory(role, userId);
        List<Map<String, String>> messages = promptBuilder.buildMessages(role, message, history);

        try {
            String reply = deepSeekService.chat(messages);

            // 命令解析：ADMIN 走 executeAdminCommand，MEMBER 走 executeMemberCommand
            if (reply.contains("[CMD:")) {
                if ("ADMIN".equals(role)) {
                    reply = executeAdminCommand(reply);
                } else if ("MEMBER".equals(role)) {
                    reply = executeMemberCommand(reply, currentUserAccount);
                }
            }

            historyService.saveMessage(role, userId, "assistant", reply);

            Map<String, Object> data = new HashMap<>();
            data.put("reply", reply);
            data.put("messageCount", history.size() + 2);
            return Result.success(data);
        } catch (IllegalStateException e) {
            // API Key 等配置错误
            return Result.error(500, e.getMessage());
        } catch (Exception e) {
            return Result.error(500, "AI 服务异常: " + e.getMessage());
        }
    }

    /**
     * 加载历史对话（前端刷新页面时调用）
     */
    @GetMapping("/history")
    @SentinelResource(value = "ai-history", blockHandler = "handleBlock")
    public Result<List<Map<String, String>>> getHistory(@RequestHeader(value = "X-User-Account", required = false) String accountHeader,
                                                        @RequestHeader(value = "X-User-Role", required = false) String roleHeader,
                                                        @RequestParam(required = false) Integer userId,
                                                        @RequestParam(required = false) String role) {
        // 优先用 header 中的信息（走网关时）
        String useRole = roleHeader != null ? roleHeader : (role != null ? role : "MEMBER");
        Integer useUserId = userId;
        if (useUserId == null && accountHeader != null) {
            try {
                useUserId = Integer.parseInt(accountHeader);
            } catch (NumberFormatException e) {
                return Result.success(Collections.emptyList());
            }
        }
        if (useUserId == null) {
            return Result.success(Collections.emptyList());
        }

        List<Map<String, String>> history = historyService.getHistory(useRole, useUserId);
        return Result.success(history);
    }

    // ==================== 会员命令执行 ====================

    /**
     * 解析并执行会员命令
     * @param reply AI 回复内容
     * @param account 当前会员账号（从 X-User-Account 注入，防止越权）
     */
    private String executeMemberCommand(String reply, String account) {
        try {
            int cmdStart = reply.indexOf("[CMD:");
            int cmdEnd = reply.indexOf("]", cmdStart);
            if (cmdStart == -1 || cmdEnd == -1) return reply;

            String cmdPart = reply.substring(cmdStart + 5, cmdEnd);
            String[] parts = cmdPart.split("\\{", 2);
            String action = parts[0].trim();
            String jsonStr = parts.length > 1 ? "{" + parts[1] : "{}";
            JSONObject params = JSON.parseObject(jsonStr);

            return executeMemberAction(action, params, account);
        } catch (Exception e) {
            return "命令执行失败：" + e.getMessage();
        }
    }

    private String executeMemberAction(String action, JSONObject p, String account) {
        return switch (action) {
            case "CHECKIN" -> {
                // 签到打卡：GYM 自主训练签到 / CLASS 课程签到
                String checkInType = p.getString("checkInType");
                if (checkInType == null || checkInType.isEmpty()) {
                    checkInType = "GYM";
                }
                JSONObject checkInBody = new JSONObject();
                checkInBody.put("memberAccount", account);
                checkInBody.put("checkInType", checkInType);
                if ("CLASS".equals(checkInType)) {
                    Integer classOrderId = p.getInteger("classOrderId");
                    if (classOrderId == null) {
                        yield "课程签到失败：缺少订单ID（classOrderId），请先输出 [CMD:QUERY_MY_ORDERS] 查询您的预约";
                    }
                    checkInBody.put("classOrderId", classOrderId);
                }
                // 查询会员姓名（签到接口需要 memberName）
                String memberName = queryMemberName(account);
                checkInBody.put("memberName", memberName);
                yield callMainServer("/api/checkin", "POST", checkInBody, account);
            }
            case "QUERY_CLASS" -> callMainServer("/api/class/list", "GET", null, account);
            case "QUERY_CLASS_DETAIL" -> {
                Integer classId = p.getInteger("classId");
                if (classId == null) {
                    yield "查询失败：缺少课程ID（classId）";
                }
                yield callMainServer("/api/class/" + classId, "GET", null, account);
            }
            case "BOOK_CLASS" -> {
                // 预约课程：需要 classId，先查课程详情补全 className/coach/classBegin
                Integer classId = p.getInteger("classId");
                if (classId == null) {
                    yield "预约失败：缺少课程ID（classId）";
                }
                // 查询课程详情
                String classInfo = callMainServer("/api/class/" + classId, "GET", null, account);
                // 简化处理：直接调用预约接口，让后端补全
                JSONObject orderBody = new JSONObject();
                orderBody.put("classId", classId);
                orderBody.put("memberAccount", account);
                yield callMainServer("/api/order/add", "POST", orderBody, account);
            }
            case "QUERY_MY_ORDERS" -> callMainServer("/api/order/member/" + account, "GET", null, account);
            case "CANCEL_ORDER" -> {
                Integer orderId = p.getInteger("classOrderId");
                if (orderId == null) {
                    yield "取消失败：缺少订单ID（classOrderId）";
                }
                yield callMainServer("/api/order/cancel/" + orderId, "POST", null, account);
            }
            case "CREATE_PLAN" -> {
                // 创建健身计划：调用 plan-server 服务
                JSONObject planBody = new JSONObject();
                planBody.put("planName", p.getString("planName"));
                planBody.put("goal", p.getString("goal"));
                planBody.put("startDate", p.getString("startDate"));
                planBody.put("endDate", p.getString("endDate"));
                planBody.put("status", "ACTIVE");
                yield callPlanServer("/api/plan/add", "POST", planBody, account);
            }
            case "QUERY_MY_PLANS" -> callPlanServer("/api/plan/member/" + account, "GET", null, account);
            case "QUERY_MY_BMI" -> {
                // 查询会员身体数据并计算 BMI
                JSONObject data = callMainServerForData("/api/member/" + account, "GET", null, account);
                if (data == null) {
                    yield "查询失败：无法获取您的身体数据";
                }
                try {
                    Double height = data.getDouble("memberHeight");
                    Double weight = data.getDouble("memberWeight");
                    String name = data.getString("memberName");
                    String gender = data.getString("memberGender");
                    Integer age = data.getInteger("memberAge");

                    if (height == null || weight == null || height <= 0) {
                        yield "查询失败：您的身高或体重信息不完整，请先到前台补全个人资料";
                    }

                    // BMI = 体重(kg) / 身高²(m)
                    double heightM = height / 100.0;
                    double bmi = weight / (heightM * heightM);
                    String category = bmiCategory(bmi);

                    StringBuilder sb = new StringBuilder();
                    sb.append("📊 会员身体数据报告\n");
                    sb.append("━━━━━━━━━━━━━━━\n");
                    sb.append(String.format("姓名：%s（%s %d岁）\n", name, gender, age));
                    sb.append(String.format("身高：%.1f cm\n", height));
                    sb.append(String.format("体重：%.1f kg\n", weight));
                    sb.append(String.format("BMI：%.1f（%s）\n", bmi, category));
                    sb.append("━━━━━━━━━━━━━━━\n");
                    sb.append("💡 健康建议：\n");
                    sb.append(bmiAdvice(bmi));
                    yield sb.toString();
                } catch (Exception e) {
                    yield "查询身体数据失败：" + e.getMessage();
                }
            }
            case "SMART_CREATE_PLAN" -> {
                // 智能创建健身计划：根据会员 BMI 自动生成计划
                String goal = p.getString("goal");
                if (goal == null || goal.isEmpty()) {
                    goal = "健康";
                }
                yield smartCreatePlan(account, goal);
            }
            default -> "未知命令：" + action;
        };
    }

    /**
     * 查询会员姓名（签到接口需要）
     */
    private String queryMemberName(String account) {
        try {
            JSONObject data = callMainServerForData("/api/member/" + account, "GET", null, account);
            if (data != null) {
                String name = data.getString("memberName");
                return name != null ? name : "会员";
            }
        } catch (Exception ignored) {}
        return "会员";
    }

    /**
     * BMI 等级判定
     */
    private String bmiCategory(double bmi) {
        if (bmi < 18.5) return "偏瘦";
        if (bmi < 24) return "正常";
        if (bmi < 28) return "偏胖";
        return "肥胖";
    }

    /**
     * BMI 对应的健康建议
     */
    private String bmiAdvice(double bmi) {
        if (bmi < 18.5) {
            return "您体重偏瘦，建议以增肌为主，增加蛋白质摄入，每周训练4-5次，重点做力量训练。";
        } else if (bmi < 24) {
            return "您体重正常，建议保持现有状态，以塑形和体能提升为主，每周训练3-4次，有氧+力量结合。";
        } else if (bmi < 28) {
            return "您体重偏胖，建议以减脂为主，控制饮食热量，每周训练4-5次，有氧运动占60%。";
        } else {
            return "您体重肥胖，建议以减脂为主，严格控制饮食，每周训练5-6次，以有氧运动为主，配合低强度力量训练。";
        }
    }

    /**
     * 智能创建健身计划：根据会员 BMI 和目标自动生成
     */
    private String smartCreatePlan(String account, String goal) {
        try {
            // 1. 查询会员身体数据
            JSONObject member = callMainServerForData("/api/member/" + account, "GET", null, account);
            if (member == null) {
                return "创建失败：无法获取您的身体数据";
            }
            Double height = member.getDouble("memberHeight");
            Double weight = member.getDouble("memberWeight");
            if (height == null || weight == null || height <= 0) {
                return "创建失败：您的身高或体重信息不完整，请先到前台补全个人资料";
            }

            // 2. 计算 BMI
            double heightM = height / 100.0;
            double bmi = weight / (heightM * heightM);

            // 3. 根据目标 + BMI 生成计划内容
            JSONObject plan = new JSONObject();
            String planName = switch (goal) {
                case "增肌" -> "智能增肌计划";
                case "减脂" -> "智能减脂计划";
                case "塑形" -> "智能塑形计划";
                default -> "智能健康计划";
            };
            plan.put("planName", planName);
            plan.put("goal", goal);
            plan.put("status", "ACTIVE");

            // 起止日期：今天 → 一个月后
            java.time.LocalDate start = java.time.LocalDate.now();
            java.time.LocalDate end = start.plusMonths(1);
            plan.put("startDate", start.toString());
            plan.put("endDate", end.toString());

            // 4. 调用 plan-server 创建计划
            String createResult = callPlanServer("/api/plan/add", "POST", plan, account);

            // 5. 根据 BMI + 目标生成训练建议
            StringBuilder sb = new StringBuilder();
            sb.append("🏋️ 根据您的身体数据已生成专属健身计划\n");
            sb.append("━━━━━━━━━━━━━━━\n");
            sb.append(String.format("身高：%.1f cm | 体重：%.1f kg | BMI：%.1f（%s）\n",
                    height, weight, bmi, bmiCategory(bmi)));
            sb.append(String.format("目标：%s\n", goal));
            sb.append(String.format("周期：%s 至 %s\n", start, end));
            sb.append("━━━━━━━━━━━━━━━\n");
            sb.append("📋 训练建议：\n");
            sb.append(buildTrainingAdvice(goal, bmi));
            sb.append("\n✅ 计划已创建，请在「健身计划」页面查看详情");
            return sb.toString();
        } catch (Exception e) {
            return "智能创建计划失败：" + e.getMessage();
        }
    }

    /**
     * 根据目标 + BMI 生成训练建议
     */
    private String buildTrainingAdvice(String goal, double bmi) {
        return switch (goal) {
            case "增肌" -> """
                    • 周一：胸肌训练（哑铃卧推 4组×8-12次）
                    • 周三：背部训练（引体向上 4组×8-12次）
                    • 周五：腿部训练（深蹲 4组×8-12次）
                    • 周日：手臂训练（杠铃弯举 4组×12次）
                    • 每日补充蛋白质 1.5-2g/kg 体重""";
            case "减脂" -> {
                if (bmi >= 28) {
                    yield """
                            • 周一/三/五：有氧训练（慢跑/椭圆机 40分钟，心率120-140）
                            • 周二/四：低强度力量训练（哑铃 3组×15次）
                            • 周六：游泳或骑行 60分钟
                            • 每日热量赤字 500kcal，控制碳水摄入""";
                }
                yield """
                        • 周一/三/五：有氧训练（慢跑 30分钟）
                        • 周二/四：HIIT 高强度间歇训练 20分钟
                        • 周六：力量训练（哑铃 4组×12次）
                        • 每日热量赤字 300kcal，均衡饮食""";
            }
            case "塑形" -> """
                    • 周一：上半身塑形（俯卧撑+哑铃飞鸟 4组×12次）
                    • 周三：下半身塑形（深蹲+弓步蹲 4组×15次）
                    • 周五：核心训练（平板支撑+卷腹 4组×20次）
                    • 周日：瑜伽或拉伸 30分钟
                    • 保持蛋白质摄入，控制脂肪摄入""";
            default -> """
                    • 周一/三/五：有氧训练 30分钟（慢跑/骑行）
                    • 周二/四：力量训练 30分钟（哑铃基础动作）
                    • 周六：瑜伽或拉伸 30分钟
                    • 保持均衡饮食，每日饮水 2L""";
        };
    }

    /**
     * 调用 plan-server 服务（健身计划微服务）
     */
    private String callPlanServer(String path, String method, JSONObject body, String account) {
        return callService("http://plan-server" + path, method, body, account);
    }

    // ==================== 管理员命令执行 ====================

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

    /**
     * 调用 main-server（gym 主服务）
     */
    private String callMainServer(String path, String method, JSONObject body) {
        return callService("http://main-server" + path, method, body, null);
    }

    private String callMainServer(String path, String method, JSONObject body, String account) {
        return callService("http://main-server" + path, method, body, account);
    }

    /**
     * 调用 main-server 并返回 data 的原始 JSON 对象（供需要解析数据的场景使用）
     */
    private JSONObject callMainServerForData(String path, String method, JSONObject body, String account) {
        return callServiceForData("http://main-server" + path, method, body, account);
    }

    /**
     * 通用服务调用方法
     * @param account 当前会员账号，会注入到 X-User-Account header，供下游服务（如 plan-server）做鉴权
     */
    private String callService(String url, String method, JSONObject body, String account) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            // 注入用户身份 header，供下游服务（plan-server 的 AuthInterceptor）鉴权
            if (account != null && !account.isEmpty()) {
                headers.set("X-User-Account", account);
                headers.set("X-User-Role", "MEMBER");
            }

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
            Integer code = result.getInteger("code");
            if (code != null && code == 200) {
                Object data = result.get("data");
                if (data != null) {
                    return "操作成功！\n" + JSON.toJSONString(data, true);
                }
                return "操作成功！" + (result.get("message") != null ? result.get("message") : "");
            }
            return "操作失败：" + result.get("message");
        } catch (Exception e) {
            return "调用失败：" + e.getMessage();
        }
    }

    /**
     * 通用服务调用方法，返回 data 字段的原始 JSON 对象（不带"操作成功"前缀）
     * 供需要解析返回数据的方法使用（如查询会员身体数据计算 BMI）
     * @return data 的 JSONObject，失败时返回 null
     */
    private JSONObject callServiceForData(String url, String method, JSONObject body, String account) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            if (account != null && !account.isEmpty()) {
                headers.set("X-User-Account", account);
                headers.set("X-User-Role", "MEMBER");
            }

            ResponseEntity<String> response;
            if ("GET".equals(method)) {
                response = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers), String.class);
            } else if ("DELETE".equals(method)) {
                response = restTemplate.exchange(url, HttpMethod.DELETE, new HttpEntity<>(headers), String.class);
            } else {
                HttpEntity<String> request = new HttpEntity<>(body != null ? body.toJSONString() : "{}", headers);
                response = restTemplate.exchange(url, "PUT".equals(method) ? HttpMethod.PUT : HttpMethod.POST,
                        request, String.class);
            }

            JSONObject result = JSON.parseObject(response.getBody());
            Integer code = result.getInteger("code");
            if (code != null && code == 200) {
                return result.getJSONObject("data");
            }
            return null;
        } catch (Exception e) {
            return null;
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
