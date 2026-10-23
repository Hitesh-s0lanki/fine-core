package com.fincore.server;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.postgresql.PostgreSQLContainer;

/** Runs against a real isolated PostgreSQL database, supplied externally or by Testcontainers. */
@org.springframework.test.context.TestPropertySource(
    properties = {"debug=false", "logging.level.root=WARN", "logging.level.org.hibernate.SQL=WARN"})
abstract class PostgresTestSupport {
  private static PostgreSQLContainer container;

  @DynamicPropertySource
  static void postgresProperties(DynamicPropertyRegistry registry) {
    String externalUrl = System.getenv("TEST_DATABASE_URL");
    if (externalUrl != null && !externalUrl.isBlank()) {
      registry.add("spring.datasource.url", () -> externalUrl);
      registry.add(
          "spring.datasource.username",
          () -> System.getenv().getOrDefault("TEST_DATABASE_USERNAME", "fincore"));
      registry.add(
          "spring.datasource.password",
          () -> System.getenv().getOrDefault("TEST_DATABASE_PASSWORD", "fincore"));
    } else {
      if (container == null) {
        container = new PostgreSQLContainer("postgres:17-alpine");
        container.start();
      }
      registry.add("spring.datasource.url", container::getJdbcUrl);
      registry.add("spring.datasource.username", container::getUsername);
      registry.add("spring.datasource.password", container::getPassword);
    }
  }
}
