package com.example.websocketbroker.model;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Represents a connection to a STOMP broker for a specific virtual host.
 * This class manages the connection lifecycle, including connecting,
 * disconnecting, sending messages, and subscribing to topics.
 *
 * @author Tobias Andraschko
 */
@Slf4j
@RequiredArgsConstructor
public class BrokerConnection {

  private final String vhost;
  private Socket socket;
  private DataOutputStream outputStream;
  private DataInputStream inputStream;
  private boolean connected;
  private final Object lock = new Object();

  private static final Set<String> TOPICS = Set.of("news", "alert", "chat");

  /**
   * Establishes a connection to the STOMP broker. This method sends a
   * CONNECT frame to the broker and starts listening for incoming messages
   * and sending heartbeats.
   */
  public void connect() {
    try {
      socket = new Socket("localhost", 8024);
      outputStream = new DataOutputStream(socket.getOutputStream());
      inputStream = new DataInputStream(socket.getInputStream());

      String connectFrame = String.format(
        "CONNECT\n" +
        "accept-version:1.2\n" +
        "host:%s\n" +
        "login:guest\n" +
        "passcode:guest\n\n\u0000",
        vhost
      );

      synchronized (lock) {
        outputStream.write(connectFrame.getBytes());
        outputStream.flush();
      }

      startListening();
      startHeartbeat();

      connected = true;
      log.info("Established STOMP connection for vhost: {}", vhost);

      subscribeToTopics();
    } catch (Exception e) {
      log.error(
        "Failed to connect STOMP broker for vhost {}: {}",
        vhost,
        e.getMessage()
      );
      connected = false;
      reconnect();
    }
  }

  private void startListening() {
    Thread listenerThread = new Thread(() -> {
      byte[] buffer = new byte[4096];
      StringBuilder messageBuilder = new StringBuilder();

      while (connected) {
        try {
          int bytesRead = inputStream.read(buffer);
          if (bytesRead == -1) break;

          messageBuilder.append(new String(buffer, 0, bytesRead));

          int nullIndex;
          while ((nullIndex = messageBuilder.indexOf("\u0000")) != -1) {
            String frame = messageBuilder.substring(0, nullIndex);
            messageBuilder.delete(0, nullIndex + 1);
            processFrame(frame);
          }
        } catch (IOException e) {
          if (connected) {
            log.error(
              "Error reading from broker {}: {}",
              vhost,
              e.getMessage()
            );
            reconnect();
            break;
          }
        }
      }
    });
    listenerThread.setDaemon(true);
    listenerThread.start();
  }

  private void startHeartbeat() {
    Thread heartbeatThread = new Thread(() -> {
      while (!Thread.currentThread().isInterrupted() && connected) {
        try {
          Thread.sleep(10000);
          if (connected) {
            synchronized (lock) {
              outputStream.write("\n".getBytes());
              outputStream.flush();
            }
          }
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
          break;
        } catch (Exception e) {
          if (connected) {
            log.error(
              "Error sending heartbeat to broker {}: {}",
              vhost,
              e.getMessage()
            );
            reconnect();
            break;
          }
        }
      }
    });
    heartbeatThread.setDaemon(true);
    heartbeatThread.start();
  }

  private void subscribeToTopics() throws IOException {
    for (String topic : TOPICS) {
      String destination = "/topic/" + topic;
      String subscribeFrame = String.format(
        "SUBSCRIBE\n" + "id:%s\n" + "destination:%s\n" + "ack:auto\n\n\u0000",
        UUID.randomUUID().toString(),
        destination
      );

      synchronized (lock) {
        outputStream.write(subscribeFrame.getBytes());
        outputStream.flush();
      }
      log.info("Subscribed to {} on vhost {}", destination, vhost);
    }
  }

  private void processFrame(String frame) {
    String[] lines = frame.split("\n");
    if (lines.length > 0 && "ERROR".equals(lines[0])) {
      log.error("Received ERROR frame from broker {}: {}", vhost, frame);
      reconnect();
    }
  }

  private void reconnect() {
    if (!connected) return;
    connected = false;

    try {
      socket.close();
    } catch (IOException e) {
      log.error(
        "Error closing socket for broker {}: {}",
        vhost,
        e.getMessage()
      );
    }

    Thread reconnectThread = new Thread(() -> {
      try {
        log.info("Attempting to reconnect broker {}", vhost);
        Thread.sleep(5000);
        connect();
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    });
    reconnectThread.setDaemon(true);
    reconnectThread.start();
  }

  /**
   * Disconnects from the STOMP broker. This method sends a DISCONNECT
   * frame and closes the socket connection.
   */
  public void disconnect() {
    connected = false;
    try {
      if (socket != null && !socket.isClosed()) {
        String disconnectFrame = "DISCONNECT\n\n\u0000";
        synchronized (lock) {
          outputStream.write(disconnectFrame.getBytes());
          outputStream.flush();
        }
        socket.close();
      }
    } catch (IOException e) {
      log.error(
        "Error disconnecting from broker {}: {}",
        vhost,
        e.getMessage()
      );
    }
  }

  /**
   * Checks if the connection to the broker is currently established.
   *
   * @return true if connected, false otherwise
   */
  public boolean isConnected() {
    return connected;
  }

  /**
   * Sends a message to the specified topic on the STOMP broker. This method
   * constructs a SEND frame and writes it to the output stream.
   *
   * @param topic the topic to send the message to
   * @param message the message content
   * @param userId the ID of the user sending the message
   */
  public void sendMessage(String topic, String message, String userId) {
    try {
      String sendFrame = String.format(
        "SEND\n" + "destination:/topic/%s\n" + "custom-user-id:%s\n\n%s\u0000",
        topic,
        userId,
        message
      );

      synchronized (lock) {
        outputStream.write(sendFrame.getBytes());
        outputStream.flush();
      }
    } catch (IOException e) {
      log.error(
        "Error sending message to broker {}: {}",
        vhost,
        e.getMessage()
      );
      reconnect();
    }
  }
}
