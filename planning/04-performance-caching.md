# Performance & Caching Enhancement Plan

## Current State Analysis
- ❌ No caching layer implemented
- ❌ Default database connection pool settings
- ❌ No query optimization monitoring
- ❌ No performance metrics collection
- ❌ No connection pool monitoring
- ❌ Basic JPA configuration without tuning

## Target State
- ✅ Multi-level caching strategy (L1/L2/Redis)
- ✅ Optimized database connection pooling
- ✅ Query performance monitoring and optimization
- ✅ Performance metrics and profiling
- ✅ Database indexing strategy
- ✅ Lazy loading optimization
- ✅ Pagination and sorting improvements

## Implementation Steps

### Step 1: Caching Infrastructure

#### 1.1 Add Caching Dependencies
```gradle
// Add to build.gradle dependencies
implementation 'org.springframework.boot:spring-boot-starter-cache'
implementation 'org.springframework.boot:spring-boot-starter-data-redis'
implementation 'com.github.ben-manes.caffeine:caffeine:3.1.8'
implementation 'org.redisson:redisson-spring-boot-starter:3.24.3'
```

#### 1.2 Cache Configuration
Create `src/main/java/com/cekinmezyucel/springboot/poc/config/CacheConfig.java`:
```java
package com.cekinmezyucel.springboot.poc.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.redisson.api.RedissonClient;
import org.redisson.spring.cache.RedissonSpringCacheManager;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.cache.support.CompositeCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.time.Duration;
import java.util.List;

@Configuration
@EnableCaching
public class CacheConfig {

    @Value("${app.cache.redis.enabled:true}")
    private boolean redisEnabled;

    @Bean
    @Primary
    public CacheManager cacheManager(RedissonClient redissonClient) {
        CompositeCacheManager compositeCacheManager = new CompositeCacheManager();
        
        if (redisEnabled) {
            // L2 Cache - Redis (distributed)
            RedissonSpringCacheManager redissonCacheManager = new RedissonSpringCacheManager(redissonClient);
            redissonCacheManager.setConfigLocation("classpath:cache-config.yaml");
            
            // L1 Cache - Caffeine (local)
            CaffeineCacheManager caffeineCacheManager = new CaffeineCacheManager();
            caffeineCacheManager.setCaffeine(Caffeine.newBuilder()
                .maximumSize(1000)
                .expireAfterWrite(Duration.ofMinutes(5))
                .recordStats());
            caffeineCacheManager.setCacheNames(List.of("users-local", "accounts-local", "user-accounts-local"));
            
            compositeCacheManager.setCacheManagers(List.of(caffeineCacheManager, redissonCacheManager));
        } else {
            // Fallback to local cache only
            CaffeineCacheManager caffeineCacheManager = new CaffeineCacheManager();
            caffeineCacheManager.setCaffeine(Caffeine.newBuilder()
                .maximumSize(5000)
                .expireAfterWrite(Duration.ofMinutes(30))
                .recordStats());
            
            compositeCacheManager.setCacheManagers(List.of(caffeineCacheManager));
        }
        
        compositeCacheManager.setFallbackToNoOpCache(false);
        return compositeCacheManager;
    }

    @Bean
    public CaffeineCacheManager localCacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        cacheManager.setCaffeine(Caffeine.newBuilder()
            .maximumSize(1000)
            .expireAfterWrite(Duration.ofMinutes(5))
            .expireAfterAccess(Duration.ofMinutes(2))
            .recordStats());
        return cacheManager;
    }
}
```

#### 1.3 Cache Configuration File
Create `src/main/resources/cache-config.yaml`:
```yaml
users:
  ttl: 300000  # 5 minutes
  maxIdleTime: 120000  # 2 minutes
  maxSize: 1000

accounts:
  ttl: 600000  # 10 minutes
  maxIdleTime: 300000  # 5 minutes
  maxSize: 500

user-accounts:
  ttl: 180000  # 3 minutes
  maxIdleTime: 60000   # 1 minute
  maxSize: 2000

query-cache:
  ttl: 60000   # 1 minute
  maxIdleTime: 30000   # 30 seconds
  maxSize: 5000
```

#### 1.4 Redis Configuration
Add to `application.yaml`:
```yaml
spring:
  data:
    redis:
      host: ${REDIS_HOST:localhost}
      port: ${REDIS_PORT:6379}
      password: ${REDIS_PASSWORD:}
      database: ${REDIS_DATABASE:0}
      timeout: 2000ms
      lettuce:
        pool:
          max-active: 20
          max-idle: 10
          min-idle: 5
          max-wait: 1000ms
        shutdown-timeout: 100ms
  cache:
    type: redis
    redis:
      time-to-live: 300000  # 5 minutes default
      cache-null-values: false
      use-key-prefix: true
      key-prefix: "springboot-poc:"

# Redisson Configuration
redisson:
  config: |
    singleServerConfig:
      address: "redis://${REDIS_HOST:localhost}:${REDIS_PORT:6379}"
      password: ${REDIS_PASSWORD:}
      database: ${REDIS_DATABASE:0}
      connectionPoolSize: 20
      connectionMinimumIdleSize: 5
      timeout: 3000
      retryAttempts: 3
      retryInterval: 1500

app:
  cache:
    redis:
      enabled: ${CACHE_REDIS_ENABLED:true}
```

### Step 2: Database Connection Pool Optimization

#### 2.1 HikariCP Configuration
Add to `application.yaml`:
```yaml
spring:
  datasource:
    type: com.zaxxer.hikari.HikariDataSource
    hikari:
      pool-name: SpringBootPOC-HikariCP
      maximum-pool-size: ${DB_POOL_MAX_SIZE:20}
      minimum-idle: ${DB_POOL_MIN_IDLE:5}
      connection-timeout: ${DB_CONNECTION_TIMEOUT:30000}
      idle-timeout: ${DB_IDLE_TIMEOUT:600000}
      max-lifetime: ${DB_MAX_LIFETIME:1800000}
      validation-timeout: ${DB_VALIDATION_TIMEOUT:5000}
      leak-detection-threshold: ${DB_LEAK_DETECTION:60000}
      connection-test-query: SELECT 1
      register-mbeans: true
      auto-commit: false
      transaction-isolation: TRANSACTION_READ_COMMITTED
      
  jpa:
    properties:
      hibernate:
        # Query Performance
        jdbc:
          batch_size: 20
          fetch_size: 50
        order_inserts: true
        order_updates: true
        batch_versioned_data: true
        
        # Statement Caching
        statement_cache_size: 50
        use_query_cache: true
        query_cache_factory: org.hibernate.cache.caffeine.CaffeineCacheRegionFactory
        
        # Statistics and Monitoring
        generate_statistics: true
        session:
          events:
            log:
              LOG_QUERIES_SLOWER_THAN_MS: 100
              
        # Connection Pool Integration
        connection:
          provider_disables_autocommit: true
          
        # Lazy Loading Optimization
        enable_lazy_load_no_trans: false
        max_fetch_depth: 3
        
        # Schema Validation
        ddl-auto: none
        show-sql: false
        format_sql: false
```

#### 2.2 Database Performance Monitoring
Create `src/main/java/com/cekinmezyucel/springboot/poc/config/DatabaseMetricsConfig.java`:
```java
package com.cekinmezyucel.springboot.poc.config;

import com.zaxxer.hikari.HikariDataSource;
import io.micrometer.core.instrument.MeterRegistry;
import org.hibernate.SessionFactory;
import org.springframework.boot.actuate.metrics.MetricsEndpoint;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

@Configuration
public class DatabaseMetricsConfig {

    @Bean
    public HikariDataSourceMetrics hikariDataSourceMetrics(DataSource dataSource, MeterRegistry meterRegistry) {
        if (dataSource instanceof HikariDataSource hikariDataSource) {
            return new HikariDataSourceMetrics(hikariDataSource, meterRegistry);
        }
        return null;
    }

    @Bean
    public HibernateMetrics hibernateMetrics(SessionFactory sessionFactory, MeterRegistry meterRegistry) {
        return new HibernateMetrics(sessionFactory, "springboot-poc", meterRegistry);
    }
    
    // Custom metrics class for HikariCP
    public static class HikariDataSourceMetrics {
        private final HikariDataSource dataSource;
        private final MeterRegistry meterRegistry;
        
        public HikariDataSourceMetrics(HikariDataSource dataSource, MeterRegistry meterRegistry) {
            this.dataSource = dataSource;
            this.meterRegistry = meterRegistry;
            bindMetrics();
        }
        
        private void bindMetrics() {
            meterRegistry.gauge("hikaricp.connections.active", dataSource, 
                ds -> ds.getHikariPoolMXBean().getActiveConnections());
            meterRegistry.gauge("hikaricp.connections.idle", dataSource, 
                ds -> ds.getHikariPoolMXBean().getIdleConnections());
            meterRegistry.gauge("hikaricp.connections.total", dataSource, 
                ds -> ds.getHikariPoolMXBean().getTotalConnections());
            meterRegistry.gauge("hikaricp.connections.waiting", dataSource, 
                ds -> ds.getHikariPoolMXBean().getThreadsAwaitingConnection());
        }
    }
    
    // Custom metrics class for Hibernate
    public static class HibernateMetrics {
        public HibernateMetrics(SessionFactory sessionFactory, String name, MeterRegistry meterRegistry) {
            if (sessionFactory.getStatistics().isStatisticsEnabled()) {
                var statistics = sessionFactory.getStatistics();
                
                meterRegistry.gauge("hibernate.query.cache.hit.count", statistics, 
                    s -> s.getQueryCacheHitCount());
                meterRegistry.gauge("hibernate.query.cache.miss.count", statistics, 
                    s -> s.getQueryCacheMissCount());
                meterRegistry.gauge("hibernate.second.level.cache.hit.count", statistics, 
                    s -> s.getSecondLevelCacheHitCount());
                meterRegistry.gauge("hibernate.second.level.cache.miss.count", statistics, 
                    s -> s.getSecondLevelCacheMissCount());
                meterRegistry.gauge("hibernate.sessions.open.count", statistics, 
                    s -> s.getSessionOpenCount());
                meterRegistry.gauge("hibernate.sessions.close.count", statistics, 
                    s -> s.getSessionCloseCount());
                meterRegistry.gauge("hibernate.transactions.count", statistics, 
                    s -> s.getTransactionCount());
            }
        }
    }
}
```

### Step 3: Enhanced Services with Caching

#### 3.1 Cached User Service
Update `src/main/java/com/cekinmezyucel/springboot/poc/service/UserService.java`:
```java
package com.cekinmezyucel.springboot.poc.service;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Observed(name = "user.service")
public class UserService {
    
    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    
    // Constructor...

    @Cacheable(value = "users", key = "'all'", unless = "#result.isEmpty()")
    @CircuitBreaker(name = "userService", fallbackMethod = "getUsersFallback")
    @Retry(name = "userService")
    @Timed(name = "user.service.getUsers")
    public List<User> getUsers() {
        log.debug("Fetching all users from database");
        return userRepository.findAll().stream()
            .map(this::toModelWithAccountIds)
            .toList();
    }

    @Cacheable(value = "users", key = "#pageable.pageNumber + '-' + #pageable.pageSize + '-' + #pageable.sort.toString()")
    @Timed(name = "user.service.getUsersPaged")
    public Page<User> getUsers(Pageable pageable) {
        log.debug("Fetching users page: {}", pageable);
        return userRepository.findAll(pageable)
            .map(this::toModelWithAccountIds);
    }

    @Cacheable(value = "users", key = "#id", unless = "#result == null")
    @Timed(name = "user.service.getUserById")
    public Optional<User> getUserById(Long id) {
        log.debug("Fetching user by id: {}", id);
        return userRepository.findById(id)
            .map(this::toModelWithAccountIds);
    }

    @Cacheable(value = "users", key = "'email:' + #email", unless = "#result == null")
    @Timed(name = "user.service.getUserByEmail")
    public Optional<User> getUserByEmail(String email) {
        log.debug("Fetching user by email: {}", email);
        return userRepository.findByEmail(email)
            .map(this::toModelWithAccountIds);
    }

    @CachePut(value = "users", key = "#result.id")
    @CacheEvict(value = "users", key = "'all'")
    @CircuitBreaker(name = "userService", fallbackMethod = "createUserFallback")
    @Retry(name = "userService")
    @Transactional
    @Timed(name = "user.service.createUser")
    public User createUser(User user) {
        log.debug("Creating user: {}", user.getEmail());
        UserEntity entity = toEntity(user);
        UserEntity saved = userRepository.save(entity);
        return toModelWithAccountIds(saved);
    }

    @CachePut(value = "users", key = "#result.id")
    @CacheEvict(value = "users", key = "'all'")
    @Transactional
    @Timed(name = "user.service.updateUser")
    public User updateUser(User user) {
        log.debug("Updating user: {}", user.getId());
        UserEntity entity = userRepository.findById(user.getId())
            .orElseThrow(() -> new ResourceNotFoundException("User not found: " + user.getId()));
        
        entity.setEmail(user.getEmail());
        entity.setName(user.getName());
        entity.setSurname(user.getSurname());
        
        UserEntity saved = userRepository.save(entity);
        return toModelWithAccountIds(saved);
    }

    @Caching(evict = {
        @CacheEvict(value = "users", key = "#userId"),
        @CacheEvict(value = "users", key = "'all'"),
        @CacheEvict(value = "user-accounts", key = "#userId + '-accounts'"),
        @CacheEvict(value = "accounts", key = "#accountId + '-users'")
    })
    @CircuitBreaker(name = "userService", fallbackMethod = "linkUserToAccountFallback")
    @Retry(name = "userService")
    @Transactional
    @Timed(name = "user.service.linkUserToAccount")
    public void linkUserToAccountWithMembership(Long userId, Long accountId) {
        log.debug("Linking user {} to account {}", userId, accountId);
        
        UserEntity user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
        AccountEntity account = accountRepository.findById(accountId)
            .orElseThrow(() -> new ResourceNotFoundException("Account not found: " + accountId));
        
        user.getAccounts().add(account);
        account.getUsers().add(user);
        
        userRepository.save(user);
        accountRepository.save(account);
    }

    @Cacheable(value = "user-accounts", key = "#userId + '-accounts'")
    @Timed(name = "user.service.getUserAccounts")
    public List<Account> getUserAccounts(Long userId) {
        log.debug("Fetching accounts for user: {}", userId);
        return userRepository.findById(userId)
            .map(user -> user.getAccounts().stream()
                .map(this::accountToModel)
                .toList())
            .orElse(Collections.emptyList());
    }

    // Enhanced mapping with account IDs
    private User toModelWithAccountIds(UserEntity entity) {
        User model = toModel(entity);
        if (entity.getAccounts() != null) {
            List<Long> accountIds = entity.getAccounts().stream()
                .map(AccountEntity::getId)
                .toList();
            model.setAccountIds(accountIds);
        }
        return model;
    }

    // Existing methods...
}
```

#### 3.2 Enhanced Repository with Custom Queries
Update `src/main/java/com/cekinmezyucel/springboot/poc/repository/UserRepository.java`:
```java
package com.cekinmezyucel.springboot.poc.repository;

import com.cekinmezyucel.springboot.poc.entity.UserEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.QueryHint;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<UserEntity, Long> {

    @QueryHints({
        @QueryHint(name = "org.hibernate.cacheable", value = "true"),
        @QueryHint(name = "org.hibernate.cacheRegion", value = "query-cache")
    })
    Optional<UserEntity> findByEmail(String email);

    @Query("SELECT u FROM UserEntity u LEFT JOIN FETCH u.accounts WHERE u.email = :email")
    @QueryHints({
        @QueryHint(name = "org.hibernate.cacheable", value = "true")
    })
    Optional<UserEntity> findByEmailWithAccounts(@Param("email") String email);

    @Query("SELECT u FROM UserEntity u WHERE u.name LIKE %:name% OR u.surname LIKE %:name%")
    @QueryHints({
        @QueryHint(name = "org.hibernate.cacheable", value = "true")
    })
    Page<UserEntity> findByNameContaining(@Param("name") String name, Pageable pageable);

    @Query("SELECT u FROM UserEntity u JOIN u.accounts a WHERE a.id = :accountId")
    @QueryHints({
        @QueryHint(name = "org.hibernate.cacheable", value = "true")
    })
    List<UserEntity> findByAccountId(@Param("accountId") Long accountId);

    @Query(value = """
        SELECT u.* FROM users u 
        WHERE u.id IN (
            SELECT ua.user_id FROM user_accounts ua 
            WHERE ua.account_id IN :accountIds
        )
        """, nativeQuery = true)
    List<UserEntity> findByAccountIds(@Param("accountIds") List<Long> accountIds);

    @Query("SELECT COUNT(u) FROM UserEntity u JOIN u.accounts a WHERE a.id = :accountId")
    long countByAccountId(@Param("accountId") Long accountId);
}
```

### Step 4: Database Indexing Strategy

#### 4.1 Database Migration for Indexes
Create `src/main/resources/db/migration/V2__add_performance_indexes.sql`:
```sql
-- Indexes for Users table
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_users_email ON users(email);
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_users_name_surname ON users(name, surname);
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_users_created_at ON users(created_at) WHERE created_at IS NOT NULL;

-- Indexes for Accounts table  
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_accounts_name ON accounts(name);
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_accounts_industry ON accounts(industry) WHERE industry IS NOT NULL;
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_accounts_created_at ON accounts(created_at) WHERE created_at IS NOT NULL;

-- Indexes for junction table
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_user_accounts_user_id ON user_accounts(user_id);
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_user_accounts_account_id ON user_accounts(account_id);
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_user_accounts_composite ON user_accounts(user_id, account_id);

-- Partial indexes for specific queries
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_users_active_email 
    ON users(email) WHERE email IS NOT NULL AND email <> '';

-- GIN index for full-text search (if needed in future)
-- CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_users_fulltext 
--     ON users USING gin(to_tsvector('english', name || ' ' || surname || ' ' || email));

-- Statistics update
ANALYZE users;
ANALYZE accounts;
ANALYZE user_accounts;
```

### Step 5: Cache Monitoring and Management

#### 5.1 Cache Metrics Configuration
Create `src/main/java/com/cekinmezyucel/springboot/poc/monitoring/CacheMetricsConfiguration.java`:
```java
package com.cekinmezyucel.springboot.poc.monitoring;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.stats.CacheStats;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.cache.CaffeineCacheMetrics;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;

@Configuration
public class CacheMetricsConfiguration {

    private final MeterRegistry meterRegistry;
    private final CacheManager cacheManager;

    public CacheMetricsConfiguration(MeterRegistry meterRegistry, CacheManager cacheManager) {
        this.meterRegistry = meterRegistry;
        this.cacheManager = cacheManager;
    }

    @EventListener
    public void bindCacheMetrics(ContextRefreshedEvent event) {
        cacheManager.getCacheNames().forEach(cacheName -> {
            org.springframework.cache.Cache cache = cacheManager.getCache(cacheName);
            if (cache instanceof CaffeineCache caffeineCache) {
                Cache<Object, Object> nativeCache = caffeineCache.getNativeCache();
                CaffeineCacheMetrics.monitor(meterRegistry, nativeCache, cacheName);
                
                // Custom metrics
                meterRegistry.gauge("cache.size", cacheName, c -> nativeCache.estimatedSize());
                meterRegistry.gauge("cache.hit.ratio", cacheName, c -> {
                    CacheStats stats = nativeCache.stats();
                    return stats.requestCount() > 0 ? stats.hitRate() : 0.0;
                });
            }
        });
    }
}
```

#### 5.2 Cache Health Indicator
Create `src/main/java/com/cekinmezyucel/springboot/poc/health/CacheHealthIndicator.java`:
```java
package com.cekinmezyucel.springboot.poc.health;

import org.springframework.boot.actuator.health.Health;
import org.springframework.boot.actuator.health.HealthIndicator;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class CacheHealthIndicator implements HealthIndicator {

    private final CacheManager cacheManager;
    private final RedisTemplate<String, Object> redisTemplate;

    public CacheHealthIndicator(CacheManager cacheManager, RedisTemplate<String, Object> redisTemplate) {
        this.cacheManager = cacheManager;
        this.redisTemplate = redisTemplate;
    }

    @Override
    public Health health() {
        Map<String, Object> details = new HashMap<>();
        boolean healthy = true;

        // Check local caches
        try {
            Map<String, Object> localCaches = new HashMap<>();
            cacheManager.getCacheNames().forEach(cacheName -> {
                var cache = cacheManager.getCache(cacheName);
                if (cache instanceof CaffeineCache caffeineCache) {
                    var nativeCache = caffeineCache.getNativeCache();
                    var stats = nativeCache.stats();
                    
                    localCaches.put(cacheName, Map.of(
                        "size", nativeCache.estimatedSize(),
                        "hitRate", String.format("%.2f%%", stats.hitRate() * 100),
                        "missRate", String.format("%.2f%%", stats.missRate() * 100),
                        "requestCount", stats.requestCount()
                    ));
                }
            });
            details.put("localCaches", localCaches);
        } catch (Exception e) {
            details.put("localCaches", "error: " + e.getMessage());
            healthy = false;
        }

        // Check Redis connection
        try {
            RedisConnection connection = redisTemplate.getConnectionFactory().getConnection();
            String pong = connection.ping();
            details.put("redis", Map.of(
                "status", "connected",
                "ping", pong
            ));
            connection.close();
        } catch (Exception e) {
            details.put("redis", Map.of(
                "status", "disconnected",
                "error", e.getMessage()
            ));
            // Don't fail health check if Redis is down, as we have local fallback
        }

        return healthy ? 
            Health.up().withDetails(details).build() : 
            Health.down().withDetails(details).build();
    }
}
```

### Step 6: Performance Testing Configuration

#### 6.1 Performance Test Profile
Create `src/main/resources/application-performance.properties`:
```properties
# Performance testing configuration
spring.datasource.hikari.maximum-pool-size=50
spring.datasource.hikari.minimum-idle=10

# Enable detailed metrics
spring.jpa.properties.hibernate.generate_statistics=true
spring.jpa.show-sql=false

# Cache configuration for performance tests
app.cache.redis.enabled=true
spring.cache.redis.time-to-live=60000

# Logging
logging.level.org.hibernate.SQL=WARN
logging.level.org.hibernate.type.descriptor.sql.BasicBinder=WARN
logging.level.com.cekinmezyucel.springboot.poc=INFO
```

#### 6.2 Performance Integration Tests
Create `src/test/java/com/cekinmezyucel/springboot/poc/performance/PerformanceIntegrationTest.java`:
```java
package com.cekinmezyucel.springboot.poc.performance;

import com.cekinmezyucel.springboot.poc.BaseIntegrationTest;
import com.cekinmezyucel.springboot.poc.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.util.StopWatch;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("performance")
class PerformanceIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private UserService userService;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private CacheManager cacheManager;

    @Test
    @DisplayName("Should demonstrate cache performance improvement")
    void shouldDemonstrateCachePerformance() {
        // Clear cache
        cacheManager.getCacheNames().forEach(cacheName -> 
            cacheManager.getCache(cacheName).clear());

        // Measure performance without cache
        StopWatch stopWatch = new StopWatch();
        stopWatch.start("without-cache");
        for (int i = 0; i < 10; i++) {
            userService.getUsers();
        }
        stopWatch.stop();

        // Measure performance with cache
        stopWatch.start("with-cache");
        for (int i = 0; i < 10; i++) {
            userService.getUsers();
        }
        stopWatch.stop();

        // Cache should be significantly faster for repeated calls
        long withoutCache = stopWatch.getTaskInfo()[0].getTimeMillis();
        long withCache = stopWatch.getTaskInfo()[1].getTimeMillis();
        
        assertThat(withCache).isLessThan(withoutCache);
    }

    @Test
    @DisplayName("Should handle concurrent requests efficiently")
    void shouldHandleConcurrentRequestsEfficiently() {
        ExecutorService executor = Executors.newFixedThreadPool(10);
        
        StopWatch stopWatch = new StopWatch();
        stopWatch.start("concurrent-requests");
        
        CompletableFuture<?>[] futures = IntStream.range(0, 50)
            .mapToObj(i -> CompletableFuture.runAsync(() -> {
                userService.getUsers();
            }, executor))
            .toArray(CompletableFuture[]::new);
        
        CompletableFuture.allOf(futures).join();
        stopWatch.stop();
        
        assertThat(stopWatch.getLastTaskTimeMillis()).isLessThan(5000); // Should complete within 5 seconds
        
        executor.shutdown();
    }

    @Test
    @DisplayName("Should maintain response times under load")
    void shouldMaintainResponseTimesUnderLoad() {
        // Warm up
        for (int i = 0; i < 5; i++) {
            userService.getUsers();
        }

        // Measure response times
        long totalTime = 0;
        int iterations = 100;
        
        for (int i = 0; i < iterations; i++) {
            long start = System.currentTimeMillis();
            userService.getUsers();
            long end = System.currentTimeMillis();
            totalTime += (end - start);
        }
        
        long averageTime = totalTime / iterations;
        assertThat(averageTime).isLessThan(50); // Average should be less than 50ms
    }
}
```

## Validation Checklist

### ✅ Caching Implementation
- [ ] Multi-level caching (L1/L2) configured
- [ ] Cache keys properly designed
- [ ] Cache eviction strategies implemented
- [ ] Cache metrics monitored
- [ ] Cache health checks working

### ✅ Database Optimization
- [ ] Connection pool tuned for load
- [ ] Query performance monitored
- [ ] Appropriate indexes created
- [ ] Batch operations configured
- [ ] Transaction isolation optimized

### ✅ Performance Monitoring
- [ ] Database connection pool metrics
- [ ] Query execution time monitoring
- [ ] Cache hit/miss ratios tracked
- [ ] JVM memory metrics collected
- [ ] Application performance metrics

### ✅ Query Optimization
- [ ] N+1 query problems resolved
- [ ] Lazy loading properly configured
- [ ] Fetch strategies optimized
- [ ] Query hints applied where appropriate
- [ ] Native queries used for complex operations

## Troubleshooting

### Common Issues
1. **Cache not working**: Check cache configuration and annotations
2. **Poor cache hit rates**: Review cache keys and eviction policies
3. **Database connection issues**: Tune HikariCP settings
4. **Slow queries**: Add appropriate indexes and optimize queries
5. **Memory issues**: Monitor cache sizes and tune heap settings

### Performance Tuning Tips
- Monitor cache hit rates and adjust TTL accordingly
- Use appropriate cache keys to maximize hit rates
- Implement cache warming strategies for critical data
- Optimize database queries before caching
- Consider cache partitioning for large datasets

## Next Steps
After implementing performance optimizations:
1. Set up application performance monitoring (APM)
2. Implement query result caching at database level
3. Add read replicas for read-heavy operations
4. Implement database sharding if needed
5. Set up automated performance regression testing

## AI Agent Notes
- Always measure before and after performance improvements
- Monitor cache metrics to ensure proper configuration
- Test cache behavior under different load scenarios
- Ensure cache invalidation works correctly
- Consider cache warming strategies for production deployment