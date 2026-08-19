package org.example.aichat.controller;

import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
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

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

        try {
            String reply;

            // 状态机：优先处理「确认/取消」，直接执行已固化的待确认操作，不再依赖 AI 重新推理
            JSONObject pending = historyService.getPending(role, userId);
            if (pending != null && isCancelMessage(message)) {
                historyService.clearPending(role, userId);
                reply = "好的，已取消该操作。如需其他帮助请随时告诉我。";
            } else if (pending != null && isConfirmMessage(message)) {
                String pendingAction = pending.getString("action");
                JSONObject pendingParams = pending.getJSONObject("params");
                if (pendingParams == null) pendingParams = new JSONObject();
                historyService.clearPending(role, userId);
                reply = executeMemberAction(pendingAction, pendingParams, currentUserAccount, history, role, userId);
            } else {
                List<Map<String, String>> messages = promptBuilder.buildMessages(role, message, history);
                reply = deepSeekService.chat(messages);

                // 命令解析：ADMIN 走 executeAdminCommand，MEMBER 走 executeMemberCommand
                if (reply.contains("[CMD:")) {
                    if ("ADMIN".equals(role)) {
                        reply = executeAdminCommand(reply);
                    } else if ("MEMBER".equals(role)) {
                        reply = executeMemberCommand(reply, currentUserAccount, history, role, userId);
                    }
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
     * @param history 当前会话历史（用于命令缺参数时自动补全课程信息）
     * @param role 用户角色（用于 pending 状态存储）
     * @param userId 用户ID（用于 pending 状态存储）
     */
    private String executeMemberCommand(String reply, String account, List<Map<String, String>> history, String role, Integer userId) {
        try {
            int cmdStart = reply.indexOf("[CMD:");
            int cmdEnd = reply.indexOf("]", cmdStart);
            if (cmdStart == -1 || cmdEnd == -1) return reply;

            String cmdPart = reply.substring(cmdStart + 5, cmdEnd);
            String[] parts = cmdPart.split("\\{", 2);
            String action = parts[0].trim();
            String jsonStr = parts.length > 1 ? "{" + parts[1] : "{}";
            JSONObject params = JSON.parseObject(jsonStr);

            return executeMemberAction(action, params, account, history, role, userId);
        } catch (Exception e) {
            return "命令执行失败：" + e.getMessage();
        }
    }

    private String executeMemberAction(String action, JSONObject p, String account, List<Map<String, String>> history, String role, Integer userId) {
        return switch (action) {
            case "PROPOSE" -> {
                // 提议写操作：把参数固化到 Redis，等待用户确认（状态机）
                String proposeAction = p.getString("action");
                if (proposeAction == null || proposeAction.isEmpty()) {
                    yield "抱歉，我无法理解该操作，请重新描述。";
                }
                JSONObject execParams = new JSONObject();
                for (String key : p.keySet()) {
                    if (!"action".equals(key) && !"confirmText".equals(key)) {
                        execParams.put(key, p.get(key));
                    }
                }
                historyService.savePending(role, userId, proposeAction, execParams);
                String confirmText = p.getString("confirmText");
                yield (confirmText != null && !confirmText.isEmpty())
                        ? confirmText
                        : "好的，我已准备好执行该操作，请回复'确认'或'取消'。";
            }
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
            case "QUERY_CLASS" -> formatClassList(callMainServerResult("/api/class/available", "GET", null, account));
            case "QUERY_CLASS_DETAIL" -> {
                Integer classId = p.getInteger("classId");
                if (classId == null) {
                    yield "查询失败：缺少课程ID（classId）";
                }
                yield formatClassDetail(callMainServerResult("/api/class/" + classId, "GET", null, account));
            }
            case "BOOK_CLASS" -> {
                // 预约课程：按课程ID预约（AI 已查询到具体课程并获用户确认后调用）
                Integer classId = p.getInteger("classId");
                if (classId == null) {
                    // AI 未带参数：自动从对话历史中提取最近提到的课程ID
                    List<Integer> ids = extractClassIdsFromHistory(history);
                    if (ids != null && !ids.isEmpty()) {
                        classId = ids.get(ids.size() - 1);
                    }
                }
                if (classId == null) {
                    yield "预约失败：缺少课程ID（classId），请告诉我课程名称或星期几，如「帮我预约瑜伽课」或「帮我预约周一的课」";
                }
                JSONObject orderBody = new JSONObject();
                orderBody.put("classId", classId);
                orderBody.put("memberAccount", account);
                String bookResult = callMainServer("/api/order/add", "POST", orderBody, account);
                if (bookResult != null && bookResult.contains("操作成功")) {
                    callPlanServerResult("/api/plan/autoSync/" + account, "POST", null, account);
                    yield bookResult + "\n📅 已自动同步到您的健身计划";
                }
                yield bookResult;
            }
            case "BOOK_CLASS_BY_NAME" -> {
                // 按课程名称或星期几智能匹配并预约
                String keyword = p.getString("keyword");
                if (keyword == null || keyword.trim().isEmpty()) {
                    // AI 未带参数：自动从对话历史中提取最近提到的课程名
                    String name = extractCourseNameFromHistory(history);
                    if (name != null) {
                        keyword = name;
                    }
                }
                if (keyword == null || keyword.trim().isEmpty()) {
                    yield "请告诉我课程名称或星期几，如「帮我预约周一的瑜伽课」";
                }
                yield smartBookClass(account, keyword.trim());
            }
            case "BOOK_MULTI" -> {
                // 批量预约多门课程
                JSONArray classIds = p.getJSONArray("classIds");
                if (classIds == null || classIds.isEmpty()) {
                    // AI 未带参数：自动从对话历史中提取最近确认的课程ID列表
                    List<Integer> ids = extractClassIdsFromHistory(history);
                    if (ids != null && !ids.isEmpty()) {
                        classIds = new JSONArray();
                        classIds.addAll(ids);
                    }
                }
                if (classIds == null || classIds.isEmpty()) {
                    yield "请告诉我课程名称或星期几，如「帮我预约瑜伽课」或「帮我预约周一的课」";
                }
                yield bookMultipleClasses(account, classIds);
            }
            case "QUERY_MY_ORDERS" -> formatOrders(callMainServerResult("/api/order/member/" + account, "GET", null, account));
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
            case "QUERY_MY_PLANS" -> formatPlans(callPlanServerResult("/api/plan/member/" + account, "GET", null, account));
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

    private static final Set<String> CONFIRM_WORDS = Set.of("确认", "确定", "好的", "好", "是的", "可以", "嗯", "行", "ok", "yes");
    private static final Set<String> CANCEL_WORDS = Set.of("取消", "不用了", "算了", "不要了", "不了", "cancel", "no");

    private boolean isConfirmMessage(String message) {
        if (message == null) return false;
        return CONFIRM_WORDS.contains(message.trim().toLowerCase());
    }

    private boolean isCancelMessage(String message) {
        if (message == null) return false;
        return CANCEL_WORDS.contains(message.trim().toLowerCase());
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
     * 智能创建健身计划：根据会员 BMI 和目标自动生成计划及训练项
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

            // 3. 根据目标 + BMI 生成训练项明细
            JSONArray items = buildPlanItems(goal, bmi);

            // 4. 构造计划（含训练项）
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
            plan.put("items", items);

            // 5. 调用 plan-server 一次性创建计划 + 训练项
            JSONObject result = callPlanServerResult("/api/plan/createWithItems", "POST", plan, account);
            if (result == null || result.getInteger("code") == null || result.getInteger("code") != 200) {
                return "创建失败：" + (result != null ? result.getString("message") : "服务不可用");
            }

            // 6. 生成友好文本（含训练项清单）
            StringBuilder sb = new StringBuilder();
            sb.append("🏋️ 根据您的身体数据已生成专属健身计划\n");
            sb.append("━━━━━━━━━━━━━━━\n");
            sb.append(String.format("身高：%.1f cm | 体重：%.1f kg | BMI：%.1f（%s）\n",
                    height, weight, bmi, bmiCategory(bmi)));
            sb.append(String.format("目标：%s\n", goal));
            sb.append(String.format("周期：%s 至 %s\n", start, end));
            sb.append("━━━━━━━━━━━━━━━\n");
            sb.append("📋 训练安排：\n");
            for (int i = 0; i < items.size(); i++) {
                JSONObject it = items.getJSONObject(i);
                sb.append("• ").append(dayOfWeekName(it.getInteger("dayOfWeek")))
                        .append("：").append(it.getString("exercise"));
                if (it.getInteger("duration") != null) sb.append(" ").append(it.getInteger("duration")).append("分钟");
                if (it.getInteger("sets") != null) sb.append(" ").append(it.getInteger("sets")).append("组");
                if (it.getInteger("reps") != null) sb.append("×").append(it.getInteger("reps")).append("次");
                if (it.getString("notes") != null) sb.append("（").append(it.getString("notes")).append("）");
                sb.append("\n");
            }
            sb.append("\n✅ 计划及训练项已创建，请在「健身计划」页面查看详情");
            return sb.toString();
        } catch (Exception e) {
            return "智能创建计划失败：" + e.getMessage();
        }
    }

    /**
     * 根据目标 + BMI 生成训练项明细列表
     */
    private JSONArray buildPlanItems(String goal, double bmi) {
        JSONArray items = new JSONArray();
        switch (goal) {
            case "增肌" -> {
                items.add(planItem(1, "胸肌训练", 60, 4, 12, "哑铃卧推"));
                items.add(planItem(3, "背部训练", 60, 4, 12, "引体向上"));
                items.add(planItem(5, "腿部训练", 60, 4, 12, "深蹲"));
                items.add(planItem(7, "手臂训练", 45, 4, 12, "杠铃弯举"));
            }
            case "减脂" -> {
                if (bmi >= 28) {
                    items.add(planItem(1, "有氧训练", 40, null, null, "慢跑/椭圆机，心率120-140"));
                    items.add(planItem(2, "低强度力量", 30, 3, 15, "哑铃"));
                    items.add(planItem(3, "有氧训练", 40, null, null, "慢跑/椭圆机"));
                    items.add(planItem(4, "低强度力量", 30, 3, 15, "哑铃"));
                    items.add(planItem(5, "有氧训练", 40, null, null, "慢跑/椭圆机"));
                    items.add(planItem(6, "游泳或骑行", 60, null, null, "中低强度"));
                } else {
                    items.add(planItem(1, "有氧训练", 30, null, null, "慢跑"));
                    items.add(planItem(2, "HIIT训练", 20, null, null, "高强度间歇"));
                    items.add(planItem(3, "有氧训练", 30, null, null, "慢跑"));
                    items.add(planItem(4, "HIIT训练", 20, null, null, "高强度间歇"));
                    items.add(planItem(5, "有氧训练", 30, null, null, "慢跑"));
                    items.add(planItem(6, "力量训练", 30, 4, 12, "哑铃"));
                }
            }
            case "塑形" -> {
                items.add(planItem(1, "上半身塑形", 45, 4, 12, "俯卧撑+哑铃飞鸟"));
                items.add(planItem(3, "下半身塑形", 45, 4, 15, "深蹲+弓步蹲"));
                items.add(planItem(5, "核心训练", 30, 4, 20, "平板支撑+卷腹"));
                items.add(planItem(7, "瑜伽/拉伸", 30, null, null, "放松拉伸"));
            }
            default -> {
                items.add(planItem(1, "有氧训练", 30, null, null, "慢跑/骑行"));
                items.add(planItem(2, "力量训练", 30, null, null, "哑铃基础动作"));
                items.add(planItem(3, "有氧训练", 30, null, null, "慢跑/骑行"));
                items.add(planItem(4, "力量训练", 30, null, null, "哑铃基础动作"));
                items.add(planItem(5, "有氧训练", 30, null, null, "慢跑/骑行"));
                items.add(planItem(6, "瑜伽/拉伸", 30, null, null, "放松拉伸"));
            }
        }
        return items;
    }

    /**
     * 构造单个训练项
     */
    private JSONObject planItem(int dayOfWeek, String exercise, int duration, Integer sets, Integer reps, String notes) {
        JSONObject item = new JSONObject();
        item.put("dayOfWeek", dayOfWeek);
        item.put("exercise", exercise);
        item.put("duration", duration);
        item.put("sets", sets);
        item.put("reps", reps);
        item.put("notes", notes);
        item.put("completed", 0);
        return item;
    }

    /**
     * 星期几转中文
     */
    private String dayOfWeekName(int dayOfWeek) {
        return switch (dayOfWeek) {
            case 1 -> "周一";
            case 2 -> "周二";
            case 3 -> "周三";
            case 4 -> "周四";
            case 5 -> "周五";
            case 6 -> "周六";
            case 7 -> "周日";
            default -> "周" + dayOfWeek;
        };
    }

    /**
     * 智能预约：根据课程名称或星期几匹配可预约课程并预约
     */
    private String smartBookClass(String account, String keyword) {
        // 1. 查询可预约课程列表
        JSONObject result = callMainServerResult("/api/class/available", "GET", null, account);
        if (result == null || result.getInteger("code") == null || result.getInteger("code") != 200) {
            return "查询课程失败，请稍后重试";
        }
        JSONArray data = result.getJSONArray("data");
        if (data == null || data.isEmpty()) {
            return "当前暂无可预约的课程";
        }

        // 2. 判断 keyword 是星期几还是课程名
        int targetDay = weekdayToNumber(keyword);

        // 3. 匹配课程
        List<JSONObject> matched = new ArrayList<>();
        for (int i = 0; i < data.size(); i++) {
            JSONObject c = data.getJSONObject(i);
            String className = c.getString("className");
            String classBegin = c.getString("classBegin");

            if (targetDay > 0) {
                // 按星期几匹配
                if (parseClassDayOfWeek(classBegin) == targetDay) {
                    matched.add(c);
                }
            } else {
                // 按课程名模糊匹配
                if (className != null && className.contains(keyword)) {
                    matched.add(c);
                }
            }
        }

        // 4. 处理匹配结果
        if (matched.isEmpty()) {
            return "抱歉，没有找到匹配「" + keyword + "」的可预约课程。您可以问我「有什么课程」查看全部课程";
        }

        if (matched.size() == 1) {
            // 唯一匹配，直接预约
            JSONObject c = matched.get(0);
            Integer classId = c.getInteger("classId");
            JSONObject orderBody = new JSONObject();
            orderBody.put("classId", classId);
            orderBody.put("memberAccount", account);
            String bookResult = callMainServer("/api/order/add", "POST", orderBody, account);

            StringBuilder sb = new StringBuilder();
            sb.append("已为您匹配到「").append(c.getString("className")).append("」，正在预约...\n");
            sb.append(bookResult);

            // 预约成功后，自动同步到健身计划
            if (bookResult != null && bookResult.contains("操作成功")) {
                JSONObject syncResult = callPlanServerResult("/api/plan/autoSync/" + account, "POST", null, account);
                if (syncResult != null && syncResult.getInteger("code") != null && syncResult.getInteger("code") == 200) {
                    sb.append("\n📅 已自动同步到您的健身计划");
                }
            }
            return sb.toString();
        }

        // 多个匹配，让用户选择
        StringBuilder sb = new StringBuilder("找到多门匹配「" + keyword + "」的课程：\n");
        for (int i = 0; i < matched.size(); i++) {
            JSONObject c = matched.get(i);
            sb.append("【ID:").append(c.getInteger("classId")).append("】 ").append(c.getString("className"));
            if (c.getString("classBegin") != null) sb.append("（").append(c.getString("classBegin")).append("）");
            if (c.getString("coach") != null) sb.append(" ").append(c.getString("coach"));
            sb.append("\n");
        }
        sb.append("\n请告诉我具体要预约哪门课程的ID，如「帮我预约ID为5的课程」");
        return sb.toString();
    }

    /**
     * 批量预约多门课程
     */
    private String bookMultipleClasses(String account, JSONArray classIds) {
        StringBuilder sb = new StringBuilder("📋 批量预约结果：\n");
        int success = 0;
        for (int i = 0; i < classIds.size(); i++) {
            Integer classId = classIds.getInteger(i);
            if (classId == null) continue;
            JSONObject orderBody = new JSONObject();
            orderBody.put("classId", classId);
            orderBody.put("memberAccount", account);
            String r = callMainServer("/api/order/add", "POST", orderBody, account);
            sb.append("• 课程ID ").append(classId).append("：").append(r).append("\n");
            if (r != null && r.contains("操作成功")) {
                success++;
            }
        }

        // 至少一门预约成功后，自动同步到健身计划
        if (success > 0) {
            JSONObject syncResult = callPlanServerResult("/api/plan/autoSync/" + account, "POST", null, account);
            if (syncResult != null && syncResult.getInteger("code") != null && syncResult.getInteger("code") == 200) {
                sb.append("\n📅 已自动同步到您的健身计划");
            }
        }
        return sb.toString();
    }

    /**
     * 从对话历史中提取课程ID（匹配 "ID:16"、"ID 16"、"ID：16"、"（ID 7）" 等写法）
     * 从最近的助手消息往前找，返回第一条含ID消息中的全部ID（保持出现顺序）
     */
    private List<Integer> extractClassIdsFromHistory(List<Map<String, String>> history) {
        if (history == null) return null;
        Pattern pattern = Pattern.compile("ID[\\s:：]*(\\d+)");
        for (int i = history.size() - 1; i >= 0; i--) {
            Map<String, String> msg = history.get(i);
            if (!"assistant".equals(msg.get("role"))) continue;
            String content = msg.get("content");
            if (content == null) continue;
            Matcher m = pattern.matcher(content);
            List<Integer> ids = new ArrayList<>();
            while (m.find()) {
                try {
                    ids.add(Integer.parseInt(m.group(1)));
                } catch (NumberFormatException ignored) {
                }
            }
            if (!ids.isEmpty()) return ids;
        }
        return null;
    }

    /**
     * 从对话历史中提取最近提到的课程名（「」或『』包裹的内容）
     */
    private String extractCourseNameFromHistory(List<Map<String, String>> history) {
        if (history == null) return null;
        Pattern pattern = Pattern.compile("[「『]([^」』]{1,30})[」』]");
        for (int i = history.size() - 1; i >= 0; i--) {
            Map<String, String> msg = history.get(i);
            if (!"assistant".equals(msg.get("role"))) continue;
            String content = msg.get("content");
            if (content == null || content.contains("[CMD:")) continue;
            Matcher m = pattern.matcher(content);
            String last = null;
            while (m.find()) {
                last = m.group(1).trim();
            }
            if (last != null) return last;
        }
        return null;
    }

    /**
     * 中文星期几转数字（1-7），无法识别返回 -1
     */
    private int weekdayToNumber(String weekday) {
        if (weekday == null) return -1;
        return switch (weekday) {
            case "周一", "星期一", "礼拜一", "周1", "星期1" -> 1;
            case "周二", "星期二", "礼拜二", "周2", "星期2" -> 2;
            case "周三", "星期三", "礼拜三", "周3", "星期3" -> 3;
            case "周四", "星期四", "礼拜四", "周4", "星期4" -> 4;
            case "周五", "星期五", "礼拜五", "周5", "星期5" -> 5;
            case "周六", "星期六", "礼拜六", "周6", "星期6" -> 6;
            case "周日", "周天", "星期日", "星期天", "礼拜日", "礼拜天", "周7", "星期7" -> 7;
            default -> -1;
        };
    }

    /**
     * 解析课程开课时间的星期几（1-7），解析失败返回 -1
     */
    private int parseClassDayOfWeek(String classBegin) {
        if (classBegin == null || classBegin.isEmpty()) return -1;
        try {
            LocalDateTime dt;
            if (classBegin.contains("T")) {
                dt = LocalDateTime.parse(classBegin);
            } else {
                dt = LocalDateTime.parse(classBegin, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            }
            return dt.getDayOfWeek().getValue();
        } catch (Exception e) {
            return -1;
        }
    }

    /**
     * 调用 plan-server 服务（健身计划微服务）
     */
    private String callPlanServer(String path, String method, JSONObject body, String account) {
        return callService("http://plan-server" + path, method, body, account);
    }

    /**
     * 调用 plan-server 并返回完整 Result JSON（含 code/message/data）
     */
    private JSONObject callPlanServerResult(String path, String method, JSONObject body, String account) {
        return callServiceResult("http://plan-server" + path, method, body, account);
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
                    case "member" -> formatAdminQuery("member", callMainServerResult("/api/member/list", "GET", null, null));
                    case "employee" -> formatAdminQuery("employee", callMainServerResult("/api/employee/list", "GET", null, null));
                    case "equipment" -> formatAdminQuery("equipment", callMainServerResult("/api/equipment/list", "GET", null, null));
                    case "class" -> formatAdminQuery("class", callMainServerResult("/api/class/list", "GET", null, null));
                    case "order" -> formatAdminQuery("order", callMainServerResult("/api/order/list", "GET", null, null));
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
     * 调用 main-server 并返回完整 Result JSON（含 code/message/data）
     */
    private JSONObject callMainServerResult(String path, String method, JSONObject body, String account) {
        return callServiceResult("http://main-server" + path, method, body, account);
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

    /**
     * 通用服务调用，返回完整的 Result JSON 对象（含 code/message/data）
     * 供查询命令格式化自然语言使用
     */
    private JSONObject callServiceResult(String url, String method, JSONObject body, String account) {
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
            return JSON.parseObject(response.getBody());
        } catch (Exception e) {
            return null;
        }
    }

    // ==================== 查询结果格式化（自然语言） ====================

    /**
     * 格式化课程列表（可预约课程）
     */
    private String formatClassList(JSONObject result) {
        if (result == null || result.getInteger("code") == null || result.getInteger("code") != 200) {
            return "查询课程失败，请稍后重试";
        }
        JSONArray data = result.getJSONArray("data");
        if (data == null || data.isEmpty()) {
            return "当前暂无可预约的课程，您可以过会儿再来看看～";
        }
        StringBuilder sb = new StringBuilder("📋 当前可预约课程如下：\n");
        for (int i = 0; i < data.size(); i++) {
            JSONObject c = data.getJSONObject(i);
            sb.append("【ID:").append(c.getInteger("classId")).append("】 ").append(c.getString("className"));
            if (c.getString("coach") != null) sb.append("（").append(c.getString("coach")).append("）");
            sb.append("\n");
            if (c.getString("classBegin") != null) sb.append("   ⏰ ").append(c.getString("classBegin"));
            if (c.getString("classTime") != null) sb.append("   ⏱️ ").append(c.getString("classTime"));
            Integer max = c.getInteger("maxCapacity");
            Integer booked = c.getInteger("bookedCount");
            if (max != null && max > 0) {
                int remain = max - (booked == null ? 0 : booked);
                sb.append("   👥 剩余名额 ").append(remain).append("/").append(max);
            }
            sb.append("\n");
        }
        sb.append("\n💡 想预约哪节课？告诉我课程ID即可，如「帮我预约ID为5的课程」");
        return sb.toString();
    }

    /**
     * 格式化单个课程详情
     */
    private String formatClassDetail(JSONObject result) {
        if (result == null || result.getInteger("code") == null || result.getInteger("code") != 200) {
            return "查询课程详情失败，请稍后重试";
        }
        JSONObject c = result.getJSONObject("data");
        if (c == null) return "课程不存在";
        StringBuilder sb = new StringBuilder("📄 课程详情：\n");
        sb.append("课程：").append(c.getString("className")).append("\n");
        if (c.getString("coach") != null) sb.append("教练：").append(c.getString("coach")).append("\n");
        if (c.getString("classBegin") != null) sb.append("开课时间：").append(c.getString("classBegin")).append("\n");
        if (c.getString("classTime") != null) sb.append("时长：").append(c.getString("classTime")).append("\n");
        Integer max = c.getInteger("maxCapacity");
        Integer booked = c.getInteger("bookedCount");
        if (max != null && max > 0) {
            sb.append("剩余名额：").append(max - (booked == null ? 0 : booked)).append("/").append(max).append("\n");
        }
        sb.append("\n💡 需要预约吗？回复「确认预约」即可");
        return sb.toString();
    }

    /**
     * 格式化我的预约记录
     */
    private String formatOrders(JSONObject result) {
        if (result == null || result.getInteger("code") == null || result.getInteger("code") != 200) {
            return "查询预约失败，请稍后重试";
        }
        JSONArray data = result.getJSONArray("data");
        if (data == null || data.isEmpty()) {
            return "您当前没有预约任何课程";
        }
        StringBuilder sb = new StringBuilder("📋 您的预约记录：\n");
        for (int i = 0; i < data.size(); i++) {
            JSONObject o = data.getJSONObject(i);
            sb.append(i + 1).append(". ").append(o.getString("className"));
            if (o.getString("coach") != null) sb.append("（").append(o.getString("coach")).append("）");
            sb.append("\n");
            if (o.getString("classBegin") != null) sb.append("   ⏰ ").append(o.getString("classBegin")).append("\n");
            if (o.getString("status") != null) sb.append("   状态：").append(statusText(o.getString("status"))).append("\n");
            if (o.getInteger("classOrderId") != null) sb.append("   订单ID：").append(o.getInteger("classOrderId")).append("\n");
        }
        sb.append("\n💡 如需取消预约，回复「取消订单X」（X为订单ID）");
        return sb.toString();
    }

    /**
     * 格式化我的健身计划
     */
    private String formatPlans(JSONObject result) {
        if (result == null || result.getInteger("code") == null || result.getInteger("code") != 200) {
            return "查询健身计划失败，请稍后重试";
        }
        JSONArray data = result.getJSONArray("data");
        if (data == null || data.isEmpty()) {
            return "您还没有健身计划，可以对我说「根据我的身体数据制定健身计划」来生成一个";
        }
        StringBuilder sb = new StringBuilder("📋 您的健身计划：\n");
        for (int i = 0; i < data.size(); i++) {
            JSONObject p = data.getJSONObject(i);
            sb.append(i + 1).append(". ").append(p.getString("planName"));
            if (p.getString("goal") != null) sb.append("（目标：").append(p.getString("goal")).append("）");
            sb.append("\n");
            if (p.getString("startDate") != null && p.getString("endDate") != null) {
                sb.append("   📅 ").append(p.getString("startDate")).append(" ~ ").append(p.getString("endDate")).append("\n");
            }
            if (p.getString("status") != null) sb.append("   状态：").append(planStatusText(p.getString("status"))).append("\n");
            JSONArray items = p.getJSONArray("items");
            if (items != null && !items.isEmpty()) {
                int done = 0;
                for (int j = 0; j < items.size(); j++) {
                    Integer completed = items.getJSONObject(j).getInteger("completed");
                    if (completed != null && completed == 1) done++;
                }
                sb.append("   训练项：").append(done).append("/").append(items.size()).append(" 已完成\n");
            }
        }
        return sb.toString();
    }

    /**
     * 订单状态转中文
     */
    private String statusText(String status) {
        return switch (status) {
            case "BOOKED" -> "已预约";
            case "COMPLETED" -> "已完成";
            case "NO_SHOW" -> "已旷课";
            case "CANCELED" -> "已取消";
            default -> status;
        };
    }

    /**
     * 计划状态转中文
     */
    private String planStatusText(String status) {
        return switch (status) {
            case "ACTIVE" -> "进行中";
            case "COMPLETED" -> "已完成";
            case "ARCHIVED" -> "已归档";
            default -> status;
        };
    }

    /**
     * 格式化管理员查询结果（按类型展示关键字段）
     */
    private String formatAdminQuery(String type, JSONObject result) {
        if (result == null || result.getInteger("code") == null || result.getInteger("code") != 200) {
            return "查询失败，请稍后重试";
        }
        JSONArray data = result.getJSONArray("data");
        if (data == null || data.isEmpty()) {
            return "未查询到相关数据";
        }
        StringBuilder sb = new StringBuilder("📋 查询结果（共 ").append(data.size()).append(" 条）：\n");
        for (int i = 0; i < data.size(); i++) {
            JSONObject o = data.getJSONObject(i);
            sb.append(i + 1).append(". ");
            switch (type) {
                case "member" -> {
                    sb.append(o.getString("memberName"));
                    if (o.getInteger("memberAccount") != null) sb.append("（账号 ").append(o.getInteger("memberAccount")).append("）");
                    if (o.getString("memberGender") != null) sb.append(" · ").append(o.getString("memberGender"));
                    if (o.getString("cardExpireDate") != null) sb.append(" · 卡到期 ").append(o.getString("cardExpireDate"));
                }
                case "employee" -> {
                    sb.append(o.getString("employeeName"));
                    if (o.getInteger("employeeAccount") != null) sb.append("（账号 ").append(o.getInteger("employeeAccount")).append("）");
                    if (o.getString("staff") != null) sb.append(" · ").append(o.getString("staff"));
                }
                case "equipment" -> {
                    sb.append(o.getString("equipmentName"));
                    if (o.getString("equipmentStatus") != null) sb.append(" · ").append(o.getString("equipmentStatus"));
                    if (o.getString("equipmentLocation") != null) sb.append(" · ").append(o.getString("equipmentLocation"));
                }
                case "class" -> {
                    sb.append(o.getString("className"));
                    if (o.getString("coach") != null) sb.append(" · ").append(o.getString("coach"));
                    if (o.getString("classBegin") != null) sb.append(" · ").append(o.getString("classBegin"));
                }
                case "order" -> {
                    sb.append(o.getString("className"));
                    if (o.getString("memberName") != null) sb.append(" · ").append(o.getString("memberName"));
                    if (o.getString("status") != null) sb.append(" · ").append(statusText(o.getString("status")));
                }
            }
            sb.append("\n");
        }
        return sb.toString();
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
