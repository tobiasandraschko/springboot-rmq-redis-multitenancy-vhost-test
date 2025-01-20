package com.example.redisstomptest.configuration;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
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

  private static final String TENANT_ATTRIBUTE = "tenant";

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
      log.debug(
        "CONNECT frame received. Authorization headers: {}",
        authorization
      );

      if (authorization != null && !authorization.isEmpty()) {
        String bearerToken = authorization.get(0);
        String tenant = bearerToken.replace("Bearer ", "");

        newAccessor.setHost(tenant);

        Map<String, Object> sessionAttributes = new ConcurrentHashMap<>();
        sessionAttributes.put(TENANT_ATTRIBUTE, tenant);
        newAccessor.setSessionAttributes(sessionAttributes);

        log.debug(
          "Setting virtual host and session attribute for tenant: {}",
          tenant
        );
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

  @Override
  public void afterSendCompletion(
    Message<?> message,
    MessageChannel channel,
    boolean sent,
    Exception ex
  ) {
    if (ex != null) {
      log.error("Error occurred while processing message", ex);
    }
  }
}
