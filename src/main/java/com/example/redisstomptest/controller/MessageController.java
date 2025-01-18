package com.example.redisstomptest.controller;

import com.example.redisstomptest.model.ChatMessage;
import com.example.redisstomptest.service.MessageService;
import java.util.Date;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
@Slf4j
public class MessageController {

  private final MessageService messageService;

  @MessageMapping("/send/{scope}")
  @SendTo("/topic/{scope}")
  public ChatMessage handleMessage(
    ChatMessage message,
    @Header("simpSessionId") String sessionId,
    @Header("simpConnectHost") String virtualHost,
    @DestinationVariable String scope
  ) {
    log.debug(
      "Handling message for tenant: {}, vhost: {}, session: {}, scope: {}",
      message.getTenant(),
      virtualHost,
      sessionId,
      scope
    );

    if (!message.getTenant().equals(virtualHost)) {
      log.warn(
        "ChatMessage tenant {} doesn't match virtual host {}",
        message.getTenant(),
        virtualHost
      );
      return null;
    }

    if (!message.getScope().equals(scope)) {
      log.warn(
        "ChatMessage scope {} doesn't match path scope {}",
        message.getScope(),
        scope
      );
      return null;
    }

    message.setTimestamp(new Date());
    message.setAcknowledged(true);
    messageService.saveMessage(virtualHost, message.getUserId(), message);

    ChatMessage ackMessage = new ChatMessage();
    BeanUtils.copyProperties(message, ackMessage);
    ackMessage.setContent(
      "ACK: backend received your message of '" + message.getContent() + "'"
    );
    ackMessage.setTimestamp(new Date());
    messageService.saveMessage(virtualHost, message.getUserId(), ackMessage);

    return ackMessage;
  }
}
