package com.citi.audit_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeAuditHistoryDTO {
    private Long employeeId;
    private String eventName;  // CREATED, UPDATED, DELETED
    private LocalDateTime eventTimestamp;
    private String eventInitiator;
    private Integer version;
    private List<FieldChangeDTO> changes;  // Only for UPDATE events
    private Object currentState;  // The state after this event
}


