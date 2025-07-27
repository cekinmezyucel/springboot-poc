# Spring Boot POC: Secure, OpenAPI-Driven Backend with Realistic Local & Test Environment

## Project Goals

- **Production-grade backend** using Spring Boot, OpenAPI, and PostgreSQL.
- **Strict separation** of controller, service, and entity/model logic.
- **Robust security**: OIDC/OAuth2 resource server, method-level authorization, custom roles, and real JWT validation.
- **Developer-friendly local setup**: Realistic OIDC mock provider, easy token generation with custom claims, and Dockerized services.
- **Fast, reliable integration tests**: Testcontainers for DB, security-aware tests, and no security shortcuts.
- **Code quality**: Spotless, Checkstyle, and OpenAPI code generation.

---

## What We Achieved

### 1. **OpenAPI-Driven Backend**

- API and model classes are generated from `openapi.yaml` using OpenAPI Generator.
- All endpoints and models are type-safe and documented.

### 2. **Database & Migrations**

- Uses PostgreSQL (Dockerized for local/dev).
- Flyway for repeatable, versioned DB migrations.
- Testcontainers for isolated, real DB in integration tests.

### 3. **Entities & Relationships**

- JPA entities for `User` and `Account` with a many-to-many relationship.
- Model classes (DTOs) are mapped cleanly from entities.

### 4. **Security**

- OIDC/OAuth2 resource server (Spring Security).
- All endpoints (except `/health`) require authentication.
- Method-level security with custom roles (e.g., `poc.users.read`).
- Custom JWT claim mapping for roles.
- Security config matches production best practices.

### 5. **Integration Tests**

- Use MockMvc and Testcontainers for full-stack integration tests.
- Security is always enabled in tests—no shortcuts.
- Test JWTs are generated with the correct roles/claims.
- Static Testcontainers for fast test runs, with JVM shutdown hook for cleanup.

### 6. **Local OIDC Mock Provider**

- Dockerized [mock-oauth2-server](https://github.com/navikt/mock-oauth2-server) for local token issuing.
- Custom claims (including roles) are configured via UI.
- Built-in debugger UI for manual token generation.
- `application-local.properties` is preconfigured for local OIDC.

### 7. **Developer Experience**

- One-command startup: `docker-compose up` for DB and OIDC.
- Easy to get a real JWT for local API testing.
- All config and usage is documented in `LOCAL_OIDC_README.md`.
- Code is auto-formatted and checked for style.

---

## How to Run Locally

1. **Start dependencies:**
   ```zsh
   docker-compose up
   ```
2. **Run the app:**
   ```zsh
   ./gradlew bootRun
   ```
3. **Get a JWT:**
   - Use the OIDC debugger at http://localhost:8081/default/debugger
   - Or use curl as shown in `LOCAL_OIDC_README.md`
4. **Call secured endpoints:**
   - Use the JWT as a Bearer token in your API requests.

---

## How to Run Tests

- Run all tests:
  ```zsh
  ./gradlew test
  ```
- Integration tests use Testcontainers for DB and security.
- Containers are cleaned up automatically after the test suite.

---

## Key Files & Docs

- `openapi.yaml`: API contract
- Custom claims for tokens are created interactively using the mock OIDC provider's built-in UI (http://localhost:8081/default/debugger)
- `LOCAL_OIDC_README.md`: Local OIDC/token usage
- `docker-compose.yml`: Local DB and OIDC services
- `src/test/java/.../BaseIntegrationTest.java`: Testcontainers setup
- `build.gradle`: All dependencies and code quality plugins

---

## Why This Setup?

- **No security shortcuts:** Tests and local dev use real JWT validation and roles, just like production.
- **Fast feedback:** Static Testcontainers and local OIDC mean you can run and debug everything quickly.
- **Easy onboarding:** All steps are documented, and everything runs with Docker and Gradle.
- **Production-ready:** You can deploy this as-is, swap the OIDC issuer for your real provider, and be confident it works.

---

## Credits & References

- [Spring Boot](https://spring.io/projects/spring-boot)
- [Testcontainers](https://www.testcontainers.org/)
- [mock-oauth2-server](https://github.com/navikt/mock-oauth2-server)
- [OpenAPI Generator](https://openapi-generator.tech/)
- [Spotless](https://github.com/diffplug/spotless)
- [Checkstyle](https://checkstyle.org/)

---

**Happy coding!**

# springboot-poc

## Local Development with Docker Compose and PostgreSQL

1. **Start PostgreSQL with Docker Compose:**

   ```sh
   docker compose up -d
   ```

2. **Run Spring Boot with the local profile:**

   - From terminal:
     ```sh
     ./gradlew bootRun --args='--spring.profiles.active=local'
     ```
   - Or, with a packaged jar:
     ```sh
     java -jar build/libs/springboot-poc-0.0.1-SNAPSHOT.jar --spring.profiles.active=local
     ```

3. **VS Code Launch Configuration:**

   - Create `.vscode/launch.json` with:
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
   - Replace `mainClass` with your actual main class if different.

4. **Stop the database:**
   ```sh
   docker compose down
   ```

---

## Git Pre-commit Hook: Run Tests Before Commit

This project includes a pre-commit hook to ensure all tests pass before you commit code.

**Setup (one-time per clone):**

```sh
git config core.hooksPath .githooks
```

This will make Git use the `.githooks/pre-commit` script automatically. The script runs `./gradlew test` and aborts the commit if any test fails.

**Best practice:**

- Always keep the pre-commit hook enabled to avoid pushing broken code.
- If you need to skip the hook (not recommended), use `git commit --no-verify`.

---

- The `application-local.properties` file is preconfigured for local development.
- You can override any environment variable in VS Code using the `env` block in `launch.json`.
