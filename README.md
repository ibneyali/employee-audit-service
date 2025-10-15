# Employee Audit Service

A Spring Boot application for managing employee data with comprehensive audit logging, version tracking, and global exception handling.

---

## 📋 Table of Contents

- [Overview](#overview)
- [Database Schema](#database-schema)
- [Features](#features)
- [Audit Logging System](#audit-logging-system)
- [API Endpoints](#api-endpoints)
- [Enhanced Audit Features](#enhanced-audit-features)
- [Exception Handling](#exception-handling)
- [Getting Started](#getting-started)
- [Testing Examples](#testing-examples)

---

## Overview

This application provides a complete employee management system with:
- ✅ Full CRUD operations for employees
- ✅ Comprehensive audit logging for all changes
- ✅ Automatic version tracking with optimistic locking
- ✅ Field-level change tracking
- ✅ Global exception handling with consistent error responses
- ✅ RESTful API with Swagger/OpenAPI documentation

---

## Database Schema

### Core Tables

#### EMPLOYEE
```sql
ID (PK)
FIRST_NAME
LAST_NAME
EMAIL (UNIQUE)
PHONE
DEPARTMENT_ID (FK → DEPARTMENT.ID)
ADDRESS_ID (FK → ADDRESS.ID)
CREATED_TIMESTAMP
UPDATED_TIMESTAMP
UPDATED_BY
VERSION (for optimistic locking)
```

#### DEPARTMENT
```sql
ID (PK)
NAME
CREATED_TIMESTAMP
UPDATED_TIMESTAMP
UPDATED_BY
```

#### ADDRESS
```sql
ID (PK)
ADDRESS_LINE_1
ADDRESS_LINE_2
ADDRESS_LINE_3
COUNTRY
POSTAL_CODE
CREATED_TIMESTAMP
UPDATED_TIMESTAMP
UPDATED_BY
```

#### TRAINING
```sql
ID (PK)
NAME
DESCRIPTION
CREATED_TIMESTAMP
UPDATED_TIMESTAMP
UPDATED_BY
```

#### EMPLOYEE_TRAINING
```sql
ID (PK)
EMP_ID (FK → EMPLOYEE.ID)
TRAINING_ID (FK → TRAINING.ID)
DATE_OF_ALLOCATION
STATUS
CREATED_TIMESTAMP
UPDATED_TIMESTAMP
UPDATED_BY
```

### Audit Table

#### AUDIT_TABLE
```sql
EVENT_ID (PK, Auto-generated)
EVENT_DOMAIN (e.g., "HR")
EVENT_ENTITY (e.g., "EMPLOYEE")
EVENT_ENTITY_ID (Employee ID)
EVENT_NAME (CREATED, UPDATED, DELETED)
EVENT_PAYLOAD (JSON with event details)
EVENT_SUMMARY (Human-readable summary)
EVENT_ENTITY_VERSION (Entity version at event time)
EVENT_TIMESTAMP (When event occurred)
ENTRY_TIMESTAMP (When audit entry created)
EVENT_INITIATOR (Who triggered the event)
```

---

## Features

### 1. **Automatic Audit Logging**
- Every CREATE, UPDATE, and DELETE operation is automatically logged
- No manual audit calls needed in controllers
- Complete audit trail for compliance and forensics

### 2. **Version Tracking**
- JPA `@Version` annotation for optimistic locking
- Automatically incremented on each update
- Prevents lost updates from concurrent modifications
- Version stored in audit logs for historical tracking

### 3. **Change Detection**
- Automatically detects which fields changed during updates
- Compares old vs new values
- Generates human-readable change summaries
- Field-by-field change tracking

### 4. **Global Exception Handling**
- Centralized exception handling with `@ControllerAdvice`
- Consistent error response format across all endpoints
- User-friendly error messages
- Proper HTTP status codes

---

## Audit Logging System

### How It Works

#### Employee Creation (CREATED Event)
```
POST /api/employees
  ↓
EmployeeService.createEmployee()
  ↓
Save to database
  ↓
AuditService.logEmployeeCreated() [automatic]
  ↓
Audit entry in AUDIT_TABLE
```

**Audit Entry:**
```json
{
  "EVENT_DOMAIN": "HR",
  "EVENT_ENTITY": "EMPLOYEE",
  "EVENT_ENTITY_ID": 123,
  "EVENT_NAME": "CREATED",
  "EVENT_PAYLOAD": "{\"id\":123,\"firstName\":\"John\",\"lastName\":\"Doe\",\"email\":\"john.doe@example.com\"}",
  "EVENT_SUMMARY": "Employee created",
  "EVENT_ENTITY_VERSION": 1,
  "EVENT_INITIATOR": "admin@company.com"
}
```

#### Employee Update (UPDATED Event)
```
PUT /api/employees/{id}
  ↓
Capture old employee state (snapshot)
  ↓
Update employee fields
  ↓
Save to database (version auto-incremented)
  ↓
AuditService.logEmployeeUpdated() [automatic]
  ↓
Audit entry with old/new comparison
```

**Audit Entry with Change Detection:**
```json
{
  "EVENT_NAME": "UPDATED",
  "EVENT_PAYLOAD": {
    "oldValue": {
      "firstName": "John",
      "lastName": "Doe",
      "email": "john.doe@example.com",
      "departmentId": 1,
      "version": 0
    },
    "newValue": {
      "firstName": "John",
      "lastName": "Smith",
      "email": "john.smith@example.com",
      "departmentId": 2,
      "version": 1
    },
    "changes": {
      "lastName": {"old": "Doe", "new": "Smith"},
      "email": {"old": "john.doe@example.com", "new": "john.smith@example.com"},
      "departmentId": {"old": 1, "new": 2},
      "version": {"old": 0, "new": 1}
    }
  },
  "EVENT_SUMMARY": "Employee updated: lastName, email, departmentId"
}
```

#### Employee Deletion (DELETED Event)
```
DELETE /api/employees/{id}
  ↓
Capture employee snapshot
  ↓
Delete from database
  ↓
AuditService.logEmployeeDeleted() [automatic]
  ↓
Audit entry with final state
```

---

## API Endpoints

### Employee Management

#### Create Employee
```http
POST /api/employees
Content-Type: application/json

{
  "firstName": "John",
  "lastName": "Doe",
  "email": "john.doe@example.com",
  "phone": "+1-555-0101",
  "departmentId": 1,
  "addressId": 1,
  "updatedBy": "admin@company.com"
}
```

#### Get All Employees
```http
GET /api/employees
```

#### Get Employee by ID
```http
GET /api/employees/{id}
```

#### Update Employee
```http
PUT /api/employees/{id}
Content-Type: application/json

{
  "firstName": "John",
  "lastName": "Smith",
  "email": "john.smith@example.com",
  "phone": "+1-555-0101",
  "departmentId": 2,
  "addressId": 2,
  "updatedBy": "admin@company.com"
}
```

#### Delete Employee
```http
DELETE /api/employees/{id}
```

#### Search Employees
```http
GET /api/employees/search?name={name}
GET /api/employees/email/{email}
GET /api/employees/department/{departmentId}
GET /api/employees/training/{trainingId}
```

### Audit Log Endpoints

#### 1. Get All Audit Events
```http
GET /api/audit
```

#### 2. Get Audit Events for Specific Employee
```http
GET /api/audit/employee/{employeeId}
```

#### 3. Get Audit Events by Event Type
```http
GET /api/audit/event/CREATED
GET /api/audit/event/UPDATED
GET /api/audit/event/DELETED
```

#### 4. Get Audit Events by Initiator
```http
GET /api/audit/initiator/{initiator}
```

#### 5. Get Audit Events by Date Range
```http
GET /api/audit/date-range?startDate=2025-10-01T00:00:00&endDate=2025-10-31T23:59:59
```

#### 6. Get Audit Events by Entity Type
```http
GET /api/audit/entity/EMPLOYEE
```

#### 7. Get Audit Events by Entity and ID
```http
GET /api/audit/entity/EMPLOYEE/id/{entityId}
```

---

## Enhanced Audit Features

### Clean Audit History (Recommended)

Get parsed, easy-to-read audit history without JSON strings:

```http
GET /api/audit/employee/{employeeId}/history
```

**Example:**
```bash
curl http://localhost:8080/api/audit/employee/7/history
```

**Response:**
```json
[
  {
    "employeeId": 7,
    "eventName": "CREATED",
    "eventTimestamp": "2025-10-15T09:01:34.857847",
    "eventInitiator": "admin",
    "version": 1,
    "changes": null,
    "currentState": {
      "id": 7,
      "firstName": "John",
      "lastName": "Doe",
      "email": "john.doe@example.com",
      "version": 0
    }
  },
  {
    "employeeId": 7,
    "eventName": "UPDATED",
    "eventTimestamp": "2025-10-15T09:34:02.520991",
    "eventInitiator": "admin",
    "version": 2,
    "changes": [
      {
        "fieldName": "firstName",
        "oldValue": "John",
        "newValue": "Jane"
      },
      {
        "fieldName": "lastName",
        "oldValue": "Doe",
        "newValue": "Smith"
      },
      {
        "fieldName": "email",
        "oldValue": "john.doe@example.com",
        "newValue": "jane.smith@example.com"
      }
    ],
    "currentState": {
      "id": 7,
      "firstName": "Jane",
      "lastName": "Smith",
      "email": "jane.smith@example.com",
      "version": 1
    }
  }
]
```

### Field-Specific Change Tracking

Track changes for a specific field:

```http
GET /api/audit/employee/{employeeId}/field/{fieldName}
```

**Examples:**

Track email changes:
```bash
curl http://localhost:8080/api/audit/employee/7/field/email
```

Track department transfers:
```bash
curl http://localhost:8080/api/audit/employee/7/field/departmentId
```

**Response:**
```json
[
  {
    "fieldName": "email",
    "oldValue": "john.doe@example.com",
    "newValue": "jane.smith@example.com"
  },
  {
    "fieldName": "email",
    "oldValue": "jane.smith@example.com",
    "newValue": "jane.doe@example.com"
  }
]
```

**Supported Fields:**
- `firstName`, `lastName`, `email`, `phone`
- `departmentId`, `addressId`
- `version`, `updatedBy`, `updatedTimestamp`

---

## Exception Handling

### Global Exception Handler

All exceptions are handled centrally by `@ControllerAdvice`, providing consistent error responses.

### Error Response Format

```json
{
  "timestamp": "2025-10-15T10:30:00",
  "status": 404,
  "error": "Not Found",
  "message": "Employee not found with id: 123",
  "path": "/api/employees/123"
}
```

### Custom Exceptions

| Exception | HTTP Status | Description |
|-----------|-------------|-------------|
| `EmployeeNotFoundException` | 404 | Employee doesn't exist |
| `InvalidDataException` | 400 | Invalid input data |
| `AuditSerializationException` | 500 | Audit logging failed |
| `DataIntegrityViolationException` | 409 | Database constraint violation |
| `MethodArgumentTypeMismatchException` | 400 | Wrong parameter type |

### Exception Examples

#### Employee Not Found
```bash
curl http://localhost:8080/api/employees/999
```
**Response (404):**
```json
{
  "timestamp": "2025-10-15T10:30:15",
  "status": 404,
  "error": "Not Found",
  "message": "Employee not found with id: 999",
  "path": "/api/employees/999"
}
```

#### Duplicate Email
```bash
curl -X POST http://localhost:8080/api/employees \
  -H "Content-Type: application/json" \
  -d '{"firstName":"John","email":"existing@email.com"}'
```
**Response (409 Conflict):**
```json
{
  "status": 409,
  "error": "Conflict",
  "message": "Data integrity violation: Duplicate entry found. Email or other unique field already exists."
}
```

#### Invalid Foreign Key
```bash
curl -X POST http://localhost:8080/api/employees \
  -d '{"firstName":"Jane","email":"jane@test.com","departmentId":999}'
```
**Response (409 Conflict):**
```json
{
  "message": "Data integrity violation: Invalid reference. Please ensure department and address IDs are valid."
}
```

---

## Getting Started

### Prerequisites
- Java 21
- Gradle

### Running the Application

```bash
# Clone the repository
git clone <repository-url>
cd employee-audit-service

# Run the application
./gradlew bootRun

# Or on Windows
gradlew.bat bootRun
```

The application will start on `http://localhost:8080`

### Access Swagger UI

```
http://localhost:8080/swagger-ui.html
```

### Database

The application uses HSQLDB (in-memory database) with:
- Sample data pre-loaded (5 employees, departments, addresses, trainings)
- Schema automatically created on startup
- All data is lost when application stops

---

## Testing Examples

### 1. Create an Employee

```bash
curl -X POST http://localhost:8080/api/employees \
  -H "Content-Type: application/json" \
  -d '{
    "firstName": "Jane",
    "lastName": "Doe",
    "email": "jane.doe@company.com",
    "phone": "+1-555-0199",
    "departmentId": 1,
    "addressId": 1,
    "updatedBy": "admin@company.com"
  }'
```

**Expected:** Employee created with `version: 0`, audit log entry created

### 2. View Audit Log

```bash
curl http://localhost:8080/api/audit/employee/6/history
```

**Expected:** Shows CREATED event with full employee details

### 3. Update Employee

```bash
curl -X PUT http://localhost:8080/api/employees/6 \
  -H "Content-Type: application/json" \
  -d '{
    "firstName": "Jane",
    "lastName": "Smith",
    "email": "jane.smith@company.com",
    "phone": "+1-555-0199",
    "departmentId": 2,
    "addressId": 2,
    "updatedBy": "admin@company.com"
  }'
```

**Expected:** Employee updated, `version` incremented to 1

### 4. View Updated Audit History

```bash
curl http://localhost:8080/api/audit/employee/6/history
```

**Expected:** Shows both CREATED and UPDATED events with field-by-field changes

### 5. Track Specific Field Changes

```bash
# Track email changes
curl http://localhost:8080/api/audit/employee/6/field/email

# Track department transfers
curl http://localhost:8080/api/audit/employee/6/field/departmentId
```

### 6. Delete Employee

```bash
curl -X DELETE http://localhost:8080/api/employees/6
```

**Expected:** Employee deleted, audit log captures final state

### 7. View Complete Audit Trail

```bash
curl http://localhost:8080/api/audit/employee/6/history
```

**Expected:** Shows all three events: CREATED → UPDATED → DELETED

---

## Version Tracking Details

### JPA @Version Annotation

```java
@Version
@Column(name = "VERSION")
private Integer version;
```

### Version Flow

```
Employee Created:
  - version = 0
  - Audit: eventEntityVersion = 1

Employee Updated (1st time):
  - version = 1 (auto-incremented by JPA)
  - Audit: eventEntityVersion = 2

Employee Updated (2nd time):
  - version = 2 (auto-incremented by JPA)
  - Audit: eventEntityVersion = 3
```

### Optimistic Locking

- Prevents concurrent update conflicts
- If two users try to update simultaneously:
  - First update succeeds
  - Second update throws `OptimisticLockException`
  - User must refresh and retry

---

## Key Benefits

### Audit Logging
✅ **Automatic** - No manual audit calls needed  
✅ **Complete History** - Track all changes to employee records  
✅ **Change Detection** - Automatically identifies what changed  
✅ **Compliance Ready** - Full audit trail for regulatory requirements  
✅ **Forensic Analysis** - Reconstruct employee state at any point in time  
✅ **User Attribution** - Track who made each change  
✅ **Transactional** - Audit logs part of same transaction (rollback safe)  

### Exception Handling
✅ **Centralized** - One place to manage all exceptions  
✅ **Consistent** - All errors follow same format  
✅ **Clean Code** - No try-catch blocks in controllers  
✅ **User-Friendly** - Clear error messages  
✅ **Proper Status Codes** - Correct HTTP status for each error  

### Version Tracking
✅ **Automatic** - JPA handles version incrementation  
✅ **Optimistic Locking** - Prevents lost updates  
✅ **Audit Integration** - Version stored in audit logs  
✅ **Conflict Detection** - Know when concurrent updates occur  

---

## Advanced Use Cases

### 1. Compliance Reporting
```bash
# Monthly report of all changes
curl "http://localhost:8080/api/audit/date-range?startDate=2025-10-01T00:00:00&endDate=2025-10-31T23:59:59"
```

### 2. Security Audit
```bash
# Track all actions by a specific user
curl http://localhost:8080/api/audit/initiator/admin@company.com
```

### 3. Data Recovery
```bash
# View final state of deleted employee
curl http://localhost:8080/api/audit/employee/123/history
```

### 4. Change Analytics
```bash
# See all updates across the system
curl http://localhost:8080/api/audit/event/UPDATED
```

---

## Technology Stack

- **Spring Boot 3.5.6**
- **Spring Data JPA**
- **HSQLDB** (in-memory database)
- **Lombok** (code generation)
- **Jackson** (JSON processing)
- **SpringDoc OpenAPI** (Swagger documentation)
- **Java 21**

---

## Project Structure

```
src/main/java/com/citi/audit_service/
├── config/
│   └── OpenApiConfig.java
├── controller/
│   ├── EmployeeController.java
│   ├── AuditController.java
│   ├── DepartmentController.java
│   ├── AddressController.java
│   └── TrainingController.java
├── dto/
│   ├── AuditEventDTO.java
│   ├── EmployeeAuditHistoryDTO.java
│   └── FieldChangeDTO.java
├── exception/
│   ├── GlobalExceptionHandler.java
│   ├── EmployeeNotFoundException.java
│   ├── InvalidDataException.java
│   ├── AuditSerializationException.java
│   └── ErrorResponse.java
├── model/
│   ├── Employee.java
│   ├── AuditEvent.java
│   ├── Department.java
│   ├── Address.java
│   └── Training.java
├── repository/
│   ├── EmployeeRepository.java
│   ├── AuditEventRepository.java
│   └── ...
├── service/
│   ├── EmployeeService.java
│   ├── AuditService.java
│   └── ...
└── AuditServiceApplication.java
```

---

## Contributing

Feel free to submit issues and enhancement requests!

---

## License

This project is for educational/demonstration purposes.

---

## Contact

For questions or support, please contact the development team.

---

**Built with ❤️ using Spring Boot**

