package com.example.redisstomptest.configuration;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

  private final TenantStompInterceptor tenantStompInterceptor;

  @Override
  public void configureMessageBroker(MessageBrokerRegistry config) {
    config
      .enableStompBrokerRelay("/topic")
      .setRelayHost("localhost")
      .setRelayPort(8024)
      .setSystemLogin("guest")
      .setSystemPasscode("guest");
    config.setApplicationDestinationPrefixes("/app");
  }

  @Override
  public void registerStompEndpoints(StompEndpointRegistry registry) {
    registry
      .addEndpoint("/ws")
      .setAllowedOrigins("http://127.0.0.1:5500", "http://localhost:5500")
      .withSockJS();
  }

  @Override
  public void configureClientInboundChannel(ChannelRegistration registration) {
    registration.interceptors(tenantStompInterceptor);
  }
}
