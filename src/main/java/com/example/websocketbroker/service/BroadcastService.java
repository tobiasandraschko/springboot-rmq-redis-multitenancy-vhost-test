package com.example.websocketbroker.service;

import com.example.websocketbroker.model.BrokerConnection;
import com.example.websocketbroker.repository.TenantRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * Service for broadcasting messages to tenants at regular intervals.
 *
 * @author Tobias Andraschko
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class BroadcastService {

  private final BrokerConnectionManager brokerConnectionManager;
  private final RedisManager redisManager;
  private final TenantRepository tenantRepository;

  /**
   * Broadcasts news to all tenants every 60 seconds.
   * The message includes a tenant identifier.
   */
  @Scheduled(fixedRate = 60000)
  public void broadcastNews() {
    List<String> tenants = tenantRepository.findAllTenants();

    tenants.forEach(tenant -> {
      String message = String.format("Broadcast from %s", tenant);
      String userId = "SYSTEM";

      redisManager.storeMessage(tenant, "news", userId, message);

      BrokerConnection brokerConnection = brokerConnectionManager.getBrokerConnection(
        tenant
      );
      if (brokerConnection != null && brokerConnection.isConnected()) {
        brokerConnection.sendMessage("news", message, userId);
        log.debug("Sent broadcast to tenant {}: {}", tenant, message);
      } else {
        log.error("No active broker connection for tenant: {}", tenant);
      }
    });
  }
}
