package com.example.redisstomptest.controller;

import com.example.redisstomptest.model.Message;
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

  @MessageMapping("/send/{scope}") // Change this line to capture the scope variable
  @SendTo("/topic/{scope}")
  public Message[] handleMessage(
    Message message,
    @Header("simpSessionId") String sessionId,
    @DestinationVariable String scope
  ) {
    log.debug(
      "Handling message for tenant: {}, session: {}, scope: {}",
      message.getTenant(),
      sessionId,
      scope
    );

    if (!message.getScope().equals(scope)) {
      log.warn(
        "Message scope {} doesn't match path scope {}",
        message.getScope(),
        scope
      );
      return new Message[0];
    }

    message.setTimestamp(new Date());
    message.setAcknowledged(true);
    messageService.saveMessage(
      message.getTenant(),
      message.getUserId(),
      message
    );

    Message ackMessage = new Message();
    BeanUtils.copyProperties(message, ackMessage);
    ackMessage.setContent("ACK: " + message.getContent());
    ackMessage.setTimestamp(new Date());
    messageService.saveMessage(
      message.getTenant(),
      message.getUserId(),
      ackMessage
    );

    Message echoMessage = new Message();
    BeanUtils.copyProperties(message, echoMessage);
    echoMessage.setContent("ECHO: " + message.getContent());
    echoMessage.setTimestamp(new Date());
    messageService.saveMessage(
      message.getTenant(),
      message.getUserId(),
      echoMessage
    );

    return new Message[] { message, ackMessage, echoMessage };
  }
}
