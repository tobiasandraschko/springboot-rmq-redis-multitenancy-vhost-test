package com.example.redisstomptest.controller;

import com.example.redisstomptest.model.ChatMessage;
import com.example.redisstomptest.service.MessageService;
import java.util.Date;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
@Slf4j
public class MessageController {

  private static final String TENANT_ATTRIBUTE = "tenant";
  private final MessageService messageService;

  @MessageMapping("/app/send/{scope}")
  @SendTo("/topic/{scope}")
  public ChatMessage handleMessage(
    ChatMessage message,
    @Header("simpSessionId") String sessionId,
    SimpMessageHeaderAccessor headerAccessor,
    @DestinationVariable String scope
  ) {
    log.debug(
      "Received message: {}, sessionId: {}, scope: {}",
      message,
      sessionId,
      scope
    );
    log.debug("Message headers: {}", headerAccessor.toMap());

    Map<String, Object> sessionAttributes = headerAccessor.getSessionAttributes();
    String tenant = sessionAttributes != null
      ? (String) sessionAttributes.get(TENANT_ATTRIBUTE)
      : null;

    log.debug("Processing message for tenant: {}", tenant);

    if (tenant == null) {
      log.error("No tenant found in session attributes");
      return null;
    }

    if (!message.getTenant().equals(tenant)) {
      log.warn(
        "ChatMessage tenant {} doesn't match session tenant {}",
        message.getTenant(),
        tenant
      );
      return null;
    }

    try {
      message.setTimestamp(new Date());
      message.setAcknowledged(true);
      messageService.saveMessage(tenant, message.getUserId(), message);

      ChatMessage ackMessage = new ChatMessage();
      BeanUtils.copyProperties(message, ackMessage);
      ackMessage.setContent(
        "ACK: backend received your message of '" + message.getContent() + "'"
      );
      ackMessage.setTimestamp(new Date());

      log.debug("Sending acknowledgment message: {}", ackMessage);
      messageService.saveMessage(tenant, message.getUserId(), ackMessage);

      return ackMessage;
    } catch (Exception e) {
      log.error("Error processing message", e);
      return null;
    }
  }
}
