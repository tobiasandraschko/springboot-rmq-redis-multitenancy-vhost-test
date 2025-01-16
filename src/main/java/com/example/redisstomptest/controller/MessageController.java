package com.example.redisstomptest.controller;

import com.example.redisstomptest.model.Message;
import com.example.redisstomptest.service.MessageService;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class MessageController {

  private final MessageService messageService;

  @MessageMapping("/send")
  @SendTo("/topic/messages")
  public List<Message> handleMessage(Message message) {
    Message ackMessage = new Message();
    BeanUtils.copyProperties(message, ackMessage);
    ackMessage.setAcknowledged(true);
    ackMessage.setTimestamp(new Date());

    Message echoMessage = new Message();
    BeanUtils.copyProperties(message, echoMessage);
    echoMessage.setContent("ECHO: " + message.getContent());
    echoMessage.setAcknowledged(true);
    echoMessage.setTimestamp(new Date());

    messageService.saveMessage(
      message.getTenant(),
      message.getUserId(),
      ackMessage
    );
    messageService.saveMessage(
      message.getTenant(),
      message.getUserId(),
      echoMessage
    );

    return Arrays.asList(ackMessage, echoMessage);
  }
}
