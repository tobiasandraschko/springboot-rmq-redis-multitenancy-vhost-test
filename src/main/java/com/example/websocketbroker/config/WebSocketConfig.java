package com.example.websocketbroker.config;

import com.example.websocketbroker.interceptors.TenantInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * Configuration class for setting up WebSocket message broker in the Spring context.
 * This class enables WebSocket message handling, configures the message broker,
 * and registers STOMP endpoints for client communication.
 *
 * @author Tobias Andraschko
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

  private final TenantInterceptor tenantInterceptor;

  @Value("${websocket.relay.host}")
  private String relayHost;

  @Value("${websocket.relay.port}")
  private int relayPort;

  @Value("${websocket.client.login}")
  private String clientLogin;

  @Value("${websocket.client.passcode}")
  private String clientPasscode;

  @Value("${websocket.system.login}")
  private String systemLogin;

  @Value("${websocket.system.passcode}")
  private String systemPasscode;

  @Value("${websocket.heartbeat.send.interval}")
  private int heartbeatSendInterval;

  @Value("${websocket.heartbeat.receive.interval}")
  private int heartbeatReceiveInterval;

  @Value("${websocket.allowed.origins}")
  private String allowedOrigins;

  @Autowired
  public WebSocketConfig(TenantInterceptor tenantInterceptor) {
    this.tenantInterceptor = tenantInterceptor;
  }

  /**
   * Configures the message broker settings for the WebSocket application.
   * This includes setting the application destination prefixes and enabling
   * the STOMP broker relay with the specified host, port, and credentials.
   *
   * @param config the MessageBrokerRegistry to configure
   */
  @Override
  public void configureMessageBroker(MessageBrokerRegistry config) {
    config.setApplicationDestinationPrefixes("/app");
    config
      .enableStompBrokerRelay("/topic")
      .setRelayHost(relayHost)
      .setRelayPort(relayPort)
      .setClientLogin(clientLogin)
      .setClientPasscode(clientPasscode)
      .setSystemLogin(systemLogin)
      .setSystemPasscode(systemPasscode)
      .setSystemHeartbeatSendInterval(heartbeatSendInterval)
      .setSystemHeartbeatReceiveInterval(heartbeatReceiveInterval);
  }

  /**
   * Registers STOMP endpoints for WebSocket communication. This method
   * specifies the endpoint URL and allowed origins for client connections.
   *
   * @param registry the StompEndpointRegistry to register endpoints
   */
  @Override
  public void registerStompEndpoints(StompEndpointRegistry registry) {
    registry
      .addEndpoint("/ws")
      .setAllowedOriginPatterns(allowedOrigins)
      .withSockJS();
  }

  /**
   * Configures the client inbound channel by adding the TenantInterceptor.
   * This interceptor can be used to handle tenant-specific logic for
   * incoming messages.
   *
   * @param registration the ChannelRegistration to configure
   */
  @Override
  public void configureClientInboundChannel(ChannelRegistration registration) {
    registration.interceptors(tenantInterceptor);
  }
}
