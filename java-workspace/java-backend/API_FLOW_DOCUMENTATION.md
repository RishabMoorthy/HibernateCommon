# Java Backend — Complete API Flow Documentation

> **Purpose:** This document explains how each API works — which method receives the request, what validation/checks happen, what database call is made, and what is returned. Each section ends with a **"How to Explain"** block — a sentence you can read directly to anyone.

---

## General Architecture (How All APIs Work)

Every API request goes through these layers in order:

```
HTTP Request
    ↓
JwtAuthFilter          (security/JwtAuthFilter.java)   — checks if token is valid
    ↓
SecurityConfig         (config/SecurityConfig.java)     — checks if the route is allowed for that role
    ↓
Controller             (controller/*.java)              — receives the request, reads body/params
    ↓
Service                (service/*.java)                 — does all validation, DB checks, logic
    ↓
Repository / JdbcTemplate (repository/*.java)           — talks to the database
    ↓
Response back to caller
```

**How to Explain:**
"When any request comes in, it first goes through a security filter called `JwtAuthFilter` which reads the `Authorization` header and validates the JWT token. If the token is valid, it sets the user identity. Then Spring Security checks if that user's role has permission to access that route. After passing security, the request lands in the Controller, which hands off the work to a Service class. The Service does all the logic — validation, DB calls, business rules. It uses Repository or JDBC classes to talk to the Oracle database. The result comes back up through the chain and is sent as a JSON response."

---

## 1. AUTH APIs — `AuthController.java` → `AuthService.java`

---

### POST `/backend/api/signIn`

**Route is PUBLIC — no token required.**

| Step | Class | Method | What happens |
|------|-------|--------|--------------|
| 1. Request enters | `AuthController` | `signIn(...)` | Reads credentials from either `Authorization: Basic <base64>` header or JSON body `{username, password}` |
| 2. Credential extraction | `AuthService` | `extractBasicAuth(authHeader)` | Decodes Base64 if Basic Auth header is used |
| 3. Validation check | `AuthController` | `signIn(...)` | Checks username and password are not empty — returns 400 if missing |
| 4. Business logic | `AuthService` | `signIn(username, password, request, response)` | Starts the actual login |
| 5. DB — fetch user | `UserRepository` | `findByUsername(username)` | Looks up user in the `USERS` table — throws 401 if not found |
| 6. Password check | `TokenService` | `verifyPassword(password, storedHash)` | BCrypt-compares entered password with stored hash — throws 401 if wrong |
| 7. Old tokens deleted | `RefreshTokenRepository` | `deleteAllByUsername(...)` | Deletes all existing refresh tokens for that user (forces single session) |
| 8. Access token created | `JwtService` | `generateAccessToken(username, userRole)` | Creates a short-lived JWT with username and role |
| 9. Refresh token created | `JwtService` | `generateRefreshToken(username)` | Creates a long-lived refresh JWT with a unique ID (JTI) |
| 10. Refresh token saved | `RefreshTokenRepository` | `save(refreshToken)` | Stores refresh token record in DB with expiry, IP, and user-agent |
| 11. Cookie set | `AuthService` | `setRefreshCookie(response, token)` | Sets `refreshToken` as an HttpOnly Secure cookie |
| 12. Response returned | `AuthController` | — | Returns `{access_token, token_type: "Bearer", expires_in}` |

**How to Explain:**
"When a user calls Sign In, the request arrives at `AuthController.signIn()`. It checks if credentials came as a Basic Auth header or in the request body. Then it calls `AuthService.signIn()`. The service looks up the user from the database using `UserRepository.findByUsername()`. If the user is not found, it throws a 401 Unauthorized. If found, it calls `TokenService.verifyPassword()` which does a BCrypt comparison — if the password is wrong, another 401. On success, it deletes any old refresh tokens for that user from the database, then calls `JwtService.generateAccessToken()` to create a short-lived access token, and `JwtService.generateRefreshToken()` for a long-lived one. The refresh token is saved in the database and also set as a secure HttpOnly cookie in the response. Finally, the access token is returned in the JSON response body."

---

### POST `/backend/api/refresh`

**Route is PUBLIC — no token required.**

| Step | Class | Method | What happens |
|------|-------|--------|--------------|
| 1. Request enters | `AuthController` | `refresh(request, response)` | No body needed — reads the cookie |
| 2. Business logic | `AuthService` | `refresh(request, response)` | Starts token refresh |
| 3. Cookie read | `AuthService` | `extractRefreshCookie(request)` | Reads `refreshToken` cookie — throws 401 if missing |
| 4. Token verified | `JwtService` | `verifyRefreshToken(cookieToken)` | Validates the refresh JWT signature and expiry — throws 401 if invalid |
| 5. DB check | `RefreshTokenRepository` | `findByJti(jti)` | Looks up the token record in DB by its unique ID (JTI) — throws 401 if not found |
| 6. Expiry check | `AuthService` | `refresh(...)` | Checks if the DB record is still within the expiry time — throws 401 if expired |
| 7. Old token deleted | `RefreshTokenRepository` | `deleteById(jti)` | Removes the used refresh token (one-time use) |
| 8. New tokens issued | `JwtService` | `generateAccessToken(...)` and `generateRefreshToken(...)` | Creates a fresh access and refresh token pair |
| 9. New refresh saved | `RefreshTokenRepository` | `save(newRt)` | Saves the new refresh token in DB |
| 10. Cookie updated | `AuthService` | `setRefreshCookie(response, newToken)` | Replaces the old refresh cookie |
| 11. Response returned | `AuthController` | — | Returns `{token: "<new_access_token>"}` |

**How to Explain:**
"When a client's access token expires and they call `/refresh`, the request enters `AuthController.refresh()`, which passes it to `AuthService.refresh()`. The service reads the `refreshToken` from the HTTP cookie. It verifies the token signature using `JwtService.verifyRefreshToken()`, then does a second check against the database using `RefreshTokenRepository.findByJti()` to confirm the token hasn't been used or revoked. Once confirmed valid, it deletes that old token record (so it can't be reused), generates a fresh access token and a new refresh token, saves the new refresh token to the database, updates the cookie, and returns the new access token in the response."

---

### POST `/backend/api/logout`

**Route is PUBLIC — no token required. (But if user is authenticated, cleans up their session.)**

| Step | Class | Method | What happens |
|------|-------|--------|--------------|
| 1. Request enters | `AuthController` | `logout(request, response, auth)` | Reads current logged-in username from Spring Security context |
| 2. Business logic | `AuthService` | `logout(request, response, username)` | Starts logout |
| 3. Cookie read | `AuthService` | `extractRefreshCookie(request)` | Reads the refresh cookie if present |
| 4. Token decode | `JwtService` | `verifyRefreshToken(...)` or `decodeWithoutVerify(...)` | Tries to verify the cookie token; if expired/invalid, decodes without verification just to get the JTI |
| 5. DB delete by JTI | `RefreshTokenRepository` | `deleteById(jti)` | Deletes the specific token from DB |
| 6. DB delete all | `RefreshTokenRepository` | `deleteAllByUsername(username)` | Deletes ALL tokens for the user (full session wipe) |
| 7. Cookie cleared | `AuthService` | `clearRefreshCookie(response)` | Sets the refresh cookie to expire immediately |
| 8. Response returned | `AuthController` | — | Returns `{message: "Logged out"}` |

**How to Explain:**
"When logout is called, `AuthController.logout()` grabs the current username from the security context and hands off to `AuthService.logout()`. The service reads the refresh token cookie. It tries to verify the token — if it is expired it still decodes it without verification just to get the token's unique ID. It then deletes that specific token from the database, and also deletes ALL refresh tokens for the user as a full session cleanup. Finally, it clears the cookie from the browser and returns a logged out confirmation."

---

### POST `/backend/api/forgot-password`

**Route is PUBLIC.**

| Step | Class | Method | What happens |
|------|-------|--------|--------------|
| 1. Request enters | `AuthController` | `forgotPassword(body)` | Reads `username` from request body |
| 2. Business logic | `AuthService` | `forgotPassword(username)` | Starts the forgot password flow |
| 3. DB check | `UserRepository` | `findByUsername(username)` | Looks up user — throws 404 if not found |
| 4. Reset token created | `JwtService` | `generateResetToken(username)` | Creates a short-lived JWT with `purpose: "password_reset"` |
| 5. Reset link built | `AuthService` | `forgotPassword(...)` | Constructs URL: `prodUrl/stubserver/resetPassword?token=<encoded_token>` |
| 6. Email sent | `MailService` | `sendEmail(email, subject, template, data)` | Sends HTML email using template with the reset link |
| 7. Response returned | `AuthController` | — | Returns `{message: "Reset email sent", token: <resetToken>}` |

**How to Explain:**
"When forgot password is called with a username, `AuthController.forgotPassword()` passes it to `AuthService.forgotPassword()`. The service checks the database to confirm the user exists. If found, it calls `JwtService.generateResetToken()` which creates a special JWT that has the purpose 'password_reset' baked in and a short expiry. It builds a reset link using the production URL and sends an HTML email to the user's registered email address using `MailService.sendEmail()`. The token is also returned in the response."

---

### POST `/backend/api/reset-password`

**Route is PUBLIC.**

| Step | Class | Method | What happens |
|------|-------|--------|--------------|
| 1. Request enters | `AuthController` | `resetPassword(body)` | Reads `token` and `newPassword` from request body |
| 2. Business logic | `AuthService` | `resetPassword(token, newPassword)` | Starts the reset flow |
| 3. Already-used check | `TokenService` | `isResetTokenUsed(token)` | Checks in-memory cache if token was already used — throws 403 if yes |
| 4. Token verified | `JwtService` | `verifyResetToken(token)` | Validates the reset JWT signature and expiry — throws 400 if invalid/expired |
| 5. Purpose check | `AuthService` | `resetPassword(...)` | Confirms JWT claim `purpose == "password_reset"` — throws 400 if wrong |
| 6. DB fetch user | `UserRepository` | `findByUsername(username)` | Loads the user record |
| 7. Password updated | `TokenService` | `hashPassword(newPassword)` | BCrypt-hashes the new password |
| 8. DB save | `UserRepository` | `save(user)` | Updates the user's password in the database |
| 9. Token marked used | `TokenService` | `markResetTokenUsed(token)` | Stores the token in the used-set so it cannot be reused |
| 10. Response returned | `AuthController` | — | Returns `{message: "Password reset successfully"}` |

**How to Explain:**
"When reset password is called, `AuthController.resetPassword()` sends the token and new password to `AuthService.resetPassword()`. First it checks an in-memory used-token store via `TokenService.isResetTokenUsed()` — if the token was already used it throws a 403 Forbidden. Then it calls `JwtService.verifyResetToken()` to validate the JWT signature and expiry. It also verifies that the token's purpose field is 'password_reset' to prevent misuse of other tokens. It loads the user, hashes the new password with BCrypt, saves it to the database, and marks the token as used so it cannot be replayed. Returns success message."

---

### POST `/backend/api/change-password`

**PROTECTED — requires valid access token.**

| Step | Class | Method | What happens |
|------|-------|--------|--------------|
| 1. Token validated | `JwtAuthFilter` | `doFilterInternal(...)` | Validates Bearer token from `Authorization` header |
| 2. Request enters | `AuthController` | `changePassword(body, auth)` | Reads `currentPassword`, `newPassword`, and optionally `token` from body |
| 3. Business logic | `AuthService` | `changePassword(contextUsername, token, currentPassword, newPassword)` | Starts change password |
| 4. Username resolution | `AuthService` | `changePassword(...)` | Uses the authenticated username from security context (or extracts from body token if provided) |
| 5. DB fetch user | `UserRepository` | `findByUsername(username)` | Loads the user record — throws 404 if missing |
| 6. Current password check | `TokenService` | `verifyPassword(currentPassword, storedHash)` | Verifies the current password is correct — throws 400 if wrong |
| 7. Password updated | `TokenService` | `hashPassword(newPassword)` | BCrypt-hashes the new password |
| 8. DB save | `UserRepository` | `save(user)` | Saves the updated password |
| 9. Response returned | `AuthController` | — | Returns `{message: "Password changed successfully"}` |

**How to Explain:**
"Change password requires the user to be already logged in. The request passes through `JwtAuthFilter` which validates the access token and sets the user's identity. `AuthController.changePassword()` passes everything to `AuthService.changePassword()`. The service resolves the username from the security context. It fetches the user from the database, then calls `TokenService.verifyPassword()` to confirm the current password is correct. If it is, it hashes the new password and saves it to the database."

---

## 2. USER MANAGEMENT APIs — `UserController.java` → `UserService.java`

**All routes require role = Admin.**

---

### GET `/backend/api/getUsersList`

| Step | Class | Method | What happens |
|------|-------|--------|--------------|
| 1. Token validated | `JwtAuthFilter` | `doFilterInternal(...)` | Validates token, sets role |
| 2. Role checked | `SecurityConfig` | `@PreAuthorize("hasRole('Admin')")` | Rejects if not Admin |
| 3. Request enters | `UserController` | `getUsersList()` | No input needed |
| 4. Business logic | `UserService` | `getUsersList()` | Fetches all users |
| 5. DB call | `UserRepository` | `findAll()` | Returns all rows from USERS table |
| 6. Filter + sort | `UserService` | `getUsersList()` | Removes the 'admin' account, sorts alphabetically by username |
| 7. Response returned | `UserController` | — | Returns list of `{USERNAME, EMAIL, FIRSTNAME, LASTNAME, USERROLE}` |

**How to Explain:**
"When admin calls Get Users List, after token and role validation, `UserController.getUsersList()` calls `UserService.getUsersList()`. The service calls `UserRepository.findAll()` to get all users from the database, then filters out the 'admin' super-user account and sorts the rest alphabetically. The cleaned list is returned as JSON."

---

### POST `/backend/api/signUp`

| Step | Class | Method | What happens |
|------|-------|--------|--------------|
| 1. Token + role check | `JwtAuthFilter` + `@PreAuthorize` | — | Must be Admin |
| 2. Request enters | `UserController` | `signUp(body)` | Reads `username, email, firstname, lastname, requestedBy, userrole` |
| 3. Business logic | `UserService` | `signUp(...)` | Starts user creation |
| 4. Admin verification | `UserService` | `verifyAdmin(requestedBy)` | DB checks if `requestedBy` user is actually an Admin — throws 403 if not |
| 5. Duplicate check | `UserRepository` | `existsByUsernameOrEmail(username, email)` | Throws 400 if username or email already exists |
| 6. Password generated | `UserService` | `randomPassword()` | Generates a random 6-char alphanumeric password |
| 7. Password hashed | `TokenService` | `hashPassword(autoPass)` | BCrypt-hashes the auto-generated password |
| 8. DB save | `UserRepository` | `save(user)` | Inserts new user into the database |
| 9. Welcome email | `MailService` | `sendEmail(...)` | Sends account creation email with the temporary password |
| 10. Response returned | `UserController` | — | Returns `{message: "User created and password sent to email."}` |

**How to Explain:**
"When admin creates a new user, the request goes to `UserController.signUp()` and then `UserService.signUp()`. The service does a double admin check — it not only checks the JWT role but also queries the database to confirm the `requestedBy` person is truly an Admin. Then it checks for duplicate username or email. If all good, it generates a random 6-character password, hashes it with BCrypt, saves the user to the database, and sends a welcome email to the new user with their temporary password. The login credentials are never returned in the API response — they only go via email."

---

### POST `/backend/api/signUp-modify`

| Step | Class | Method | What happens |
|------|-------|--------|--------------|
| 1. Token + role check | `JwtAuthFilter` + `@PreAuthorize` | — | Must be Admin |
| 2. Request enters | `UserController` | `modifyUser(body)` | Reads `username, firstname, lastname, email, updatedBy, userrole` |
| 3. Business logic | `UserService` | `modifyUser(...)` | Starts modification |
| 4. Admin verification | `UserService` | `verifyAdmin(updatedBy)` | DB checks if updater is Admin |
| 5. DB fetch user | `UserRepository` | `findByUsername(username)` | Loads the target user — throws 404 if not found |
| 6. No-change check | `UserService` | `modifyUser(...)` | Compares old vs new values — returns "No changes" message if identical |
| 7. DB save | `UserRepository` | `save(user)` | Updates user fields and sets `updatedAt` timestamp |
| 8. Response returned | `UserController` | — | Returns `{message: "User updated successfully"}` |

**How to Explain:**
"When admin modifies a user, `UserController.modifyUser()` passes the data to `UserService.modifyUser()`. It verifies the admin identity in the database, loads the target user, checks if anything actually changed (if nothing changed it returns early with 'No changes'), then saves the updated details to the database with an updated timestamp."

---

### DELETE `/backend/api/delete`

| Step | Class | Method | What happens |
|------|-------|--------|--------------|
| 1. Token + role check | `JwtAuthFilter` + `@PreAuthorize` | — | Must be Admin |
| 2. Request enters | `UserController` | `deleteUser(body)` | Reads `username` and `requestedBy` from body |
| 3. Business logic | `UserService` | `deleteUser(username, requestedBy)` | Starts deletion |
| 4. Admin verification | `UserService` | `verifyAdmin(requestedBy)` | DB checks if requestedBy is Admin |
| 5. Existence check | `UserRepository` | `existsById(username)` | Throws 404 if user not found |
| 6. DB delete | `UserRepository` | `deleteById(username)` | Removes user from database |
| 7. Response returned | `UserController` | — | Returns `{message: "User deleted successfully"}` |

**How to Explain:**
"Delete user flows through `UserController.deleteUser()` to `UserService.deleteUser()`. It first verifies the requesting user is an Admin via a database check. Then checks if the target user exists. If yes, it calls `UserRepository.deleteById()` to remove the user from the database."

---

## 3. SERVER APIs — `ServerController.java` → `ServerService.java`

---

### GET `/backend/api/getServerLists?environment=QA`

**Requires Admin role.**

| Step | Class | Method | What happens |
|------|-------|--------|--------------|
| 1. Token + role check | `JwtAuthFilter` + `@PreAuthorize` | — | Must be Admin |
| 2. Request enters | `ServerController` | `getServerLists(environment)` | Reads `environment` query param |
| 3. Business logic | `ServerService` | `getServerLists(environment)` | Validates environment is QA or UAT — throws 400 if not |
| 4. Table resolved | `TableNames` | `auditTable(environment)` | Picks the right DB table name based on environment |
| 5. DB call | `EnvTableRepository` | `getAuditLogsByEnvForServerList(auditTable)` | Fetches latest audit log entries for the server |
| 6. Status determined | `ServerService` | `getServerLists(...)` | If latest row is "SERVER START" → status is "Running", else "Stopped" |
| 7. Response returned | `ServerController` | — | Returns `[{SERVERNAME, PORT, STATUS, LASTUPDATE, ENVIRONMENT}]` |

**How to Explain:**
"Get Server Lists reads the `environment` query parameter (QA or UAT), validates it, then resolves the correct audit table name using `TableNames`. It queries the audit logs for that environment to find the most recent action — if the last action was 'SERVER START' it reports the server as Running, otherwise Stopped. The server name and port come from application configuration."

---

### POST `/backend/api/run-batch`

**Requires Admin role.**

| Step | Class | Method | What happens |
|------|-------|--------|--------------|
| 1. Token + role check | `JwtAuthFilter` + `@PreAuthorize` | — | Must be Admin |
| 2. Request enters | `ServerController` | `runBatch(body)` | Reads `action` (Start/Stop) and `servername` |
| 3. Business logic | `ServerService` | `runBatch(action, servername)` | Validates inputs — throws 400 if missing |
| 4a. If Stop | `ServerService` | `runBatch(...)` | Reads `JAVA_STOP_BAT` path from config, runs `cmd.exe /c start cmd /c <bat>` as a system process |
| 4b. If Start | `ServerService` | `runBatch(...)` | Checks if already running, reads `JAVA_START_BAT` path, runs it as a background process |
| 5. State updated | `ServerService` | `runBatch(...)` | Sets `javaAppRunning` flag in memory (AtomicBoolean) |
| 6. Response returned | `ServerController` | — | Returns `{message: "Server Started"}` or `{message: "Server Stopped"}` |

**How to Explain:**
"Run Batch is how the admin starts or stops the StubServer application. The request goes to `ServerController.runBatch()`, then to `ServerService.runBatch()`. Based on the `action` field, it reads the configured `.bat` file path from application properties and runs it using Java's `ProcessBuilder` — which triggers a system command (Windows `.bat` file). If Start is called and the server is already running (tracked in memory via an AtomicBoolean), it skips and returns 'already running'. The response just confirms the action."

---

### GET `/backend/api/getLiveStatus`

**Requires Admin role.**

| Step | Class | Method | What happens |
|------|-------|--------|--------------|
| 1. Token + role check | `JwtAuthFilter` + `@PreAuthorize` | — | Must be Admin |
| 2. Request enters | `ServerController` | `getLiveStatus()` | No input needed |
| 3. Business logic | `ServerService` | `getLiveStatus()` | Checks if server port is in use via `netstat` command |
| 4. Port check | `ServerService` | `isPortInUse(port)` | Runs `netstat -aon` and searches for the configured port in LISTENING state |
| 5. Health check | `ServerService` | `checkHealth(healthApi)` | If port is in use, does an HTTP GET to the health API URL and expects response body = "success" |
| 6. Start time tracked | `ServerService` | `getLiveStatus()` | First time health passes, captures current time as `serverStartTime` |
| 7. Response returned | `ServerController` | — | Returns `{liveStatus: "Running", upAndRunning: "<timestamp>"}` or `{liveStatus: "Stopped"}` |

**How to Explain:**
"Get Live Status does a real-time check of whether the StubServer application is running. It calls `ServerService.getLiveStatus()`, which first runs a `netstat` command on the server's OS to check if the configured port is actively listening. If the port is free, it immediately returns Stopped. If the port is in use, it makes an HTTP GET call to the configured health check URL — if that URL returns the text 'success', the server is Running. The first time it passes health, the current time is saved as the start time, which is also included in the response."

---

### GET `/backend/api/serverTimeInfo`

**Requires Admin, ApplicationUser, or Guest role.**

| Step | Class | Method | What happens |
|------|-------|--------|--------------|
| 1. Token + role check | `JwtAuthFilter` + `@PreAuthorize` | — | Any logged-in role |
| 2. Request enters | `ServerController` | `serverTimeInfo()` | No input needed |
| 3. Business logic | `ServerService` | `serverTimeInfo()` | Gets current system timezone and local time |
| 4. Response returned | `ServerController` | — | Returns `{serverTimeZone: "IST", serverLocalTime: "2025-06-01T10:00:00"}` |

**How to Explain:**
"Server Time Info is a simple utility API that returns the current server time and timezone. It calls `ServerService.serverTimeInfo()`, which reads the JVM's system default timezone, extracts the abbreviation (like IST or UTC), formats the current local time, and returns both. This is used by the front end to display time-based information relative to the server's actual time."

---

## 4. CATALOG APIs — `CatalogController.java` → `CatalogService.java`

---

### GET `/backend/api/getServiceGroupTagsList`

**Requires Admin role.**

| Step | Class | Method | What happens |
|------|-------|--------|--------------|
| 1. Token + role check | `JwtAuthFilter` + `@PreAuthorize` | — | Must be Admin |
| 2. Request enters | `CatalogController` | `getServiceGroupTagsList()` | No input needed |
| 3. Business logic | `CatalogService` | `getServiceGroupTagsList()` | Queries the catalog table |
| 4. DB call | `NamedParameterJdbcTemplate` | SQL on `READYAPI_VS_CATALOG` | Fetches VSNAME, GROUP, TAGS for all rows |
| 5. Tags split | `CatalogService` | `getServiceGroupTagsList()` | Splits comma-separated TAGS string into a list |
| 6. Response returned | `CatalogController` | — | Returns `{totalService: N, services: [{servicename, group, tags:[...]}]}` |

**How to Explain:**
"Get Service Group Tags List fetches all virtual services from the `READYAPI_VS_CATALOG` table. The service calls a JDBC query, then processes the raw data — the TAGS column is stored as a comma-separated string in the database, so the service splits it into a proper array. Returns a list of services with their group and tags, plus the total count."

---

### POST `/backend/api/masterCatalog/check`

**Requires Admin role.**

| Step | Class | Method | What happens |
|------|-------|--------|--------------|
| 1. Token + role check | `JwtAuthFilter` + `@PreAuthorize` | — | Must be Admin |
| 2. Request enters | `CatalogController` | `checkMasterCatalog(body)` | Reads `serviceName`, `port`, `environment` |
| 3. Business logic | `CatalogService` | `checkMasterCatalog(serviceName, port, environment)` | Validates environment, validates that at least one of serviceName or port is provided |
| 4. Table resolved | `TableNames` | `masterCatalogTable(env)` | Gets the right catalog table for QA or UAT |
| 5. DB call | `EnvTableRepository` | `checkMasterCatalog(tableName, serviceName, port)` | Runs a query that returns `NAME_MATCH` and `PORT_MATCH` flags |
| 6. Response returned | `CatalogController` | — | Returns `{environment, serviceName, port, exists, nameMatch, portMatch}` |

**How to Explain:**
"Check Master Catalog lets you verify if a service name or port already exists in the master catalog for QA or UAT. `CatalogService.checkMasterCatalog()` resolves the right environment table via `TableNames`, then queries the database with both the service name and port. The DB query returns separate flags for whether the name matched and whether the port matched. The response tells you exactly which one matched, or if neither exists."

---

## 5. SERVICE MANAGEMENT APIs — `ServiceController.java` → `ServiceManagementService.java`

---

### POST `/backend/api/assignServices`

**Requires Admin role.**

| Step | Class | Method | What happens |
|------|-------|--------|--------------|
| 1. Token + role check | `JwtAuthFilter` + `@PreAuthorize` | — | Must be Admin |
| 2. Request enters | `ServiceController` | `assignServices(body)` | Reads `username`, `assignedServices` (list), `environment` |
| 3. Business logic | `ServiceManagementService` | `assignServices(username, services, environment)` | Starts assignment |
| 4. Table resolved | `TableNames` | `assignedServicesTable(environment)` | Gets the right assignment table for the environment |
| 5. DB delete | `EnvTableRepository` | `deleteAssignedServices(table, username)` | Removes all existing service assignments for the user in that environment |
| 6. DB insert | `EnvTableRepository` | `insertAssignedServices(table, username, services)` | Inserts the new list of services for the user |
| 7. Response returned | `ServiceController` | — | Returns `{message: "Services assigned successfully in <table>"}` |

**How to Explain:**
"Assign Services is a replace operation — it fully replaces a user's service assignments in a given environment. `ServiceManagementService.assignServices()` resolves the right database table using `TableNames`, then deletes all existing assignments for that user, and inserts the new list. If an empty list is passed, the user ends up with no assigned services."

---

### POST `/backend/api/getAssignedServices`

**Requires Admin or ApplicationUser role.**

| Step | Class | Method | What happens |
|------|-------|--------|--------------|
| 1. Token + role check | `JwtAuthFilter` + `@PreAuthorize` | — | Admin or ApplicationUser |
| 2. Request enters | `ServiceController` | `getAssignedServices(body)` | Reads `USERNAME` and `environment` |
| 3. Business logic | `ServiceManagementService` | `getAssignedServices(username, environment)` | Resolves table and queries DB |
| 4. DB call | `EnvTableRepository` | `getAssignedServices(table, username)` | Returns list of service names assigned to the user |
| 5. Response returned | `ServiceController` | — | Returns `{username, assignedService: [...]}` |

**How to Explain:**
"Get Assigned Services queries the database for all services assigned to a specific user in a specific environment. It resolves the correct table name, runs the query, and returns the username along with their list of assigned service names."

---

### GET `/backend/api/getGroupTagsConfig?serviceName=X&env=QA`

**Requires Admin or ApplicationUser role.**

| Step | Class | Method | What happens |
|------|-------|--------|--------------|
| 1. Token + role check | `JwtAuthFilter` + `@PreAuthorize` | — | Admin or ApplicationUser |
| 2. Request enters | `ServiceController` | `getGroupTagsConfig(serviceName, env)` | Reads query params |
| 3. Business logic | `ServiceManagementService` | `getGroupTagsConfig(serviceName, env)` | Defaults env to QA if not provided |
| 4. Table resolved | `TableNames` | `vsDetailsTable(environment)` | Gets VS details table for the environment |
| 5. DB call | `EnvTableRepository` | `getGroupTagsConfig(table, serviceName)` | Fetches GROUP and TAGS for that service |
| 6. Tags split | `ServiceManagementService` | `getGroupTagsConfig(...)` | Splits comma-separated TAGS into a list |
| 7. Response returned | `ServiceController` | — | Returns `{group, tags: [...], env}` |

**How to Explain:**
"Get Group Tags Config fetches the group and tags configuration for a specific service in a specific environment. It queries the VS details table for that environment, then splits the comma-separated tags into an array. If no environment is provided, it defaults to QA."

---

### POST `/backend/api/updateGroup`

**Requires Admin role.**

| Step | Class | Method | What happens |
|------|-------|--------|--------------|
| 1. Token + role check | `JwtAuthFilter` + `@PreAuthorize` | — | Must be Admin |
| 2. Request enters | `ServiceController` | `updateGroup(body)` | Reads `serviceName`, `group`, `env` |
| 3. Business logic | `ServiceManagementService` | `updateGroup(serviceName, group, env)` | Validates env is QA or UAT |
| 4. DB update | `EnvTableRepository` | `updateGroup(table, serviceName, groupVal)` | Updates the GROUP column — sets null if group is empty |
| 5. Response returned | `ServiceController` | — | Returns `{success: true}` |

**How to Explain:**
"Update Group sets or clears the group label for a service in a given environment. The service validates the environment, resolves the table, and calls a DB update. If the provided group value is blank or empty, it sets the group to null in the database — effectively removing it."

---

### POST `/backend/api/updateTags`

**Requires Admin role.**

| Step | Class | Method | What happens |
|------|-------|--------|--------------|
| 1. Token + role check | `JwtAuthFilter` + `@PreAuthorize` | — | Must be Admin |
| 2. Request enters | `ServiceController` | `updateTags(body)` | Reads `serviceName`, `tags` (array), `env` |
| 3. Business logic | `ServiceManagementService` | `updateTags(serviceName, tags, env)` | Validates env, trims tags, checks for duplicates — throws 400 if any duplicates |
| 4. Tags joined | `ServiceManagementService` | `updateTags(...)` | Joins array back to comma-separated string (or null if empty) |
| 5. DB update | `EnvTableRepository` | `updateTags(table, serviceName, tagString)` | Updates TAGS column |
| 6. Response returned | `ServiceController` | — | Returns `{success: true}` |

**How to Explain:**
"Update Tags replaces the entire tags list for a service. The service trims whitespace from each tag, checks for duplicates (throws a 400 if any found), joins the array into a comma-separated string, and saves it to the TAGS column in the database. If an empty array is passed, TAGS is set to null."

---

### GET `/backend/api/getDatasourceLists?environment=QA`

**Requires Admin or ApplicationUser role.**

| Step | Class | Method | What happens |
|------|-------|--------|--------------|
| 1. Token + role check | `JwtAuthFilter` + `@PreAuthorize` | — | Admin or ApplicationUser |
| 2. Request enters | `ServiceController` | `getDatasourceLists(environment)` | Reads env query param |
| 3. Business logic | `ServiceManagementService` | `getDatasourceLists(environment)` | Validates env, resolves table |
| 4. DB call | `EnvTableRepository` | `getDatasourceEnabled(table)` | Returns services where datasource is enabled |
| 5. Response returned | `ServiceController` | — | Returns `{environment, data: [{serviceName, datasourceEnabled: true}]}` |

**How to Explain:**
"Get Datasource Lists returns the list of services that have datasource enabled in a given environment. It queries the VS details table for the environment, filters for services where the datasource flag is enabled, and returns those as a list."

---

### GET `/backend/api/getDatasets?serviceName=X`

**Requires Admin or ApplicationUser role.**

| Step | Class | Method | What happens |
|------|-------|--------|--------------|
| 1. Token + role check | `JwtAuthFilter` + `@PreAuthorize` | — | Admin or ApplicationUser |
| 2. Request enters | `ServiceController` | `getDatasets(serviceName)` | Reads serviceName query param |
| 3. Business logic | `ServiceManagementService` | `getDatasets(serviceName)` | Security check via `FilePathGuard.isSafeFileName()` — rejects if path traversal attempted |
| 4. File scan | `ServiceManagementService` | `getDatasets(...)` | Reads configured dataset root directories, looks for `<root>/<serviceName>/` folder |
| 5. Directory read | Java NIO `Files.newDirectoryStream` | — | Lists all regular files in that folder |
| 6. File attributes | Java NIO `Files.readAttributes` | — | Reads each file's name, size, and last modified time |
| 7. Sort | `ServiceManagementService` | `getDatasets(...)` | Sorts by last modified time, newest first |
| 8. Response returned | `ServiceController` | — | Returns `{status, serviceName, count, datasetFiles: [{fileName, size, lastModified}]}` |

**How to Explain:**
"Get Datasets reads files from the filesystem — specifically from a configured dataset directory. The service name is validated for safety to prevent path traversal attacks. It then scans the service's folder across one or more configured root directories, collects file metadata (name, size, last modified), sorts them newest first, and returns the list. If no files are found, it throws a 404."

---

### GET `/backend/api/getDatasets/download?serviceName=X&fileName=Y`

**Requires Admin or ApplicationUser role.**

| Step | Class | Method | What happens |
|------|-------|--------|--------------|
| 1. Token + role check | `JwtAuthFilter` + `@PreAuthorize` | — | Admin or ApplicationUser |
| 2. Request enters | `ServiceController` | `downloadDataset(serviceName, fileName, response)` | Reads query params |
| 3. File location | `ServiceManagementService` | `findDatasetFile(serviceName, fileName)` | Validates both names for safety, scans configured roots to find the actual file |
| 4. Stream response | `ServiceController` | `downloadDataset(...)` | Sets content-disposition header as attachment, streams the file bytes to response |

**How to Explain:**
"Download Dataset locates the requested file on the filesystem using `ServiceManagementService.findDatasetFile()`, which validates the file names are safe (no `..` path traversal) and scans the configured directories. Once found, the file bytes are streamed directly to the HTTP response as a download attachment."

---

### DELETE `/backend/api/getDatasets/delete?serviceName=X&fileName=Y`

| Step | Class | Method | What happens |
|------|-------|--------|--------------|
| 1. Token + role check | `JwtAuthFilter` + `@PreAuthorize` | — | Admin or ApplicationUser |
| 2. Request enters | `ServiceController` | `deleteDataset(serviceName, fileName)` | Reads query params |
| 3. File location | `ServiceManagementService` | `findDatasetFile(serviceName, fileName)` | Finds the file (throws 404 if not found) |
| 4. File deleted | `Files.delete(file)` | — | Deletes the file from the filesystem |
| 5. Response returned | `ServiceController` | — | Returns `{status, message, serviceName, fileName}` |

**How to Explain:**
"Delete Dataset first locates the file using the same `findDatasetFile()` method as download — which validates names and finds the actual path. Then it calls Java NIO `Files.delete()` to remove it from the filesystem. If the file is not found, a 404 is returned before deletion is attempted."

---

## 6. METRICS APIs — `MetricsController.java` → `MetricsService.java`

**All Metrics APIs require Admin role.**

---

### GET `/backend/api/getLifeTimeHits`

| Step | Class | Method | What happens |
|------|-------|--------|--------------|
| 1. Token + role check | `JwtAuthFilter` + `@PreAuthorize` | — | Must be Admin |
| 2. Request enters | `MetricsController` | `getLifetimeHits()` | No input needed |
| 3. Business logic | `MetricsService` | `getLifetimeHits()` | Runs a SQL query |
| 4. DB call | `NamedParameterJdbcTemplate` | SQL UNION on `READYAPI_MONTHLY_METRICS` + `READYAPI_DAILY_METRICS` | Combines all historical monthly data with current-month daily data to get total hits per service |
| 5. Response returned | `MetricsController` | — | Returns list of `{serviceName, counts: {total, qa, uat}}` |

**How to Explain:**
"Get Lifetime Hits runs a SQL UNION query that adds up all historical monthly metric records with the current month's daily records to produce a grand total hit count per service. The counts are broken down into total, QA-environment, and UAT-environment hits."

---

### POST `/backend/api/getMonthlyHits`

| Step | Class | Method | What happens |
|------|-------|--------|--------------|
| 1. Token + role check | `JwtAuthFilter` + `@PreAuthorize` | — | Must be Admin |
| 2. Request enters | `MetricsController` | `getMonthlyHits(body)` | Reads `fromMonth` and `toMonth` (format: `MON-YYYY`) |
| 3. Validation | `MetricsService` | `getMonthlyHits(fromMonth, toMonth)` | Throws 400 if either is null |
| 4. DB call | `NamedParameterJdbcTemplate` | SQL on `READYAPI_MONTHLY_METRICS` | Filters by date range and groups by service, month, year |
| 5. Response returned | `MetricsController` | — | Returns `{data: [{serviceName, month, year, totalCount, totalQACount, totalUATCount}]}` |

**How to Explain:**
"Get Monthly Hits takes a from-month and to-month range in `MON-YYYY` format, queries the monthly metrics table, and returns per-service per-month hit counts broken down by QA and UAT environments."

---

### POST `/backend/api/getCustomReport`

| Step | Class | Method | What happens |
|------|-------|--------|--------------|
| 1. Token + role check | `JwtAuthFilter` + `@PreAuthorize` | — | Must be Admin |
| 2. Request enters | `MetricsController` | `getCustomReport(body)` | Reads `fromDate` and `toDate` (format: `DD/MM/YYYY`) |
| 3. Validation | `MetricsService` | `getCustomReport(fromDate, toDate)` | Throws 400 if either is null |
| 4. DB call | `NamedParameterJdbcTemplate` | SQL on `READYAPI_DAILY_METRICS` | Filters by date range, groups by service and date |
| 5. Response returned | `MetricsController` | — | Returns list of `{serviceName, transDate, counts: {total, qa, uat}}` |

**How to Explain:**
"Get Custom Report is a day-level report. You provide a `fromDate` and `toDate` in `DD/MM/YYYY` format. It queries the daily metrics table, groups by service and transaction date, and returns QA and UAT counts per day per service."

---

### POST `/backend/api/getDormantServiceLists`

| Step | Class | Method | What happens |
|------|-------|--------|--------------|
| 1. Token + role check | `JwtAuthFilter` + `@PreAuthorize` | — | Must be Admin |
| 2. Request enters | `MetricsController` | `getDormantServiceLists(body)` | Reads `serverIP` |
| 3. Validation | `MetricsService` | `getDormantServiceLists(serverIP)` | Throws 400 if serverIP is empty |
| 4. DB call | Complex SQL with CTEs | — | Joins `READYAPI_VS_CATALOG`, `READYAPI_DAILY_METRICS`, `READYAPI_MONTHLY_METRICS` to compute 3-month and 6-month hit counts for Active services on that server |
| 5. Categorize | `MetricsService` | `getDormantServiceLists(...)` | Groups services into `count_0`, `count_1_50`, `count_51_100` for both 3-month and 6-month windows |
| 6. Response returned | `MetricsController` | — | Returns `{last_3_months: {...}, last_6_months: {...}}` |

**How to Explain:**
"Get Dormant Services finds services that are Active in the catalog but have very low or no usage over the past 3 and 6 months. The SQL query uses CTEs to cross-join the catalog, daily metrics, and monthly metrics for a specific server IP. Services are then categorized by hit count range — zero hits, 1 to 50 hits, or 51 to 100 hits — for both the 3-month and 6-month windows. This helps identify services that may no longer be needed."

---

### POST `/backend/api/getResponseTime`

| Step | Class | Method | What happens |
|------|-------|--------|--------------|
| 1. Token + role check | `JwtAuthFilter` + `@PreAuthorize` | — | Must be Admin |
| 2. Request enters | `MetricsController` | `getResponseTime(body)` | Reads `serviceName`, `serverIP`, `fromDateTime`, `toDateTime` (format: `YYYY-MM-DDTHH:mm`) |
| 3. Validation | `MetricsService` | `getResponseTime(...)` | Validates all fields present; validates datetime format with regex |
| 4. Timezone convert | `DateTimeUtil` | `istToUtcMinuteString(...)` | Converts IST datetime input to UTC for the DB query |
| 5. DB call | `NamedParameterJdbcTemplate` | SQL JOIN on `READYAPI_RESPONSE_TIME` + `READYAPI_DAILY_METRICS` | Fetches response time records for the service within the time range |
| 6. Response returned | `MetricsController` | — | Returns `{total: N, data: [{VSNAME, VIRTSERVERNAME, STARTTIME, ENDTIME, AVGRESPTIME, MAXRESPTIME, AVGTPS}]}` |

**How to Explain:**
"Get Response Time returns the response time records for a service on a specific server within a given time window. The datetime inputs are in IST format and the service converts them to UTC using `DateTimeUtil.istToUtcMinuteString()` before querying the database — because the database stores timestamps in UTC. The query joins the response time table with the daily metrics table to filter by service name and server IP, returning average response time, max response time, and TPS for each recorded interval."

---

## 7. AUDIT APIs — `AuditController.java` → `AuditService.java`

---

### GET `/backend/api/getAuditLogs?environment=QA`

**Requires Admin or ApplicationUser role.**

| Step | Class | Method | What happens |
|------|-------|--------|--------------|
| 1. Token + role check | `JwtAuthFilter` + `@PreAuthorize` | — | Admin or ApplicationUser |
| 2. Request enters | `AuditController` | `getAuditLogs(environment)` | Reads `environment` query param |
| 3. Validation | `AuditService` | `validateEnv(environment)` | Throws 400 if not QA or UAT |
| 4. Table resolved | `TableNames` | `auditTable(environment)` | Picks the correct audit table |
| 5. DB call | `EnvTableRepository` | `getAuditLogs(table)` | Fetches all audit log rows |
| 6. Response returned | `AuditController` | — | Returns `{logs: [{id, user, serviceName, action, remark, timestamp}]}` |

**How to Explain:**
"Get Audit Logs fetches the complete audit history for a given environment. The environment determines which audit table to query. The service validates the environment, resolves the table name via `TableNames`, queries the database, and returns the full log list."

---

### POST `/backend/api/logAudit`

**Requires Admin, ApplicationUser, or Guest role.**

| Step | Class | Method | What happens |
|------|-------|--------|--------------|
| 1. Token + role check | `JwtAuthFilter` + `@PreAuthorize` | — | Any logged-in role |
| 2. Request enters | `AuditController` | `logAudit(body)` | Reads `username`, `serviceName`, `action`, `environment`, `remark` |
| 3. Validation | `AuditService` | `logAudit(...)` | Throws 400 if any required field is missing |
| 4. Remark handling | `AuditService` | `logAudit(...)` | Only stores remark for specific actions (deploy, re-deploy, response delay, dataset upload, dataset delete); others get "-" |
| 5. DB insert | `EnvTableRepository` | `insertAuditLog(table, username, serviceName, action, remark)` | Inserts a new row into the audit log table |
| 6. Response returned | `AuditController` | — | Returns `{message: "Audit log inserted"}` |

**How to Explain:**
"Log Audit records a user action against a service in the audit table. The service checks that all required fields are present, then decides whether to store the remark — only specific action types like 'deploy', 're-deploy', and 'dataset upload' store the remark; for all other actions, the remark is saved as a dash. The record is then inserted into the environment-specific audit table."

---

## 8. LOG APIs — `LogController.java` → `LogService.java`

**All Log APIs require Admin role.**

---

### POST `/backend/api/listLogFiles`

| Step | Class | Method | What happens |
|------|-------|--------|--------------|
| 1. Token + role check | `JwtAuthFilter` + `@PreAuthorize` | — | Must be Admin |
| 2. Request enters | `LogController` | `listLogFiles(body)` | Reads `logType`, `startDateTime`, `endDateTime` |
| 3. Business logic | `LogService` | `listLogFiles(logType, startDateTime, endDateTime)` | Validates inputs, reads configured log directory |
| 4. File scan | `Files.list(Path.of(logDir))` | — | Lists all files in the log directory |
| 5. Filter | `LogService` | `listLogFiles(...)` | If `logType=error`, includes only `stubserver-error*` files; otherwise all `stubserver*` (non-error) files |
| 6. Date filter | `LogService` | `listLogFiles(...)` | Keeps files whose last-modified time falls within the start-to-end window |
| 7. Response returned | `LogController` | — | Returns list of `{name, size, modifiedDate}` |

**How to Explain:**
"List Log Files scans the server's log directory on the filesystem. Based on the `logType` (error or regular), it filters files by name prefix. Then it further filters by the modified-date window provided. The result is a list of matching log file metadata — name, size, and last modified date — that the admin can then choose to download."

---

### GET `/backend/api/downloadSingleLog?fileName=X`

| Step | Class | Method | What happens |
|------|-------|--------|--------------|
| 1. Token + role check | `JwtAuthFilter` + `@PreAuthorize` | — | Must be Admin |
| 2. Request enters | `LogController` | `downloadSingleLog(fileName, response)` | Reads `fileName` query param |
| 3. Safety check | `FilePathGuard` | `isSafeFileName(fileName)` | Validates no `..` or `/` path traversal — throws 400 if unsafe |
| 4. Path check | `FilePathGuard` | `isPathInside(logDir, filePath)` | Confirms resolved path is inside the log directory |
| 5. Existence check | `Files.exists(filePath)` | — | Throws 404 if file not found |
| 6. Stream response | `Files.copy(filePath, response.getOutputStream())` | — | Streams file bytes as download |

**How to Explain:**
"Download Single Log validates the file name for path traversal safety, confirms the resolved path is actually inside the log directory (double safety check), and if the file exists, streams it directly to the client as a download attachment."

---

### POST `/backend/api/downloadSelectedLogs`

| Step | Class | Method | What happens |
|------|-------|--------|--------------|
| 1. Token + role check | `JwtAuthFilter` + `@PreAuthorize` | — | Must be Admin |
| 2. Request enters | `LogController` | `downloadSelectedLogs(body, response)` | Reads `files` (list of file names) |
| 3. Business logic | `LogService` | `downloadSelectedLogs(files, response)` | Validates each file name individually for safety |
| 4. Sort + limit | `LogService` | `downloadSelectedLogs(...)` | Sorts by modified time (newest first), limits to max 120 files |
| 5. Zip creation | `ZipUtil` | `writeZip(paths, outputStream)` | Streams all selected files into a single ZIP archive |
| 6. Response returned | `LogController` | — | Streams the ZIP with headers `X-Limit-Applied`, `X-Original-Count`, `X-Selected-Count` |

**How to Explain:**
"Download Selected Logs accepts a list of file names and returns them as a single ZIP archive. Each file name is validated for path safety. Files are sorted by newest-first and capped at 120 to prevent large downloads. The response is a streamed ZIP file, and custom headers tell the client if the 120-file limit was applied."

---

### GET `/backend/api/reqresp/getLogFiles?serviceName=X`

| Step | Class | Method | What happens |
|------|-------|--------|--------------|
| 1. Token + role check | `JwtAuthFilter` + `@PreAuthorize` | — | Must be Admin |
| 2. Request enters | `LogController` | `getReqRespLogFiles(serviceName)` | Reads `serviceName` query param |
| 3. Business logic | `LogService` | `getReqRespLogFiles(serviceName)` | Reads the configured reqresp log directory |
| 4. File scan + filter | `Files.list(...)` | — | Includes only files that start with `serviceName` and end with `.log` |
| 5. Response returned | `LogController` | — | Returns `{status, serviceName, logFiles: [{fileName, lastModified}]}` |

**How to Explain:**
"Get Req/Resp Log Files lists request-response log files for a specific service. The log directory is separate from the main server logs. It scans the directory and returns only files whose name matches `<serviceName>*.log`."

---

### GET `/backend/api/reqresp/downloadLogFile?fileName=X`

| Step | Class | Method | What happens |
|------|-------|--------|--------------|
| 1. Token + role check | `JwtAuthFilter` + `@PreAuthorize` | — | Must be Admin |
| 2. Request enters | `LogController` | `downloadReqRespLogFile(fileName, response)` | Reads `fileName` param |
| 3. Business logic | `LogService` | `downloadReqRespLogFile(fileName, response)` | Locates file in reqresp log directory — throws 404 if not found |
| 4. Stream response | `Files.copy(filePath, response.getOutputStream())` | — | Streams the file as a download |

---

### GET `/backend/api/reqresp/downloadAllLogs`

| Step | Class | Method | What happens |
|------|-------|--------|--------------|
| 1. Token + role check | `JwtAuthFilter` + `@PreAuthorize` | — | Must be Admin |
| 2. Request enters | `LogController` | `downloadAllReqRespLogs(response)` | No params needed |
| 3. Business logic | `LogService` | `downloadAllReqRespLogs(response)` | Lists all `.log` files in the reqresp directory — throws 404 if empty |
| 4. Zip creation | `ZipUtil` | `writeZip(logFiles, outputStream)` | Zips all files into one archive |
| 5. Response returned | `LogController` | — | Streams a ZIP file named `all_logs_<timestamp>.zip` |

**How to Explain:**
"Download All Req/Resp Logs collects every `.log` file from the request-response log directory and zips them all into a single archive. The zip file name includes a timestamp to make each download unique."

---

## 9. PORT APIs — `PortController.java` → `PortService.java`

**All Port APIs require Admin role.**

---

### GET `/backend/api/getAppPortLists`

| Step | Class | Method | What happens |
|------|-------|--------|--------------|
| 1. Token + role check | `JwtAuthFilter` + `@PreAuthorize` | — | Must be Admin |
| 2. Request enters | `PortController` | `getAppPortLists()` | No input needed |
| 3. Business logic | `PortService` | `getAppPortLists()` | Fetches all port range records |
| 4. DB call | `PortRangeRepository` | `findAll()` | Returns all rows from port range table |
| 5. Sort | `PortService` | `getAppPortLists()` | Sorts alphabetically by app name |
| 6. Response returned | `PortController` | — | Returns `{totalApp: N, data: [{PORTID, APPNAME, PORTS}]}` |

---

### POST `/backend/api/addAppPortDetails`

| Step | Class | Method | What happens |
|------|-------|--------|--------------|
| 1. Token + role check | `JwtAuthFilter` + `@PreAuthorize` | — | Must be Admin |
| 2. Request enters | `PortController` | `addAppPortDetails(body)` | Reads `appname`, `port`, `updatedby` |
| 3. Validation | `PortService` | `addAppPortDetails(...)` | Throws 400 if any field is null |
| 4. Duplicate check | `PortRangeRepository` | `existsByAppName(appname)` | Throws 409 Conflict if app name already exists |
| 5. ID generation | `PortRangeRepository` | `findMaxPortId()` | Gets current max ID and increments by 1 |
| 6. DB save | `PortRangeRepository` | `save(portRange)` | Inserts new port range record |
| 7. Response returned | `PortController` | — | Returns `{message: "Application & port added successfully"}` |

---

### POST `/backend/api/modifyAppPortDetails`

| Step | Class | Method | What happens |
|------|-------|--------|--------------|
| 1. Token + role check | `JwtAuthFilter` + `@PreAuthorize` | — | Must be Admin |
| 2. Request enters | `PortController` | `modifyAppPortDetails(body)` | Reads `portid`, `appname`, `port`, `updatedby` |
| 3. DB fetch | `PortRangeRepository` | `findById(portid)` | Loads existing record — throws 404 if not found |
| 4. No-change check | `PortService` | `modifyAppPortDetails(...)` | Returns "No changes" if values are identical |
| 5. DB save | `PortRangeRepository` | `save(existing)` | Updates the record |
| 6. Response returned | `PortController` | — | Returns `{message: "Application port details updated successfully"}` |

---

### DELETE `/backend/api/deleteAppPort`

| Step | Class | Method | What happens |
|------|-------|--------|--------------|
| 1. Token + role check | `JwtAuthFilter` + `@PreAuthorize` | — | Must be Admin |
| 2. Request enters | `PortController` | `deleteAppPort(body)` | Reads `portid` from body |
| 3. Existence check | `PortRangeRepository` | `existsById(portid)` | Throws 404 if not found |
| 4. DB delete | `PortRangeRepository` | `deleteById(portid)` | Removes the record |
| 5. Response returned | `PortController` | — | Returns `{message: "Application port deleted successfully"}` |

---

### POST `/backend/api/getAvailablePorts`

| Step | Class | Method | What happens |
|------|-------|--------|--------------|
| 1. Token + role check | `JwtAuthFilter` + `@PreAuthorize` | — | Must be Admin |
| 2. Request enters | `PortController` | `getAvailablePorts(body)` | Reads `appName` and `portRange` (e.g. `"8080-8090"` or `"8080,8082"`) |
| 3. Business logic | `PortService` | `getAvailablePorts(appName, portRange)` | Loads all catalog entries to build a port→status map |
| 4. Port list parsed | `PortService` | `parsePorts(portRange)` | Parses ranges (`8080-8085`) and single ports (`8090`) into a list of integers |
| 5. Status lookup | `PortService` | `getAvailablePorts(...)` | For each requested port, checks if it's in the catalog: Active = `USED(ACTIVE)`, Inactive = `ASSIGNED(INACTIVE)`, not found = `NOT_ASSIGNED` |
| 6. Response returned | `PortController` | — | Returns `{appName, availablePorts: [{port, status}], totalAvailable}` |

**How to Explain:**
"Get Available Ports checks which ports in a requested range are already in use. The service loads the entire VS catalog, builds a map of port to its active/inactive status, then for each port in the requested range, classifies it as USED(ACTIVE) if actively in use, ASSIGNED(INACTIVE) if assigned but inactive, or NOT_ASSIGNED if free. Port ranges like `8080-8090` are expanded into individual port numbers."

---

## 10. EXECUTION MODE APIs — `ExecutionModeController.java` → `ExecutionModeService.java`

**All require Admin or ApplicationUser role.**

---

### GET `/backend/api/getExecutionMode?serviceName=X&serverIP=Y`

| Step | Class | Method | What happens |
|------|-------|--------|--------------|
| 1. Token + role check | `JwtAuthFilter` + `@PreAuthorize` | — | Admin or ApplicationUser |
| 2. Request enters | `ExecutionModeController` | `getExecutionMode(serviceName, serverIP)` | Reads query params |
| 3. Business logic | `ExecutionModeService` | `getExecutionMode(serviceName, serverIP)` | Looks up the service |
| 4. Catalog lookup | `VsCatalogRepository` | `findByVsname(serviceName)` | Gets the Master ID for the service — throws 404 if service not in catalog |
| 5. Execution mode lookup | `ExecutionModeRepository` | `findByMasterIdAndVirtServer(masterId, serverIP)` | Finds the execution mode record for that service+server combo |
| 6. Live URLs lookup | `LiveUrlRepository` | `findByVsid(vsid)` | Gets all live URL records associated with that VSID |
| 7. Response returned | `ExecutionModeController` | — | Returns `{executionMode, vsid, liveUrls: [{vsurlId, host, isActive}]}` |

**How to Explain:**
"Get Execution Mode finds how a service is configured to run on a specific server. It starts by looking up the service in the VS catalog to get its master ID. Then it uses that master ID plus the server IP to find the execution mode record (e.g., Failover, Stand In, Live Invocation). It also fetches all live URLs configured for that service-server pair. Returns the execution mode and the list of live URLs."

---

### POST `/backend/api/updateExecutionMode`

| Step | Class | Method | What happens |
|------|-------|--------|--------------|
| 1. Token + role check | `JwtAuthFilter` + `@PreAuthorize` | — | Admin or ApplicationUser |
| 2. Request enters | `ExecutionModeController` | `updateExecutionMode(body)` | Reads `serviceName`, `serverIP`, `executionMode` |
| 3. Validation | `ExecutionModeService` | `updateExecutionMode(...)` | Throws 400 if `executionMode` is not one of: `Failover`, `Stand In`, `Live Invocation`, `Recording` |
| 4. Catalog lookup | `VsCatalogRepository` | `findByVsname(serviceName)` | Gets master ID — throws 404 if not found |
| 5. Execution mode lookup | `ExecutionModeRepository` | `findByMasterIdAndVirtServer(...)` | Gets the record to update — throws 404 if not found |
| 6. DB update | `ExecutionModeRepository` | `save(exec)` | Updates the execution mode value |
| 7. Response returned | `ExecutionModeController` | — | Returns `{message: "Execution mode updated successfully"}` |

**How to Explain:**
"Update Execution Mode changes how a service handles requests on a specific server. Valid modes are Failover, Stand In, Live Invocation, and Recording — any other value is rejected with a 400. The service then looks up the catalog and the execution mode record, updates the mode, and saves it."

---

### POST `/backend/api/addLiveURLs`

| Step | Class | Method | What happens |
|------|-------|--------|--------------|
| 1. Token + role check | `JwtAuthFilter` + `@PreAuthorize` | — | Admin or ApplicationUser |
| 2. Request enters | `ExecutionModeController` | `addLiveUrl(body)` | Reads `serviceName`, `serverIP`, `liveurl`, `updatedBy` |
| 3. Catalog lookup | `VsCatalogRepository` | `findByVsname(serviceName)` | Gets master ID — throws 404 if not found |
| 4. VSID lookup | `ExecutionModeRepository` | `findByMasterIdAndVirtServer(...)` | Gets the VSID for the service+server — throws 404 if not found |
| 5. Duplicate check | `LiveUrlRepository` | `existsByVsidAndHost(vsid, liveurl)` | Throws 409 Conflict if that URL already exists for this VSID |
| 6. ID generation | `LiveUrlRepository` | `findMaxVsurlId()` | Gets max ID and increments by 1 |
| 7. DB save | `LiveUrlRepository` | `save(liveUrl)` | Inserts new live URL with `isActive = "N"` (inactive by default) |
| 8. Response returned | `ExecutionModeController` | — | Returns 201 Created with `{message: "Live URL inserted successfully"}` |

**How to Explain:**
"Add Live URL adds a new live URL to a service for a specific server. It looks up the service in the catalog to get its master ID, then resolves the VSID from the execution mode table. It checks for duplicate URLs. If not a duplicate, it inserts the URL with active status set to 'N' — meaning it is added but not yet active. Returns HTTP 201 Created."

---

### DELETE `/backend/api/deleteLiveURL`

| Step | Class | Method | What happens |
|------|-------|--------|--------------|
| 1. Token + role check | `JwtAuthFilter` + `@PreAuthorize` | — | Admin or ApplicationUser |
| 2. Request enters | `ExecutionModeController` | `deleteLiveUrl(body)` | Reads `vsurlid` |
| 3. DB fetch | `LiveUrlRepository` | `findById(vsurlid)` | Loads the URL record — throws 404 if not found |
| 4. Active URL guard | `ExecutionModeService` | `deleteLiveUrl(...)` | Throws 409 Conflict if the URL is currently active (`isActive = "Y"`) |
| 5. DB delete | `LiveUrlRepository` | `deleteById(vsurlid)` | Removes the URL record |
| 6. Response returned | `ExecutionModeController` | — | Returns `{message: "Deleted successfully"}` |

**How to Explain:**
"Delete Live URL removes a live URL entry by its ID. Before deleting, it checks if the URL is currently active — if it is, deletion is blocked with a 409 Conflict. You cannot delete an active URL; it must first be deactivated."

---

### POST `/backend/api/setActiveLiveURL`

| Step | Class | Method | What happens |
|------|-------|--------|--------------|
| 1. Token + role check | `JwtAuthFilter` + `@PreAuthorize` | — | Admin or ApplicationUser |
| 2. Request enters | `ExecutionModeController` | `setActiveLiveUrl(body)` | Reads `vsurlid`, `vsid`, `active`, `host`, `updatedby` |
| 3. Active check | `ExecutionModeController` | `setActiveLiveUrl(...)` | Returns 400 immediately if `active != "Y"` |
| 4. Business logic | `ExecutionModeService` | `setActiveLiveUrl(vsurlid, vsid, host, updatedBy)` | Starts the activation |
| 5. Deactivate all | `LiveUrlRepository` | `findByVsid(vsid)` + `saveAll(...)` | Loads ALL live URLs for that VSID and sets each one to `isActive = "N"` |
| 6. Target URL verified | `LiveUrlRepository` | `findById(vsurlid)` | Loads the target URL — throws 404 if not found |
| 7. VSID check | `ExecutionModeService` | `setActiveLiveUrl(...)` | Throws 409 if the URL's VSID doesn't match the provided VSID |
| 8. Host check | `ExecutionModeService` | `setActiveLiveUrl(...)` | Throws 409 if the URL's host doesn't match the provided host |
| 9. Activate | `LiveUrlRepository` | `save(target)` | Sets `isActive = "Y"` on the target URL |
| 10. Response returned | `ExecutionModeController` | — | Returns `{message: "Activated successfully"}` |

**How to Explain:**
"Set Active Live URL switches which live URL is the active one for a service. It is a toggle operation — first it deactivates ALL live URLs under that VSID by setting each one to inactive. Then it verifies the target URL's VSID and host match what was sent in the request (as a safety check to prevent misuse). Finally, it activates just that one URL by setting `isActive` to 'Y'. Only one URL can be active at a time."

---

## Quick Reference — Who Can Access What

| Role | What they can access |
|------|----------------------|
| **Admin** | All APIs |
| **ApplicationUser** | `serverTimeInfo`, `getAssignedServices`, `getGroupTagsConfig`, `getDatasourceLists`, `getDatasets`, `getDatasets/download`, `getDatasets/delete`, `getAuditLogs`, `logAudit`, `getExecutionMode`, `updateExecutionMode`, `addLiveURLs`, `deleteLiveURL`, `setActiveLiveURL` |
| **Guest** | `serverTimeInfo`, `logAudit` |
| **Public (no login)** | `signIn`, `refresh`, `logout`, `forgot-password`, `reset-password` |

---

## Error Responses

All errors are handled globally in `GlobalExceptionHandler.java` and return consistent JSON:

| Exception | HTTP Status | When it is thrown |
|-----------|-------------|-------------------|
| `BadRequestException` | 400 | Missing/invalid input |
| `UnauthorizedException` | 401 | Wrong password, invalid token |
| `ForbiddenException` | 403 | Not an admin / token already used |
| `NotFoundException` | 404 | User/file/record not found |
| `ConflictException` | 409 | Duplicate record / active URL delete attempt |
| Any other `Exception` | 500 | Unexpected server error |

**How to Explain:**
"All error handling is centralized in `GlobalExceptionHandler`. When a service throws a custom exception like `NotFoundException` or `UnauthorizedException`, the handler automatically converts it to the appropriate HTTP status code and returns a JSON error response. This means no controller needs to write its own error handling — it just throws the right exception type."
