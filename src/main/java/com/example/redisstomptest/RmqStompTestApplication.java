package com.example.redisstomptest;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class RmqStompTestApplication {

  public static void main(String[] args) {
    SpringApplication.run(RmqStompTestApplication.class, args);
  }
}
