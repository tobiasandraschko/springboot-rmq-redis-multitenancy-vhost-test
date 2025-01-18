package com.example.redisstomptest.configuration;

import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageBuilder;
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

    if (accessor == null || accessor.getCommand() == null) {
      return message;
    }

    if (StompCommand.CONNECT.equals(accessor.getCommand())) {
      StompHeaderAccessor newAccessor = StompHeaderAccessor.create(
        StompCommand.CONNECT
      );
      newAccessor.copyHeaders(accessor.toMap());

      List<String> authorization = accessor.getNativeHeader("Authorization");

      if (authorization != null && !authorization.isEmpty()) {
        String bearerToken = authorization.get(0);
        String tenant = bearerToken.replace("Bearer ", "");
        newAccessor.setHost(tenant);
        log.debug("Setting virtual host to: {}", tenant);
      } else {
        log.warn("No Authorization header present in CONNECT frame");
        newAccessor.setHost("default");
      }

      return MessageBuilder.createMessage(
        message.getPayload(),
        newAccessor.getMessageHeaders()
      );
    }

    return message;
  }
}
