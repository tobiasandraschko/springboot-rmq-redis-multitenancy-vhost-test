package com.example.websocketbroker.controller;

import com.example.websocketbroker.model.BrokerConnection;
import com.example.websocketbroker.service.BrokerConnectionManager;
import com.example.websocketbroker.service.RedisManager;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;

/**
 * Controller for handling WebSocket messages. This class processes incoming
 * messages sent to specific topics and manages the interactions with the
 * Redis data store and broker connections.
 *
 * @author Tobias Andraschko
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class WebSocketController {

  private final RedisManager redisManager;
  private final BrokerConnectionManager brokerConnectionManager;

  // Set of valid topics that can be subscribed to or sent messages.
  private static final Set<String> VALID_TOPICS = Set.of(
    "news",
    "alert",
    "chat"
  );

  /**
   * Handles incoming messages sent to the specified topic. This method
   * checks if the topic is valid, retrieves the tenant and user ID from
   * the message headers, logs the received message, stores it in Redis,
   * and attempts to send it to the appropriate broker connection if active.
   *
   * @param message the message payload received from the client
   * @param headerAccessor the header accessor to retrieve message headers
   * @param topic the topic to which the message is sent
   */
  @MessageMapping("/send/{topic}")
  public void handleMessage(
    @Payload String message,
    SimpMessageHeaderAccessor headerAccessor,
    @DestinationVariable String topic
  ) {
    if (!VALID_TOPICS.contains(topic)) {
      return;
    }

    String tenant = headerAccessor
      .getFirstNativeHeader("authorization")
      .replace("Bearer ", "");
    String userId = headerAccessor.getFirstNativeHeader("custom-user-id");

    log.info("Received message for tenant: {}, topic: {}", tenant, topic);

    redisManager.storeMessage(tenant, topic, userId, message);

    BrokerConnection brokerConnection = brokerConnectionManager.getBrokerConnection(
      tenant
    );
    if (brokerConnection != null && brokerConnection.isConnected()) {
      brokerConnection.sendMessage(topic, message, userId);
    } else {
      log.error("No active broker connection for tenant: {}", tenant);
    }
  }
}
