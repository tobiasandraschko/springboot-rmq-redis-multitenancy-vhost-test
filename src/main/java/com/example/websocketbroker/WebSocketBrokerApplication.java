package com.example.websocketbroker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * The entry point of the WebSocket Broker application. This class
 * initializes the Spring Boot application and enables scheduling
 * capabilities necessary for our broadcasting service.
 *
 * @author Tobias Andraschko
 */
@SpringBootApplication
@EnableScheduling
public class WebSocketBrokerApplication {

  public static void main(String[] args) {
    SpringApplication.run(WebSocketBrokerApplication.class, args);
  }
}
