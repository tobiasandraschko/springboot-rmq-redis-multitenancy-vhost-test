package com.example.redisstomptest.model;

import java.util.Date;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Message {

  private String content;
  private String tenant;
  private String userId;
  private boolean acknowledged;
  private Date timestamp;
}
