# Observability & Monitoring Enhancement Plan

## Current State Analysis
- ✅ Basic Spring Boot Actuator enabled
- ❌ No metrics collection beyond basic health
- ❌ No distributed tracing
- ❌ No structured logging
- ❌ No monitoring dashboards
- ❌ No alerting system

## Target State
- ✅ Comprehensive metrics with Micrometer + Prometheus
- ✅ Distributed tracing with OpenTelemetry + Zipkin
- ✅ Structured JSON logging with correlation IDs
- ✅ Application Performance Monitoring (APM)
- ✅ Real-time dashboards with Grafana
- ✅ Proactive alerting system

## Implementation Steps

### Step 1: Enhanced Metrics Collection

#### 1.1 Update Dependencies
```gradle
// Add to build.gradle dependencies block
implementation 'io.micrometer:micrometer-registry-prometheus'
implementation 'io.micrometer:micrometer-core'
implementation 'io.micrometer:micrometer-registry-jmx'
```

#### 1.2 Configure Actuator Endpoints
Create/update `src/main/resources/application.yaml`:
```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus,loggers
      base-path: /actuator
  endpoint:
    health:
      show-details: when-authorized
      show-components: always
    metrics:
      enabled: true
    prometheus:
      enabled: true
  metrics:
    export:
      prometheus:
        enabled: true
    distribution:
      percentiles-histogram:
        http.server.requests: true
      percentiles:
        http.server.requests: 0.5, 0.95, 0.99
      sli:
        maximum-expected-value:
          http.server.requests: 5s
```

#### 1.3 Custom Metrics Configuration
Create `src/main/java/com/cekinmezyucel/springboot/poc/config/MetricsConfig.java`:
```java
package com.cekinmezyucel.springboot.poc.config;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.config.MeterFilter;
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MetricsConfig {

    @Bean
    public MeterRegistryCustomizer<MeterRegistry> meterRegistryCustomizer() {
        return registry -> {
            registry.config()
                .meterFilter(MeterFilter.deny(id -> 
                    id.getName().startsWith("jvm.threads.states")))
                .meterFilter(MeterFilter.maximumExpectedValue(
                    "http.server.requests", Duration.ofSeconds(5)))
                .commonTags("application", "springboot-poc");
        };
    }
}
```

### Step 2: Distributed Tracing

#### 2.1 Add Tracing Dependencies
```gradle
// Add to build.gradle dependencies
implementation 'io.micrometer:micrometer-tracing-bridge-otel'
implementation 'io.opentelemetry:opentelemetry-exporter-zipkin'
implementation 'net.ttddyy.observation:datasource-micrometer-spring-boot:1.0.3'
```

#### 2.2 Tracing Configuration
Add to `application.yaml`:
```yaml
management:
  tracing:
    sampling:
      probability: 1.0
  zipkin:
    tracing:
      endpoint: http://localhost:9411/api/v2/spans

logging:
  pattern:
    correlation: '[${spring.application.name:},%X{traceId:-},%X{spanId:-}]'
    console: '%d{yyyy-MM-dd HH:mm:ss.SSS} %5p ${logging.pattern.correlation} --- [%15.15t] %-40.40logger{39} : %m%n'
```

#### 2.3 Tracing Configuration Class
Create `src/main/java/com/cekinmezyucel/springboot/poc/config/TracingConfig.java`:
```java
package com.cekinmezyucel.springboot.poc.config;

import io.micrometer.observation.ObservationRegistry;
import io.micrometer.observation.aop.ObservedAspect;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TracingConfig {

    @Bean
    public ObservedAspect observedAspect(ObservationRegistry observationRegistry) {
        return new ObservedAspect(observationRegistry);
    }
}
```

### Step 3: Structured Logging

#### 3.1 Logging Configuration
Create `src/main/resources/logback-spring.xml`:
```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    <springProfile name="!local">
        <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
            <encoder class="net.logstash.logback.encoder.LoggingEventCompositeJsonEncoder">
                <providers>
                    <timestamp/>
                    <logLevel/>
                    <loggerName/>
                    <mdc/>
                    <message/>
                    <stackTrace/>
                    <pattern>
                        <pattern>
                            {
                                "traceId": "%X{traceId:-}",
                                "spanId": "%X{spanId:-}",
                                "service": "springboot-poc"
                            }
                        </pattern>
                    </pattern>
                </providers>
            </encoder>
        </appender>
    </springProfile>
    
    <springProfile name="local">
        <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
            <encoder>
                <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} %5p [%X{traceId:-},%X{spanId:-}] --- [%15.15t] %-40.40logger{39} : %m%n</pattern>
            </encoder>
        </appender>
    </springProfile>
    
    <root level="INFO">
        <appender-ref ref="CONSOLE"/>
    </root>
    
    <logger name="com.cekinmezyucel.springboot.poc" level="DEBUG"/>
    <logger name="org.springframework.security" level="WARN"/>
    <logger name="org.springframework.web" level="INFO"/>
</configuration>
```

#### 3.2 Add Logging Dependencies
```gradle
// Add to build.gradle dependencies
implementation 'net.logstash.logback:logstash-logback-encoder:7.4'
```

### Step 4: Application Performance Monitoring

#### 4.1 Custom Metrics in Services
Update `src/main/java/com/cekinmezyucel/springboot/poc/service/UserService.java`:
```java
package com.cekinmezyucel.springboot.poc.service;

import io.micrometer.core.annotation.Counted;
import io.micrometer.core.annotation.Timed;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

@Service
@Observed(name = "user.service")
public class UserService {
    // existing code...

    @Timed(name = "user.service.getUsers", description = "Time taken to get all users")
    @Counted(name = "user.service.getUsers.count", description = "Number of times getUsers is called")
    public List<User> getUsers() {
        // existing implementation
    }

    @Timed(name = "user.service.createUser", description = "Time taken to create user")
    @Counted(name = "user.service.createUser.count", description = "Number of times createUser is called")
    public User createUser(User user) {
        // existing implementation
    }
}
```

#### 4.2 Database Metrics
Update `application.yaml`:
```yaml
spring:
  datasource:
    hikari:
      register-mbeans: true
  jpa:
    properties:
      hibernate:
        generate_statistics: true
        session:
          events:
            log:
              LOG_QUERIES_SLOWER_THAN_MS: 100
```

### Step 5: Health Checks Enhancement

#### 5.1 Custom Health Indicators
Create `src/main/java/com/cekinmezyucel/springboot/poc/health/DatabaseHealthIndicator.java`:
```java
package com.cekinmezyucel.springboot.poc.health;

import org.springframework.boot.actuator.health.Health;
import org.springframework.boot.actuator.health.HealthIndicator;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class DatabaseHealthIndicator implements HealthIndicator {

    private final JdbcTemplate jdbcTemplate;

    public DatabaseHealthIndicator(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Health health() {
        try {
            Integer result = jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            if (result != null && result == 1) {
                return Health.up()
                    .withDetail("database", "PostgreSQL")
                    .withDetail("status", "Connected")
                    .build();
            }
        } catch (DataAccessException e) {
            return Health.down()
                .withDetail("database", "PostgreSQL")
                .withDetail("error", e.getMessage())
                .build();
        }
        return Health.down()
            .withDetail("database", "PostgreSQL")
            .withDetail("error", "Unknown error")
            .build();
    }
}
```

### Step 6: Docker Compose for Monitoring Stack

#### 6.1 Update docker-compose.yml
```yaml
version: "3.8"
services:
  postgres:
    # existing postgres config
    
  mock-oidc:
    # existing mock-oidc config
    
  prometheus:
    image: prom/prometheus:v2.45.0
    container_name: springboot-poc-prometheus
    ports:
      - "9090:9090"
    volumes:
      - ./monitoring/prometheus.yml:/etc/prometheus/prometheus.yml
    command:
      - '--config.file=/etc/prometheus/prometheus.yml'
      - '--storage.tsdb.path=/prometheus'
      - '--web.console.libraries=/etc/prometheus/console_libraries'
      - '--web.console.templates=/etc/prometheus/consoles'
      - '--web.enable-lifecycle'

  grafana:
    image: grafana/grafana:10.0.0
    container_name: springboot-poc-grafana
    ports:
      - "3000:3000"
    environment:
      - GF_SECURITY_ADMIN_PASSWORD=admin
    volumes:
      - grafana-data:/var/lib/grafana
      - ./monitoring/grafana/dashboards:/etc/grafana/provisioning/dashboards
      - ./monitoring/grafana/datasources:/etc/grafana/provisioning/datasources

  zipkin:
    image: openzipkin/zipkin:2.24
    container_name: springboot-poc-zipkin
    ports:
      - "9411:9411"

volumes:
  pgdata:
  grafana-data:
```

#### 6.2 Create Monitoring Configuration
Create `monitoring/prometheus.yml`:
```yaml
global:
  scrape_interval: 15s

scrape_configs:
  - job_name: 'springboot-poc'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['host.docker.internal:8080']
```

### Step 7: Testing the Implementation

#### 7.1 Verification Commands
```bash
# Start monitoring stack
docker-compose up -d

# Run application
./gradlew bootRun --args='--spring.profiles.active=local'

# Test metrics endpoint
curl http://localhost:8080/actuator/prometheus

# Test health with details
curl http://localhost:8080/actuator/health

# Generate some traffic for metrics
curl -H "Authorization: Bearer $(get_jwt_token)" http://localhost:8080/users
```

#### 7.2 Integration Test Updates
Create `src/test/java/com/cekinmezyucel/springboot/poc/monitoring/ObservabilityIntegrationTest.java`:
```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ObservabilityIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private MeterRegistry meterRegistry;

    @Test
    @DisplayName("Should expose Prometheus metrics endpoint")
    void shouldExposePrometheusMetrics() {
        ResponseEntity<String> response = restTemplate.getForEntity("/actuator/prometheus", String.class);
        
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("jvm_memory_used_bytes");
        assertThat(response.getBody()).contains("http_server_requests_seconds");
    }

    @Test
    @DisplayName("Should record custom metrics for user operations")
    void shouldRecordCustomMetrics() {
        Timer timer = meterRegistry.find("user.service.getUsers").timer();
        assertThat(timer).isNotNull();
    }
}
```

## Validation Checklist

### ✅ Metrics Collection
- [ ] Prometheus endpoint accessible at `/actuator/prometheus`
- [ ] Custom application metrics visible
- [ ] JVM metrics being collected
- [ ] HTTP request metrics with percentiles

### ✅ Distributed Tracing
- [ ] Trace IDs in logs
- [ ] Spans visible in Zipkin UI
- [ ] Database queries traced
- [ ] Cross-service tracing working

### ✅ Structured Logging
- [ ] JSON format in non-local profiles
- [ ] Correlation IDs in logs
- [ ] Log levels configurable via actuator
- [ ] Structured fields present

### ✅ Health Checks
- [ ] Enhanced health endpoint with details
- [ ] Database connectivity check
- [ ] Custom health indicators working
- [ ] Kubernetes readiness/liveness ready

### ✅ Monitoring Stack
- [ ] Prometheus scraping metrics
- [ ] Grafana dashboards accessible
- [ ] Zipkin collecting traces
- [ ] Alerts configured (future enhancement)

## Troubleshooting

### Common Issues
1. **Metrics not appearing**: Check actuator endpoint exposure in application.yaml
2. **Tracing not working**: Verify Zipkin endpoint configuration and network connectivity
3. **JSON logging not formatted**: Check logback-spring.xml profile configuration
4. **Health checks failing**: Verify database connectivity and custom indicators

### Performance Considerations
- Set appropriate sampling rates for tracing in production (0.1 instead of 1.0)
- Configure metric retention policies
- Monitor dashboard query performance
- Set up log rotation and retention

## Next Steps
After implementing observability:
1. Set up alerting rules in Prometheus
2. Create Grafana dashboards for business metrics
3. Implement log aggregation (ELK/EFK stack)
4. Add performance monitoring and profiling
5. Set up automated anomaly detection

## AI Agent Notes
- Always verify current configuration before making changes
- Test metrics collection after each step
- Ensure backward compatibility with existing health checks
- Update integration tests to verify observability features
- Document any custom metrics or dashboards created