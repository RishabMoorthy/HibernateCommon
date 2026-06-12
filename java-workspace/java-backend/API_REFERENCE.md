# StubServer Backend — API Reference

Base URL: `http://your-server:9092/backend/api`

All protected endpoints require:
```
Authorization: Bearer <access_token>
```

---

## Authentication

### POST /signIn
No auth required.

**Request (JSON body):**
```json
{ "username": "john", "password": "pass123" }
```
**Request (Basic Auth header):**
```
Authorization: Basic am9objpwYXNzMTIz
```
**Response 200:**
```json
{ "access_token": "eyJ...", "token_type": "Bearer", "expires_in": 900 }
```
**Response 401:**
```json
{ "error": "Invalid credentials" }
```

---

### POST /refresh
No auth required. Reads `refreshToken` from HttpOnly cookie.

**Request:** No body.

**Response 200:**
```json
{ "token": "eyJ..." }
```
**Response 401:**
```json
{ "error": "Missing refresh token" }
```
```json
{ "error": "Invalid or expired refresh token" }
```

---

### POST /logout
No auth required.

**Request:** No body.

**Response 200:**
```json
{ "message": "Logged out" }
```

---

### POST /forgot-password
No auth required.

**Request:**
```json
{ "username": "john" }
```
**Response 200:**
```json
{ "message": "Reset email sent", "token": "eyJ..." }
```
**Response 404:**
```json
{ "error": "User not found" }
```

---

### POST /reset-password
No auth required.

**Request:**
```json
{ "token": "eyJ...", "newPassword": "newpass123" }
```
**Response 200:**
```json
{ "message": "Password reset successfully" }
```
**Response 400:**
```json
{ "error": "Invalid or expired token" }
```
**Response 403:**
```json
{ "error": "Token has already been used" }
```

---

### POST /change-password
Requires auth.

**Request:**
```json
{ "currentPassword": "oldpass", "newPassword": "newpass123" }
```
**Response 200:**
```json
{ "message": "Password changed successfully" }
```
**Response 400:**
```json
{ "error": "Current password is incorrect" }
```

---

## Users
Roles: `Admin` only (except where noted).

### GET /getUsersList
**Response 200:**
```json
[
  { "USERNAME": "john", "EMAIL": "john@corp.com", "FIRSTNAME": "John", "LASTNAME": "Doe", "USERROLE": "ApplicationUser" }
]
```

---

### POST /signUp
**Request:**
```json
{ "username": "john", "email": "john@corp.com", "firstname": "John", "lastname": "Doe", "requestedBy": "admin", "userrole": "ApplicationUser" }
```
**Response 200:**
```json
{ "message": "User created and password sent to email." }
```
**Response 400:**
```json
{ "error": "Username or Email already exists" }
```
**Response 403:**
```json
{ "error": "Only admin can perform this action" }
```

---

### POST /signUp-modify
**Request:**
```json
{ "username": "john", "firstname": "John", "lastname": "Smith", "email": "john@corp.com", "updatedBy": "admin", "userrole": "Admin" }
```
**Response 200:**
```json
{ "message": "User updated successfully" }
```
```json
{ "message": "No changes were made." }
```

---

### DELETE /delete
**Request:**
```json
{ "username": "john", "requestedBy": "admin" }
```
**Response 200:**
```json
{ "message": "User deleted successfully" }
```
**Response 404:**
```json
{ "error": "User Not Found" }
```

---

## Audit Logs
Roles: `Admin`, `ApplicationUser` (except logAudit which also allows `Guest`).

### GET /getAuditLogs
**Response 200:**
```json
{
  "logs": [
    { "id": 1, "user": "john", "serviceName": "MyService", "action": "deploy", "remark": "v1.2", "timestamp": "2025-05-01T10:30:00" }
  ]
}
```

---

### POST /logAudit
**Request:**
```json
{ "username": "john", "serviceName": "MyService", "action": "deploy", "remark": "v1.2" }
```
**Response 200:**
```json
{ "message": "Audit log inserted" }
```

---

## Catalog

### GET /getServiceGroupTagsList
Role: `Admin`

**Response 200:**
```json
{
  "totalService": 2,
  "services": [
    { "servicename": "MyService", "group": "GroupA", "tags": ["tag1", "tag2"] },
    { "servicename": "OtherService", "group": "", "tags": [] }
  ]
}
```

---

### POST /masterCatalog/check
Role: `Admin`

**Request:**
```json
{ "serviceName": "MyService", "port": "9100" }
```
**Response 200 — found:**
```json
{ "serviceName": "MyService", "port": "9100", "exists": true, "nameMatch": true, "portMatch": true }
```
**Response 200 — not found:**
```json
{ "serviceName": "Unknown", "port": null, "exists": false, "nameMatch": false, "portMatch": false }
```
**Response 400:**
```json
{ "error": "Provide at least one of: serviceName or port" }
```

---

## Execution Mode
Roles: `Admin`, `ApplicationUser`.

### GET /getExecutionMode?serviceName=MyService&serverIP=10.0.0.1
**Response 200:**
```json
{
  "executionMode": "Failover",
  "vsid": 101,
  "liveUrls": [
    { "vsurlId": 1, "host": "http://live-backend.corp.com", "isActive": "Y" }
  ]
}
```
**Response 200 — not found:**
```json
{ "error": "Execution mode not found." }
```

---

### POST /updateExecutionMode
**Request:**
```json
{ "serviceName": "MyService", "serverIP": "10.0.0.1", "executionMode": "Stand In" }
```
Valid modes: `Failover`, `Stand In`, `Live Invocation`, `Recording`

**Response 200:**
```json
{ "message": "Execution mode updated successfully" }
```
**Response 400:**
```json
{ "error": "Invalid execution mode" }
```

---

### POST /addLiveURLs
**Request:**
```json
{ "serviceName": "MyService", "serverIP": "10.0.0.1", "liveurl": "http://live-backend.corp.com", "updatedBy": "john" }
```
**Response 201:**
```json
{ "message": "Live URL inserted successfully" }
```
**Response 409:**
```json
{ "message": "Live URL already exists" }
```

---

### DELETE /deleteLiveURL
**Request:**
```json
{ "vsurlid": 1 }
```
**Response 200:**
```json
{ "message": "Deleted successfully" }
```
**Response 409:**
```json
{ "message": "Cannot delete an active URL" }
```

---

### POST /setActiveLiveURL
**Request:**
```json
{ "vsurlid": 1, "vsid": 101, "host": "http://live-backend.corp.com", "updatedby": "john", "active": "Y" }
```
**Response 200:**
```json
{ "message": "Activated successfully" }
```
**Response 409:**
```json
{ "message": "Host mismatch" }
```

---

## Logs
Role: `Admin`.

### POST /listLogFiles
**Request:**
```json
{ "logType": "Info", "startDateTime": "2025-05-01T00:00:00Z", "endDateTime": "2025-05-12T23:59:59Z" }
```
`logType`: `Info` or `Error`

**Response 200:**
```json
[
  { "name": "stubserver.log", "size": 204800, "modifiedDate": "2025-05-12T10:00:00Z" }
]
```

---

### GET /downloadSingleLog?fileName=stubserver.log
**Response 200:** File download (`application/octet-stream`)

**Response 404:**
```json
{ "error": "File not found." }
```

---

### POST /downloadSelectedLogs
**Request:**
```json
{ "files": ["stubserver.log", "stubserver-error.log"] }
```
**Response 200:** ZIP file download (`application/zip`)

Response headers:
```
X-Original-Count: 2
X-Selected-Count: 2
X-Limit-Applied: false
```

---

### GET /reqresp/getLogFiles?serviceName=MyService
**Response 200:**
```json
{
  "status": "success",
  "serviceName": "MyService",
  "logFiles": [
    { "fileName": "MyService_2025-05-12.log", "lastModified": "2025-05-12T10:00:00Z" }
  ]
}
```

---

### GET /reqresp/downloadLogFile?fileName=MyService_2025-05-12.log
**Response 200:** File download (`application/octet-stream`)

---

### GET /reqresp/downloadAllLogs
**Response 200:** ZIP file download containing all `.log` files (`application/zip`)

---

## Metrics
Role: `Admin`.

### GET /getLifeTimeHits
**Response 200:**
```json
[
  {
    "serviceName": "MyService",
    "counts": { "total": 5000, "qa": 3000, "uat": 2000 }
  }
]
```

---

### POST /getMonthlyHits
**Request:**
```json
{ "fromMonth": "JAN-2025", "toMonth": "MAY-2025" }
```
**Response 200:**
```json
{
  "data": [
    { "serviceName": "MyService", "month": "jan", "year": "2025", "totalCount": 500, "totalQACount": 300, "totalUATCount": 200 }
  ]
}
```

---

### POST /getCustomReport
**Request:**
```json
{ "fromDate": "01/05/2025", "toDate": "12/05/2025" }
```
**Response 200:**
```json
[
  {
    "serviceName": "MyService",
    "transDate": "01-MAY-25",
    "counts": { "total": 100, "qa": 60, "uat": 40 }
  }
]
```

---

### POST /getDormantServiceLists
**Request:**
```json
{ "serverIP": "10.0.0.1" }
```
**Response 200:**
```json
{
  "last_3_months": {
    "count_0":    [{ "VSNAME": "InactiveService", "COUNT": 0 }],
    "count_1_50": [{ "VSNAME": "LowService", "COUNT": 20 }],
    "count_51_100": []
  },
  "last_6_months": {
    "count_0":    [],
    "count_1_50": [{ "VSNAME": "LowService", "COUNT": 45 }],
    "count_51_100": []
  }
}
```

---

### POST /getResponseTime
**Request:**
```json
{ "serviceName": "MyService", "serverIP": "10.0.0.1", "fromDateTime": "2025-05-12T09:00", "toDateTime": "2025-05-12T17:00" }
```
Datetime format: `YYYY-MM-DDTHH:mm` (IST — auto-converted to UTC internally)

**Response 200:**
```json
{ "total": 1, "data": [{ "VSNAME": "MyService", "STARTTIME": "...", "AVGRESPTIME": 120, "MAXRESPTIME": 350, "AVGTPS": 5.2 }] }
```

---

## Port Management
Role: `Admin`.

### GET /getAppPortLists
**Response 200:**
```json
{ "totalApp": 1, "data": [{ "PORTID": 1, "APPNAME": "MyApp", "PORTS": "9100-9200" }] }
```

---

### POST /addAppPortDetails
**Request:**
```json
{ "appname": "MyApp", "port": "9100-9200", "updatedby": "john" }
```
**Response 200:**
```json
{ "message": "Application & port added successfully" }
```
**Response 409:**
```json
{ "message": "Requested Application name already exists" }
```

---

### POST /modifyAppPortDetails
**Request:**
```json
{ "portid": 1, "appname": "MyApp", "port": "9100-9300", "updatedby": "john" }
```
**Response 200:**
```json
{ "message": "Application port details updated successfully" }
```
```json
{ "message": "No changes made. Data is identical." }
```

---

### DELETE /deleteAppPort
**Request:**
```json
{ "portid": 1 }
```
**Response 200:**
```json
{ "message": "Application port deleted successfully" }
```

---

### POST /getAvailablePorts
**Request:**
```json
{ "appName": "MyApp", "portRange": "9100-9110,9200" }
```
**Response 200:**
```json
{
  "appName": "MyApp",
  "totalAvailable": 3,
  "availablePorts": [
    { "port": 9100, "status": "USED(ACTIVE)" },
    { "port": 9101, "status": "ASSIGNED(INACTIVE)" },
    { "port": 9102, "status": "NOT_ASSIGNED" }
  ]
}
```

---

## Server Management
Role: `Admin` (except `/serverTimeInfo`).

### GET /getServerLists
**Response 200:**
```json
[
  { "SERVERNAME": "StubServer", "PORT": 9093, "STATUS": "Running", "LASTUPDATE": "2025-05-12T09:00:00" }
]
```
`STATUS`: `Running` or `Stopped`

---

### POST /run-batch
**Request:**
```json
{ "action": "Start", "servername": "StubServer" }
```
`action`: `Start` or `Stop`

**Response 200:**
```json
{ "message": "Server Started" }
```
```json
{ "message": "Server Stopped" }
```
```json
{ "message": "Java app running already" }
```

---

### GET /getLiveStatus
**Response 200 — running:**
```json
{ "liveStatus": "Running", "upAndRunning": "2025-05-12T06:00:00Z" }
```
**Response 200 — stopped:**
```json
{ "liveStatus": "Stopped" }
```

---

### GET /serverTimeInfo
Roles: `Admin`, `ApplicationUser`, `Guest`

**Response 200:**
```json
{ "serverTimeZone": "IST", "serverLocalTime": "2025-05-12T15:30:00" }
```

---

## Service Management
Roles: `Admin`, `ApplicationUser` (except assign/updateGroup/updateTags which are `Admin` only).

### POST /assignServices
Role: `Admin`

**Request:**
```json
{ "username": "john", "assignedServices": ["MyService", "OtherService"] }
```
**Response 200:**
```json
{ "message": "Services assigned successfully" }
```

---

### POST /getAssignedServices
**Request:**
```json
{ "USERNAME": "john" }
```
**Response 200:**
```json
{ "username": "john", "assignedService": ["MyService", "OtherService"] }
```

---

### GET /getGroupTagsConfig?serviceName=MyService
**Response 200:**
```json
{ "group": "GroupA", "tags": ["tag1", "tag2"] }
```

---

### POST /updateGroup
Role: `Admin`

**Request:**
```json
{ "serviceName": "MyService", "group": "GroupB" }
```
**Response 200:**
```json
{ "success": true }
```

---

### POST /updateTags
Role: `Admin`

**Request:**
```json
{ "serviceName": "MyService", "tags": ["tag1", "tag3"] }
```
**Response 200:**
```json
{ "success": true }
```
**Response 400:**
```json
{ "error": "Duplicate tags are not allowed." }
```

---

### GET /getDatasourceLists
**Response 200:**
```json
{
  "data": [
    { "serviceName": "MyService", "datasourceEnabled": true }
  ]
}
```

---

### GET /getDatasets?serviceName=MyService
**Response 200:**
```json
{
  "status": "success",
  "serviceName": "MyService",
  "count": 2,
  "datasetFiles": [
    { "fileName": "dataset_v2.csv", "size": 10240, "lastModified": "2025-05-12T10:00:00Z" },
    { "fileName": "dataset_v1.csv", "size": 8192,  "lastModified": "2025-05-10T08:00:00Z" }
  ]
}
```

---

### GET /getDatasets/download?serviceName=MyService&fileName=dataset_v2.csv
**Response 200:** File download (`application/octet-stream`)

---

### DELETE /getDatasets/delete?serviceName=MyService&fileName=dataset_v1.csv
**Response 200:**
```json
{ "status": "success", "message": "File deleted successfully.", "serviceName": "MyService", "fileName": "dataset_v1.csv" }
```

---

## Common Error Responses

| Status | Body |
|--------|------|
| 400 | `{ "error": "..." }` |
| 401 | `{ "error": "..." }` |
| 403 | `{ "error": "..." }` |
| 404 | `{ "error": "..." }` |
| 409 | `{ "message": "..." }` |
| 500 | `{ "error": "Internal server error" }` |
