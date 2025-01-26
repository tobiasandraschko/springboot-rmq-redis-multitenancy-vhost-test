package com.example.websocketbroker.service;

import com.example.websocketbroker.model.MessageData;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/**
 * Manages interactions with Redis for storing message data.
 *
 * @author Tobias Andraschko
 */
@Service
@RequiredArgsConstructor
public class RedisManager {

  private final StringRedisTemplate redisTemplate;
  private final ObjectMapper objectMapper;

  /**
   * Stores a message in Redis for a specific tenant and topic.
   *
   * @param tenant the tenant identifier
   * @param topic the topic under which the message is stored
   * @param customUserId the custom user identifier for the message
   * @param message the message content
   * @throws RuntimeException if there is an error serializing the message data
   */
  public void storeMessage(
    String tenant,
    String topic,
    String customUserId,
    String message
  ) {
    try {
      MessageData messageData = new MessageData();
      messageData.setTimestamp(LocalDateTime.now());
      messageData.setMessage(message);
      messageData.setCustomUserId(customUserId);

      String key = String.format("messages:%s:%s", tenant, topic);
      String json = objectMapper.writeValueAsString(messageData);
      redisTemplate.opsForList().rightPush(key, json);
    } catch (JsonProcessingException e) {
      throw new RuntimeException("Error serializing message data", e);
    }
  }
}
