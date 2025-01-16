package com.example.redisstomptest.service;

import com.example.redisstomptest.controller.MessageController;
import com.example.redisstomptest.model.Message;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class NewsSchedulerService {

  private final SimpMessagingTemplate messagingTemplate;

  @Scheduled(fixedRate = 10000)
  public void broadcastNews() {
    Message newsMessage = new Message();
    newsMessage.setContent("Scheduled news broadcast: " + LocalDateTime.now());
    newsMessage.setTenant("tenant1");
    newsMessage.setUserId("system");
    newsMessage.setScope("news");
    newsMessage.setTimestamp(new Date());

    Map<String, Object> headers = new HashMap<>();
    headers.put("host", "tenant1");

    messagingTemplate.convertAndSend("/topic/news", newsMessage, headers);
  }
}
