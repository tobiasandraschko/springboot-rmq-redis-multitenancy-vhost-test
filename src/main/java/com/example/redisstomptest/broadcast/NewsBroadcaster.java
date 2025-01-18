package com.example.redisstomptest.broadcast;

import com.example.redisstomptest.configuration.MultitenantBrokerWebSocketConfig;
import java.util.Arrays;
import java.util.Date;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.messaging.tcp.TcpConnection;
import org.springframework.messaging.tcp.TcpConnectionHandler;
import org.springframework.messaging.tcp.reactor.ReactorNettyTcpClient;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class NewsBroadcaster {

  private final MultitenantBrokerWebSocketConfig brokerConfig;

  @Scheduled(fixedRate = 10000)
  public void broadcastNews() {
    String newsMessage = "Latest news update at " + new Date();

    StompHeaderAccessor headers = StompHeaderAccessor.create(StompCommand.SEND);
    headers.setDestination("/topic/news");

    Message<byte[]> stompMessage = MessageBuilder.createMessage(
      newsMessage.getBytes(),
      headers.getMessageHeaders()
    );

    for (String tenant : Arrays.asList("tenant1", "tenant2")) {
      log.debug("Broadcasting news to tenant: {}", tenant);
      ReactorNettyTcpClient<byte[]> client = brokerConfig.getClientForTenant(
        tenant
      );
      if (client != null) {
        client.connectAsync(
          new TcpConnectionHandler<byte[]>() {
            @Override
            public void afterConnected(TcpConnection<byte[]> connection) {
              connection
                .sendAsync(stompMessage)
                .whenComplete((result, ex) -> {
                  if (ex != null) {
                    log.error(
                      "Failed to broadcast to tenant {}: {}",
                      tenant,
                      ex.getMessage()
                    );
                  } else {
                    log.debug("Successfully broadcast to tenant {}", tenant);
                  }
                });
            }

            @Override
            public void handleMessage(Message<byte[]> message) {
              // Handle any response
            }

            @Override
            public void handleFailure(Throwable ex) {
              log.error(
                "Connection failure for tenant {}: {}",
                tenant,
                ex.getMessage()
              );
            }

            @Override
            public void afterConnectionClosed() {
              log.debug("Connection closed for tenant {}", tenant);
            }

            @Override
            public void afterConnectFailure(Throwable ex) {
              log.error(
                "Connect failure for tenant {}: {}",
                tenant,
                ex.getMessage()
              );
            }
          }
        );
      }
    }
  }
}
