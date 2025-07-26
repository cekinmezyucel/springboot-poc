# How to use the local OIDC mock provider

1. **Start the mock OIDC server**

   In your project root, run:

   ```zsh
   docker-compose up -d
   ```

   This will start the mock OIDC provider on http://localhost:8081.

2. **OIDC Discovery Endpoint**

   - Well-known config: http://localhost:8081/default/.well-known/openid-configuration
   - Issuer: http://localhost:8081/default

3. **Get a token with custom claims (e.g., roles)**
   - To get a token:
     ```zsh
     curl -X POST \
       http://localhost:8081/default/token \
       -d 'grant_type=client_credentials&scope=openid' \
       -H 'Content-Type: application/x-www-form-urlencoded'
     ```
   - For quick manual testing with custom claims, you can still use the built-in debugger:
     - Open http://localhost:8081/default/debugger in your browser.
     - Enter your desired claims (e.g., `{ "roles": ["employee.default"] }`), and get a token interactively.

4. **Configure your app**

   - Your `application-local.properties` is already set to use the mock issuer.

5. **Use the token**
   - Copy the token from the debugger or curl response and use it as a Bearer token in your API requests.

---

For advanced config (static claims, etc.), see and edit `mock-oidc-config.json` in the project root, or:

- https://github.com/navikt/mock-oauth2-server#standalone-server
- https://github.com/navikt/mock-oauth2-server#json_config
