package com.citi.audit_service.controller;

import com.citi.audit_service.model.AuditEvent;
import com.citi.audit_service.service.AuditService;
import com.citi.audit_service.dto.EmployeeAuditHistoryDTO;
import com.citi.audit_service.dto.FieldChangeDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/audit")
@RequiredArgsConstructor
public class AuditController {

    private final AuditService auditService;

    /**
     * Get all audit events
     * GET /api/audit
     */
    @GetMapping
    public ResponseEntity<List<AuditEvent>> getAllAuditEvents() {
        List<AuditEvent> auditEvents = auditService.getAllAuditEvents();
        return ResponseEntity.ok(auditEvents);
    }

    /**
     * Get audit events for a specific employee
     * GET /api/audit/employee/{employeeId}
     */
    @GetMapping("/employee/{employeeId}")
    public ResponseEntity<List<AuditEvent>> getAuditEventsByEmployeeId(@PathVariable Long employeeId) {
        List<AuditEvent> auditEvents = auditService.getAuditEventsByEmployeeId(employeeId);
        return ResponseEntity.ok(auditEvents);
    }

    /**
     * Get audit events by entity type (e.g., EMPLOYEE)
     * GET /api/audit/entity/{entity}
     */
    @GetMapping("/entity/{entity}")
    public ResponseEntity<List<AuditEvent>> getAuditEventsByEntity(@PathVariable String entity) {
        List<AuditEvent> auditEvents = auditService.getAuditEventsByEntity(entity);
        return ResponseEntity.ok(auditEvents);
    }

    /**
     * Get audit events by event name (CREATED, UPDATED, DELETED)
     * GET /api/audit/event/{eventName}
     */
    @GetMapping("/event/{eventName}")
    public ResponseEntity<List<AuditEvent>> getAuditEventsByEventName(@PathVariable String eventName) {
        List<AuditEvent> auditEvents = auditService.getAuditEventsByEventName(eventName);
        return ResponseEntity.ok(auditEvents);
    }

    /**
     * Get audit events by initiator (who made the change)
     * GET /api/audit/initiator/{initiator}
     */
    @GetMapping("/initiator/{initiator}")
    public ResponseEntity<List<AuditEvent>> getAuditEventsByInitiator(@PathVariable String initiator) {
        List<AuditEvent> auditEvents = auditService.getAuditEventsByInitiator(initiator);
        return ResponseEntity.ok(auditEvents);
    }

    /**
     * Get audit events within a date range
     * GET /api/audit/date-range?startDate=2024-01-01T00:00:00&endDate=2024-12-31T23:59:59
     */
    @GetMapping("/date-range")
    public ResponseEntity<List<AuditEvent>> getAuditEventsByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        List<AuditEvent> auditEvents = auditService.getAuditEventsByDateRange(startDate, endDate);
        return ResponseEntity.ok(auditEvents);
    }

    /**
     * Get audit events for a specific entity and entity ID
     * GET /api/audit/entity/{entity}/id/{entityId}
     */
    @GetMapping("/entity/{entity}/id/{entityId}")
    public ResponseEntity<List<AuditEvent>> getAuditEventsByEntityAndId(
            @PathVariable String entity,
            @PathVariable Long entityId) {
        List<AuditEvent> auditEvents = auditService.getAuditEventsByEntityAndId(entity, entityId);
        return ResponseEntity.ok(auditEvents);
    }

    /**
     * Get employee audit history with parsed changes (cleaner format)
     * GET /api/audit/employee/{employeeId}/history
     */
    @GetMapping("/employee/{employeeId}/history")
    public ResponseEntity<List<EmployeeAuditHistoryDTO>> getEmployeeAuditHistory(@PathVariable Long employeeId) {
        List<EmployeeAuditHistoryDTO> history = auditService.getEmployeeAuditHistory(employeeId);
        return ResponseEntity.ok(history);
    }

    /**
     * Get specific field change history for an employee
     * GET /api/audit/employee/{employeeId}/field/{fieldName}
     * Example: /api/audit/employee/7/field/email
     */
    @GetMapping("/employee/{employeeId}/field/{fieldName}")
    public ResponseEntity<List<FieldChangeDTO>> getEmployeeFieldChanges(
            @PathVariable Long employeeId,
            @PathVariable String fieldName) {
        List<FieldChangeDTO> changes = auditService.getEmployeeFieldChanges(employeeId, fieldName);
        return ResponseEntity.ok(changes);
    }
}
