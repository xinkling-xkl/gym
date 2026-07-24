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

    private static final String PREFIX = "ai:chat:";
    private static final int MAX_HISTORY = 20;
    private static final int EXPIRE_HOURS = 2;

    /**
     * 保存一轮对话
     */
    public void saveMessage(Integer userId, String role, String content) {
        String key = PREFIX + userId;
        Map<String, String> msg = Map.of("role", role, "content", content);
        redisTemplate.opsForList().rightPush(key, JSON.toJSONString(msg));
        // 只保留最近 N 轮
        Long size = redisTemplate.opsForList().size(key);
        if (size != null && size > MAX_HISTORY) {
            redisTemplate.opsForList().trim(key, size - MAX_HISTORY, -1);
        }
        redisTemplate.expire(key, EXPIRE_HOURS, TimeUnit.HOURS);
    }

    /**
     * 获取历史对话
     */
    public List<Map<String, String>> getHistory(Integer userId) {
        String key = PREFIX + userId;
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
    public void clearHistory(Integer userId) {
        redisTemplate.delete(PREFIX + userId);
    }
}
