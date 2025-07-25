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

- The `application-local.properties` file is preconfigured for local development.
- You can override any environment variable in VS Code using the `env` block in `launch.json`.
