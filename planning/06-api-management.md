# API Management Enhancement Plan

## Current State Analysis
- ✅ OpenAPI 3.0 specification with code generation
- ✅ Basic CRUD operations for Users and Accounts
- ❌ No API versioning strategy
- ❌ No comprehensive input validation
- ❌ No pagination standards
- ❌ No API documentation portal
- ❌ No API analytics/monitoring
- ❌ No response transformation standards

## Target State
- ✅ Comprehensive API versioning strategy
- ✅ Standardized pagination and sorting
- ✅ Advanced input validation and transformation
- ✅ Interactive API documentation portal
- ✅ API analytics and usage monitoring
- ✅ Response caching and transformation
- ✅ API deprecation management
- ✅ Client SDK generation

## Implementation Steps

### Step 1: API Versioning Strategy

#### 1.1 Versioning Configuration
Create `src/main/java/com/cekinmezyucel/springboot/poc/config/ApiVersioningConfig.java`:
```java
package com.cekinmezyucel.springboot.poc.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class ApiVersioningConfig implements WebMvcConfigurer {

    @Override
    public void configurePathMatch(PathMatchConfigurer configurer) {
        configurer.addPathPrefix("/api/v1", c -> c.isAnnotationPresent(RestControllerV1.class));
        configurer.addPathPrefix("/api/v2", c -> c.isAnnotationPresent(RestControllerV2.class));
    }
}
```

#### 1.2 Version Annotations
Create `src/main/java/com/cekinmezyucel/springboot/poc/api/version/RestControllerV1.java`:
```java
package com.cekinmezyucel.springboot.poc.api.version;

import org.springframework.web.bind.annotation.RestController;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@RestController
public @interface RestControllerV1 {
}
```

Create `src/main/java/com/cekinmezyucel/springboot/poc/api/version/RestControllerV2.java`:
```java
package com.cekinmezyucel.springboot.poc.api.version;

import org.springframework.web.bind.annotation.RestController;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@RestController
public @interface RestControllerV2 {
}
```

#### 1.3 Enhanced OpenAPI Specification
Update `src/main/resources/openapi.yaml`:
```yaml
openapi: 3.0.1
info:
  title: SpringBoot POC API
  description: |
    Enterprise-grade API for User and Account management with comprehensive features:
    - JWT-based authentication
    - Role-based authorization
    - Pagination and sorting
    - Input validation
    - Rate limiting
    - Audit logging
  version: 2.0.0
  contact:
    name: API Support
    email: api-support@springboot-poc.com
  license:
    name: MIT
    url: https://opensource.org/licenses/MIT

servers:
  - url: /api/v2
    description: Production API (v2)
  - url: /api/v1
    description: Legacy API (v1) - Deprecated

paths:
  /health:
    get:
      summary: Get application health status
      operationId: getHealth
      tags:
        - Health
      responses:
        "200":
          description: Application is healthy
          content:
            application/json:
              schema:
                $ref: "#/components/schemas/HealthResponse"

  /users:
    get:
      summary: Get users with pagination and filtering
      operationId: getUsers
      tags:
        - Users
      parameters:
        - $ref: "#/components/parameters/PageParam"
        - $ref: "#/components/parameters/SizeParam"
        - $ref: "#/components/parameters/SortParam"
        - name: search
          in: query
          description: Search term for name, surname, or email
          schema:
            type: string
            maxLength: 100
        - name: accountId
          in: query
          description: Filter users by account ID
          schema:
            type: integer
            format: int64
      responses:
        "200":
          description: Paginated list of users
          content:
            application/json:
              schema:
                $ref: "#/components/schemas/PagedUsers"
        "400":
          $ref: "#/components/responses/BadRequest"
        "401":
          $ref: "#/components/responses/Unauthorized"
        "403":
          $ref: "#/components/responses/Forbidden"

    post:
      summary: Create a new user
      operationId: createUser
      tags:
        - Users
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: "#/components/schemas/CreateUserRequest"
      responses:
        "201":
          description: User created successfully
          content:
            application/json:
              schema:
                $ref: "#/components/schemas/UserResponse"
        "400":
          $ref: "#/components/responses/BadRequest"
        "409":
          $ref: "#/components/responses/Conflict"

  /users/{userId}:
    get:
      summary: Get user by ID
      operationId: getUserById
      tags:
        - Users
      parameters:
        - $ref: "#/components/parameters/UserIdParam"
      responses:
        "200":
          description: User details
          content:
            application/json:
              schema:
                $ref: "#/components/schemas/UserResponse"
        "404":
          $ref: "#/components/responses/NotFound"

    put:
      summary: Update user
      operationId: updateUser
      tags:
        - Users
      parameters:
        - $ref: "#/components/parameters/UserIdParam"
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: "#/components/schemas/UpdateUserRequest"
      responses:
        "200":
          description: User updated successfully
          content:
            application/json:
              schema:
                $ref: "#/components/schemas/UserResponse"
        "409":
          $ref: "#/components/responses/OptimisticLockError"

    delete:
      summary: Soft delete user
      operationId: deleteUser
      tags:
        - Users
      parameters:
        - $ref: "#/components/parameters/UserIdParam"
      responses:
        "204":
          description: User deleted successfully
        "404":
          $ref: "#/components/responses/NotFound"

components:
  parameters:
    PageParam:
      name: page
      in: query
      description: Page number (0-based)
      schema:
        type: integer
        minimum: 0
        default: 0
    SizeParam:
      name: size
      in: query
      description: Number of items per page
      schema:
        type: integer
        minimum: 1
        maximum: 100
        default: 20
    SortParam:
      name: sort
      in: query
      description: |
        Sort criteria. Format: field,direction
        Example: name,asc or createdAt,desc
        Multiple sort criteria supported: sort=name,asc&sort=createdAt,desc
      schema:
        type: array
        items:
          type: string
          pattern: '^[a-zA-Z][a-zA-Z0-9]*,(asc|desc)$'
      style: form
      explode: true
    UserIdParam:
      name: userId
      in: path
      required: true
      description: User ID
      schema:
        type: integer
        format: int64
        minimum: 1

  schemas:
    HealthResponse:
      type: object
      properties:
        status:
          type: string
          enum: [UP, DOWN, OUT_OF_SERVICE, UNKNOWN]
        timestamp:
          type: string
          format: date-time
        components:
          type: object
          additionalProperties:
            type: object

    CreateUserRequest:
      type: object
      required:
        - email
        - name
        - surname
      properties:
        email:
          type: string
          format: email
          maxLength: 255
          example: "john.doe@example.com"
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

    UpdateUserRequest:
      type: object
      required:
        - version
      properties:
        email:
          type: string
          format: email
          maxLength: 255
        name:
          type: string
          minLength: 1
          maxLength: 100
          pattern: '^[a-zA-Z\s\-\.]+$'
        surname:
          type: string
          minLength: 1
          maxLength: 100
          pattern: '^[a-zA-Z\s\-\.]+$'
        version:
          type: integer
          format: int64
          description: Version for optimistic locking

    UserResponse:
      type: object
      properties:
        id:
          type: integer
          format: int64
          readOnly: true
        email:
          type: string
          format: email
        name:
          type: string
        surname:
          type: string
        fullName:
          type: string
          readOnly: true
        accountIds:
          type: array
          items:
            type: integer
            format: int64
        version:
          type: integer
          format: int64
          readOnly: true
        createdAt:
          type: string
          format: date-time
          readOnly: true
        updatedAt:
          type: string
          format: date-time
          readOnly: true
        _links:
          $ref: "#/components/schemas/Links"

    PagedUsers:
      type: object
      properties:
        content:
          type: array
          items:
            $ref: "#/components/schemas/UserResponse"
        page:
          $ref: "#/components/schemas/PageMetadata"
        _links:
          $ref: "#/components/schemas/PageLinks"

    PageMetadata:
      type: object
      properties:
        size:
          type: integer
          description: Number of items per page
        number:
          type: integer
          description: Current page number (0-based)
        totalElements:
          type: integer
          format: int64
          description: Total number of items
        totalPages:
          type: integer
          description: Total number of pages
        first:
          type: boolean
          description: Whether this is the first page
        last:
          type: boolean
          description: Whether this is the last page

    Links:
      type: object
      properties:
        self:
          $ref: "#/components/schemas/Link"
        accounts:
          $ref: "#/components/schemas/Link"

    PageLinks:
      type: object
      properties:
        self:
          $ref: "#/components/schemas/Link"
        first:
          $ref: "#/components/schemas/Link"
        prev:
          $ref: "#/components/schemas/Link"
        next:
          $ref: "#/components/schemas/Link"
        last:
          $ref: "#/components/schemas/Link"

    Link:
      type: object
      properties:
        href:
          type: string
          format: uri

    ErrorResponse:
      type: object
      properties:
        timestamp:
          type: string
          format: date-time
        status:
          type: integer
        error:
          type: string
        code:
          type: string
        message:
          type: string
        path:
          type: string
        traceId:
          type: string
        details:
          type: object

  responses:
    BadRequest:
      description: Bad request - Invalid input
      content:
        application/json:
          schema:
            $ref: "#/components/schemas/ErrorResponse"
    Unauthorized:
      description: Unauthorized - Authentication required
      content:
        application/json:
          schema:
            $ref: "#/components/schemas/ErrorResponse"
    Forbidden:
      description: Forbidden - Insufficient permissions
      content:
        application/json:
          schema:
            $ref: "#/components/schemas/ErrorResponse"
    NotFound:
      description: Resource not found
      content:
        application/json:
          schema:
            $ref: "#/components/schemas/ErrorResponse"
    Conflict:
      description: Conflict - Resource already exists
      content:
        application/json:
          schema:
            $ref: "#/components/schemas/ErrorResponse"
    OptimisticLockError:
      description: Optimistic locking failure - Resource was modified
      content:
        application/json:
          schema:
            $ref: "#/components/schemas/ErrorResponse"

  securitySchemes:
    bearerAuth:
      type: http
      scheme: bearer
      bearerFormat: JWT

security:
  - bearerAuth: []

tags:
  - name: Health
    description: Application health and status
  - name: Users
    description: User management operations
  - name: Accounts
    description: Account management operations
```

### Step 2: Enhanced API Implementation

#### 2.1 Pagination and Sorting Support
Create `src/main/java/com/cekinmezyucel/springboot/poc/api/dto/PagedResponse.java`:
```java
package com.cekinmezyucel.springboot.poc.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.Map;

public class PagedResponse<T> {
    
    private List<T> content;
    private PageMetadata page;
    
    @JsonProperty("_links")
    private Map<String, Link> links;
    
    public PagedResponse(Page<T> page, String baseUrl) {
        this.content = page.getContent();
        this.page = new PageMetadata(page);
        this.links = createLinks(page, baseUrl);
    }
    
    private Map<String, Link> createLinks(Page<T> page, String baseUrl) {
        Map<String, Link> links = new HashMap<>();
        
        // Self link
        links.put("self", new Link(createPageUrl(baseUrl, page.getNumber(), page.getSize())));
        
        // First page link
        if (!page.isFirst()) {
            links.put("first", new Link(createPageUrl(baseUrl, 0, page.getSize())));
        }
        
        // Previous page link
        if (page.hasPrevious()) {
            links.put("prev", new Link(createPageUrl(baseUrl, page.getNumber() - 1, page.getSize())));
        }
        
        // Next page link
        if (page.hasNext()) {
            links.put("next", new Link(createPageUrl(baseUrl, page.getNumber() + 1, page.getSize())));
        }
        
        // Last page link
        if (!page.isLast() && page.getTotalPages() > 0) {
            links.put("last", new Link(createPageUrl(baseUrl, page.getTotalPages() - 1, page.getSize())));
        }
        
        return links;
    }
    
    private String createPageUrl(String baseUrl, int page, int size) {
        return String.format("%s?page=%d&size=%d", baseUrl, page, size);
    }
    
    // Getters and setters
    public List<T> getContent() { return content; }
    public void setContent(List<T> content) { this.content = content; }
    
    public PageMetadata getPage() { return page; }
    public void setPage(PageMetadata page) { this.page = page; }
    
    public Map<String, Link> getLinks() { return links; }
    public void setLinks(Map<String, Link> links) { this.links = links; }
    
    public static class PageMetadata {
        private int size;
        private int number;
        private long totalElements;
        private int totalPages;
        private boolean first;
        private boolean last;
        
        public PageMetadata(Page<?> page) {
            this.size = page.getSize();
            this.number = page.getNumber();
            this.totalElements = page.getTotalElements();
            this.totalPages = page.getTotalPages();
            this.first = page.isFirst();
            this.last = page.isLast();
        }
        
        // Getters and setters
        public int getSize() { return size; }
        public int getNumber() { return number; }
        public long getTotalElements() { return totalElements; }
        public int getTotalPages() { return totalPages; }
        public boolean isFirst() { return first; }
        public boolean isLast() { return last; }
    }
    
    public static class Link {
        private String href;
        
        public Link(String href) {
            this.href = href;
        }
        
        public String getHref() { return href; }
        public void setHref(String href) { this.href = href; }
    }
}
```

#### 2.2 Enhanced API Controller
Create `src/main/java/com/cekinmezyucel/springboot/poc/api/v2/UsersApiV2Impl.java`:
```java
package com.cekinmezyucel.springboot.poc.api.v2;

import com.cekinmezyucel.springboot.poc.api.dto.PagedResponse;
import com.cekinmezyucel.springboot.poc.api.version.RestControllerV2;
import com.cekinmezyucel.springboot.poc.model.User;
import com.cekinmezyucel.springboot.poc.service.UserService;
import io.micrometer.core.annotation.Timed;
import jakarta.annotation.security.RolesAllowed;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestControllerV2
@RequestMapping("/users")
@Validated
public class UsersApiV2Impl {

    private final UserService userService;

    public UsersApiV2Impl(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    @RolesAllowed("poc.users.read")
    @Timed(name = "api.users.list", description = "Time taken to list users")
    public ResponseEntity<PagedResponse<User>> getUsers(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) int size,
            @RequestParam(required = false) List<String> sort,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long accountId,
            HttpServletRequest request) {

        // Validate page size
        if (size > 100) {
            size = 100;
        }

        // Build sort criteria
        Sort sortCriteria = buildSort(sort);
        Pageable pageable = PageRequest.of(page, size, sortCriteria);

        // Get users with filters
        Page<User> userPage;
        if (search != null && !search.trim().isEmpty()) {
            userPage = userService.searchUsers(search.trim(), pageable);
        } else if (accountId != null) {
            userPage = userService.getUsersByAccountId(accountId, pageable);
        } else {
            userPage = userService.getUsers(pageable);
        }

        // Create paged response with HATEOAS links
        String baseUrl = request.getRequestURL().toString();
        PagedResponse<User> response = new PagedResponse<>(userPage, baseUrl);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{userId}")
    @RolesAllowed("poc.users.read")
    @Timed(name = "api.users.get", description = "Time taken to get user by ID")
    public ResponseEntity<User> getUserById(@PathVariable @Min(1) Long userId) {
        Optional<User> user = userService.getUserById(userId);
        return user.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @RolesAllowed("poc.users.write")
    @Timed(name = "api.users.create", description = "Time taken to create user")
    public ResponseEntity<User> createUser(@Valid @RequestBody User user) {
        User createdUser = userService.createUser(user);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdUser);
    }

    @PutMapping("/{userId}")
    @RolesAllowed("poc.users.write")
    @Timed(name = "api.users.update", description = "Time taken to update user")
    public ResponseEntity<User> updateUser(
            @PathVariable @Min(1) Long userId,
            @Valid @RequestBody User user) {
        
        user.setId(userId);
        User updatedUser = userService.updateUser(user);
        return ResponseEntity.ok(updatedUser);
    }

    @DeleteMapping("/{userId}")
    @RolesAllowed("poc.users.delete")
    @Timed(name = "api.users.delete", description = "Time taken to delete user")
    public ResponseEntity<Void> deleteUser(@PathVariable @Min(1) Long userId) {
        userService.deleteUser(userId);
        return ResponseEntity.noContent().build();
    }

    private Sort buildSort(List<String> sortParams) {
        if (sortParams == null || sortParams.isEmpty()) {
            return Sort.by(Sort.Direction.ASC, "id");
        }

        List<Sort.Order> orders = sortParams.stream()
            .map(this::parseSort)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .toList();

        return orders.isEmpty() ? Sort.unsorted() : Sort.by(orders);
    }

    private Optional<Sort.Order> parseSort(String sortParam) {
        if (sortParam == null || !sortParam.contains(",")) {
            return Optional.empty();
        }

        String[] parts = sortParam.split(",");
        if (parts.length != 2) {
            return Optional.empty();
        }

        String property = parts[0].trim();
        String direction = parts[1].trim();

        // Validate property names (whitelist approach)
        if (!isValidSortProperty(property)) {
            return Optional.empty();
        }

        Sort.Direction sortDirection = "desc".equalsIgnoreCase(direction) 
            ? Sort.Direction.DESC 
            : Sort.Direction.ASC;

        return Optional.of(new Sort.Order(sortDirection, property));
    }

    private boolean isValidSortProperty(String property) {
        return List.of("id", "email", "name", "surname", "createdAt", "updatedAt")
            .contains(property);
    }
}
```

### Step 3: API Documentation Portal

#### 3.1 SpringDoc OpenAPI Configuration
Add dependency to `build.gradle`:
```gradle
implementation 'org.springdoc:springdoc-openapi-starter-webmvc-ui:2.2.0'
implementation 'org.springdoc:springdoc-openapi-starter-webmvc-api:2.2.0'
```

#### 3.2 Documentation Configuration
Create `src/main/java/com/cekinmezyucel/springboot/poc/config/OpenApiConfig.java`:
```java
package com.cekinmezyucel.springboot.poc.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Value("${app.api.version:2.0.0}")
    private String apiVersion;

    @Value("${app.api.title:SpringBoot POC API}")
    private String apiTitle;

    @Value("${server.servlet.context-path:}")
    private String contextPath;

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title(apiTitle)
                .version(apiVersion)
                .description("""
                    Enterprise-grade API for User and Account management.
                    
                    ## Features
                    - JWT-based authentication and authorization
                    - Comprehensive input validation
                    - Pagination and sorting
                    - Rate limiting and throttling
                    - Audit logging and data tracking
                    - RESTful design with HATEOAS
                    
                    ## Authentication
                    All endpoints (except health checks) require JWT authentication.
                    Include the JWT token in the Authorization header: `Authorization: Bearer <token>`
                    
                    ## Versioning
                    - v1: Legacy API (deprecated)
                    - v2: Current API with enhanced features
                    
                    ## Rate Limiting
                    API calls are rate limited per user/IP address.
                    Check response headers for rate limit information.
                    """)
                .contact(new Contact()
                    .name("API Support Team")
                    .email("api-support@springboot-poc.com")
                    .url("https://springboot-poc.com/support"))
                .license(new License()
                    .name("MIT License")
                    .url("https://opensource.org/licenses/MIT")))
            .servers(List.of(
                new Server().url(contextPath + "/api/v2").description("Current API (v2)"),
                new Server().url(contextPath + "/api/v1").description("Legacy API (v1)")
            ))
            .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
            .components(new io.swagger.v3.oas.models.Components()
                .addSecuritySchemes("bearerAuth", new SecurityScheme()
                    .type(SecurityScheme.Type.HTTP)
                    .scheme("bearer")
                    .bearerFormat("JWT")
                    .description("JWT token obtained from your OIDC provider")));
    }

    @Bean
    public GroupedOpenApi publicApi() {
        return GroupedOpenApi.builder()
            .group("public")
            .pathsToMatch("/api/**")
            .packagesToScan("com.cekinmezyucel.springboot.poc.api")
            .build();
    }

    @Bean
    public GroupedOpenApi actuatorApi() {
        return GroupedOpenApi.builder()
            .group("actuator")
            .pathsToMatch("/actuator/**")
            .build();
    }

    @Bean
    public GroupedOpenApi v1Api() {
        return GroupedOpenApi.builder()
            .group("v1-deprecated")
            .pathsToMatch("/api/v1/**")
            .build();
    }

    @Bean
    public GroupedOpenApi v2Api() {
        return GroupedOpenApi.builder()
            .group("v2-current")
            .pathsToMatch("/api/v2/**")
            .build();
    }
}
```

### Step 4: API Analytics and Monitoring

#### 4.1 API Metrics Configuration
Create `src/main/java/com/cekinmezyucel/springboot/poc/monitoring/ApiMetricsConfiguration.java`:
```java
package com.cekinmezyucel.springboot.poc.monitoring;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class ApiMetricsInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(ApiMetricsInterceptor.class);

    private final MeterRegistry meterRegistry;
    private final Counter apiCallsCounter;
    private final Timer apiResponseTimer;

    public ApiMetricsInterceptor(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        this.apiCallsCounter = Counter.builder("api.calls.total")
            .description("Total number of API calls")
            .register(meterRegistry);
        this.apiResponseTimer = Timer.builder("api.response.time")
            .description("API response time")
            .register(meterRegistry);
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        request.setAttribute("startTime", System.currentTimeMillis());
        
        // Log API call
        log.info("API Call: {} {} from {}", 
            request.getMethod(), 
            request.getRequestURI(), 
            getClientIpAddress(request));
        
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, 
            Object handler, Exception ex) {
        
        Long startTime = (Long) request.getAttribute("startTime");
        if (startTime != null) {
            long duration = System.currentTimeMillis() - startTime;
            
            // Record metrics
            apiCallsCounter.increment(
                "method", request.getMethod(),
                "uri", request.getRequestURI(),
                "status", String.valueOf(response.getStatus())
            );
            
            apiResponseTimer.record(duration, java.util.concurrent.TimeUnit.MILLISECONDS,
                "method", request.getMethod(),
                "uri", request.getRequestURI(),
                "status", String.valueOf(response.getStatus())
            );
            
            // Log response
            log.info("API Response: {} {} - {} in {}ms", 
                request.getMethod(),
                request.getRequestURI(),
                response.getStatus(),
                duration);
        }
    }

    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
```

### Step 5: Response Transformation and Caching

#### 5.1 Response Enhancement Interceptor
Create `src/main/java/com/cekinmezyucel/springboot/poc/api/interceptor/ResponseEnhancementInterceptor.java`:
```java
package com.cekinmezyucel.springboot.poc.api.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class ResponseEnhancementInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // Add API versioning headers
        response.setHeader("API-Version", "2.0");
        response.setHeader("API-Deprecated", "false");
        
        // Add cache control headers
        if (request.getMethod().equals("GET")) {
            response.setHeader("Cache-Control", "public, max-age=300"); // 5 minutes
        } else {
            response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        }
        
        // Add security headers
        response.setHeader("X-API-Rate-Limit", "100");
        response.setHeader("X-API-Request-ID", java.util.UUID.randomUUID().toString());
        
        return true;
    }
}
```

### Step 6: API Testing and Validation

#### 6.1 API Integration Tests
Create `src/test/java/com/cekinmezyucel/springboot/poc/api/ApiManagementIntegrationTest.java`:
```java
package com.cekinmezyucel.springboot.poc.api;

import com.cekinmezyucel.springboot.poc.BaseIntegrationTest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

class ApiManagementIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("Should expose OpenAPI documentation")
    void shouldExposeOpenApiDocumentation() {
        ResponseEntity<String> response = restTemplate.getForEntity("/v3/api-docs", String.class);
        
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("SpringBoot POC API");
        assertThat(response.getBody()).contains("\"version\":\"2.0.0\"");
    }

    @Test
    @DisplayName("Should expose Swagger UI")
    void shouldExposeSwaggerUI() {
        ResponseEntity<String> response = restTemplate.getForEntity("/swagger-ui/index.html", String.class);
        
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("Should support pagination parameters")
    void shouldSupportPaginationParameters() {
        ResponseEntity<String> response = restTemplate
            .withBasicAuth("test", "test")
            .getForEntity("/api/v2/users?page=0&size=5&sort=name,asc", String.class);
        
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("\"page\":");
        assertThat(response.getBody()).contains("\"_links\":");
    }

    @Test
    @DisplayName("Should include API version headers")
    void shouldIncludeApiVersionHeaders() {
        ResponseEntity<String> response = restTemplate.getForEntity("/api/v2/users", String.class);
        
        assertThat(response.getHeaders().get("API-Version")).contains("2.0");
        assertThat(response.getHeaders().get("API-Deprecated")).contains("false");
    }

    @Test
    @DisplayName("Should validate input parameters")
    void shouldValidateInputParameters() {
        // Test invalid page parameter
        ResponseEntity<String> response = restTemplate.getForEntity("/api/v2/users?page=-1", String.class);
        
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }
}
```

## Validation Checklist

### ✅ API Versioning
- [ ] Version paths configured (/api/v1, /api/v2)
- [ ] Version headers included in responses
- [ ] Deprecation strategy implemented
- [ ] Backward compatibility maintained
- [ ] Version-specific documentation

### ✅ Pagination and Sorting
- [ ] Page and size parameters working
- [ ] Sort parameters with validation
- [ ] HATEOAS links included
- [ ] Page metadata complete
- [ ] Performance optimized

### ✅ API Documentation
- [ ] OpenAPI 3.0 specification complete
- [ ] Swagger UI accessible
- [ ] Examples and descriptions provided
- [ ] Authentication documented
- [ ] Error responses documented

### ✅ Input Validation
- [ ] Request body validation
- [ ] Query parameter validation
- [ ] Path parameter validation
- [ ] Custom validation rules
- [ ] Error messages clear

### ✅ API Analytics
- [ ] Request/response metrics collected
- [ ] API usage tracked
- [ ] Performance metrics monitored
- [ ] Error rates tracked
- [ ] Client identification working

## Troubleshooting

### Common Issues
1. **OpenAPI generation fails**: Check YAML syntax and schema definitions
2. **Pagination not working**: Verify Pageable parameter binding
3. **Validation errors**: Check annotation configurations and custom validators
4. **Documentation not updating**: Clear cache and regenerate OpenAPI specs
5. **Version routing issues**: Verify controller annotations and path configuration

### Best Practices
- Use semantic versioning for API versions
- Provide comprehensive examples in documentation
- Implement gradual deprecation strategies
- Monitor API usage patterns and performance
- Use consistent error response formats

## Next Steps
After implementing API management enhancements:
1. Set up API gateway for additional features
2. Implement API key management
3. Add request/response transformation
4. Set up API analytics dashboard
5. Implement automated API testing

## AI Agent Notes
- Always validate OpenAPI specification syntax before regenerating
- Test pagination with large datasets to ensure performance
- Verify that all endpoints include proper validation
- Ensure documentation examples are accurate and up-to-date
- Monitor API metrics to identify usage patterns and optimization opportunities