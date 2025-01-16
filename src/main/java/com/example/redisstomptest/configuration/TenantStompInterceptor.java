package com.example.redisstomptest.configuration;

import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class TenantStompInterceptor implements ChannelInterceptor {

  @Override
  public Message<?> preSend(Message<?> message, MessageChannel channel) {
    StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(
      message,
      StompHeaderAccessor.class
    );

    if (StompCommand.CONNECT.equals(accessor.getCommand())) {
      List<String> authorization = accessor.getNativeHeader("Authorization");

      if (authorization != null && !authorization.isEmpty()) {
        String bearerToken = authorization.get(0);
        String tenant = bearerToken.replace("Bearer ", "");
        accessor.setHost(tenant);
        log.debug("Setting virtual host to: {}", tenant);
      } else {
        log.warn("No Authorization header present in CONNECT frame");
        accessor.setHost("default");
      }
    }
    return message;
  }
}
