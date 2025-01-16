package com.example.redisstomptest.configuration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

@Component
public class ScopeHeaderInterceptor implements ChannelInterceptor {

  @Override
  public Message<?> preSend(Message<?> message, MessageChannel channel) {
    StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(
      message,
      StompHeaderAccessor.class
    );

    if (StompCommand.SEND.equals(accessor.getCommand())) {
      try {
        String payload = new String((byte[]) message.getPayload());
        JsonNode jsonNode = new ObjectMapper().readTree(payload);
        String scope = jsonNode.get("scope").asText();
        accessor.setHeader("scope", scope);
      } catch (Exception e) {
        // Handle exception
      }
    }
    return message;
  }
}
