package com.example.websocketbroker.interceptors;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;

/**
 * Interceptor for handling STOMP messages related to tenant information.
 * This interceptor modifies the message headers based on the tenant
 * extracted from the authorization header during the CONNECT command.
 *
 * @author Tobias Andraschko
 */
@Configuration
public class TenantInterceptor implements ChannelInterceptor {

  /**
   * Intercepts messages before they are sent to the message channel.
   * If the message is a STOMP CONNECT command, it extracts the tenant
   * from the authorization header and sets the destination and host
   * accordingly.
   *
   * @param message the message to be sent
   * @param channel the channel to which the message is being sent
   * @return the modified message
   */
  @Override
  public Message<?> preSend(Message<?> message, MessageChannel channel) {
    StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(
      message,
      StompHeaderAccessor.class
    );

    if (StompCommand.CONNECT.equals(accessor.getCommand())) {
      String authHeader = accessor.getFirstNativeHeader("authorization");
      if (authHeader != null && authHeader.startsWith("Bearer ")) {
        String tenant = authHeader.substring(7);
        accessor.setDestination("/queue/");
        accessor.setHost(tenant);
      }
    }
    return message;
  }
}
