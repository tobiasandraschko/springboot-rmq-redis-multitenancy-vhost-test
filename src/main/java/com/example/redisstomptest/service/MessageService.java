package com.example.redisstomptest.service;

import com.example.redisstomptest.model.ChatMessage;
import java.util.Collections;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MessageService {

  private final RedisTemplate<String, ChatMessage> redisTemplate;

  public void saveMessage(String tenant, String userId, ChatMessage message) {
    String key = String.format(
      "messages:%s:%s:%s",
      tenant,
      userId,
      message.getScope()
    );
    redisTemplate.opsForList().rightPush(key, message);
  }

  public List<ChatMessage> getMessages(
    String tenant,
    String userId,
    String scope
  ) {
    String key = String.format("messages:%s:%s:%s", tenant, userId, scope);
    Long size = redisTemplate.opsForList().size(key);
    if (size == null || size == 0) {
      return Collections.emptyList();
    }
    return redisTemplate.opsForList().range(key, 0, size - 1);
  }
}
