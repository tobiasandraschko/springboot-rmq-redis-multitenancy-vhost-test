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

  private static final String TENANT_ATTRIBUTE = "tenant";
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

  private final Map<String, ReactorNettyTcpClient<byte[]>> clients = new ConcurrentHashMap<>();
  private final Map<String, TcpConnection<byte[]>> connections = new ConcurrentHashMap<>();

  @PostConstruct
  public void initializeConnections() {
    virtualHosts.forEach(this::initializeClientConnection);
  }

  private void initializeClientConnection(String vhost) {
    log.info("Initializing STOMP client connection for vhost: {}", vhost);
    ReactorNettyTcpClient<byte[]> client = createStompClient();
    clients.put(vhost, client);
    connectToBroker(client, vhost);
    log.debug("Stored client connection for vhost: {}", vhost);
  }

  private ReactorNettyTcpClient<byte[]> createStompClient() {
    return new ReactorNettyTcpClient<>(
      options -> options.host(host).port(stompPort),
      new StompReactorNettyCodec()
    );
  }

  private void connectToBroker(
    ReactorNettyTcpClient<byte[]> client,
    String vhost
  ) {
    client.connectAsync(new BrokerConnectionHandler(vhost));
  }

  @Bean
  public MessageChannel stompInputChannel() {
    log.debug("Creating STOMP input channel");
    DirectChannel channel = new DirectChannel();
    channel.addInterceptor(tenantStompInterceptor);
    channel.subscribe(this::handleIncomingMessage);
    return channel;
  }

  private void handleIncomingMessage(Message<?> message) {
    try {
      StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
      String sessionId = accessor.getSessionId();

      log.debug(
        "Processing message: Command={}, SessionId={}, Destination={}",
        accessor.getCommand(),
        sessionId,
        accessor.getDestination()
      );

      // Get tenant from session attributes
      Map<String, Object> sessionAttributes = accessor.getSessionAttributes();
      String tenant = sessionAttributes != null
        ? (String) sessionAttributes.get(TENANT_ATTRIBUTE)
        : null;

      if (tenant != null) {
        handleMessageForTenant(message, tenant, sessionId);
      } else if (StompCommand.CONNECT.equals(accessor.getCommand())) {
        handleConnectMessage(message, accessor, sessionId);
      } else {
        log.error("No tenant found in session attributes");
      }
    } catch (Exception e) {
      log.error("Error processing incoming message", e);
    }
  }

  private void handleConnectMessage(
    Message<?> message,
    StompHeaderAccessor accessor,
    String sessionId
  ) {
    String authorization = accessor.getFirstNativeHeader("Authorization");
    if (authorization != null && authorization.startsWith("Bearer ")) {
      String tenant = authorization.substring(7);
      handleMessageForTenant(message, tenant, sessionId);
    } else {
      log.error("No Authorization header in CONNECT message");
    }
  }

  private void handleMessageForTenant(
    Message<?> message,
    String tenant,
    String sessionId
  ) {
    log.debug(
      "Routing message for tenant {} with session {}",
      tenant,
      sessionId
    );
    ReactorNettyTcpClient<byte[]> client = getClientForTenant(tenant);

    if (client != null) {
      forwardMessageToBroker(message, client, tenant, sessionId);
    } else {
      log.error("No broker client found for tenant: {}", tenant);
    }
  }

  private void forwardMessageToBroker(
    Message<?> message,
    ReactorNettyTcpClient<byte[]> client,
    String tenant,
    String sessionId
  ) {
    client.connectAsync(
      new MessageForwardingHandler(message, tenant, sessionId)
    );
  }

  @Bean
  public SubscribableChannel stompOutputChannel() {
    log.debug("Creating STOMP output channel");
    PublishSubscribeChannel channel = new PublishSubscribeChannel();
    channel.addInterceptor(tenantStompInterceptor);
    return channel;
  }

  private class BrokerConnectionHandler
    implements TcpConnectionHandler<byte[]> {

    private final String vhost;

    BrokerConnectionHandler(String vhost) {
      this.vhost = vhost;
    }

    @Override
    public void afterConnected(TcpConnection<byte[]> connection) {
      log.info(
        "Successfully established TCP connection to broker for vhost: {}",
        vhost
      );
      connections.put(vhost, connection);
      sendConnectFrame(connection);
    }

    private void sendConnectFrame(TcpConnection<byte[]> connection) {
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
        .whenComplete((result, ex) -> handleConnectCompletion(ex));
    }

    private void handleConnectCompletion(Throwable ex) {
      if (ex != null) {
        log.error(
          "Failed to send STOMP CONNECT frame to vhost {}: {}",
          vhost,
          ex.getMessage(),
          ex
        );
      } else {
        log.info("Successfully sent STOMP CONNECT frame to vhost: {}", vhost);
      }
    }

    @Override
    public void handleMessage(Message<byte[]> message) {
      StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
      log.debug(
        "Received message from vhost {} for session {}: {}",
        vhost,
        accessor.getSessionId(),
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
      connections.remove(vhost);
    }

    @Override
    public void afterConnectionClosed() {
      log.warn("Connection closed for vhost: {}", vhost);
      connections.remove(vhost);
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

  private class MessageForwardingHandler
    implements TcpConnectionHandler<byte[]> {

    private final Message<?> message;
    private final String tenant;
    private final String sessionId;

    MessageForwardingHandler(
      Message<?> message,
      String tenant,
      String sessionId
    ) {
      this.message = message;
      this.tenant = tenant;
      this.sessionId = sessionId;
    }

    @Override
    public void afterConnected(TcpConnection<byte[]> connection) {
      try {
        StompHeaderAccessor headers = StompHeaderAccessor.wrap(message);
        headers.setSessionId(sessionId);
        headers.setNativeHeader("ws-session-id", sessionId);
        headers.setHost(tenant);

        Message<byte[]> messageToSend = MessageBuilder
          .withPayload((byte[]) message.getPayload())
          .copyHeaders(headers.toMap())
          .build();

        log.debug(
          "Forwarding message to broker for tenant: {}, command: {}",
          tenant,
          headers.getCommand()
        );

        connection
          .sendAsync(messageToSend)
          .whenComplete((result, ex) -> handleSendCompletion(ex));
      } catch (Exception e) {
        log.error("Error preparing message for broker", e);
      }
    }

    private void handleSendCompletion(Throwable ex) {
      if (ex != null) {
        log.error(
          "Error forwarding message to tenant broker: {}",
          ex.getMessage(),
          ex
        );
      } else {
        log.debug(
          "Successfully forwarded message to tenant broker: {}",
          tenant
        );
      }
    }

    @Override
    public void handleMessage(Message<byte[]> responseMessage) {
      try {
        StompHeaderAccessor headers = StompHeaderAccessor.wrap(responseMessage);
        headers.setSessionId(sessionId);

        Message<byte[]> wrappedMessage = MessageBuilder
          .withPayload(responseMessage.getPayload())
          .copyHeaders(headers.toMap())
          .build();

        log.debug("Received response from broker for tenant: {}", tenant);
        stompOutputChannel().send(wrappedMessage);
      } catch (Exception e) {
        log.error("Error handling broker response", e);
      }
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
