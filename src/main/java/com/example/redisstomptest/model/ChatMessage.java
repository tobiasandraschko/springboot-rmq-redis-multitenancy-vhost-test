package com.example.redisstomptest.model;

import java.util.Date;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessage {

  private String content;
  private String tenant;
  private String userId;
  private String scope;
  private boolean acknowledged;
  private Date timestamp;
}
