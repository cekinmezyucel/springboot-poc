# Security Hardening Enhancement Plan

## Current State Analysis
- ✅ OAuth2/JWT resource server configuration
- ✅ Method-level security with @RolesAllowed
- ✅ Basic global exception handler
- ❌ No input validation at API level
- ❌ No rate limiting or throttling
- ❌ Missing security headers
- ❌ No audit logging for security events
- ❌ No CORS configuration
- ❌ No secret management strategy

## Target State
- ✅ Comprehensive input validation
- ✅ Rate limiting and API throttling
- ✅ Security headers (OWASP recommendations)
- ✅ Security audit logging
- ✅ Proper CORS configuration
- ✅ Secret management integration
- ✅ API request/response validation
- ✅ Security scanning integration

## Implementation Steps

### Step 1: Input Validation & API Security

#### 1.1 Add Validation Dependencies
```gradle
// Add to build.gradle dependencies
implementation 'org.springframework.boot:spring-boot-starter-validation'
implementation 'org.hibernate.validator:hibernate-validator'
```

#### 1.2 Enhanced OpenAPI with Validation
Update `src/main/resources/openapi.yaml`:
```yaml
components:
  schemas:
    User:
      type: object
      required:
        - email
        - name
        - surname
      properties:
        id:
          type: integer
          format: int64
          readOnly: true
        email:
          type: string
          format: email
          maxLength: 255
          pattern: '^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$'
          example: "user@example.com"
        name:
          type: string
          minLength: 1
          maxLength: 100
          pattern: '^[a-zA-Z\s\-\.]+$'
          example: "John"
        surname:
          type: string
          minLength: 1
          maxLength: 100
          pattern: '^[a-zA-Z\s\-\.]+$'
          example: "Doe"
        accountIds:
          type: array
          items:
            type: integer
            format: int64
          maxItems: 50
    
    Account:
      type: object
      required:
        - name
      properties:
        id:
          type: integer
          format: int64
          readOnly: true
        name:
          type: string
          minLength: 1
          maxLength: 200
          pattern: '^[a-zA-Z0-9\s\-\.\&]+$'
          example: "Acme Corporation"
        industry:
          type: string
          enum: [
            "TECHNOLOGY",
            "FINANCE",
            "HEALTHCARE",
            "MANUFACTURING",
            "RETAIL",
            "EDUCATION",
            "OTHER"
          ]
          example: "TECHNOLOGY"
        userIds:
          type: array
          items:
            type: integer
            format: int64
          maxItems: 1000

  securitySchemes:
    bearerAuth:
      type: http
      scheme: bearer
      bearerFormat: JWT

security:
  - bearerAuth: []
```

#### 1.3 Validation Annotations in Models
After regenerating OpenAPI classes, create validation configuration:
```java
// Create src/main/java/com/cekinmezyucel/springboot/poc/config/ValidationConfig.java
package com.cekinmezyucel.springboot.poc.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.validation.beanvalidation.MethodValidationPostProcessor;

@Configuration
public class ValidationConfig {

    @Bean
    public LocalValidatorFactoryBean validator() {
        return new LocalValidatorFactoryBean();
    }

    @Bean
    public MethodValidationPostProcessor methodValidationPostProcessor() {
        MethodValidationPostProcessor processor = new MethodValidationPostProcessor();
        processor.setValidator(validator());
        return processor;
    }
}
```

#### 1.4 Enhanced Global Exception Handler
Update `src/main/java/com/cekinmezyucel/springboot/poc/exception/GlobalExceptionHandler.java`:
```java
package com.cekinmezyucel.springboot.poc.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@ControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private static final Logger securityLog = LoggerFactory.getLogger("SECURITY");

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<Map<String, Object>> handleValidationException(
            MethodArgumentNotValidException ex, HttpServletRequest request) {
        
        logSecurityEvent("VALIDATION_FAILURE", request, ex.getMessage());
        
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error -> 
            errors.put(error.getField(), error.getDefaultMessage())
        );
        
        Map<String, Object> response = createErrorResponse(
            "VALIDATION_ERROR", 
            "Input validation failed", 
            errors,
            HttpStatus.BAD_REQUEST
        );
        
        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<Map<String, Object>> handleConstraintViolation(
            ConstraintViolationException ex, HttpServletRequest request) {
        
        logSecurityEvent("CONSTRAINT_VIOLATION", request, ex.getMessage());
        
        Map<String, String> errors = ex.getConstraintViolations().stream()
            .collect(Collectors.toMap(
                violation -> violation.getPropertyPath().toString(),
                ConstraintViolation::getMessage
            ));
        
        Map<String, Object> response = createErrorResponse(
            "CONSTRAINT_VIOLATION", 
            "Constraint validation failed", 
            errors,
            HttpStatus.BAD_REQUEST
        );
        
        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ResponseEntity<Map<String, Object>> handleAccessDenied(
            AccessDeniedException ex, HttpServletRequest request) {
        
        logSecurityEvent("ACCESS_DENIED", request, ex.getMessage());
        
        Map<String, Object> response = createErrorResponse(
            "ACCESS_DENIED", 
            "Access denied", 
            null,
            HttpStatus.FORBIDDEN
        );
        
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }

    // Keep existing exception handlers...

    private Map<String, Object> createErrorResponse(String code, String message, 
            Object details, HttpStatus status) {
        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", Instant.now().toString());
        response.put("status", status.value());
        response.put("error", status.getReasonPhrase());
        response.put("code", code);
        response.put("message", message);
        response.put("path", MDC.get("request.uri"));
        response.put("traceId", MDC.get("traceId"));
        
        if (details != null) {
            response.put("details", details);
        }
        
        return response;
    }

    private void logSecurityEvent(String eventType, HttpServletRequest request, String details) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String user = auth != null ? auth.getName() : "anonymous";
        String userAgent = request.getHeader("User-Agent");
        String clientIp = getClientIpAddress(request);
        
        securityLog.warn("Security Event: {} | User: {} | IP: {} | URI: {} | UserAgent: {} | Details: {}", 
            eventType, user, clientIp, request.getRequestURI(), userAgent, details);
    }

    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        return request.getRemoteAddr();
    }
}
```

### Step 2: Rate Limiting & Throttling

#### 2.1 Add Rate Limiting Dependencies
```gradle
// Add to build.gradle dependencies
implementation 'com.github.vladimir-bukhtoyarov:bucket4j-core:7.6.0'
implementation 'com.github.vladimir-bukhtoyarov:bucket4j-redis:7.6.0'
implementation 'org.springframework.boot:spring-boot-starter-data-redis'
```

#### 2.2 Rate Limiting Configuration
Create `src/main/java/com/cekinmezyucel/springboot/poc/config/RateLimitingConfig.java`:
```java
package com.cekinmezyucel.springboot.poc.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

@Configuration
public class RateLimitingConfig {

    @Value("${app.rate-limiting.redis.uri:redis://localhost:6379}")
    private String redisUri;

    private final ConcurrentHashMap<String, Bucket> cache = new ConcurrentHashMap<>();

    @Bean
    public LettuceBasedProxyManager<String> proxyManager() {
        RedisClient redisClient = RedisClient.create(RedisURI.create(redisUri));
        return LettuceBasedProxyManager.builderFor(redisClient)
            .withExpirationStrategy(Duration.ofMinutes(10))
            .build();
    }

    public Bucket createBucket(String key, RateLimitType type) {
        return cache.computeIfAbsent(key, k -> {
            BucketConfiguration configuration = BucketConfiguration.builder()
                .addLimit(getBandwidth(type))
                .build();
            
            return proxyManager().builder().build(k, configuration);
        });
    }

    private Bandwidth getBandwidth(RateLimitType type) {
        return switch (type) {
            case USER_API -> Bandwidth.simple(100, Duration.ofMinutes(1));
            case ACCOUNT_API -> Bandwidth.simple(100, Duration.ofMinutes(1));
            case GLOBAL -> Bandwidth.simple(1000, Duration.ofMinutes(1));
        };
    }

    public enum RateLimitType {
        USER_API, ACCOUNT_API, GLOBAL
    }
}
```

#### 2.3 Rate Limiting Interceptor
Create `src/main/java/com/cekinmezyucel/springboot/poc/security/RateLimitingInterceptor.java`:
```java
package com.cekinmezyucel.springboot.poc.security;

import com.cekinmezyucel.springboot.poc.config.RateLimitingConfig;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class RateLimitingInterceptor implements HandlerInterceptor {
    
    private static final Logger log = LoggerFactory.getLogger(RateLimitingInterceptor.class);
    private static final Logger securityLog = LoggerFactory.getLogger("SECURITY");
    
    private final RateLimitingConfig rateLimitingConfig;
    
    public RateLimitingInterceptor(RateLimitingConfig rateLimitingConfig) {
        this.rateLimitingConfig = rateLimitingConfig;
    }
    
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, 
            Object handler) throws Exception {
        
        // Skip rate limiting for health endpoint
        if (request.getRequestURI().startsWith("/actuator/health")) {
            return true;
        }
        
        String key = getUserKey(request);
        RateLimitingConfig.RateLimitType type = getRateLimitType(request);
        
        Bucket bucket = rateLimitingConfig.createBucket(key, type);
        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);
        
        if (probe.isConsumed()) {
            response.addHeader("X-Rate-Limit-Remaining", String.valueOf(probe.getRemainingTokens()));
            return true;
        } else {
            securityLog.warn("Rate limit exceeded for user: {} | IP: {} | URI: {}", 
                getCurrentUser(), getClientIp(request), request.getRequestURI());
            
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.addHeader("X-Rate-Limit-Retry-After-Seconds", 
                String.valueOf(probe.getNanosToWaitForRefill() / 1_000_000_000));
            response.getWriter().write("{\"error\":\"Rate limit exceeded\",\"retryAfter\":" 
                + (probe.getNanosToWaitForRefill() / 1_000_000_000) + "}");
            return false;
        }
    }
    
    private String getUserKey(HttpServletRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName())) {
            return "user:" + auth.getName();
        }
        return "ip:" + getClientIp(request);
    }
    
    private RateLimitingConfig.RateLimitType getRateLimitType(HttpServletRequest request) {
        String uri = request.getRequestURI();
        if (uri.startsWith("/users")) {
            return RateLimitingConfig.RateLimitType.USER_API;
        } else if (uri.startsWith("/accounts")) {
            return RateLimitingConfig.RateLimitType.ACCOUNT_API;
        }
        return RateLimitingConfig.RateLimitType.GLOBAL;
    }
    
    private String getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null ? auth.getName() : "anonymous";
    }
    
    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
```

### Step 3: Security Headers & CORS

#### 3.1 Enhanced Security Configuration
Update `src/main/java/com/cekinmezyucel/springboot/poc/SecurityConfig.java`:
```java
package com.cekinmezyucel.springboot.poc;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

import static com.cekinmezyucel.springboot.poc.util.ApplicationConstants.AUTHORITIES_CLAIM_NAME;
import static com.cekinmezyucel.springboot.poc.util.ApplicationConstants.AUTHORITY_PREFIX;

@Configuration
@EnableMethodSecurity(jsr250Enabled = true)
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .headers(headers -> headers
                .frameOptions().deny()
                .contentTypeOptions().and()
                .httpStrictTransportSecurity(hstsConfig -> hstsConfig
                    .maxAgeInSeconds(31536000)
                    .includeSubdomains(true)
                    .preload(true))
                .referrerPolicy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN)
                .permissionsPolicy(policy -> policy
                    .policy("camera", "none")
                    .policy("microphone", "none")
                    .policy("geolocation", "none")))
            .authorizeHttpRequests(authz -> authz
                .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                .requestMatchers("/actuator/**").authenticated()
                .anyRequest().authenticated())
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())));
        
        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(List.of(
            "http://localhost:*",
            "https://*.yourdomain.com"
        ));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);
        configuration.setExposedHeaders(Arrays.asList(
            "X-Rate-Limit-Remaining", 
            "X-Rate-Limit-Retry-After-Seconds"
        ));
        configuration.setMaxAge(3600L);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter grantedAuthoritiesConverter = new JwtGrantedAuthoritiesConverter();
        grantedAuthoritiesConverter.setAuthorityPrefix(AUTHORITY_PREFIX);
        grantedAuthoritiesConverter.setAuthoritiesClaimName(AUTHORITIES_CLAIM_NAME);
        
        JwtAuthenticationConverter jwtAuthenticationConverter = new JwtAuthenticationConverter();
        jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(grantedAuthoritiesConverter);
        return jwtAuthenticationConverter;
    }
}
```

### Step 4: Audit Logging

#### 4.1 Audit Event Configuration
Create `src/main/java/com/cekinmezyucel/springboot/poc/audit/AuditEventConfig.java`:
```java
package com.cekinmezyucel.springboot.poc.audit;

import org.springframework.boot.actuate.audit.AuditEventRepository;
import org.springframework.boot.actuate.audit.InMemoryAuditEventRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AuditEventConfig {

    @Bean
    public AuditEventRepository auditEventRepository() {
        return new InMemoryAuditEventRepository(1000);
    }
}
```

#### 4.2 Security Audit Listener
Create `src/main/java/com/cekinmezyucel/springboot/poc/audit/SecurityAuditListener.java`:
```java
package com.cekinmezyucel.springboot.poc.audit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.actuate.audit.AuditEvent;
import org.springframework.boot.actuate.audit.listener.AuditApplicationEvent;
import org.springframework.boot.actuate.security.AuthenticationAuditListener;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AbstractAuthenticationEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.authentication.event.AuthenticationFailureEvent;
import org.springframework.stereotype.Component;

@Component
public class SecurityAuditListener extends AuthenticationAuditListener {
    
    private static final Logger auditLog = LoggerFactory.getLogger("AUDIT");
    
    @EventListener
    public void onAuditEvent(AuditApplicationEvent event) {
        AuditEvent auditEvent = event.getAuditEvent();
        auditLog.info("Audit Event: {} | Principal: {} | Data: {}", 
            auditEvent.getType(), 
            auditEvent.getPrincipal(), 
            auditEvent.getData());
    }
    
    @Override
    public void onApplicationEvent(AbstractAuthenticationEvent event) {
        if (event instanceof AuthenticationSuccessEvent) {
            auditLog.info("Authentication Success: {} | Details: {}", 
                event.getAuthentication().getName(),
                event.getAuthentication().getDetails());
        } else if (event instanceof AuthenticationFailureEvent) {
            auditLog.warn("Authentication Failure: {} | Exception: {}", 
                ((AuthenticationFailureEvent) event).getAuthentication().getName(),
                ((AuthenticationFailureEvent) event).getException().getMessage());
        }
        super.onApplicationEvent(event);
    }
}
```

### Step 5: Configuration Updates

#### 5.1 Application Configuration
Add to `application.yaml`:
```yaml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: ${JWT_ISSUER_URI:https://auth.springboot-poc.com}
  
  data:
    redis:
      url: ${REDIS_URL:redis://localhost:6379}
      timeout: 2000ms
      lettuce:
        pool:
          max-active: 8
          max-idle: 8
          min-idle: 0

app:
  rate-limiting:
    redis:
      uri: ${REDIS_URL:redis://localhost:6379}
  cors:
    allowed-origins: ${CORS_ALLOWED_ORIGINS:http://localhost:*,https://*.yourdomain.com}
  security:
    audit:
      enabled: true

management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus,auditevents
  endpoint:
    auditevents:
      enabled: true

logging:
  level:
    AUDIT: INFO
    SECURITY: WARN
    org.springframework.security: WARN
```

### Step 6: Testing Security Enhancements

#### 6.1 Security Integration Tests
Create `src/test/java/com/cekinmezyucel/springboot/poc/security/SecurityHardeningIntegrationTest.java`:
```java
package com.cekinmezyucel.springboot.poc.security;

import com.cekinmezyucel.springboot.poc.BaseIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

class SecurityHardeningIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    @DisplayName("Should include security headers in responses")
    void shouldIncludeSecurityHeaders() {
        ResponseEntity<String> response = restTemplate.getForEntity("/actuator/health", String.class);
        
        HttpHeaders headers = response.getHeaders();
        assertThat(headers.get("X-Content-Type-Options")).contains("nosniff");
        assertThat(headers.get("X-Frame-Options")).contains("DENY");
        assertThat(headers.get("Referrer-Policy")).contains("strict-origin-when-cross-origin");
    }

    @Test
    @DisplayName("Should reject requests with invalid input")
    void shouldRejectInvalidInput() {
        String invalidUser = """
            {
                "email": "invalid-email",
                "name": "",
                "surname": "Test123!"
            }
            """;
        
        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "application/json");
        HttpEntity<String> request = new HttpEntity<>(invalidUser, headers);
        
        ResponseEntity<String> response = restTemplate.postForEntity("/users", request, String.class);
        
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).contains("VALIDATION_ERROR");
    }

    @Test
    @DisplayName("Should handle CORS preflight requests")
    void shouldHandleCorsPreflightRequests() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Origin", "http://localhost:3000");
        headers.set("Access-Control-Request-Method", "POST");
        headers.set("Access-Control-Request-Headers", "Authorization");
        
        HttpEntity<Void> request = new HttpEntity<>(headers);
        
        ResponseEntity<Void> response = restTemplate.exchange("/users", 
            org.springframework.http.HttpMethod.OPTIONS, request, Void.class);
        
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().get("Access-Control-Allow-Origin"))
            .contains("http://localhost:3000");
    }
}
```

## Validation Checklist

### ✅ Input Validation
- [ ] OpenAPI spec includes validation constraints
- [ ] API endpoints validate request payloads
- [ ] Proper error responses for validation failures
- [ ] Path parameter validation working

### ✅ Rate Limiting
- [ ] Rate limiting applies to authenticated users
- [ ] Different limits for different endpoints
- [ ] Proper HTTP 429 responses with retry headers
- [ ] Redis-based distributed rate limiting

### ✅ Security Headers
- [ ] HSTS header included
- [ ] X-Frame-Options set to DENY
- [ ] X-Content-Type-Options set to nosniff
- [ ] Referrer-Policy configured
- [ ] Permissions-Policy configured

### ✅ CORS Configuration
- [ ] Proper origin validation
- [ ] Preflight requests handled
- [ ] Credentials allowed where appropriate
- [ ] Security headers exposed

### ✅ Audit Logging
- [ ] Authentication events logged
- [ ] Security violations logged
- [ ] Audit events accessible via actuator
- [ ] Structured logging format

## Troubleshooting

### Common Issues
1. **Validation not working**: Verify @Valid annotations and OpenAPI regeneration
2. **Rate limiting not applying**: Check Redis connectivity and configuration
3. **CORS issues**: Verify origin patterns and allowed methods
4. **Security headers missing**: Check security configuration and profile settings

### Security Considerations
- Regularly update security dependencies
- Monitor for new security vulnerabilities
- Review and rotate secrets periodically
- Implement security scanning in CI/CD
- Set up security monitoring and alerting

## Next Steps
After implementing security hardening:
1. Set up Web Application Firewall (WAF)
2. Implement API key management
3. Add OAuth2 scope-based authorization
4. Set up intrusion detection
5. Implement automated security testing

## AI Agent Notes
- Always validate current security configuration before changes
- Test security features with both valid and invalid inputs
- Ensure rate limiting doesn't affect health checks
- Verify CORS configuration matches environment requirements
- Update security tests when adding new endpoints