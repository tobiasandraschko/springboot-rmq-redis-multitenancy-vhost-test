package com.example.redisstomptest.service;

import com.example.redisstomptest.model.Message;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MessageService {

  private final RedisTemplate<String, Message> redisTemplate;

  public void saveMessage(String tenant, String userId, Message message) {
    String key = String.format("messages:%s:%s", tenant, userId);
    redisTemplate.opsForList().rightPush(key, message);
  }

  public List<Message> getMessages(String tenant, String userId) {
    String key = String.format("messages:%s:%s", tenant, userId);
    Long size = redisTemplate.opsForList().size(key);
    if (size == null || size == 0) {
      return new ArrayList<>();
    }
    return redisTemplate.opsForList().range(key, 0, size - 1);
  }
}
