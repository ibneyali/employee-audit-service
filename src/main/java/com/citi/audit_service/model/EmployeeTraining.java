package com.citi.audit_service.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "EMPLOYEE_TRAINING")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmployeeTraining {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "EMP_ID", nullable = false)
    private Long empId;

    @Column(name = "TRAINING_ID", nullable = false)
    private Long trainingId;

    @Column(name = "DATE_OF_ALLOCATION")
    private LocalDate dateOfAllocation;

    @Column(name = "STATUS")
    private String status;

    @Column(name = "CREATED_TIMESTAMP", nullable = false)
    private LocalDateTime createdTimestamp;

    @Column(name = "UPDATED_TIMESTAMP", nullable = false)
    private LocalDateTime updatedTimestamp;

    @Column(name = "UPDATED_BY")
    private String updatedBy;

    // Status constants for convenience
    public static final String STATUS_ALLOCATED = "ALLOCATED";
    public static final String STATUS_IN_PROGRESS = "IN_PROGRESS";
    public static final String STATUS_COMPLETED = "COMPLETED";
    public static final String STATUS_CANCELLED = "CANCELLED";
}
