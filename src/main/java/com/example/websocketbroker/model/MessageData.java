package com.example.websocketbroker.model;

import java.time.LocalDateTime;
import lombok.Data;

/**
 * Represents a message in the WebSocket broker. This class holds
 * information about the message content, its timestamp
 * and the custom user ID associated with the
 * message.
 *
 * @author Tobias Andraschko
 */
@Data
public class MessageData {

  private LocalDateTime timestamp;
  private String message;
  private String customUserId;
}
