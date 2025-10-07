# AI Agent Guide for SpringBoot POC Repository

## Repository Overview

This is a **production-grade Spring Boot backend** project that demonstrates best practices for building secure, OpenAPI-driven REST APIs with robust local development and testing environments.

### Key Characteristics
- **Primary Language**: Java 21
- **Framework**: Spring Boot 3.5.6
- **Build Tool**: Gradle 
- **Database**: PostgreSQL with Flyway migrations
- **Security**: OAuth2/OIDC Resource Server with JWT validation
- **API Documentation**: OpenAPI 3.0 with code generation
- **Testing**: Integration tests with Testcontainers
- **Code Quality**: Spotless formatting, Checkstyle
- **Local Development**: Docker Compose with mock OIDC provider

## Project Architecture

### Layer Structure
```
├── API Layer (Controllers)
│   ├── Generated from OpenAPI spec
│   └── Implementation delegates with security annotations
├── Service Layer
│   ├── Business logic
│   └── Entity-to-Model mapping
├── Repository Layer
│   └── Spring Data JPA repositories
├── Entity Layer
│   └── JPA entities with relationships
└── Security Layer
    └── JWT-based OAuth2 resource server
```

### Domain Model
- **UserEntity**: Users with email, name, surname
- **AccountEntity**: Accounts with name, industry
- **Many-to-Many Relationship**: Users ↔ Accounts via join table

### Security Model
- All endpoints except `/health` require authentication
- JWT tokens with custom claims (roles)
- Method-level security with `@RolesAllowed`
- Custom authority mapping from JWT claims

## Key Files for AI Agents

### Configuration Files
- `build.gradle` - Build configuration, dependencies, plugins
- `src/main/resources/openapi.yaml` - API contract (SINGLE SOURCE OF TRUTH)
- `docker-compose.yml` - Local development services
- `src/main/resources/application*.properties` - Environment configs

### Source Code Structure
- `src/main/java/com/cekinmezyucel/springboot/poc/`
  - `Application.java` - Spring Boot main class
  - `SecurityConfig.java` - Security configuration
  - `api/` - API implementations (delegates)
  - `service/` - Business logic layer
  - `entity/` - JPA entities
  - `repository/` - Data access layer
  - `util/ApplicationConstants.java` - Shared constants

### Testing Infrastructure
- `src/test/java/.../BaseIntegrationTest.java` - Testcontainers setup
- `src/test/java/.../BaseUnitTest.java` - Unit test base
- Integration tests for all API endpoints with security

### Database
- `src/main/resources/db/migration/V1__init.sql` - Database schema
- Flyway manages versioned migrations

## Working with This Repository

### Prerequisites for Development
1. **Java 21** (required)
2. **Docker & Docker Compose** (for local services)
3. **Gradle** (wrapper included)

### Essential Commands

#### Local Development
```bash
# Start dependencies (PostgreSQL + Mock OIDC)
docker-compose up -d

# Run application with local profile
./gradlew bootRun --args='--spring.profiles.active=local'

# Stop dependencies
docker-compose down
```

#### Build & Test
```bash
# Build project (includes OpenAPI generation + code formatting)
./gradlew build

# Run tests only
./gradlew test

# Format code
./gradlew spotlessApply

# Generate OpenAPI classes
./gradlew openApiGenerate
```

### Development Workflow

#### 1. API-First Development
- **ALWAYS** modify `src/main/resources/openapi.yaml` first
- Run `./gradlew openApiGenerate` to regenerate API interfaces
- Implement the delegate methods in `api/*ApiImpl.java` classes
- **DO NOT** modify generated code in `build/generated/`

#### 2. Database Changes
- Create new migration files: `src/main/resources/db/migration/V{n}__{description}.sql`
- Follow Flyway naming conventions
- Test migrations locally with Docker PostgreSQL

#### 3. Security Implementation
- All endpoints (except `/health`) require authentication
- Add `@RolesAllowed("role.name")` to methods needing authorization
- Use JWT claims for custom roles via `roles` claim

#### 4. Testing Strategy
- Integration tests extend `BaseIntegrationTest`
- Use Testcontainers for real database testing
- Mock JWT tokens for security testing
- All tests run with security enabled (no shortcuts)

### Local Development Setup

#### Getting JWT Tokens for Testing
1. Start services: `docker-compose up -d`
2. Visit http://localhost:8081/default/debugger
3. Add custom claims: `{"roles": ["poc.users.read"]}`
4. Generate JWT token
5. Use as Bearer token in API requests

#### VS Code Configuration
Create `.vscode/launch.json`:
```json
{
  "version": "0.2.0",
  "configurations": [
    {
      "type": "java",
      "name": "Spring Boot (local profile)",
      "request": "launch",
      "mainClass": "com.cekinmezyucel.springboot.poc.Application",
      "env": {
        "SPRING_PROFILES_ACTIVE": "local"
      }
    }
  ]
}
```

## AI Agent Best Practices

### When Making Changes

#### 1. Understand the Change Scope
- **API Changes**: Modify OpenAPI spec first, then implementations
- **Database Changes**: Create migrations, update entities
- **Business Logic**: Focus on service layer
- **Security Changes**: Update SecurityConfig and role annotations

#### 2. Follow the Architecture
- **DON'T** bypass security layers
- **DON'T** modify generated OpenAPI code
- **DO** maintain separation of concerns
- **DO** write integration tests for new endpoints

#### 3. Testing Requirements
- Run `./gradlew test` before committing
- Ensure all tests pass with security enabled
- Add tests for new functionality
- Use Testcontainers for database-dependent tests

#### 4. Code Quality
- Code is auto-formatted via Spotless
- Follow existing patterns for entity mapping
- Use dependency injection throughout
- Maintain consistent error handling

### Common Tasks

#### Adding New API Endpoint
1. Update `openapi.yaml` with new path and schema
2. Run `./gradlew openApiGenerate`
3. Implement delegate method in appropriate `*ApiImpl.java`
4. Add business logic to service layer
5. Create/update entity if needed
6. Write integration tests
7. Test with JWT tokens locally

#### Adding New Database Table
1. Create migration file `V{n}__{description}.sql`
2. Create JPA entity class
3. Create repository interface
4. Update service layer for CRUD operations
5. Add to OpenAPI schema if exposed via API
6. Write tests

#### Modifying Security Rules
1. Update method-level `@RolesAllowed` annotations
2. Modify JWT claim mapping if needed
3. Update test JWT tokens with correct roles
4. Test authorization scenarios

### Dependencies & Versions

#### Core Spring Boot Stack
- Spring Boot 3.5.6
- Spring Security (OAuth2 Resource Server)
- Spring Data JPA
- Spring Web

#### Database & Migration
- PostgreSQL 42.7.7
- Flyway 11.10.5

#### Code Generation & Quality
- OpenAPI Generator 7.16.0
- Spotless 7.2.1

#### Testing
- Testcontainers 1.21.3
- JUnit 5 (Platform)

### Environment Profiles

#### Production (`application.properties`)
- Real OIDC issuer URL
- Production database connection
- Minimal logging

#### Local (`application-local.properties`)
- Mock OIDC issuer (localhost:8081)
- Local Docker PostgreSQL
- Debug logging enabled

#### Test (`application-test.properties`)
- Testcontainers database
- Test-specific configurations

## Troubleshooting

### Common Issues

#### Build Failures
- Ensure Java 21 is installed and active
- Run `./gradlew clean build` for clean rebuild
- Check OpenAPI spec syntax if generation fails

#### Test Failures
- Ensure Docker is running for Testcontainers
- Check JWT token generation for security tests
- Verify database migrations apply correctly

#### Local Development Issues
- Confirm Docker Compose services are running
- Check JWT token has correct roles/claims
- Verify application-local.properties configuration

### Getting Help
- Check `README.md` for basic setup
- Review `LOCAL_OIDC_README.md` for token generation
- Use Postman collection for API testing examples
- Integration tests serve as usage examples

## Repository Maintenance

### Automated Updates
- Dependabot configured for daily dependency updates
- GitHub Actions for CI/CD pipeline
- Pre-commit hooks for test validation

### Code Quality Gates
- All code auto-formatted via Spotless
- OpenAPI contract validation
- Comprehensive test coverage
- Security-enabled testing

This guide should help AI agents navigate the codebase effectively and implement changes following the established patterns and best practices.