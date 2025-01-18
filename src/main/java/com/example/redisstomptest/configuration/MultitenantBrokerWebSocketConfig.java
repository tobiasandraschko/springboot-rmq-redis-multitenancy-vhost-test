package com.example.redisstomptest.configuration;

import jakarta.annotation.PostConstruct;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.channel.PublishSubscribeChannel;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.SubscribableChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.simp.stomp.StompReactorNettyCodec;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.messaging.tcp.TcpConnection;
import org.springframework.messaging.tcp.TcpConnectionHandler;
import org.springframework.messaging.tcp.reactor.ReactorNettyTcpClient;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.messaging.StompSubProtocolHandler;
import org.springframework.web.socket.messaging.SubProtocolWebSocketHandler;

@Slf4j
@Configuration
@EnableWebSocket
@EnableIntegration
@RequiredArgsConstructor
public class MultitenantBrokerWebSocketConfig implements WebSocketConfigurer {

  private final TenantStompInterceptor tenantStompInterceptor;

  @Value("${rabbitmq.host}")
  private String host;

  @Value("${rabbitmq.stomp.port}")
  private int stompPort;

  @Value("${rabbitmq.username}")
  private String username;

  @Value("${rabbitmq.password}")
  private String password;

  @Value("#{'${rabbitmq.virtual-hosts}'.split(',')}")
  private List<String> virtualHosts;

  private Map<String, ReactorNettyTcpClient<byte[]>> clients = new ConcurrentHashMap<>();

  @PostConstruct
  public void initializeConnections() {
    for (String vhost : virtualHosts) {
      log.info("Initializing STOMP client connection for vhost: {}", vhost);

      ReactorNettyTcpClient<byte[]> client = new ReactorNettyTcpClient<>(
        options -> options.host(host).port(stompPort),
        new StompReactorNettyCodec()
      );

      client.connectAsync(
        new TcpConnectionHandler<byte[]>() {
          @Override
          public void afterConnected(TcpConnection<byte[]> connection) {
            log.info(
              "Successfully established TCP connection to broker for vhost: {}",
              vhost
            );

            StompHeaderAccessor headers = StompHeaderAccessor.create(
              StompCommand.CONNECT
            );
            headers.setAcceptVersion("1.2");
            headers.setHost(vhost);
            headers.setLogin(username);
            headers.setPasscode(password);
            headers.setNativeHeader("vhost", vhost);

            Message<byte[]> connectMessage = MessageBuilder.createMessage(
              new byte[0],
              headers.getMessageHeaders()
            );

            connection
              .sendAsync(connectMessage)
              .whenComplete((result, ex) -> {
                if (ex != null) {
                  log.error(
                    "Failed to send STOMP CONNECT frame to vhost {}: {}",
                    vhost,
                    ex.getMessage(),
                    ex
                  );
                } else {
                  log.info(
                    "Successfully sent STOMP CONNECT frame to vhost: {}",
                    vhost
                  );
                }
              });
          }

          @Override
          public void handleMessage(Message<byte[]> message) {
            StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
            String sessionId = accessor.getSessionId();
            log.debug(
              "Received message from vhost {} for session {}: {}",
              vhost,
              sessionId,
              message
            );
            stompOutputChannel().send(message);
          }

          @Override
          public void handleFailure(Throwable ex) {
            log.error(
              "Connection failure on vhost {}: {}",
              vhost,
              ex.getMessage(),
              ex
            );
          }

          @Override
          public void afterConnectionClosed() {
            log.warn("Connection closed for vhost: {}", vhost);
          }

          @Override
          public void afterConnectFailure(Throwable ex) {
            log.error(
              "Failed to establish connection for vhost {}: {}",
              vhost,
              ex.getMessage(),
              ex
            );
          }
        }
      );

      clients.put(vhost, client);
      log.debug("Stored client connection for vhost: {}", vhost);
    }
  }

  @Bean
  public MessageChannel stompInputChannel() {
    log.debug("Creating STOMP input channel");
    DirectChannel channel = new DirectChannel();
    channel.addInterceptor(tenantStompInterceptor);
    channel.subscribe(message -> {
      StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
      String authorization = accessor.getFirstNativeHeader("Authorization");
      String sessionId = accessor.getSessionId();

      if (authorization != null && authorization.startsWith("Bearer ")) {
        String tenant = authorization.substring(7);
        log.debug(
          "Routing message for tenant {} with session {}",
          tenant,
          sessionId
        );

        ReactorNettyTcpClient<byte[]> client = getClientForTenant(tenant);
        if (client != null) {
          client.connectAsync(
            new TcpConnectionHandler<byte[]>() {
              @Override
              public void afterConnected(TcpConnection<byte[]> connection) {
                StompHeaderAccessor headers = StompHeaderAccessor.wrap(message);
                headers.setSessionId(sessionId);
                headers.setNativeHeader("ws-session-id", sessionId);

                Message<byte[]> messageToSend = MessageBuilder
                  .withPayload((byte[]) message.getPayload())
                  .copyHeaders(headers.toMap())
                  .build();

                connection
                  .sendAsync(messageToSend)
                  .whenComplete((result, ex) -> {
                    if (ex != null) {
                      log.error(
                        "Error forwarding message to tenant broker: {}",
                        ex.getMessage()
                      );
                    } else {
                      log.debug(
                        "Successfully forwarded message to tenant broker: {}",
                        tenant
                      );
                    }
                  });
              }

              @Override
              public void handleMessage(Message<byte[]> responseMessage) {
                StompHeaderAccessor headers = StompHeaderAccessor.wrap(
                  responseMessage
                );
                headers.setSessionId(sessionId);
                Message<byte[]> wrappedMessage = MessageBuilder
                  .withPayload(responseMessage.getPayload())
                  .copyHeaders(headers.toMap())
                  .build();
                stompOutputChannel().send(wrappedMessage);
              }

              @Override
              public void handleFailure(Throwable ex) {
                log.error(
                  "Connection failure for tenant {}: {}",
                  tenant,
                  ex.getMessage(),
                  ex
                );
              }

              @Override
              public void afterConnectionClosed() {
                log.warn("Connection closed for tenant: {}", tenant);
              }

              @Override
              public void afterConnectFailure(Throwable ex) {
                log.error(
                  "Failed to establish connection for tenant {}: {}",
                  tenant,
                  ex.getMessage(),
                  ex
                );
              }
            }
          );
        } else {
          log.error("No broker client found for tenant: {}", tenant);
        }
      } else {
        log.error("No tenant information in Authorization header");
      }
    });
    return channel;
  }

  @Bean
  public SubscribableChannel stompOutputChannel() {
    log.debug("Creating STOMP output channel");
    PublishSubscribeChannel channel = new PublishSubscribeChannel();
    channel.addInterceptor(tenantStompInterceptor);
    return channel;
  }

  public ReactorNettyTcpClient<byte[]> getClientForTenant(String tenant) {
    return clients.get(tenant);
  }

  @Bean
  public Map<String, ReactorNettyTcpClient<byte[]>> stompClients() {
    return clients;
  }

  @Bean
  public WebSocketHandler webSocketHandler() {
    SubProtocolWebSocketHandler handler = new SubProtocolWebSocketHandler(
      stompInputChannel(),
      stompOutputChannel()
    );
    handler.addProtocolHandler(new StompSubProtocolHandler());
    return handler;
  }

  @Override
  public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
    registry
      .addHandler(webSocketHandler(), "/ws")
      .setAllowedOrigins("http://localhost:5500", "http://127.0.0.1:5500")
      .withSockJS();
  }
}
