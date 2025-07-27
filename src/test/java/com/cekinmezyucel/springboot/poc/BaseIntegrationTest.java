package com.cekinmezyucel.springboot.poc;

import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import static com.cekinmezyucel.springboot.poc.util.ApplicationConstants.AUTHORITY_PREFIX;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class BaseIntegrationTest {

  /**
   * @see <a
   *     href="https://testcontainers.com/guides/testcontainers-container-lifecycle/">Testcontainers
   *     container lifecycle management using JUnit 5</a>
   */
  static final PostgreSQLContainer<?> postgres =
      new PostgreSQLContainer<>(DockerImageName.parse("postgres:16-alpine"));

  static {
    postgres.start();

    // Add shutdown hook to stop the PostgreSQL container after test suites finishes
    Runtime.getRuntime()
        .addShutdownHook(
            new Thread(
                () -> {
                  if (postgres.isRunning()) {
                    postgres.stop();
                  }
                }));
  }

  @DynamicPropertySource
  static void configureProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", postgres::getJdbcUrl);
    registry.add("spring.datasource.username", postgres::getUsername);
    registry.add("spring.datasource.password", postgres::getPassword);
    registry.add("spring.datasource.driver-class-name", postgres::getDriverClassName);
  }

  protected String withAuthorityPrefix(String roleKey) {
    return AUTHORITY_PREFIX + roleKey;
  }
}
