package com.citi.audit_service.controller;

import com.citi.audit_service.model.Employee;
import lombok.RequiredArgsConstructor;
import org.javers.core.diff.Change;
import org.javers.core.metamodel.object.CdoSnapshot;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * REST Controller to query JaVers history and snapshots
 * Provides endpoints to retrieve versioning information for audited entities
 */
@RestController
@RequestMapping("/api/javers")
@RequiredArgsConstructor
public class JaversQueryController {

    private final com.citi.audit_service.service.JaversAuditService javersAuditService;

    /**
     * Get all snapshots for a specific employee
     * Returns complete version history with all field values at each point in time
     */
    @GetMapping("/employee/{employeeId}/snapshots")
    public ResponseEntity<List<SnapshotDTO>> getEmployeeSnapshots(@PathVariable Long employeeId) {
        List<CdoSnapshot> snapshots = javersAuditService.getEntitySnapshots(Employee.class, employeeId);

        List<SnapshotDTO> dtos = snapshots.stream()
                .map(snapshot -> new SnapshotDTO(
                        snapshot.getVersion(),
                        snapshot.getState(),
                        snapshot.getCommitMetadata().getAuthor(),
                        snapshot.getCommitMetadata().getCommitDate(),
                        snapshot.getCommitMetadata().getId().toString()
                ))
                .collect(Collectors.toList());

        return ResponseEntity.ok(dtos);
    }

    /**
     * Get all changes (diffs) for a specific employee
     * Returns field-level changes between versions
     */
    @GetMapping("/employee/{employeeId}/changes")
    public ResponseEntity<List<Change>> getEmployeeChanges(@PathVariable Long employeeId) {
        List<Change> changes = javersAuditService.getEntityChanges(Employee.class, employeeId);
        return ResponseEntity.ok(changes);
    }

    /**
     * DTO for snapshot information
     */
    public static class SnapshotDTO {
        public long version;
        public Object state;
        public String author;
        public Object commitDate;
        public String commitId;

        public SnapshotDTO(long version, Object state, String author, Object commitDate, String commitId) {
            this.version = version;
            this.state = state;
            this.author = author;
            this.commitDate = commitDate;
            this.commitId = commitId;
        }
    }
}

