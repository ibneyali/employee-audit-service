package com.citi.audit_service.dto;

import com.fasterxml.jackson.annotation.JsonRawValue;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AuditEventDTO {
    private Long eventId;
    private String eventDomain;
    private String eventEntity;
    private Long eventEntityId;
    private String eventName;

    @JsonRawValue  // This will parse the JSON string into actual JSON object
    private String eventPayload;

    private String eventSummary;
    private Integer eventEntityVersion;
    private LocalDateTime eventTimestamp;
    private LocalDateTime entryTimestamp;
    private String eventInitiator;
}