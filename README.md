# Employee Audit Service

A Spring Boot application for managing employee data with **Generic AOP-Based Audit Logging**, comprehensive change tracking, version control, and global exception handling.

---

## 📋 Table of Contents

- [Overview](#overview)
- [Key Features](#key-features)
- [🆕 Generic AOP Audit System](#-generic-aop-audit-system)
- [Database Schema](#database-schema)
- [How Audit System Works](#how-audit-system-works)
- [API Endpoints](#api-endpoints)
- [Enhanced Audit Features](#enhanced-audit-features)
- [Exception Handling](#exception-handling)
- [Getting Started](#getting-started)
- [Testing Examples](#testing-examples)
- [Advanced Topics](#advanced-topics)

---

## Overview

This application provides a complete employee management system with:
- ✅ **Generic AOP-Based Audit Logging** - Works for ANY entity (Employee, Department, Address, etc.)
- ✅ Full CRUD operations for employees, departments, addresses, and trainings
- ✅ Automatic version tracking with optimistic locking
- ✅ **Detailed field-level change tracking** with old/new value comparison
- ✅ Global exception handling with consistent error responses
- ✅ RESTful API with Swagger/OpenAPI documentation
- ✅ Zero business logic pollution - audit logic completely separated using AOP

---

## Key Features

### 🎯 Generic AOP-Based Auditing
- **One Annotation, Automatic Auditing**: Just add `@Auditable` to any service method
- **Works for ALL Entities**: Employee, Department, Address, Training, or any future entity
- **Zero Code Duplication**: Audit logic written once, reused everywhere
- **Deep Copy Change Detection**: Accurately captures field-by-field changes
- **Thread-Safe**: Uses ThreadLocal for concurrent request handling

### 🔍 Detailed Change Tracking
- **Field-Level Detection**: Knows exactly which fields changed
- **Old vs New Comparison**: Captures both states for every update
- **Human-Readable Summaries**: "EMPLOYEE updated: firstName (from 'John' to 'Jane')"
- **JSON Payload Storage**: Complete audit trail with full entity data

### 🔐 Version Control & Optimistic Locking
- **JPA @Version**: Automatic version tracking
- **Concurrent Update Protection**: Prevents lost updates
- **Audit Version Tracking**: Each audit event captures entity version

### 🚨 Comprehensive Exception Handling
- **Centralized Error Handling**: `@ControllerAdvice` for consistent responses
- **User-Friendly Messages**: Clear, actionable error descriptions
- **Proper HTTP Status Codes**: 404, 409, 400, 500, etc.

---

## 🆕 Generic AOP Audit System

### What is AOP (Aspect-Oriented Programming)?

AOP is a programming paradigm that allows you to separate **cross-cutting concerns** (like auditing, logging, security) from your business logic.

**Without AOP (❌ Old Approach):**
```java
public Employee updateEmployee(Long id, Employee details) {
    // ❌ Business logic mixed with audit code
    Employee oldEmployee = getEmployeeById(id);
    Employee updatedEmployee = repository.save(employee);
    auditService.logEmployeeUpdated(oldEmployee, updatedEmployee, initiator); // Manual!
    return updatedEmployee;
}
```

**With AOP (✅ New Approach):**
```java
@Auditable(action = AuditAction.UPDATE, domain = "HR", entity = "EMPLOYEE")
public Employee updateEmployee(Long id, Employee details) {
    // ✅ Pure business logic - audit happens automatically!
    return repository.save(employee);
}
```

### Architecture Overview

```
┌─────────────────────────────────────────────────────────────────┐
│                    CLIENT REQUEST (REST API)                    │
└────────────────────────────┬────────────────────────────────────┘
                             ↓
┌─────────────────────────────────────────────────────────────────┐
│                      CONTROLLER LAYER                           │
│  • EmployeeController                                           │
│  • DepartmentController                                         │
│  • AddressController                                            │
└────────────────────────────┬────────────────────────────────────┘
                             ↓
┌─────────────────────────────────────────────────────────────────┐
│                  🎯 AOP ASPECT (AuditAspect)                   │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │ @Before Advice - Captures OLD state (deep copy)         │  │
│  │   • Executed BEFORE service method                       │  │
│  │   • For UPDATE/DELETE operations                         │  │
│  │   • Stores snapshot in ThreadLocal                       │  │
│  └──────────────────────────────────────────────────────────┘  │
└────────────────────────────┬────────────────────────────────────┘
                             ↓
┌─────────────────────────────────────────────────────────────────┐
│                      SERVICE LAYER                              │
│  • EmployeeService                                              │
│  • DepartmentService                                            │
│  • AddressService                                               │
│  • @Auditable annotated methods                                 │
└────────────────────────────┬────────────────────────────────────┘
                             ↓
┌─────────────────────────────────────────────────────────────────┐
│                  🎯 AOP ASPECT (AuditAspect)                   │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │ @AfterReturning Advice - Logs audit event               │  │
│  │   • Executed AFTER successful service method             │  │
│  │   • Compares OLD vs NEW state                            │  │
│  │   • Calls AuditService to persist event                  │  │
│  └──────────────────────────────────────────────────────────┘  │
└────────────────────────────┬────────────────────────────────────┘
                             ↓
┌─────────────────────────────────────────────────────────────────┐
│                      AUDIT SERVICE                              │
│  • logEntityCreated()                                           │
│  • logEntityUpdated()                                           │
│  • logEntityDeleted()                                           │
│  • detectChanges() - Field comparison                           │
└────────────────────────────┬────────────────────────────────────┘
                             ↓
┌─────────────────────────────────────────────────────────────────┐
│                      DATABASE                                   │
│  • EMPLOYEE, DEPARTMENT, ADDRESS tables                         │
│  • AUDIT_TABLE (audit events)                                   │
└─────────────────────────────────────────────────────────────────┘
```

### How AOP Auditing Works (Step-by-Step)

#### **Step 1: Method Called with @Auditable Annotation**

```java
// In EmployeeService.java
@Auditable(action = AuditAction.UPDATE, domain = "HR", entity = "EMPLOYEE")
public Employee updateEmployee(Long id, Employee details) {
    Employee employee = employeeRepository.findById(id)
        .orElseThrow(() -> new EmployeeNotFoundException(id));
    
    employee.setFirstName(details.getFirstName());
    employee.setLastName(details.getLastName());
    // ... update other fields
    
    return employeeRepository.save(employee);
}
```

#### **Step 2: @Before Advice Intercepts (For UPDATE/DELETE)**

```java
// In AuditAspect.java
@Before("@annotation(auditable)")
public void beforeAuditableMethod(JoinPoint joinPoint, Auditable auditable) {
    if (auditable.action() == AuditAction.UPDATE || auditable.action() == AuditAction.DELETE) {
        // 1. Extract entity ID from method arguments
        Long entityId = (Long) args[0];  // First argument is ID
        
        // 2. Dynamically find the entity (e.g., getEmployeeById)
        Object currentEntity = findEntityById(service, entityId, "EMPLOYEE");
        
        // 3. Create DEEP COPY to capture old state
        Object deepCopy = createDeepCopy(currentEntity);
        
        // 4. Store in ThreadLocal (thread-safe storage)
        oldEntityState.set(deepCopy);
    }
}
```

**Why Deep Copy?**
- Without deep copy: old and new would reference the same object → No changes detected! ❌
- With deep copy: old and new are independent objects → Changes properly detected! ✅

#### **Step 3: Original Service Method Executes**

```java
// Business logic executes normally
employee.setFirstName("Jane");
employee.setLastName("Smith");
return employeeRepository.save(employee);  // Version auto-incremented
```

#### **Step 4: @AfterReturning Advice Logs Audit**

```java
// In AuditAspect.java
@AfterReturning(pointcut = "@annotation(auditable)", returning = "result")
public void afterAuditableMethod(JoinPoint joinPoint, Auditable auditable, Object result) {
    String domain = auditable.domain();    // "HR"
    String entity = auditable.entity();    // "EMPLOYEE"
    String initiator = extractInitiator(args, result);  // "admin@company.com"
    
    switch (auditable.action()) {
        case UPDATE:
            Object oldEntity = oldEntityState.get();  // Get from ThreadLocal
            auditService.logEntityUpdated(domain, entity, oldEntity, result, initiator);
            break;
        // ... other cases
    }
    
    oldEntityState.remove();  // Clean up ThreadLocal
}
```

#### **Step 5: AuditService Detects Changes**

```java
// In AuditService.java
public void logEntityUpdated(String domain, String entity, Object oldEntity, Object newEntity, String initiator) {
    // 1. Detect changes field-by-field
    Map<String, Map<String, Object>> changes = detectChanges(oldEntity, newEntity);
    
    // Example changes:
    // {
    //   "firstName": {"old": "John", "new": "Jane"},
    //   "lastName": {"old": "Doe", "new": "Smith"},
    //   "email": {"old": "john@example.com", "new": "jane@example.com"}
    // }
    
    // 2. Generate human-readable summary
    String summary = "EMPLOYEE updated: firstName (from 'John' to 'Jane'), lastName (from 'Doe' to 'Smith')";
    
    // 3. Create JSON payload with old/new/changes
    Map<String, Object> payload = new HashMap<>();
    payload.put("oldValue", oldEntity);
    payload.put("newValue", newEntity);
    payload.put("changes", changes);
    
    // 4. Save to AUDIT_TABLE
    logEvent(domain, entity, entityId, "UPDATED", payload, summary, version, initiator);
}
```

#### **Step 6: Audit Event Persisted**

```sql
INSERT INTO AUDIT_TABLE (
    EVENT_DOMAIN, EVENT_ENTITY, EVENT_ENTITY_ID, EVENT_NAME,
    EVENT_PAYLOAD, EVENT_SUMMARY, EVENT_ENTITY_VERSION,
    EVENT_TIMESTAMP, EVENT_INITIATOR
) VALUES (
    'HR',
    'EMPLOYEE',
    7,
    'UPDATED',
    '{"oldValue":{...},"newValue":{...},"changes":{...}}',
    'EMPLOYEE updated: firstName (from ''John'' to ''Jane''), lastName (from ''Doe'' to ''Smith'')',
    2,
    '2025-10-17 10:30:15',
    'admin@company.com'
);
```

### Applying Auditing to Any Service

#### Example 1: Employee Service (Already Implemented)

```java
@Service
@RequiredArgsConstructor
@Transactional
public class EmployeeService {

    @Auditable(action = AuditAction.CREATE, domain = "HR", entity = "EMPLOYEE")
    public Employee createEmployee(Employee employee) {
        return employeeRepository.save(employee);
    }

    @Auditable(action = AuditAction.UPDATE, domain = "HR", entity = "EMPLOYEE")
    public Employee updateEmployee(Long id, Employee details) {
        // Business logic only - no audit code!
        return employeeRepository.save(updatedEmployee);
    }

    @Auditable(action = AuditAction.DELETE, domain = "HR", entity = "EMPLOYEE")
    public void deleteEmployee(Long id) {
        employeeRepository.delete(employee);
    }
}
```

#### Example 2: Department Service (Already Implemented)

```java
@Service
@RequiredArgsConstructor
@Transactional
public class DepartmentService {

    @Auditable(action = AuditAction.CREATE, domain = "HR", entity = "DEPARTMENT")
    public Department createDepartment(Department department) {
        return departmentRepository.save(department);
    }

    @Auditable(action = AuditAction.UPDATE, domain = "HR", entity = "DEPARTMENT")
    public Department updateDepartment(Long id, Department details) {
        return departmentRepository.save(updatedDepartment);
    }

    @Auditable(action = AuditAction.DELETE, domain = "HR", entity = "DEPARTMENT")
    public void deleteDepartment(Long id) {
        departmentRepository.delete(department);
    }
}
```

#### Example 3: Address Service (Already Implemented)

```java
@Service
@RequiredArgsConstructor
@Transactional
public class AddressService {

    @Auditable(action = AuditAction.CREATE, domain = "HR", entity = "ADDRESS")
    public Address createAddress(Address address) {
        return addressRepository.save(address);
    }

    @Auditable(action = AuditAction.UPDATE, domain = "HR", entity = "ADDRESS")
    public Address updateAddress(Long id, Address details) {
        return addressRepository.save(updatedAddress);
    }

    @Auditable(action = AuditAction.DELETE, domain = "HR", entity = "ADDRESS")
    public void deleteAddress(Long id) {
        addressRepository.delete(address);
    }
}
```

### @Auditable Annotation Parameters

| Parameter | Required | Description | Example Values |
|-----------|----------|-------------|----------------|
| `action` | ✅ Yes | Type of operation | `AuditAction.CREATE`<br>`AuditAction.UPDATE`<br>`AuditAction.DELETE` |
| `domain` | ❌ No | Business domain | `"HR"`, `"FINANCE"`, `"IT"`<br>Default: `"HR"` |
| `entity` | ❌ No | Entity type name | `"EMPLOYEE"`, `"DEPARTMENT"`, `"ADDRESS"`<br>Default: `"EMPLOYEE"` |

### Key Components

#### 1. AuditAspect.java (The Heart of AOP)

```java
@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class AuditAspect {
    
    private final AuditService auditService;
    private final ObjectMapper objectMapper;
    private final ThreadLocal<Object> oldEntityState = new ThreadLocal<>();
    
    // Intercepts BEFORE method execution
    @Before("@annotation(auditable)")
    public void beforeAuditableMethod(JoinPoint joinPoint, Auditable auditable) {
        // Captures old state for UPDATE/DELETE
    }
    
    // Intercepts AFTER successful method execution
    @AfterReturning(pointcut = "@annotation(auditable)", returning = "result")
    public void afterAuditableMethod(JoinPoint joinPoint, Auditable auditable, Object result) {
        // Logs audit event with change detection
    }
    
    // Creates deep copy to avoid reference issues
    private Object createDeepCopy(Object entity) {
        String json = objectMapper.writeValueAsString(entity);
        return objectMapper.readValue(json, entity.getClass());
    }
}
```

#### 2. AuditService.java (Generic Audit Methods)

```java
@Service
@RequiredArgsConstructor
public class AuditService {
    
    // Generic method - works for ANY entity
    public void logEntityCreated(String domain, String entityType, Object entity, String initiator) {
        String payload = objectMapper.writeValueAsString(entity);
        String summary = entityType + " created";
        logEvent(domain, entityType, entityId, "CREATED", payload, summary, 1, initiator);
    }
    
    // Generic method with change detection
    public void logEntityUpdated(String domain, String entityType, Object oldEntity, Object newEntity, String initiator) {
        Map<String, Object> changePayload = new HashMap<>();
        changePayload.put("oldValue", oldEntity);
        changePayload.put("newValue", newEntity);
        changePayload.put("changes", detectChanges(oldEntity, newEntity));
        
        String summary = generateGenericUpdateSummary(entityType, oldEntity, newEntity);
        logEvent(domain, entityType, entityId, "UPDATED", payload, summary, version, initiator);
    }
    
    // Detects field-by-field changes
    private Map<String, Map<String, Object>> detectChanges(Object oldObj, Object newObj) {
        Map<String, Object> oldMap = objectMapper.convertValue(oldObj, Map.class);
        Map<String, Object> newMap = objectMapper.convertValue(newObj, Map.class);
        
        // Compare each field...
    }
}
```

#### 3. @Auditable Annotation

```java
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Auditable {
    AuditAction action();              // CREATE, UPDATE, DELETE
    String domain() default "HR";      // Business domain
    String entity() default "EMPLOYEE"; // Entity type
}
```

#### 4. AuditAction Enum

```java
public enum AuditAction {
    CREATE,   // For insert operations
    UPDATE,   // For update operations
    DELETE    // For delete operations
}
```

### Benefits of This Approach

| Benefit | Description |
|---------|-------------|
| ✅ **Non-Invasive** | Business logic stays clean - no audit code mixed in |
| ✅ **Reusable** | Write audit logic once, use everywhere |
| ✅ **Consistent** | Same audit structure for all entities |
| ✅ **Easy to Maintain** | Change audit logic in one place |
| ✅ **Type-Safe** | Compile-time checking with annotations |
| ✅ **Automatic** | Developers can't forget to log audits |
| ✅ **Thread-Safe** | ThreadLocal ensures concurrent request safety |
| ✅ **Accurate** | Deep copy ensures correct change detection |

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
