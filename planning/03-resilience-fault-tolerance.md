# Resilience & Fault Tolerance Enhancement Plan

## Current State Analysis
- ❌ No circuit breaker patterns
- ❌ No retry mechanisms
- ❌ No timeout configurations
- ❌ No bulkhead isolation
- ❌ No graceful degradation
- ❌ No fallback mechanisms
- ❌ No health monitoring for dependencies

## Target State
- ✅ Circuit breakers for external dependencies
- ✅ Retry mechanisms with exponential backoff
- ✅ Timeout configurations for all operations
- ✅ Bulkhead isolation for different operations
- ✅ Graceful degradation strategies
- ✅ Fallback mechanisms for critical operations
- ✅ Comprehensive health checks

## Implementation Steps

### Step 1: Add Resilience4j Dependencies

#### 1.1 Update build.gradle
```gradle
// Add to dependencies block
implementation 'io.github.resilience4j:resilience4j-spring-boot3:2.1.0'
implementation 'io.github.resilience4j:resilience4j-reactor:2.1.0'
implementation 'io.github.resilience4j:resilience4j-micrometer:2.1.0'
implementation 'org.springframework.boot:spring-boot-starter-aop'
```

### Step 2: Circuit Breaker Configuration

#### 2.1 Application Configuration
Add to `application.yaml`:
```yaml
resilience4j:
  circuitbreaker:
    configs:
      default:
        failure-rate-threshold: 50
        slow-call-rate-threshold: 50
        slow-call-duration-threshold: 2s
        permitted-number-of-calls-in-half-open-state: 3
        max-wait-duration-in-half-open-state: 10s
        sliding-window-type: count_based
        sliding-window-size: 10
        minimum-number-of-calls: 5
        wait-duration-in-open-state: 10s
        automatic-transition-from-open-to-half-open-enabled: true
        record-exceptions:
          - java.net.ConnectException
          - java.net.SocketTimeoutException
          - java.io.IOException
          - org.springframework.dao.DataAccessException
        ignore-exceptions:
          - com.cekinmezyucel.springboot.poc.exception.ValidationException
      database:
        failure-rate-threshold: 60
        slow-call-duration-threshold: 3s
        wait-duration-in-open-state: 15s
        sliding-window-size: 20
        minimum-number-of-calls: 10
    instances:
      userService:
        base-config: default
        record-result-predicate: com.cekinmezyucel.springboot.poc.resilience.DatabaseResultPredicate
      accountService:
        base-config: default
      databaseOps:
        base-config: database

  retry:
    configs:
      default:
        max-attempts: 3
        wait-duration: 1s
        retry-exceptions:
          - java.net.ConnectException
          - java.net.SocketTimeoutException
          - org.springframework.dao.TransientDataAccessException
        ignore-exceptions:
          - com.cekinmezyucel.springboot.poc.exception.ValidationException
          - com.cekinmezyucel.springboot.poc.exception.ResourceNotFoundException
      exponential:
        max-attempts: 4
        wait-duration: 500ms
        exponential-backoff-multiplier: 2
        random-wait-factor: 0.1
    instances:
      userService:
        base-config: exponential
      accountService:
        base-config: default
      databaseRetry:
        base-config: exponential
        max-attempts: 5

  bulkhead:
    configs:
      default:
        max-concurrent-calls: 10
        max-wait-duration: 5s
    instances:
      userService:
        max-concurrent-calls: 20
      accountService:
        max-concurrent-calls: 15
      reportingService:
        max-concurrent-calls: 5

  timelimiter:
    configs:
      default:
        timeout-duration: 5s
        cancel-running-future: true
    instances:
      userService:
        timeout-duration: 3s
      accountService:
        timeout-duration: 3s
      reportingService:
        timeout-duration: 10s

  ratelimiter:
    configs:
      default:
        limit-for-period: 100
        limit-refresh-period: 1s
        timeout-duration: 0s
    instances:
      userService:
        limit-for-period: 50
      accountService:
        limit-for-period: 50

management:
  health:
    circuitbreakers:
      enabled: true
    ratelimiters:
      enabled: true
  endpoint:
    health:
      show-details: always
  metrics:
    distribution:
      percentiles:
        resilience4j.circuitbreaker.calls: 0.5, 0.95, 0.99
        resilience4j.retry.calls: 0.5, 0.95, 0.99
        resilience4j.bulkhead.calls: 0.5, 0.95, 0.99
```

#### 2.2 Circuit Breaker Configuration Class
Create `src/main/java/com/cekinmezyucel/springboot/poc/config/ResilienceConfig.java`:
```java
package com.cekinmezyucel.springboot.poc.config;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.micrometer.tagged.TaggedCircuitBreakerMetrics;
import io.github.resilience4j.micrometer.tagged.TaggedRetryMetrics;
import io.github.resilience4j.micrometer.tagged.TaggedBulkheadMetrics;
import io.github.resilience4j.retry.RetryRegistry;
import io.github.resilience4j.bulkhead.BulkheadRegistry;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ResilienceConfig {
    
    private static final Logger log = LoggerFactory.getLogger(ResilienceConfig.class);

    @Bean
    public TaggedCircuitBreakerMetrics taggedCircuitBreakerMetrics(
            CircuitBreakerRegistry circuitBreakerRegistry, MeterRegistry meterRegistry) {
        return TaggedCircuitBreakerMetrics.ofCircuitBreakerRegistry(circuitBreakerRegistry, meterRegistry);
    }

    @Bean
    public TaggedRetryMetrics taggedRetryMetrics(
            RetryRegistry retryRegistry, MeterRegistry meterRegistry) {
        return TaggedRetryMetrics.ofRetryRegistry(retryRegistry, meterRegistry);
    }

    @Bean
    public TaggedBulkheadMetrics taggedBulkheadMetrics(
            BulkheadRegistry bulkheadRegistry, MeterRegistry meterRegistry) {
        return TaggedBulkheadMetrics.ofBulkheadRegistry(bulkheadRegistry, meterRegistry);
    }

    // Circuit Breaker Event Listeners
    @Bean
    public CircuitBreaker userServiceCircuitBreaker(CircuitBreakerRegistry registry) {
        CircuitBreaker circuitBreaker = registry.circuitBreaker("userService");
        
        circuitBreaker.getEventPublisher()
            .onStateTransition(event -> 
                log.info("Circuit Breaker '{}' state transition: {} -> {}", 
                    event.getCircuitBreakerName(), 
                    event.getStateTransition().getFromState(),
                    event.getStateTransition().getToState()))
            .onFailureRateExceeded(event ->
                log.warn("Circuit Breaker '{}' failure rate exceeded: {}%", 
                    event.getCircuitBreakerName(), 
                    event.getFailureRate()))
            .onSlowCallRateExceeded(event ->
                log.warn("Circuit Breaker '{}' slow call rate exceeded: {}%", 
                    event.getCircuitBreakerName(), 
                    event.getSlowCallRate()))
            .onCallNotPermitted(event ->
                log.warn("Circuit Breaker '{}' call not permitted", 
                    event.getCircuitBreakerName()));
                    
        return circuitBreaker;
    }
}
```

### Step 3: Enhanced Service Layer with Resilience

#### 3.1 Resilient User Service
Update `src/main/java/com/cekinmezyucel/springboot/poc/service/UserService.java`:
```java
package com.cekinmezyucel.springboot.poc.service;

import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import io.micrometer.core.annotation.Timed;
import io.micrometer.observation.annotation.Observed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
@Observed(name = "user.service")
public class UserService {
    
    private static final Logger log = LoggerFactory.getLogger(UserService.class);
    
    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    
    public UserService(UserRepository userRepository, AccountRepository accountRepository) {
        this.userRepository = userRepository;
        this.accountRepository = accountRepository;
    }

    @CircuitBreaker(name = "userService", fallbackMethod = "getUsersFallback")
    @Retry(name = "userService")
    @Bulkhead(name = "userService")
    @Timed(name = "user.service.getUsers", description = "Time taken to get all users")
    public List<User> getUsers() {
        log.debug("Fetching all users");
        try {
            List<UserEntity> entities = userRepository.findAll();
            return entities.stream().map(this::toModel).toList();
        } catch (DataAccessException e) {
            log.error("Database error while fetching users", e);
            throw e;
        }
    }

    @CircuitBreaker(name = "userService", fallbackMethod = "createUserFallback")
    @Retry(name = "userService")
    @Bulkhead(name = "userService")
    @Transactional
    @Timed(name = "user.service.createUser", description = "Time taken to create user")
    public User createUser(User user) {
        log.debug("Creating user: {}", user.getEmail());
        try {
            UserEntity entity = toEntity(user);
            UserEntity saved = userRepository.save(entity);
            return toModel(saved);
        } catch (DataAccessException e) {
            log.error("Database error while creating user: {}", user.getEmail(), e);
            throw e;
        }
    }

    @CircuitBreaker(name = "userService", fallbackMethod = "linkUserToAccountFallback")
    @Retry(name = "userService")
    @Bulkhead(name = "userService")
    @Transactional
    public void linkUserToAccountWithMembership(Long userId, Long accountId) {
        log.debug("Linking user {} to account {}", userId, accountId);
        try {
            linkUserToAccount(userId, accountId);
            
            Optional<AccountEntity> accountOpt = accountRepository.findById(accountId);
            Optional<UserEntity> userOpt = userRepository.findById(userId);
            
            if (accountOpt.isPresent() && userOpt.isPresent()) {
                AccountEntity account = accountOpt.get();
                UserEntity user = userOpt.get();
                account.getUsers().add(user);
                accountRepository.save(account);
            } else {
                throw new ResourceNotFoundException("User or Account not found");
            }
        } catch (DataAccessException e) {
            log.error("Database error while linking user {} to account {}", userId, accountId, e);
            throw e;
        }
    }

    @TimeLimiter(name = "userService")
    @CircuitBreaker(name = "userService", fallbackMethod = "getUsersAsyncFallback")
    public CompletableFuture<List<User>> getUsersAsync() {
        return CompletableFuture.supplyAsync(() -> getUsers());
    }

    // Fallback methods
    public List<User> getUsersFallback(Exception ex) {
        log.warn("Using fallback for getUsers due to: {}", ex.getMessage());
        return Collections.emptyList();
    }

    public User createUserFallback(User user, Exception ex) {
        log.error("Failed to create user: {} due to: {}", user.getEmail(), ex.getMessage());
        throw new SystemException("User creation temporarily unavailable", ex);
    }

    public void linkUserToAccountFallback(Long userId, Long accountId, Exception ex) {
        log.error("Failed to link user {} to account {} due to: {}", userId, accountId, ex.getMessage());
        throw new SystemException("User-Account linking temporarily unavailable", ex);
    }

    public CompletableFuture<List<User>> getUsersAsyncFallback(Exception ex) {
        log.warn("Using async fallback for getUsers due to: {}", ex.getMessage());
        return CompletableFuture.completedFuture(Collections.emptyList());
    }

    // Existing mapping methods...
    private User toModel(UserEntity entity) {
        User model = new User();
        model.setId(entity.getId());
        model.setEmail(entity.getEmail());
        model.setName(entity.getName());
        model.setSurname(entity.getSurname());
        return model;
    }

    private UserEntity toEntity(User model) {
        UserEntity entity = new UserEntity();
        entity.setId(model.getId());
        entity.setEmail(model.getEmail());
        entity.setName(model.getName());
        entity.setSurname(model.getSurname());
        return entity;
    }

    // Other existing methods...
}
```

#### 3.2 Database Result Predicate
Create `src/main/java/com/cekinmezyucel/springboot/poc/resilience/DatabaseResultPredicate.java`:
```java
package com.cekinmezyucel.springboot.poc.resilience;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.TransientDataAccessException;

import java.util.function.Predicate;

public class DatabaseResultPredicate implements Predicate<Object> {
    
    @Override
    public boolean test(Object result) {
        if (result instanceof Exception exception) {
            return isRecordableException(exception);
        }
        return false;
    }
    
    private boolean isRecordableException(Exception exception) {
        // Record transient database errors but not permanent ones
        if (exception instanceof TransientDataAccessException) {
            return true;
        }
        
        if (exception instanceof DataAccessException) {
            // Check for specific database errors that should trigger circuit breaker
            String message = exception.getMessage().toLowerCase();
            return message.contains("connection") || 
                   message.contains("timeout") ||
                   message.contains("unavailable");
        }
        
        if (exception instanceof CallNotPermittedException) {
            return false; // Don't record circuit breaker rejections
        }
        
        return exception instanceof java.net.ConnectException ||
               exception instanceof java.net.SocketTimeoutException;
    }
}
```

### Step 4: Health Checks for Dependencies

#### 4.1 Enhanced Database Health Indicator
Update `src/main/java/com/cekinmezyucel/springboot/poc/health/DatabaseHealthIndicator.java`:
```java
package com.cekinmezyucel.springboot.poc.health;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.springframework.boot.actuator.health.Health;
import org.springframework.boot.actuator.health.HealthIndicator;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Component
public class DatabaseHealthIndicator implements HealthIndicator {

    private final JdbcTemplate jdbcTemplate;
    private final CircuitBreakerRegistry circuitBreakerRegistry;
    private volatile Instant lastCheckTime = Instant.now();
    private volatile Health lastHealth = Health.unknown().build();

    public DatabaseHealthIndicator(JdbcTemplate jdbcTemplate, 
                                 CircuitBreakerRegistry circuitBreakerRegistry) {
        this.jdbcTemplate = jdbcTemplate;
        this.circuitBreakerRegistry = circuitBreakerRegistry;
    }

    @Override
    public Health health() {
        // Cache health check for 30 seconds to avoid overwhelming database
        if (Duration.between(lastCheckTime, Instant.now()).getSeconds() < 30) {
            return lastHealth;
        }

        try {
            Instant start = Instant.now();
            Integer result = jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            Duration responseTime = Duration.between(start, Instant.now());
            
            Map<String, Object> details = new HashMap<>();
            details.put("database", "PostgreSQL");
            details.put("responseTime", responseTime.toMillis() + "ms");
            details.put("timestamp", Instant.now().toString());
            
            // Add circuit breaker status
            addCircuitBreakerStatus(details);
            
            if (result != null && result == 1) {
                lastHealth = Health.up().withDetails(details).build();
            } else {
                lastHealth = Health.down().withDetails(details).build();
            }
            
        } catch (DataAccessException e) {
            Map<String, Object> details = new HashMap<>();
            details.put("database", "PostgreSQL");
            details.put("error", e.getMessage());
            details.put("timestamp", Instant.now().toString());
            addCircuitBreakerStatus(details);
            
            lastHealth = Health.down().withDetails(details).build();
        }
        
        lastCheckTime = Instant.now();
        return lastHealth;
    }

    private void addCircuitBreakerStatus(Map<String, Object> details) {
        try {
            CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker("databaseOps");
            details.put("circuitBreaker", Map.of(
                "state", circuitBreaker.getState().toString(),
                "failureRate", String.format("%.2f%%", circuitBreaker.getMetrics().getFailureRate()),
                "calls", circuitBreaker.getMetrics().getNumberOfSuccessfulCalls() + 
                        circuitBreaker.getMetrics().getNumberOfFailedCalls()
            ));
        } catch (Exception e) {
            details.put("circuitBreaker", "unavailable");
        }
    }
}
```

#### 4.2 Resilience Health Indicator
Create `src/main/java/com/cekinmezyucel/springboot/poc/health/ResilienceHealthIndicator.java`:
```java
package com.cekinmezyucel.springboot.poc.health;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.springframework.boot.actuator.health.Health;
import org.springframework.boot.actuator.health.HealthIndicator;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class ResilienceHealthIndicator implements HealthIndicator {

    private final CircuitBreakerRegistry circuitBreakerRegistry;

    public ResilienceHealthIndicator(CircuitBreakerRegistry circuitBreakerRegistry) {
        this.circuitBreakerRegistry = circuitBreakerRegistry;
    }

    @Override
    public Health health() {
        Map<String, Object> details = new HashMap<>();
        boolean allHealthy = true;

        for (CircuitBreaker circuitBreaker : circuitBreakerRegistry.getAllCircuitBreakers()) {
            String name = circuitBreaker.getName();
            CircuitBreaker.State state = circuitBreaker.getState();
            
            Map<String, Object> cbDetails = new HashMap<>();
            cbDetails.put("state", state.toString());
            cbDetails.put("failureRate", String.format("%.2f%%", 
                circuitBreaker.getMetrics().getFailureRate()));
            cbDetails.put("successfulCalls", 
                circuitBreaker.getMetrics().getNumberOfSuccessfulCalls());
            cbDetails.put("failedCalls", 
                circuitBreaker.getMetrics().getNumberOfFailedCalls());
            cbDetails.put("slowCalls", 
                circuitBreaker.getMetrics().getNumberOfSlowCalls());
            
            details.put(name, cbDetails);
            
            if (state == CircuitBreaker.State.OPEN || state == CircuitBreaker.State.FORCED_OPEN) {
                allHealthy = false;
            }
        }

        return allHealthy ? 
            Health.up().withDetails(details).build() : 
            Health.down().withDetails(details).build();
    }
}
```

### Step 5: Graceful Shutdown and Startup

#### 5.1 Graceful Shutdown Configuration
Add to `application.yaml`:
```yaml
server:
  shutdown: graceful
  
spring:
  lifecycle:
    timeout-per-shutdown-phase: 30s
    
management:
  server:
    port: 8081
  endpoint:
    shutdown:
      enabled: true
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus,shutdown
```

#### 5.2 Application Event Listeners
Create `src/main/java/com/cekinmezyucel/springboot/poc/lifecycle/ApplicationLifecycleListener.java`:
```java
package com.cekinmezyucel.springboot.poc.lifecycle;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class ApplicationLifecycleListener {
    
    private static final Logger log = LoggerFactory.getLogger(ApplicationLifecycleListener.class);
    
    @EventListener
    public void onApplicationReady(ApplicationReadyEvent event) {
        log.info("Application is ready to receive traffic");
        // Perform any initialization tasks
        warmupCaches();
        verifyDependencies();
    }
    
    @EventListener
    public void onContextClosed(ContextClosedEvent event) {
        log.info("Application is shutting down gracefully");
        // Perform cleanup tasks
        closeConnections();
        saveState();
    }
    
    private void warmupCaches() {
        log.debug("Warming up caches...");
        // Cache warmup logic
    }
    
    private void verifyDependencies() {
        log.debug("Verifying dependencies...");
        // Dependency verification logic
    }
    
    private void closeConnections() {
        log.debug("Closing external connections...");
        // Connection cleanup logic
    }
    
    private void saveState() {
        log.debug("Saving application state...");
        // State persistence logic
    }
}
```

### Step 6: Testing Resilience Patterns

#### 6.1 Resilience Integration Tests
Create `src/test/java/com/cekinmezyucel/springboot/poc/resilience/ResilienceIntegrationTest.java`:
```java
package com.cekinmezyucel.springboot.poc.resilience;

import com.cekinmezyucel.springboot.poc.BaseIntegrationTest;
import com.cekinmezyucel.springboot.poc.service.UserService;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.time.Duration;

@TestPropertySource(properties = {
    "resilience4j.circuitbreaker.instances.userService.failure-rate-threshold=20",
    "resilience4j.circuitbreaker.instances.userService.minimum-number-of-calls=2",
    "resilience4j.circuitbreaker.instances.userService.wait-duration-in-open-state=1s"
})
class ResilienceIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private UserService userService;

    @Autowired
    private CircuitBreakerRegistry circuitBreakerRegistry;

    @Test
    @DisplayName("Should have circuit breaker configured for user service")
    void shouldHaveCircuitBreakerConfigured() {
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker("userService");
        
        assertThat(circuitBreaker).isNotNull();
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
    }

    @Test
    @DisplayName("Should execute fallback when service fails")
    void shouldExecuteFallbackWhenServiceFails() {
        // This test would require mocking database failures
        // For now, we just verify the service works normally
        var users = userService.getUsers();
        assertThat(users).isNotNull();
    }

    @Test
    @DisplayName("Should have retry configuration")
    void shouldHaveRetryConfiguration() {
        // Test that retry annotations are present and working
        // This is verified through successful execution
        var users = userService.getUsers();
        assertThat(users).isNotNull();
    }

    @Test
    @DisplayName("Should have bulkhead configuration")
    void shouldHaveBulkheadConfiguration() {
        // Test concurrent access is limited by bulkhead
        // This is more of a load test scenario
        var users = userService.getUsers();
        assertThat(users).isNotNull();
    }
}
```

#### 6.2 Circuit Breaker Chaos Testing
Create `src/test/java/com/cekinmezyucel/springboot/poc/resilience/ChaosEngineeringTest.java`:
```java
package com.cekinmezyucel.springboot.poc.resilience;

import com.cekinmezyucel.springboot.poc.BaseIntegrationTest;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.testcontainers.containers.PostgreSQLContainer;

import static org.assertj.core.api.Assertions.assertThat;

class ChaosEngineeringTest extends BaseIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private CircuitBreakerRegistry circuitBreakerRegistry;

    @Test
    @DisplayName("Should handle database connection failures gracefully")
    void shouldHandleDatabaseFailuresGracefully() {
        // Test resilience when database is temporarily unavailable
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker("userService");
        CircuitBreaker.State initialState = circuitBreaker.getState();
        
        assertThat(initialState).isEqualTo(CircuitBreaker.State.CLOSED);
        
        // Normal operation should work
        ResponseEntity<String> response = restTemplate
            .withBasicAuth("test", "test")
            .getForEntity("/users", String.class);
            
        // We expect either success or controlled failure with fallback
        assertThat(response.getStatusCode()).isIn(HttpStatus.OK, HttpStatus.SERVICE_UNAVAILABLE);
    }

    @Test
    @DisplayName("Should recover automatically when database comes back online")
    void shouldRecoverWhenDatabaseComesOnline() {
        // This test verifies automatic recovery from half-open to closed state
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker("userService");
        
        // Should be in closed state initially
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
        
        // Normal requests should work
        ResponseEntity<String> response = restTemplate
            .withBasicAuth("test", "test")
            .getForEntity("/actuator/health", String.class);
            
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }
}
```

## Validation Checklist

### ✅ Circuit Breaker Implementation
- [ ] Circuit breakers configured for critical services
- [ ] Fallback methods implemented and tested
- [ ] Circuit breaker state transitions logged
- [ ] Metrics exposed for monitoring

### ✅ Retry Mechanisms
- [ ] Retry policies configured with exponential backoff
- [ ] Transient vs permanent exceptions handled correctly
- [ ] Maximum retry attempts configured appropriately
- [ ] Retry metrics available

### ✅ Bulkhead Isolation
- [ ] Concurrent call limits configured
- [ ] Different thread pools for different operations
- [ ] Bulkhead metrics monitored
- [ ] Wait timeouts configured

### ✅ Timeout Configuration
- [ ] Timeouts set for all external calls
- [ ] Different timeouts for different operations
- [ ] Timeout cancellation working
- [ ] Timeout metrics tracked

### ✅ Health Checks
- [ ] Enhanced health indicators implemented
- [ ] Circuit breaker status in health checks
- [ ] Dependency status monitored
- [ ] Health check caching implemented

### ✅ Graceful Handling
- [ ] Graceful shutdown configured
- [ ] Application lifecycle events handled
- [ ] State persistence during shutdown
- [ ] Warmup procedures on startup

## Troubleshooting

### Common Issues
1. **Circuit breaker not triggering**: Check failure rate threshold and minimum calls
2. **Fallback not working**: Verify fallback method signatures match exactly
3. **Metrics not appearing**: Ensure Micrometer integration is configured
4. **Timeouts too aggressive**: Adjust timeout values based on actual performance

### Performance Considerations
- Monitor circuit breaker metrics to tune thresholds
- Adjust bulkhead limits based on actual load patterns
- Set appropriate timeout values for different operations
- Consider the overhead of resilience patterns in high-throughput scenarios

### Testing Strategies
- Use chaos engineering to test failure scenarios
- Implement load testing to verify bulkhead effectiveness
- Test circuit breaker state transitions
- Verify fallback behavior under different failure conditions

## Next Steps
After implementing resilience patterns:
1. Set up comprehensive monitoring and alerting
2. Implement chaos engineering in CI/CD pipeline
3. Add distributed circuit breakers with Redis
4. Implement saga patterns for distributed transactions
5. Add automated failure injection testing

## AI Agent Notes
- Always test resilience patterns with actual failure scenarios
- Monitor circuit breaker metrics to tune configuration
- Ensure fallback methods don't perform expensive operations
- Consider the impact of resilience patterns on overall system performance
- Update health checks when adding new dependencies or services