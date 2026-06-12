# StubServer Backend — Postman Automation Plan (Prompt-Style)

> Use this document as a single, copy-paste prompt to generate the complete
> Postman collection. It describes **what to build**, **how to structure it**,
> **what to chain**, **what to assert**, and **how to organize suites** for
> Smoke / Sanity / Regression runs.

---

## 1. Context & Goal

You are building a **Postman collection** to automate the **StubServer Backend API**
(50+ endpoints, base URL `http://your-server:9092/backend/api`).

The collection must support **three execution levels**:

| Level       | Purpose                                              | When to run                          |
|-------------|------------------------------------------------------|--------------------------------------|
| **Smoke**     | Is the build alive? Auth + 1-2 critical reads only.  | After every deploy / restart.        |
| **Sanity**    | Core happy-paths per module work.                    | After bug fixes / small enhancements.|
| **Regression**| Everything: happy-path + negative + chained flows.   | Before a release / major change.     |

Use Postman **folders + tags** so the same requests can be reused across all
three suites without duplication.

---

## 2. Collection Structure

```
StubServer-Backend-Automation/
├── 00_Setup/
│   ├── Health Check (GET /serverTimeInfo)
│   └── Admin Login (POST /signIn)        ← stores admin_token
├── 01_Authentication/
│   ├── Sign In - valid
│   ├── Sign In - invalid credentials
│   ├── Sign In - Basic Auth header
│   ├── Refresh Token - missing cookie
│   ├── Logout
│   ├── Forgot Password - valid user
│   ├── Forgot Password - unknown user
│   ├── Reset Password - valid token
│   ├── Reset Password - reused token
│   └── Change Password - flow (see §5.1)
├── 02_Users/
│   ├── Get Users List
│   ├── Sign Up - new user
│   ├── Sign Up - duplicate
│   ├── Sign Up Modify
│   └── Delete User
├── 03_AuditLogs/
│   ├── Log Audit
│   └── Get Audit Logs
├── 04_Catalog/
│   ├── Get Service Group Tags List
│   ├── Master Catalog Check - exists
│   ├── Master Catalog Check - not found
│   └── Master Catalog Check - missing params (400)
├── 05_ExecutionMode/
│   ├── Get Execution Mode
│   ├── Update Execution Mode - valid
│   ├── Update Execution Mode - invalid value (400)
│   ├── Add Live URL
│   ├── Add Live URL - duplicate (409)
│   ├── Set Active Live URL
│   ├── Delete Live URL - inactive
│   └── Delete Live URL - active (409)
├── 06_Logs/
│   ├── List Log Files - general
│   ├── List Log Files - error
│   ├── Download Single Log
│   ├── Download Single Log - not found (404)
│   ├── Download Selected Logs (ZIP)
│   ├── ReqResp Get Log Files
│   ├── ReqResp Download Log File
│   └── ReqResp Download All Logs
├── 07_Metrics/
│   ├── Get Lifetime Hits
│   ├── Get Monthly Hits
│   ├── Get Custom Report
│   ├── Get Dormant Services
│   └── Get Response Time
├── 08_PortManagement/
│   ├── Get App Port Lists
│   ├── Add App Port
│   ├── Add App Port - duplicate (409)
│   ├── Modify App Port
│   ├── Get Available Ports
│   └── Delete App Port
├── 09_ServerManagement/
│   ├── Get Server Lists
│   ├── Run Batch - Start
│   ├── Run Batch - Stop
│   ├── Get Live Status
│   └── Server Time Info
├── 10_ServiceManagement/
│   ├── Assign Services
│   ├── Get Assigned Services
│   ├── Get Group Tags Config
│   ├── Update Group
│   ├── Update Tags - valid
│   ├── Update Tags - duplicate (400)
│   ├── Get Datasource Lists
│   ├── Get Datasets
│   ├── Download Dataset
│   └── Delete Dataset
├── 11_Chained_Flows/                     ← see §5
│   ├── Flow_Password_Cycle
│   ├── Flow_User_Lifecycle
│   ├── Flow_Group_Tag_Cycle
│   ├── Flow_LiveURL_Cycle
│   ├── Flow_AppPort_Cycle
│   └── Flow_ExecutionMode_Cycle
└── 99_Teardown/
    └── Logout
```

Every request gets one or more of these **Postman tags**:
`smoke`, `sanity`, `regression`, `negative`, `chained`, `cleanup`.

Run a suite via Newman with `--folder` or by filtering on tag.

---

## 3. Environments

Create **3 Postman environments**: `DEV`, `QA`, `UAT`.

Variables in each environment:

| Key                  | Example value                              | Notes                            |
|----------------------|--------------------------------------------|----------------------------------|
| `baseUrl`            | `http://stub-qa.corp.com:9092/backend/api` | Per env                          |
| `admin_username`     | `admin`                                    | Pre-seeded admin                 |
| `admin_password`     | `<from secret store>`                      | **Never hardcode in collection** |
| `test_username`      | `autotest_user`                            | Created/torn down by suite       |
| `test_email`         | `autotest@corp.com`                        |                                  |
| `test_service`       | `MyService`                                | Must exist in catalog            |
| `test_serverIP`      | `10.0.0.1`                                 |                                  |
| `environment`        | `QA`                                       | For env-scoped endpoints         |
| `default_app_name`   | `AutoTestApp`                              | For port mgmt cycle              |
| `default_port_range` | `9500-9510`                                | Safe range to add/delete         |

**Collection-level variables (set dynamically by scripts):**
`admin_token`, `reset_token`, `created_vsurlid`, `created_portid`,
`created_user`, `original_group`, `original_tags` (JSON string).

---

## 4. Global Pre-request & Test Scripts

### 4.1 Collection-level Pre-request Script
```javascript
// Auto-attach bearer token to every request unless explicitly opted out
const skipAuth = pm.request.headers.has('X-Skip-Auth');
if (!skipAuth && pm.collectionVariables.get('admin_token')) {
    pm.request.headers.upsert({
        key: 'Authorization',
        value: 'Bearer ' + pm.collectionVariables.get('admin_token')
    });
}
```

### 4.2 Collection-level Test Script (runs after every request)
```javascript
// Universal assertions
pm.test('Status is not 5xx', () => {
    pm.expect(pm.response.code).to.be.below(500);
});
pm.test('Response time < 3000ms', () => {
    pm.expect(pm.response.responseTime).to.be.below(3000);
});
pm.test('Response is valid JSON or file', () => {
    const ct = pm.response.headers.get('Content-Type') || '';
    if (ct.includes('json')) {
        pm.expect(() => pm.response.json()).to.not.throw();
    }
});
```

### 4.3 Login request — specific test script
```javascript
pm.test('Login returns 200', () => pm.response.to.have.status(200));
pm.test('access_token present', () => {
    const j = pm.response.json();
    pm.expect(j.access_token).to.be.a('string').and.not.empty;
    pm.collectionVariables.set('admin_token', j.access_token);
});
pm.test('Token type is Bearer', () => {
    pm.expect(pm.response.json().token_type).to.eql('Bearer');
});
```

---

## 5. Chained Flows (the heart of regression)

These are **stateful sequences**. Each step's response feeds the next step's
request. They run as ordered folders. **Always end in a cleanup step that
restores original state**, so the suite can be re-run without manual reset.

### 5.1 Flow_Password_Cycle
```
Step 1: POST /signIn          (user=test_username, password=ORIGINAL)   → save token
Step 2: POST /change-password (current=ORIGINAL, new=TEMP_PWD)          → 200
Step 3: POST /signIn          (user=test_username, password=TEMP_PWD)   → 200 (confirms change)
Step 4: POST /forgot-password (user=test_username)                      → save reset_token
Step 5: POST /reset-password  (token=reset_token, new=ORIGINAL)         → 200 (restored)
Step 6: POST /reset-password  (same token reused)                       → 403 (negative)
Step 7: POST /signIn          (user=test_username, password=ORIGINAL)   → 200 (final verify)
```

### 5.2 Flow_User_Lifecycle
```
Step 1: POST /signUp          (test_username)                           → 200
Step 2: GET  /getUsersList    → assert test_username present
Step 3: POST /signUp          (same payload)                            → 400 duplicate
Step 4: POST /signUp-modify   (change lastname)                         → 200
Step 5: GET  /getUsersList    → assert lastname updated
Step 6: DELETE /delete        (test_username)                           → 200
Step 7: GET  /getUsersList    → assert test_username absent
```

### 5.3 Flow_Group_Tag_Cycle  ← *this is the one you described*
```
Step 1: GET  /getGroupTagsConfig?serviceName=MyService&env=QA
        → save original_group, original_tags into collection vars
Step 2: POST /updateGroup     (group=GroupX)                            → 200
Step 3: POST /updateTags      (tags=[tagA, tagB])                       → 200
Step 4: GET  /getGroupTagsConfig
        → assert group=GroupX AND tags contains tagA, tagB
Step 5: POST /updateTags      (tags=[tagA, tagA])                       → 400 duplicate
Step 6: POST /updateGroup     (group=original_group)                    → 200  (restore)
Step 7: POST /updateTags      (tags=original_tags)                      → 200  (restore)
Step 8: GET  /getGroupTagsConfig
        → assert fully restored to original
```

### 5.4 Flow_LiveURL_Cycle
```
Step 1: POST /addLiveURLs     (host=http://temp-host)                   → 201, save vsurlid
Step 2: POST /addLiveURLs     (same)                                    → 409 duplicate
Step 3: POST /setActiveLiveURL (vsurlid, active=Y)                      → 200
Step 4: DELETE /deleteLiveURL (vsurlid)                                 → 409 cannot delete active
Step 5: POST /setActiveLiveURL (some_other_vsurlid, active=Y)           → 200 (switch active)
Step 6: DELETE /deleteLiveURL (vsurlid)                                 → 200
```

### 5.5 Flow_AppPort_Cycle
```
Step 1: POST /addAppPortDetails  (AutoTestApp, 9500-9510)               → 200, save portid
Step 2: POST /addAppPortDetails  (same)                                 → 409
Step 3: GET  /getAppPortLists                                           → assert present
Step 4: POST /getAvailablePorts                                         → assert array shape
Step 5: POST /modifyAppPortDetails (range=9500-9520)                    → 200
Step 6: POST /modifyAppPortDetails (same again)                         → "No changes made"
Step 7: DELETE /deleteAppPort  (portid)                                 → 200
```

### 5.6 Flow_ExecutionMode_Cycle
```
Step 1: GET  /getExecutionMode                                          → save original mode
Step 2: POST /updateExecutionMode (Failover)                            → 200
Step 3: POST /updateExecutionMode (InvalidMode)                         → 400
Step 4: POST /updateExecutionMode (Stand In)                            → 200
Step 5: GET  /getExecutionMode                                          → assert Stand In
Step 6: POST /updateExecutionMode (original mode)                       → 200  (restore)
```

---

## 6. Assertion Standards (per request)

Every request **must** include:

1. **Status code** assertion (specific code, not just `200`).
2. **Schema / key existence** assertion for JSON responses.
3. **Value assertion** for at least one business-critical field.
4. **Negative case partner** — if it's a happy-path, there is a sibling
   request that triggers a documented 4xx.

Example for `GET /getUsersList`:
```javascript
pm.test('200 OK', () => pm.response.to.have.status(200));
const body = pm.response.json();
pm.test('Body is array', () => pm.expect(body).to.be.an('array'));
pm.test('Each row has required keys', () => {
    body.forEach(u => {
        pm.expect(u).to.include.all.keys('USERNAME','EMAIL','USERROLE');
    });
});
```

---

## 7. Negative Test Matrix (must be in regression)

| Endpoint                  | Negative case                       | Expected |
|---------------------------|-------------------------------------|----------|
| `/signIn`                 | wrong password                      | 401      |
| `/signIn`                 | missing body                        | 400      |
| `/refresh`                | no cookie                           | 401      |
| `/reset-password`         | reused token                        | 403      |
| `/reset-password`         | expired/invalid token               | 400      |
| `/change-password`        | wrong current password              | 400      |
| `/signUp`                 | duplicate username                  | 400      |
| `/signUp`                 | non-admin caller                    | 403      |
| `/delete`                 | unknown user                        | 404      |
| `/masterCatalog/check`    | no serviceName & no port            | 400      |
| `/updateExecutionMode`    | mode not in enum                    | 400      |
| `/addLiveURLs`            | duplicate host                      | 409      |
| `/deleteLiveURL`          | active URL                          | 409      |
| `/setActiveLiveURL`       | host mismatch                       | 409      |
| `/downloadSingleLog`      | missing file                        | 404      |
| `/addAppPortDetails`      | duplicate app                       | 409      |
| `/updateTags`             | duplicate tags in array             | 400      |
| Any protected endpoint    | no `Authorization` header           | 401      |
| Any protected endpoint    | tampered / expired token            | 401      |
| Admin-only endpoint       | ApplicationUser token               | 403      |

---

## 8. Tag-to-Suite Mapping

Tag each request. Newman runs filter by tag.

| Folder/Request                          | smoke | sanity | regression |
|-----------------------------------------|:-----:|:------:|:----------:|
| Setup → Health Check                    | ✅    | ✅     | ✅         |
| Setup → Admin Login                     | ✅    | ✅     | ✅         |
| Auth → Sign In valid                    | ✅    | ✅     | ✅         |
| Auth → all negative cases               |       |        | ✅         |
| Users → Get Users List                  | ✅    | ✅     | ✅         |
| Users → Sign Up + Modify + Delete       |       | ✅     | ✅         |
| Catalog → check exists                  |       | ✅     | ✅         |
| ExecutionMode → Get                     | ✅    | ✅     | ✅         |
| ExecutionMode → Update (happy)          |       | ✅     | ✅         |
| Logs → List + Download single           |       | ✅     | ✅         |
| Logs → Download ZIP                     |       |        | ✅         |
| Metrics → Lifetime Hits                 | ✅    | ✅     | ✅         |
| Metrics → Monthly / Custom / Dormant    |       | ✅     | ✅         |
| ServerMgmt → Live Status + Server Time  | ✅    | ✅     | ✅         |
| ServerMgmt → Run Batch start/stop       |       |        | ✅ (risky) |
| Chained Flows (all six)                 |       |        | ✅         |
| Teardown → Logout                       | ✅    | ✅     | ✅         |

> ⚠️ **Server Start/Stop and Reset Password should be flagged `regression` only.**
> They mutate the system; smoke/sanity must stay read-mostly + idempotent.

---

## 9. Newman Commands

```bash
# Smoke (fast, post-deploy)
newman run StubServer-Backend-Automation.json \
  -e QA.postman_environment.json \
  --folder "00_Setup" --folder "01_Authentication" \
  --bail --reporters cli,html --reporter-html-export smoke.html

# Sanity (after small fixes)
newman run StubServer-Backend-Automation.json \
  -e QA.postman_environment.json \
  --env-var "suite=sanity" \
  --reporters cli,htmlextra --reporter-htmlextra-export sanity.html

# Regression (full)
newman run StubServer-Backend-Automation.json \
  -e QA.postman_environment.json \
  --reporters cli,htmlextra,junit \
  --reporter-htmlextra-export regression.html \
  --reporter-junit-export regression.xml
```

Add a one-liner per suite in `package.json` or a shell script so the
infrequent reruns are zero-friction.

---

## 10. Reusable JS Snippets (drop these in collection scripts)

### 10.1 Login helper (call from any folder pre-request)
```javascript
function ensureLoggedIn(done) {
    if (pm.collectionVariables.get('admin_token')) return done();
    pm.sendRequest({
        url: pm.environment.get('baseUrl') + '/signIn',
        method: 'POST',
        header: { 'Content-Type': 'application/json' },
        body: { mode: 'raw', raw: JSON.stringify({
            username: pm.environment.get('admin_username'),
            password: pm.environment.get('admin_password')
        })}
    }, (err, res) => {
        if (!err) pm.collectionVariables.set('admin_token', res.json().access_token);
        done();
    });
}
ensureLoggedIn(() => {});
```

### 10.2 Backup-before-mutate helper
```javascript
// Use before a destructive update so cleanup can restore
pm.sendRequest({
    url: pm.environment.get('baseUrl') + '/getGroupTagsConfig?serviceName='
         + pm.environment.get('test_service') + '&env=' + pm.environment.get('environment'),
    method: 'GET',
    header: { 'Authorization': 'Bearer ' + pm.collectionVariables.get('admin_token') }
}, (err, res) => {
    const j = res.json();
    pm.collectionVariables.set('original_group', j.group);
    pm.collectionVariables.set('original_tags', JSON.stringify(j.tags));
});
```

---

## 11. Acceptance Criteria for the Generated Collection

The collection is "done" when **all** of the following are true:

- [ ] All 50 endpoints from `API_REFERENCE.md` have at least one happy-path request.
- [ ] Every endpoint in the Negative Test Matrix (§7) has a matching negative request.
- [ ] All six chained flows in §5 execute end-to-end and restore original state.
- [ ] Tags `smoke`, `sanity`, `regression` resolve to 3 runnable, non-overlapping (but nested) suites.
- [ ] Zero hardcoded secrets — all sensitive values come from environment variables.
- [ ] Newman regression run produces an HTML report with **0 failed assertions** on a clean QA env.
- [ ] Re-running the regression suite **twice in a row** passes both times (idempotency proof).

---

## 12. Deliverables Expected

When you act on this prompt, produce:

1. `StubServer-Backend-Automation.postman_collection.json`
2. `QA.postman_environment.json` (+ DEV, UAT variants)
3. `README.md` with: how to import, how to run each suite, prerequisites,
   how to seed `test_username`, troubleshooting common 401/403.
4. `run-smoke.sh`, `run-sanity.sh`, `run-regression.sh` Newman wrappers.

---

*End of prompt. Hand this file to the assistant along with `API_REFERENCE.md`
to generate the full Postman collection.*
