package com.example.websocketbroker.repository;

import java.util.List;

/**
 * Interface for retrieving tenant information.
 *
 * @author Tobias Andraschko
 */
public interface TenantRepository {
  /**
   * Retrieves a list of all tenants.
   *
   * @return a list of tenant identifiers.
   */
  List<String> findAllTenants();
}
