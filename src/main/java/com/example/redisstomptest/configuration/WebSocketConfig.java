package com.example.redisstomptest.configuration;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketTransportRegistration;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

  private final TenantStompInterceptor tenantStompInterceptor;
  private final ScopeHeaderInterceptor scopeHeaderInterceptor;

  @Override
  public void configureMessageBroker(MessageBrokerRegistry config) {
    config
      .enableStompBrokerRelay("/topic")
      .setRelayHost("localhost")
      .setRelayPort(8024)
      .setSystemLogin("guest")
      .setSystemPasscode("guest")
      .setClientLogin("guest")
      .setClientPasscode("guest")
      .setSystemHeartbeatSendInterval(10000)
      .setSystemHeartbeatReceiveInterval(10000);

    config.setApplicationDestinationPrefixes("/app");
    config.setPreservePublishOrder(true);
  }

  @Override
  public void registerStompEndpoints(StompEndpointRegistry registry) {
    registry
      .addEndpoint("/ws")
      .setAllowedOrigins("http://127.0.0.1:5500", "http://localhost:5500")
      .withSockJS()
      .setDisconnectDelay(30 * 1000)
      .setHeartbeatTime(10000);
  }

  @Override
  public void configureClientInboundChannel(ChannelRegistration registration) {
    registration.interceptors(tenantStompInterceptor, scopeHeaderInterceptor);
  }

  @Override
  public void configureWebSocketTransport(
    WebSocketTransportRegistration registry
  ) {
    registry
      .setMessageSizeLimit(64 * 1024)
      .setSendBufferSizeLimit(512 * 1024)
      .setSendTimeLimit(20000);
  }
}
