package com.example.websocketbroker.service;

import com.example.websocketbroker.model.BrokerConnection;
import com.example.websocketbroker.repository.TenantRepository;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Manages broker connections for each tenant.
 *
 * @author Tobias Andraschko
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class BrokerConnectionManager {

  private final Map<String, BrokerConnection> brokerConnections = new ConcurrentHashMap<>();
  private final TenantRepository tenantRepository;

  /**
   * Initializes broker connections for all tenants.
   */
  @PostConstruct
  public void init() {
    List<String> tenants = tenantRepository.findAllTenants();
    tenants.forEach(tenant -> {
      BrokerConnection broker = new BrokerConnection(tenant);
      brokerConnections.put(tenant, broker);
      broker.connect();
    });
  }

  /**
   * Cleans up all broker connections on application shutdown.
   */
  @PreDestroy
  public void cleanup() {
    brokerConnections.values().forEach(BrokerConnection::disconnect);
  }

  /**
   * Retrieves the broker connection for a specific tenant.
   *
   * @param tenant the tenant identifier
   * @return the BrokerConnection for the specified tenant
   */
  public BrokerConnection getBrokerConnection(String tenant) {
    return brokerConnections.get(tenant);
  }
}
