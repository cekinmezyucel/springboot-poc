# Testing Strategy Enhancement Plan

## Current State Analysis
- ✅ Integration tests with Testcontainers
- ✅ Basic unit tests for services
- ✅ Security testing with mock JWT tokens
- ❌ No performance testing
- ❌ No contract testing
- ❌ No chaos engineering
- ❌ No end-to-end API testing
- ❌ No load testing
- ❌ No mutation testing

## Target State
- ✅ Comprehensive test pyramid implementation
- ✅ Performance and load testing
- ✅ Contract testing with consumer-driven contracts
- ✅ Chaos engineering and fault injection
- ✅ End-to-end API testing with real scenarios
- ✅ Mutation testing for test quality assessment
- ✅ Test automation in CI/CD pipeline
- ✅ Test reporting and quality metrics

## Implementation Steps

### Step 1: Enhanced Test Architecture

#### 1.1 Test Categories Configuration
Update `build.gradle` with test categories:
```gradle
// Add to dependencies
testImplementation 'org.junit.jupiter:junit-jupiter-api'
testImplementation 'org.junit.jupiter:junit-jupiter-engine'
testImplementation 'org.junit.jupiter:junit-jupiter-params'

// Performance Testing
testImplementation 'org.springframework.boot:spring-boot-starter-test'
testImplementation 'io.rest-assured:rest-assured:5.3.0'
testImplementation 'io.rest-assured:spring-mock-mvc:5.3.0'
testImplementation 'org.testcontainers:junit-jupiter'
testImplementation 'org.testcontainers:postgresql'

// Load Testing
testImplementation 'org.apache.jmeter:ApacheJMeter_core:5.6.2'
testImplementation 'org.apache.jmeter:ApacheJMeter_http:5.6.2'
testImplementation 'org.apache.jmeter:ApacheJMeter_java:5.6.2'

// Contract Testing
testImplementation 'org.springframework.cloud:spring-cloud-starter-contract-verifier:4.0.4'
testImplementation 'org.springframework.cloud:spring-cloud-contract-wiremock:4.0.4'

// Chaos Engineering
testImplementation 'org.springframework.boot:spring-boot-starter-actuator'
testImplementation 'de.codecentric:chaos-monkey-spring-boot:3.0.1'

// Mutation Testing
id 'info.solidsoft.pitest' version '1.15.0'

// Test configurations
configurations {
    unitTests
    integrationTests
    performanceTests
    contractTests
    e2eTests
}

// Test tasks
test {
    useJUnitPlatform {
        excludeTags 'integration', 'performance', 'contract', 'e2e'
    }
    finalizedBy jacocoTestReport
}

task integrationTest(type: Test) {
    useJUnitPlatform {
        includeTags 'integration'
    }
    testClassesDirs = sourceSets.test.output.classesDirs
    classpath = sourceSets.test.runtimeClasspath
    shouldRunAfter test
}

task performanceTest(type: Test) {
    useJUnitPlatform {
        includeTags 'performance'
    }
    testClassesDirs = sourceSets.test.output.classesDirs
    classpath = sourceSets.test.runtimeClasspath
    shouldRunAfter integrationTest
}

task contractTest(type: Test) {
    useJUnitPlatform {
        includeTags 'contract'
    }
    testClassesDirs = sourceSets.test.output.classesDirs
    classpath = sourceSets.test.runtimeClasspath
}

task e2eTest(type: Test) {
    useJUnitPlatform {
        includeTags 'e2e'
    }
    testClassesDirs = sourceSets.test.output.classesDirs
    classpath = sourceSets.test.runtimeClasspath
    shouldRunAfter performanceTest
}

// Mutation testing configuration
pitest {
    targetClasses = ['com.cekinmezyucel.springboot.poc.*']
    excludedClasses = [
        'com.cekinmezyucel.springboot.poc.config.*',
        'com.cekinmezyucel.springboot.poc.Application',
        'com.cekinmezyucel.springboot.poc.entity.*'
    ]
    threads = 4
    outputFormats = ['XML', 'HTML']
    timestampedReports = false
    mutationThreshold = 75
    coverageThreshold = 80
}

// JaCoCo configuration
jacocoTestReport {
    dependsOn test, integrationTest
    reports {
        xml.enabled true
        html.enabled true
    }
    executionData fileTree(dir: 'build/jacoco', include: '**/*.exec')
}
```

#### 1.2 Test Tags and Categories
Create `src/test/java/com/cekinmezyucel/springboot/poc/TestTags.java`:
```java
package com.cekinmezyucel.springboot.poc;

public final class TestTags {
    public static final String UNIT = "unit";
    public static final String INTEGRATION = "integration";
    public static final String PERFORMANCE = "performance";
    public static final String CONTRACT = "contract";
    public static final String E2E = "e2e";
    public static final String CHAOS = "chaos";
    public static final String SECURITY = "security";
    public static final String SMOKE = "smoke";
    
    private TestTags() {}
}
```

### Step 2: Performance Testing

#### 2.1 Load Testing with JMeter
Create `src/test/java/com/cekinmezyucel/springboot/poc/performance/LoadTestConfiguration.java`:
```java
package com.cekinmezyucel.springboot.poc.performance;

import org.apache.jmeter.control.LoopController;
import org.apache.jmeter.engine.StandardJMeterEngine;
import org.apache.jmeter.protocol.http.sampler.HTTPSampler;
import org.apache.jmeter.reporters.ResultCollector;
import org.apache.jmeter.reporters.Summariser;
import org.apache.jmeter.testelement.TestPlan;
import org.apache.jmeter.threads.ThreadGroup;
import org.apache.jmeter.util.JMeterUtils;
import org.apache.jorphan.collections.HashTree;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class LoadTestConfiguration {
    
    private final String testName;
    private final String serverUrl;
    private final int threadCount;
    private final int rampUpPeriod;
    private final int loopCount;
    
    public LoadTestConfiguration(String testName, String serverUrl, 
                               int threadCount, int rampUpPeriod, int loopCount) {
        this.testName = testName;
        this.serverUrl = serverUrl;
        this.threadCount = threadCount;
        this.rampUpPeriod = rampUpPeriod;
        this.loopCount = loopCount;
    }
    
    public void executeLoadTest() throws IOException {
        // Initialize JMeter
        File jmeterHome = new File(System.getProperty("user.home") + "/.gradle/jmeter");
        if (!jmeterHome.exists()) {
            jmeterHome.mkdirs();
        }
        
        JMeterUtils.setJMeterHome(jmeterHome.getAbsolutePath());
        JMeterUtils.initLocale();
        JMeterUtils.initLogging();
        
        // Create Test Plan
        TestPlan testPlan = new TestPlan(testName);
        testPlan.setProperty(TestPlan.FUNCTIONAL_MODE, false);
        testPlan.setProperty(TestPlan.SERIALIZE_THREADGROUPS, false);
        
        // Create Thread Group
        ThreadGroup threadGroup = new ThreadGroup();
        threadGroup.setName("Load Test Thread Group");
        threadGroup.setNumThreads(threadCount);
        threadGroup.setRampUp(rampUpPeriod);
        
        // Create Loop Controller
        LoopController loopController = new LoopController();
        loopController.setLoops(loopCount);
        loopController.setFirst(true);
        loopController.initialize();
        threadGroup.setSamplerController(loopController);
        
        // Create HTTP Samplers
        HTTPSampler healthCheckSampler = createHttpSampler(
            "Health Check", "GET", "/actuator/health", "");
        HTTPSampler getUsersSampler = createHttpSampler(
            "Get Users", "GET", "/api/v2/users", "");
        HTTPSampler createUserSampler = createHttpSampler(
            "Create User", "POST", "/api/v2/users", 
            "{\"email\":\"test@example.com\",\"name\":\"Test\",\"surname\":\"User\"}");
        
        // Create Test Tree
        HashTree testPlanTree = new HashTree();
        HashTree threadGroupTree = testPlanTree.add(testPlan, threadGroup);
        threadGroupTree.add(healthCheckSampler);
        threadGroupTree.add(getUsersSampler);
        threadGroupTree.add(createUserSampler);
        
        // Add Result Collector
        Summariser summer = new Summariser("summary");
        ResultCollector logger = new ResultCollector(summer);
        logger.setFilename("build/test-results/load-test-results.jtl");
        testPlanTree.add(testPlanTree.getArray()[0], logger);
        
        // Run Test
        StandardJMeterEngine jmeter = new StandardJMeterEngine();
        jmeter.configure(testPlanTree);
        jmeter.run();
    }
    
    private HTTPSampler createHttpSampler(String name, String method, String path, String body) {
        HTTPSampler sampler = new HTTPSampler();
        sampler.setName(name);
        sampler.setDomain("localhost");
        sampler.setPort(8080);
        sampler.setPath(path);
        sampler.setMethod(method);
        sampler.setProtocol("http");
        
        if (!body.isEmpty()) {
            sampler.setPostBodyRaw(true);
            sampler.addNonEncodedArgument("", body, "");
            sampler.getHeaderManager().add(
                new org.apache.jmeter.protocol.http.control.Header(
                    "Content-Type", "application/json"));
        }
        
        return sampler;
    }
}
```

#### 2.2 Performance Test Implementation
Create `src/test/java/com/cekinmezyucel/springboot/poc/performance/PerformanceTest.java`:
```java
package com.cekinmezyucel.springboot.poc.performance;

import com.cekinmezyucel.springboot.poc.BaseIntegrationTest;
import com.cekinmezyucel.springboot.poc.TestTags;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.stream.IntStream;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Tag(TestTags.PERFORMANCE)
class PerformanceTest extends BaseIntegrationTest {

    @LocalServerPort
    private int port;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
    }

    @Test
    @DisplayName("Should handle concurrent user requests efficiently")
    void shouldHandleConcurrentUserRequests() throws InterruptedException, ExecutionException {
        int concurrentUsers = 50;
        int requestsPerUser = 10;
        ExecutorService executor = Executors.newFixedThreadPool(concurrentUsers);
        
        List<CompletableFuture<PerformanceMetrics>> futures = new ArrayList<>();
        
        // Create concurrent requests
        for (int i = 0; i < concurrentUsers; i++) {
            futures.add(CompletableFuture.supplyAsync(() -> {
                List<Long> responseTimes = new ArrayList<>();
                int successCount = 0;
                
                for (int j = 0; j < requestsPerUser; j++) {
                    Instant start = Instant.now();
                    try {
                        Response response = given()
                            .header("Authorization", "Bearer " + generateTestJwtToken())
                            .when()
                            .get("/api/v2/users")
                            .then()
                            .extract().response();
                        
                        long responseTime = Duration.between(start, Instant.now()).toMillis();
                        responseTimes.add(responseTime);
                        
                        if (response.getStatusCode() == 200) {
                            successCount++;
                        }
                    } catch (Exception e) {
                        // Count as failure
                        responseTimes.add(Duration.between(start, Instant.now()).toMillis());
                    }
                }
                
                return new PerformanceMetrics(responseTimes, successCount, requestsPerUser);
            }, executor));
        }
        
        // Collect results
        List<PerformanceMetrics> results = futures.stream()
            .map(CompletableFuture::join)
            .toList();
        
        executor.shutdown();
        
        // Analyze performance
        double totalSuccessRate = results.stream()
            .mapToDouble(PerformanceMetrics::getSuccessRate)
            .average()
            .orElse(0.0);
        
        List<Long> allResponseTimes = results.stream()
            .flatMap(result -> result.getResponseTimes().stream())
            .sorted()
            .toList();
        
        long averageResponseTime = (long) allResponseTimes.stream()
            .mapToLong(Long::longValue)
            .average()
            .orElse(0.0);
        
        long p95ResponseTime = allResponseTimes.get((int) (allResponseTimes.size() * 0.95));
        long p99ResponseTime = allResponseTimes.get((int) (allResponseTimes.size() * 0.99));
        
        // Performance assertions
        assertThat(totalSuccessRate).isGreaterThan(0.95); // 95% success rate
        assertThat(averageResponseTime).isLessThan(500); // Average response time < 500ms
        assertThat(p95ResponseTime).isLessThan(1000); // 95th percentile < 1000ms
        assertThat(p99ResponseTime).isLessThan(2000); // 99th percentile < 2000ms
        
        System.out.printf("Performance Results:%n");
        System.out.printf("Success Rate: %.2f%%%n", totalSuccessRate * 100);
        System.out.printf("Average Response Time: %d ms%n", averageResponseTime);
        System.out.printf("95th Percentile: %d ms%n", p95ResponseTime);
        System.out.printf("99th Percentile: %d ms%n", p99ResponseTime);
    }

    @Test
    @DisplayName("Should maintain performance under sustained load")
    void shouldMaintainPerformanceUnderSustainedLoad() {
        int duration = 60; // seconds
        int threadsPerSecond = 5;
        
        Instant endTime = Instant.now().plusSeconds(duration);
        List<Long> responseTimes = new ArrayList<>();
        int totalRequests = 0;
        int successfulRequests = 0;
        
        while (Instant.now().isBefore(endTime)) {
            Instant start = Instant.now();
            
            try {
                Response response = given()
                    .header("Authorization", "Bearer " + generateTestJwtToken())
                    .when()
                    .get("/api/v2/users")
                    .then()
                    .extract().response();
                
                long responseTime = Duration.between(start, Instant.now()).toMillis();
                responseTimes.add(responseTime);
                totalRequests++;
                
                if (response.getStatusCode() == 200) {
                    successfulRequests++;
                }
            } catch (Exception e) {
                totalRequests++;
                responseTimes.add(Duration.between(start, Instant.now()).toMillis());
            }
            
            // Throttle to maintain consistent load
            try {
                Thread.sleep(1000 / threadsPerSecond);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        
        double successRate = (double) successfulRequests / totalRequests;
        double averageResponseTime = responseTimes.stream()
            .mapToLong(Long::longValue)
            .average()
            .orElse(0.0);
        
        // Sustained load assertions
        assertThat(successRate).isGreaterThan(0.98); // 98% success rate under sustained load
        assertThat(averageResponseTime).isLessThan(300); // Average response time < 300ms
        
        System.out.printf("Sustained Load Results:%n");
        System.out.printf("Total Requests: %d%n", totalRequests);
        System.out.printf("Success Rate: %.2f%%%n", successRate * 100);
        System.out.printf("Average Response Time: %.2f ms%n", averageResponseTime);
    }

    @Test
    @DisplayName("Should handle memory stress gracefully")
    void shouldHandleMemoryStressGracefully() {
        // Create requests that might cause memory pressure
        List<CompletableFuture<Void>> futures = IntStream.range(0, 100)
            .mapToObj(i -> CompletableFuture.runAsync(() -> {
                try {
                    given()
                        .header("Authorization", "Bearer " + generateTestJwtToken())
                        .queryParam("size", 100) // Large page size
                        .when()
                        .get("/api/v2/users")
                        .then()
                        .statusCode(200);
                } catch (Exception e) {
                    // Acceptable under stress
                }
            }))
            .toList();
        
        // Wait for all requests to complete
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        
        // Verify application is still responsive
        given()
            .when()
            .get("/actuator/health")
            .then()
            .statusCode(200);
    }

    private String generateTestJwtToken() {
        // Generate a test JWT token with appropriate claims
        return "test-jwt-token"; // Implement proper test token generation
    }

    private static class PerformanceMetrics {
        private final List<Long> responseTimes;
        private final int successCount;
        private final int totalCount;

        public PerformanceMetrics(List<Long> responseTimes, int successCount, int totalCount) {
            this.responseTimes = responseTimes;
            this.successCount = successCount;
            this.totalCount = totalCount;
        }

        public List<Long> getResponseTimes() {
            return responseTimes;
        }

        public double getSuccessRate() {
            return (double) successCount / totalCount;
        }
    }
}
```

### Step 3: Contract Testing

#### 3.1 Provider Contract Tests
Create `src/test/java/com/cekinmezyucel/springboot/poc/contract/UserApiContractTest.java`:
```java
package com.cekinmezyucel.springboot.poc.contract;

import com.cekinmezyucel.springboot.poc.BaseIntegrationTest;
import com.cekinmezyucel.springboot.poc.TestTags;
import com.cekinmezyucel.springboot.poc.entity.UserEntity;
import com.cekinmezyucel.springboot.poc.repository.UserRepository;
import io.restassured.module.mockmvc.RestAssuredMockMvc;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.verifier.messaging.boot.AutoConfigureMessageVerifier;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
@DirtiesContext
@AutoConfigureMessageVerifier
@Tag(TestTags.CONTRACT)
public class UserApiContractTest extends BaseIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    public void setup() {
        RestAssuredMockMvc.mockMvc(MockMvcBuilders.webAppContextSetup(context).build());
        
        // Setup test data for contract verification
        setupContractTestData();
    }

    private void setupContractTestData() {
        // Clear existing data
        userRepository.deleteAll();
        
        // Create test users for contract verification
        UserEntity user1 = new UserEntity("john.doe@example.com", "John", "Doe");
        UserEntity user2 = new UserEntity("jane.smith@example.com", "Jane", "Smith");
        
        userRepository.save(user1);
        userRepository.save(user2);
    }
}
```

#### 3.2 Contract Definition
Create `src/test/resources/contracts/user_api.groovy`:
```groovy
package contracts

import org.springframework.cloud.contract.spec.Contract

[
    Contract.make {
        description "should return list of users"
        request {
            method GET()
            url "/api/v2/users"
            headers {
                contentType(applicationJson())
                header("Authorization", "Bearer valid-token")
            }
        }
        response {
            status OK()
            headers {
                contentType(applicationJson())
            }
            body("""
                {
                    "content": [
                        {
                            "id": $(anyPositiveInt()),
                            "email": $(email()),
                            "name": $(anyNonBlankString()),
                            "surname": $(anyNonBlankString()),
                            "fullName": $(anyNonBlankString()),
                            "accountIds": $(anyArray()),
                            "version": $(anyPositiveInt()),
                            "createdAt": $(anyDateTime()),
                            "updatedAt": $(anyDateTime())
                        }
                    ],
                    "page": {
                        "size": $(anyPositiveInt()),
                        "number": $(anyPositiveInt()),
                        "totalElements": $(anyPositiveInt()),
                        "totalPages": $(anyPositiveInt()),
                        "first": $(anyBoolean()),
                        "last": $(anyBoolean())
                    }
                }
            """)
        }
    },
    
    Contract.make {
        description "should create a new user"
        request {
            method POST()
            url "/api/v2/users"
            headers {
                contentType(applicationJson())
                header("Authorization", "Bearer valid-token")
            }
            body("""
                {
                    "email": "new.user@example.com",
                    "name": "New",
                    "surname": "User"
                }
            """)
        }
        response {
            status CREATED()
            headers {
                contentType(applicationJson())
            }
            body("""
                {
                    "id": $(anyPositiveInt()),
                    "email": "new.user@example.com",
                    "name": "New",
                    "surname": "User",
                    "fullName": "New User",
                    "accountIds": [],
                    "version": 0,
                    "createdAt": $(anyDateTime()),
                    "updatedAt": $(anyDateTime())
                }
            """)
        }
    }
]
```

### Step 4: Chaos Engineering

#### 4.1 Chaos Monkey Configuration
Add to `application-test.properties`:
```properties
# Chaos Monkey Configuration
chaos.monkey.enabled=true
chaos.monkey.watcher.controller=true
chaos.monkey.watcher.restController=true
chaos.monkey.watcher.service=true
chaos.monkey.watcher.repository=true

# Assault Configuration
chaos.monkey.assaults.level=5
chaos.monkey.assaults.latencyActive=true
chaos.monkey.assaults.latencyRangeStart=1000
chaos.monkey.assaults.latencyRangeEnd=3000
chaos.monkey.assaults.exceptionsActive=true
chaos.monkey.assaults.killApplicationActive=false
chaos.monkey.assaults.memoryActive=true
chaos.monkey.assaults.memoryMillisecondsHoldFilledMemory=90000
chaos.monkey.assaults.memoryMillisecondsWaitNextIncrease=1000
chaos.monkey.assaults.memoryFillIncrementFraction=0.15
chaos.monkey.assaults.memoryFillTargetFraction=0.25
```

#### 4.2 Chaos Engineering Tests
Create `src/test/java/com/cekinmezyucel/springboot/poc/chaos/ChaosEngineeringTest.java`:
```java
package com.cekinmezyucel.springboot.poc.chaos;

import com.cekinmezyucel.springboot.poc.BaseIntegrationTest;
import com.cekinmezyucel.springboot.poc.TestTags;
import de.codecentric.spring.boot.chaos.monkey.component.ChaosMonkey;
import de.codecentric.spring.boot.chaos.monkey.configuration.AssaultProperties;
import de.codecentric.spring.boot.chaos.monkey.configuration.ChaosMonkeyProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles({"test", "chaos"})
@Tag(TestTags.CHAOS)
class ChaosEngineeringTest extends BaseIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ChaosMonkey chaosMonkey;

    @Autowired
    private ChaosMonkeyProperties chaosMonkeyProperties;

    @Test
    @DisplayName("Should maintain availability during latency injection")
    void shouldMaintainAvailabilityDuringLatencyInjection() {
        // Enable latency assault
        AssaultProperties assaultProperties = chaosMonkeyProperties.getAssaults();
        assaultProperties.setLatencyActive(true);
        assaultProperties.setLatencyRangeStart(500);
        assaultProperties.setLatencyRangeEnd(1500);
        
        chaosMonkey.getChaosMonkeyRequestScope().getChaosMonkeySettings().setAssaults(assaultProperties);
        
        // Run requests and measure success rate
        int totalRequests = 50;
        int successfulRequests = 0;
        
        for (int i = 0; i < totalRequests; i++) {
            try {
                ResponseEntity<String> response = restTemplate
                    .withBasicAuth("test", "test")
                    .getForEntity("/api/v2/users", String.class);
                
                if (response.getStatusCode() == HttpStatus.OK) {
                    successfulRequests++;
                }
            } catch (Exception e) {
                // Count as failure
            }
        }
        
        double successRate = (double) successfulRequests / totalRequests;
        
        // Even with latency injection, should maintain reasonable success rate
        assertThat(successRate).isGreaterThan(0.8); // 80% success rate minimum
    }

    @Test
    @DisplayName("Should handle exception injection gracefully")
    void shouldHandleExceptionInjectionGracefully() {
        // Enable exception assault
        AssaultProperties assaultProperties = chaosMonkeyProperties.getAssaults();
        assaultProperties.setExceptionsActive(true);
        assaultProperties.setLevel(3); // Lower level for exception testing
        
        chaosMonkey.getChaosMonkeyRequestScope().getChaosMonkeySettings().setAssaults(assaultProperties);
        
        // Test that health endpoint remains available even with exceptions
        ResponseEntity<String> healthResponse = restTemplate.getForEntity("/actuator/health", String.class);
        assertThat(healthResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        
        // Test API resilience
        int attempts = 20;
        int successCount = 0;
        
        for (int i = 0; i < attempts; i++) {
            try {
                ResponseEntity<String> response = restTemplate
                    .withBasicAuth("test", "test")
                    .getForEntity("/api/v2/users", String.class);
                
                if (response.getStatusCode() == HttpStatus.OK) {
                    successCount++;
                }
            } catch (Exception e) {
                // Expected due to chaos monkey
            }
        }
        
        // Should have some successful requests despite chaos
        assertThat(successCount).isGreaterThan(0);
    }

    @Test
    @DisplayName("Should recover from memory pressure")
    void shouldRecoverFromMemoryPressure() {
        // Enable memory assault
        AssaultProperties assaultProperties = chaosMonkeyProperties.getAssaults();
        assaultProperties.setMemoryActive(true);
        assaultProperties.setMemoryFillTargetFraction(0.8);
        
        chaosMonkey.getChaosMonkeyRequestScope().getChaosMonkeySettings().setAssaults(assaultProperties);
        
        // Wait for memory pressure to be applied
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // Test that application can still respond
        ResponseEntity<String> response = restTemplate.getForEntity("/actuator/health", String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        
        // Disable memory assault and verify recovery
        assaultProperties.setMemoryActive(false);
        chaosMonkey.getChaosMonkeyRequestScope().getChaosMonkeySettings().setAssaults(assaultProperties);
        
        // Allow time for recovery
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // Verify full functionality is restored
        ResponseEntity<String> usersResponse = restTemplate
            .withBasicAuth("test", "test")
            .getForEntity("/api/v2/users", String.class);
        assertThat(usersResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("Should maintain circuit breaker functionality under chaos")
    void shouldMaintainCircuitBreakerFunctionalityUnderChaos() throws InterruptedException, ExecutionException, TimeoutException {
        // Enable latency assault to trigger circuit breaker
        AssaultProperties assaultProperties = chaosMonkeyProperties.getAssaults();
        assaultProperties.setLatencyActive(true);
        assaultProperties.setLatencyRangeStart(3000);
        assaultProperties.setLatencyRangeEnd(5000);
        assaultProperties.setLevel(8); // High level to ensure assault triggers
        
        chaosMonkey.getChaosMonkeyRequestScope().getChaosMonkeySettings().setAssaults(assaultProperties);
        
        // Make multiple concurrent requests to trigger circuit breaker
        CompletableFuture<?>[] futures = IntStream.range(0, 20)
            .mapToObj(i -> CompletableFuture.runAsync(() -> {
                try {
                    restTemplate
                        .withBasicAuth("test", "test")
                        .getForEntity("/api/v2/users", String.class);
                } catch (Exception e) {
                    // Expected due to chaos and circuit breaker
                }
            }))
            .toArray(CompletableFuture[]::new);
        
        CompletableFuture.allOf(futures).get(30, TimeUnit.SECONDS);
        
        // Verify that circuit breaker metrics are being collected
        ResponseEntity<String> metricsResponse = restTemplate.getForEntity("/actuator/metrics", String.class);
        assertThat(metricsResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(metricsResponse.getBody()).contains("resilience4j");
    }
}
```

### Step 5: End-to-End Testing

#### 5.1 E2E Test Suite
Create `src/test/java/com/cekinmezyucel/springboot/poc/e2e/UserManagementE2ETest.java`:
```java
package com.cekinmezyucel.springboot.poc.e2e;

import com.cekinmezyucel.springboot.poc.BaseIntegrationTest;
import com.cekinmezyucel.springboot.poc.TestTags;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Tag(TestTags.E2E)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class UserManagementE2ETest extends BaseIntegrationTest {

    @LocalServerPort
    private int port;

    private static String createdUserId;
    private static String createdAccountId;
    private static String authToken;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
        
        if (authToken == null) {
            authToken = generateTestJwtToken();
        }
    }

    @Test
    @Order(1)
    @DisplayName("E2E: Should authenticate and access API")
    void shouldAuthenticateAndAccessApi() {
        given()
            .header("Authorization", "Bearer " + authToken)
        .when()
            .get("/api/v2/users")
        .then()
            .statusCode(200)
            .body("content", notNullValue())
            .body("page", notNullValue());
    }

    @Test
    @Order(2)
    @DisplayName("E2E: Should create a new user with complete workflow")
    void shouldCreateNewUserWithCompleteWorkflow() {
        // Create user
        Response createResponse = given()
            .header("Authorization", "Bearer " + authToken)
            .header("Content-Type", "application/json")
            .body("""
                {
                    "email": "e2e.test@example.com",
                    "name": "E2E",
                    "surname": "Test"
                }
                """)
        .when()
            .post("/api/v2/users")
        .then()
            .statusCode(201)
            .body("email", equalTo("e2e.test@example.com"))
            .body("name", equalTo("E2E"))
            .body("surname", equalTo("Test"))
            .body("fullName", equalTo("E2E Test"))
            .body("id", notNullValue())
            .body("version", equalTo(0))
            .body("createdAt", notNullValue())
            .extract().response();

        createdUserId = createResponse.path("id").toString();

        // Verify user appears in user list
        given()
            .header("Authorization", "Bearer " + authToken)
        .when()
            .get("/api/v2/users")
        .then()
            .statusCode(200)
            .body("content.find { it.id == " + createdUserId + " }.email", 
                  equalTo("e2e.test@example.com"));

        // Get specific user
        given()
            .header("Authorization", "Bearer " + authToken)
        .when()
            .get("/api/v2/users/" + createdUserId)
        .then()
            .statusCode(200)
            .body("email", equalTo("e2e.test@example.com"))
            .body("id", equalTo(Integer.parseInt(createdUserId)));
    }

    @Test
    @Order(3)
    @DisplayName("E2E: Should create account and link to user")
    void shouldCreateAccountAndLinkToUser() {
        // Create account
        Response accountResponse = given()
            .header("Authorization", "Bearer " + authToken)
            .header("Content-Type", "application/json")
            .body("""
                {
                    "name": "E2E Test Company",
                    "industry": "TECHNOLOGY"
                }
                """)
        .when()
            .post("/api/v2/accounts")
        .then()
            .statusCode(201)
            .body("name", equalTo("E2E Test Company"))
            .body("industry", equalTo("TECHNOLOGY"))
            .extract().response();

        createdAccountId = accountResponse.path("id").toString();

        // Link user to account
        given()
            .header("Authorization", "Bearer " + authToken)
        .when()
            .post("/api/v2/users/" + createdUserId + "/accounts/" + createdAccountId)
        .then()
            .statusCode(200);

        // Verify user has account linked
        given()
            .header("Authorization", "Bearer " + authToken)
        .when()
            .get("/api/v2/users/" + createdUserId)
        .then()
            .statusCode(200)
            .body("accountIds", hasItem(Integer.parseInt(createdAccountId)));
    }

    @Test
    @Order(4)
    @DisplayName("E2E: Should update user information")
    void shouldUpdateUserInformation() {
        // Get current user data for version
        Response currentUser = given()
            .header("Authorization", "Bearer " + authToken)
        .when()
            .get("/api/v2/users/" + createdUserId)
        .then()
            .statusCode(200)
            .extract().response();

        int currentVersion = currentUser.path("version");

        // Update user
        given()
            .header("Authorization", "Bearer " + authToken)
            .header("Content-Type", "application/json")
            .body(String.format("""
                {
                    "email": "updated.e2e.test@example.com",
                    "name": "Updated E2E",
                    "surname": "Test User",
                    "version": %d
                }
                """, currentVersion))
        .when()
            .put("/api/v2/users/" + createdUserId)
        .then()
            .statusCode(200)
            .body("email", equalTo("updated.e2e.test@example.com"))
            .body("name", equalTo("Updated E2E"))
            .body("surname", equalTo("Test User"))
            .body("version", equalTo(currentVersion + 1));
    }

    @Test
    @Order(5)
    @DisplayName("E2E: Should handle pagination and filtering")
    void shouldHandlePaginationAndFiltering() {
        // Test pagination
        given()
            .header("Authorization", "Bearer " + authToken)
            .queryParam("page", 0)
            .queryParam("size", 5)
        .when()
            .get("/api/v2/users")
        .then()
            .statusCode(200)
            .body("page.size", equalTo(5))
            .body("page.number", equalTo(0))
            .body("content.size()", lessThanOrEqualTo(5));

        // Test sorting
        given()
            .header("Authorization", "Bearer " + authToken)
            .queryParam("sort", "name,asc")
        .when()
            .get("/api/v2/users")
        .then()
            .statusCode(200)
            .body("content", notNullValue());

        // Test search
        given()
            .header("Authorization", "Bearer " + authToken)
            .queryParam("search", "Updated E2E")
        .when()
            .get("/api/v2/users")
        .then()
            .statusCode(200)
            .body("content.find { it.id == " + createdUserId + " }", notNullValue());
    }

    @Test
    @Order(6)
    @DisplayName("E2E: Should handle error scenarios gracefully")
    void shouldHandleErrorScenariosGracefully() {
        // Test invalid user creation
        given()
            .header("Authorization", "Bearer " + authToken)
            .header("Content-Type", "application/json")
            .body("""
                {
                    "email": "invalid-email",
                    "name": "",
                    "surname": ""
                }
                """)
        .when()
            .post("/api/v2/users")
        .then()
            .statusCode(400)
            .body("code", equalTo("VALIDATION_ERROR"));

        // Test non-existent user
        given()
            .header("Authorization", "Bearer " + authToken)
        .when()
            .get("/api/v2/users/99999")
        .then()
            .statusCode(404);

        // Test unauthorized access
        given()
            .header("Content-Type", "application/json")
        .when()
            .get("/api/v2/users")
        .then()
            .statusCode(401);
    }

    @Test
    @Order(7)
    @DisplayName("E2E: Should perform cleanup operations")
    void shouldPerformCleanupOperations() {
        // Unlink user from account
        given()
            .header("Authorization", "Bearer " + authToken)
        .when()
            .delete("/api/v2/users/" + createdUserId + "/accounts/" + createdAccountId)
        .then()
            .statusCode(200);

        // Soft delete user
        given()
            .header("Authorization", "Bearer " + authToken)
        .when()
            .delete("/api/v2/users/" + createdUserId)
        .then()
            .statusCode(204);

        // Verify user is not in active list
        given()
            .header("Authorization", "Bearer " + authToken)
        .when()
            .get("/api/v2/users/" + createdUserId)
        .then()
            .statusCode(404);
    }

    private String generateTestJwtToken() {
        // Generate a test JWT token with appropriate claims
        return "test-jwt-token"; // Implement proper test token generation
    }
}
```

## Validation Checklist

### ✅ Test Architecture
- [ ] Test pyramid properly implemented
- [ ] Test categories clearly defined
- [ ] Parallel test execution configured
- [ ] Test data management strategy
- [ ] Test reporting and metrics

### ✅ Performance Testing
- [ ] Load testing scenarios defined
- [ ] Performance benchmarks established
- [ ] Stress testing implemented
- [ ] Memory and resource testing
- [ ] Performance regression detection

### ✅ Contract Testing
- [ ] Provider contracts defined
- [ ] Consumer contracts verified
- [ ] Contract evolution strategy
- [ ] Breaking change detection
- [ ] API compatibility testing

### ✅ Chaos Engineering
- [ ] Fault injection configured
- [ ] Resilience patterns tested
- [ ] Recovery scenarios validated
- [ ] System limits identified
- [ ] Chaos automation implemented

### ✅ End-to-End Testing
- [ ] Complete user journeys tested
- [ ] Cross-system integration verified
- [ ] Real-world scenarios covered
- [ ] Data consistency validated
- [ ] Error handling tested

## Troubleshooting

### Common Issues
1. **Test instability**: Implement proper test isolation and cleanup
2. **Performance test variability**: Use consistent test environments
3. **Contract test failures**: Maintain contract versioning and compatibility
4. **Chaos test unpredictability**: Control chaos parameters and timeouts
5. **E2E test maintenance**: Keep tests focused and maintainable

### Best Practices
- Run tests in isolated environments
- Use test data builders for consistent setup
- Implement proper assertions and validations
- Monitor test execution metrics
- Regular review and maintenance of test suites

## Next Steps
After implementing comprehensive testing strategy:
1. Set up continuous test execution and monitoring
2. Implement test result analytics and reporting
3. Add automated test generation and maintenance
4. Set up performance baseline tracking
5. Implement advanced chaos engineering scenarios

## AI Agent Notes
- Always consider test execution time and resource usage
- Ensure tests are deterministic and repeatable
- Use appropriate test doubles and mocks
- Implement proper test data lifecycle management
- Monitor and analyze test results for insights and improvements