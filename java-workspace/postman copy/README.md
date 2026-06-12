# StubServer Backend — Postman Automation Collection

Complete automation collection covering all 48 endpoints across 13 modules, with full role-based access control testing.

---

## Files

| File | Purpose |
|------|---------|
| `StubServer-Backend-Automation.postman_collection.json` | Main collection (138 requests) |
| `QA.postman_environment.json` | QA environment variables |
| `UAT.postman_environment.json` | UAT environment variables |

---

## Prerequisites

```bash
npm install -g newman newman-reporter-htmlextra
```

---

## User Accounts Required

The collection uses three user roles. Ensure all three accounts exist in your system before running:

| Role | Username | Password | Access Level |
|------|----------|----------|-------------|
| Admin | `admin` | *(set in env)* | Full access to all endpoints |
| ApplicationUser | `appuser` | `appuser` | Shared endpoints + execution mode, assigned services, datasets |
| Guest | `Guest` | `guestuser` | logAudit and serverTimeInfo only |

---

## How Authorization Works

The collection handles tokens automatically:

1. **`00_Setup`** — logs in as all three users and stores tokens:
   - `Admin Login` → stores `admin_token` in collection variable
   - `AppUser Login` → stores `appuser_token`
   - `Guest Login` → stores `guest_token`
2. Every protected request carries an explicit `Authorization: Bearer {{<token>}}` header — no hidden injection
3. **Password Cycle** flow logs in as `test_username` and stores to `test_user_token` (keeps `admin_token` untouched)
4. **`99_Teardown`** logs out admin (revokes all refresh tokens) and clears all four token variables

> **Token behavior**: Signing in again with the same user invalidates all existing refresh tokens for that user and issues a new one. The short-lived access token (JWT) remains valid until it expires — `forgot-password` and `reset-password` do **not** invalidate existing access tokens.

---

## Import into Postman

1. Open Postman → **Import**
2. Drop in `StubServer-Backend-Automation.postman_collection.json`
3. Drop in the environment file for your target (`QA.postman_environment.json` or `UAT.postman_environment.json`)
4. Select the imported environment from the top-right dropdown
5. Fill in real values for `admin_password` and `test_password` in the environment editor

---

## Running via Newman

```bash
cd postman/
newman run StubServer-Backend-Automation.postman_collection.json \
  -e QA.postman_environment.json \
  --reporters cli,htmlextra \
  --reporter-htmlextra-export report.html
```

Target UAT instead:
```bash
newman run StubServer-Backend-Automation.postman_collection.json \
  -e UAT.postman_environment.json \
  --reporters cli,htmlextra \
  --reporter-htmlextra-export report.html
```

---

## Collection Structure

| Folder | What it covers |
|--------|---------------|
| `00_Setup` | Admin Login, AppUser Login, Guest Login, Health Check |
| `01_Authentication` | Sign-in, refresh, forgot/reset/change password, logout |
| `02_Users` | Get, create, modify, delete users (Admin only) |
| `03_AuditLogs` | Log audit (all roles), get audit logs (Admin+AppUser) |
| `04_Catalog` | Service group tags, master catalog check (Admin only) |
| `05_ExecutionMode` | Get/update execution mode, live URL management (Admin+AppUser) |
| `06_Logs` | Log file listing, download, ReqResp logs (Admin only) |
| `07_Metrics` | Lifetime/monthly hits, custom reports, dormant services (Admin only) |
| `08_PortManagement` | App port CRUD (Admin only) |
| `09_ServerManagement` | Server list, batch ops, live status (Admin only) |
| `10_ServiceManagement` | Assigned services, group/tag config, datasets (Admin+AppUser) |
| `11_Chained_Flows` | 6 end-to-end flows: Password Cycle, User Lifecycle, Group/Tag, LiveURL, AppPort, ExecutionMode |
| `12_RoleAccessControl` | 16 role-based tests — AppUser/Guest denied on Admin-only, allowed on shared endpoints |
| `99_Teardown` | Re-login admin, logout (clears all tokens) |

---

## Role Access Control Tests (`12_RoleAccessControl`)

| Test | Token | Endpoint | Expected |
|------|-------|----------|----------|
| AppUser - getUsersList | appuser_token | GET /getUsersList | 403 |
| AppUser - signUp | appuser_token | POST /signUp | 403 |
| AppUser - listLogFiles | appuser_token | POST /listLogFiles | 403 |
| AppUser - getLifeTimeHits | appuser_token | GET /getLifeTimeHits | 403 |
| AppUser - getAppPortLists | appuser_token | GET /getAppPortLists | 403 |
| AppUser - logAudit | appuser_token | POST /logAudit | 200 |
| AppUser - serverTimeInfo | appuser_token | GET /serverTimeInfo | 200 |
| AppUser - getExecutionMode | appuser_token | GET /getExecutionMode | 200 |
| AppUser - getGroupTagsConfig | appuser_token | GET /getGroupTagsConfig | 200 |
| Guest - getUsersList | guest_token | GET /getUsersList | 403 |
| Guest - getLifeTimeHits | guest_token | GET /getLifeTimeHits | 403 |
| Guest - getExecutionMode | guest_token | GET /getExecutionMode | 403 |
| Guest - getAuditLogs | guest_token | GET /getAuditLogs | 403 |
| Guest - getDatasourceLists | guest_token | GET /getDatasourceLists | 403 |
| Guest - logAudit | guest_token | POST /logAudit | 200 |
| Guest - serverTimeInfo | guest_token | GET /serverTimeInfo | 200 |

---

## Chained Flows

| Flow | Steps | What it proves |
|------|-------|---------------|
| Flow_Password_Cycle | 7 | Full password change + reset cycle, reused-token 403 |
| Flow_User_Lifecycle | 7 | Create → verify → modify → verify → delete → verify |
| Flow_Group_Tag_Cycle | 8 | Mutate group/tags, assert, verify duplicate-tag 400, restore |
| Flow_LiveURL_Cycle | 9 | Add URL → set active → 409 delete → switch active → delete |
| Flow_AppPort_Cycle | 7 | Add port → 409 dup → modify → no-change → delete |
| Flow_ExecutionMode_Cycle | 6 | Failover → invalid 400 → Stand In → verify → restore |

All flows restore original state so the collection can run twice in a row (idempotent).

---

## Troubleshooting

### 401 on every request after the first module
- `Admin Login` in `00_Setup` failed — check `admin_username` / `admin_password`.

### AppUser or Guest login fails (401)
- The `appuser` and `Guest` accounts don't exist yet. Create them via `POST /signUp` as admin.

### 403 on admin endpoints
- `admin_username` must have role `Admin` in the system. Check via `GET /getUsersList`.

### Chained flow fails mid-way
- A collection variable wasn't saved because a prior step failed.
- Run the flow folder in isolation in Postman to debug step by step.

### `test_username` duplicate on re-run
- If the User Lifecycle flow failed before the delete step, the user may still exist.
- Delete manually: `DELETE /delete` with `{"username":"autotest_user","requestedBy":"admin"}`.

---

## Environment Variables Reference

| Key | Description |
|-----|-------------|
| `baseUrl` | API base URL, e.g. `http://host:9092/backend/api` |
| `admin_username` | Admin login username |
| `admin_password` | Admin login password (**never commit real values**) |
| `appuser_username` | ApplicationUser login username (`appuser`) |
| `appuser_password` | ApplicationUser login password (`appuser`) |
| `guest_username` | Guest login username (`Guest`) |
| `guest_password` | Guest login password (`guestuser`) |
| `test_username` | Username for lifecycle/password tests |
| `test_email` | Email for `test_username` |
| `test_password` | Password for `test_username` |
| `test_service` | Service name that exists in the catalog |
| `test_serverIP` | Server IP for execution mode / metrics endpoints |
| `environment` | Environment label: `QA` or `UAT` |
| `default_app_name` | App name used in port management cycle (`AutoTestApp`) |
| `default_port_range` | Safe port range for tests (`9500-9510`) |
| `sample_log_file` | Log filename for download tests (optional) |
| `sample_reqresp_log` | ReqResp log filename for download tests (optional) |
| `sample_dataset_file` | Dataset filename for download tests (optional) |
