# Deployment Guide — Multi-Server Setup (Option B)

## Overview

This guide explains how to deploy the `java-backend` application to multiple servers
using a single JAR build with server-specific external config files.

---

## Folder Structure — java-backend/src/main/resources

```
resources/
├── application.yml           → Main app config (DB, paths, JWT, mail, CORS)
├── db-tables.properties      → DB table names (environment-specific)
├── logback-spring.xml        → Logging config
└── templates/
    ├── accountCreationEmailTemplate.html
    └── resetPasswordEmailTemplate.html
```

---

## The Problem (Before Fix)

`application.yml` and `db-tables.properties` were baked inside the JAR on every build.
To change any config (DB credentials, table names, file paths), you had to:
- Edit the source code
- Rebuild the JAR
- Redeploy

This meant **one JAR per server** and required source code + Maven on the build machine every time.

---

## The Solution — Option B (External Config)

Spring Boot automatically picks up `application.yml` placed **next to the JAR** on the server.

For `db-tables.properties`, a **1 line code change** was made in `TableNames.java` to support external loading.

### Code Change Made

**File:** `src/main/java/com/stubserver/backend/database/config/TableNames.java`

**Before:**
```java
@PropertySource(value = "classpath:db-tables.properties", ignoreResourceNotFound = true)
```

**After:**
```java
@PropertySource(value = {"file:./db-tables.properties", "classpath:db-tables.properties"}, ignoreResourceNotFound = true)
```

**Why:** `classpath:` only looks inside the JAR. Adding `file:./` tells Spring Boot to look next to the JAR first.

---

## How Config Loading Works

### application.yml
```
Spring Boot starts
       ↓
Looks for application.yml NEXT TO the JAR first  (built-in Spring Boot behavior)
       ↓
FOUND?     → Uses external file (ignores the one inside JAR)
NOT FOUND? → Falls back to application.yml inside JAR
```

### db-tables.properties
```
Spring Boot starts
       ↓
Looks for db-tables.properties NEXT TO the JAR first  (file:./)
       ↓
FOUND?     → Uses external file (ignores the one inside JAR)
NOT FOUND? → Falls back to db-tables.properties inside JAR
```

---

## What to Keep in src/resources (Recommended)

**Do NOT delete these files from src.** Keep them but with obviously broken/placeholder values.

| File | What to put inside src |
|---|---|
| `application.yml` | All sensitive values as `CHANGE_ME` |
| `db-tables.properties` | All table names as `CHANGE_ME` |

**Why:** If someone copies the JAR to a server but forgets to place external config files,
the app will **fail loudly** on startup (CHANGE_ME credentials will fail to connect)
instead of silently connecting to the wrong DB or wrong tables.

---

## Step-by-Step Deployment

### Step 1 — Make the code change (already done)
The 1 line change in `TableNames.java` is already applied.

### Step 2 — Build the JAR once
```
mvn clean package -DskipTests
```
JAR will be generated at:
```
java-backend/target/backend-0.0.1-SNAPSHOT.jar
```
This is the **one JAR** used for all servers.

### Step 3 — Prepare config files for each server
Create separate `application.yml` and `db-tables.properties` for each server with correct values.

### Step 4 — Copy to each server

**Server 1 — QA (10.11.11.123)**
```
D:/StubServerPortal/app/
├── backend-0.0.1-SNAPSHOT.jar
├── application.yml          ← QA config
└── db-tables.properties     ← QA table names
```

**Server 2 — PROD (10.22.22.234)**
```
D:/StubServerPortal/app/
├── backend-0.0.1-SNAPSHOT.jar   ← exact same JAR
├── application.yml              ← PROD config
└── db-tables.properties         ← PROD table names
```

### Step 5 — Run via Task Scheduler on each server
```
java -jar D:/StubServerPortal/app/backend-0.0.1-SNAPSHOT.jar
```

---

## What to Change in application.yml Per Server

```yaml
app:
  datasource:
    oracle:
      user: <server specific DB user>
      password: <server specific DB password>
      connect-string: <server specific DB host>:1521/ORCLPDB1

  cors-origin: https://<server domain>
  prod-url: https://<server domain>

  server-log-dir: <path on that server>
  reqresp-log-dir: <path on that server>
  dataset-paths: <path on that server>
  batch:
    java-start: <path on that server>/start.bat
    java-stop: <path on that server>/stop.bat
```

## What to Change in db-tables.properties Per Server

```properties
# Update suffix per environment (_QA, _PROD, _UAT etc.)
table.auditLogs=STUBSERVERAUDITLOGS_PROD
table.assignedServices=STUBASSIGNEDSERVICES_PROD
table.vsDetails=STUBSERVERPROD_VSDETAILS
table.masterCatalog=STUBSERVER_MASTER_CATALOG_PROD
```

---

## Future Maintenance

| Situation | Action |
|---|---|
| Bug fix / new feature in code | Rebuild JAR → copy new JAR to servers → restart (config files stay untouched) |
| DB password changed | Edit `application.yml` on that server directly → restart (no new JAR needed) |
| Table name changed | Edit `db-tables.properties` on that server directly → restart (no new JAR needed) |
| New server to onboard | Copy same JAR → create config files for that server → run |

---

## Key Rule

> **JAR = Code. Config files = Environment settings. They are independent.**
>
> One JAR. Many servers. Each server has its own config files sitting next to the JAR.
