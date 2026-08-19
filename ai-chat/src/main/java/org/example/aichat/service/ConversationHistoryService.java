package org.example.aichat.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
public class ConversationHistoryService {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    // Redis key 前缀，存储在 db0
    private static final String PREFIX = "ai:chat:";
    // 每个用户最多保留的对话轮数（user + assistant 各算一条）
    private static final int MAX_HISTORY = 20;
    // 对话历史过期时间（小时）
    private static final int EXPIRE_HOURS = 2;

    // 待确认操作 key 前缀与过期时间
    private static final String PENDING_PREFIX = "ai:pending:";
    private static final int PENDING_EXPIRE_MINUTES = 30;

    /**
     * 构建用户专属 key：ai:chat:{role}:{userId}
     * 不同角色（MEMBER/EMPLOYEE/ADMIN）即使 userId 相同也不会冲突
     */
    private String buildKey(String role, Integer userId) {
        String r = (role == null || role.isEmpty()) ? "MEMBER" : role.toUpperCase();
        return PREFIX + r + ":" + userId;
    }

    private String buildPendingKey(String role, Integer userId) {
        String r = (role == null || role.isEmpty()) ? "MEMBER" : role.toUpperCase();
        return PENDING_PREFIX + r + ":" + userId;
    }

    /**
     * 保存一轮对话
     */
    public void saveMessage(String role, Integer userId, String roleOfMsg, String content) {
        String key = buildKey(role, userId);
        Map<String, String> msg = Map.of("role", roleOfMsg, "content", content);
        redisTemplate.opsForList().rightPush(key, JSON.toJSONString(msg));
        // 只保留最近 N 条
        Long size = redisTemplate.opsForList().size(key);
        if (size != null && size > MAX_HISTORY) {
            redisTemplate.opsForList().trim(key, size - MAX_HISTORY, -1);
        }
        redisTemplate.expire(key, EXPIRE_HOURS, TimeUnit.HOURS);
    }

    /**
     * 获取历史对话
     */
    public List<Map<String, String>> getHistory(String role, Integer userId) {
        String key = buildKey(role, userId);
        List<Object> list = redisTemplate.opsForList().range(key, 0, -1);
        List<Map<String, String>> history = new ArrayList<>();
        if (list != null) {
            for (Object obj : list) {
                history.add(JSON.parseObject(obj.toString(),
                        new com.alibaba.fastjson.TypeReference<Map<String, String>>() {}));
            }
        }
        return history;
    }

    /**
     * 清空历史
     */
    public void clearHistory(String role, Integer userId) {
        redisTemplate.delete(buildKey(role, userId));
    }

    /**
     * 保存待确认操作（AI 提议后、用户确认前，把操作参数固化到 Redis）
     * @param action 执行命令名（如 BOOK_CLASS / BOOK_MULTI / CHECKIN 等）
     * @param params 执行参数
     */
    public void savePending(String role, Integer userId, String action, JSONObject params) {
        JSONObject obj = new JSONObject();
        obj.put("action", action);
        obj.put("params", params == null ? new JSONObject() : params);
        redisTemplate.opsForValue().set(buildPendingKey(role, userId), obj.toJSONString(),
                PENDING_EXPIRE_MINUTES, TimeUnit.MINUTES);
    }

    /**
     * 读取待确认操作，不存在返回 null
     */
    public JSONObject getPending(String role, Integer userId) {
        Object val = redisTemplate.opsForValue().get(buildPendingKey(role, userId));
        if (val == null) return null;
        return JSON.parseObject(val.toString());
    }

    /**
     * 清除待确认操作
     */
    public void clearPending(String role, Integer userId) {
        redisTemplate.delete(buildPendingKey(role, userId));
    }
}
