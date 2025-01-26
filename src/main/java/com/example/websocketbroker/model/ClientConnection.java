package com.example.websocketbroker.model;

import java.util.HashSet;
import java.util.Set;
import lombok.Data;
import org.springframework.web.socket.WebSocketSession;

/**
 * Represents a client connection in the WebSocket broker. This class
 * holds information about the WebSocket session, the tenant associated
 * with the connection, the user ID, and the subscriptions made by the
 * client.
 *
 * @author Tobias Andraschko
 */
@Data
public class ClientConnection {

  private final WebSocketSession session;
  private final String tenant;
  private final String userId;
  private final Set<String> subscriptions = new HashSet<>();
}
