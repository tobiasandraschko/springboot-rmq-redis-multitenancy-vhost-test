package com.example.websocketbroker.repository;

import java.util.Arrays;
import java.util.List;
import org.springframework.stereotype.Repository;

/**
 * In-memory implementation of the TenantRepository interface.
 * This class simulates a database request by returning a fixed list of tenants.
 *
 * @author Tobias Andraschko
 */
@Repository
public class InMemoryTenantRepository implements TenantRepository {

  /**
   * Simulates a database request by returning a fixed list of tenants.
   *
   * @return a list containing "tenant1" and "tenant2".
   */
  @Override
  public List<String> findAllTenants() {
    return Arrays.asList("tenant1", "tenant2");
  }
}
