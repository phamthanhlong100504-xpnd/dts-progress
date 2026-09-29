package com.dts.progress.service;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class UserPreferenceService {

    private final RedisTemplate<String, Object> redisTemplate;

    public UserPreferenceService(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void updateLearningProgram(String userId, String programCode) {
        String key = "user:" + userId + ":program";
        // Lưu Hạng Bằng vào Redis
        redisTemplate.opsForValue().set(key, programCode);
        
        // Publish thông báo để Invalidate Cache ở các service khác
        String message = String.format("{\"userId\":\"%s\", \"program\":\"%s\"}", userId, programCode);
        redisTemplate.convertAndSend("channel:user_program_events", message);
    }
}
