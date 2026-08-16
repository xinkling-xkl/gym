package org.example.aichat.service;

import com.alibaba.fastjson.JSON;
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

    /**
     * 构建用户专属 key：ai:chat:{role}:{userId}
     * 不同角色（MEMBER/EMPLOYEE/ADMIN）即使 userId 相同也不会冲突
     */
    private String buildKey(String role, Integer userId) {
        String r = (role == null || role.isEmpty()) ? "MEMBER" : role.toUpperCase();
        return PREFIX + r + ":" + userId;
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
}
