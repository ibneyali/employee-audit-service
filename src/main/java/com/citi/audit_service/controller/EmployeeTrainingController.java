package com.citi.audit_service.controller;

import com.citi.audit_service.model.EmployeeTraining;
import com.citi.audit_service.service.EmployeeTrainingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/employee-trainings")
@RequiredArgsConstructor
public class EmployeeTrainingController {

    private final EmployeeTrainingService employeeTrainingService;

    @GetMapping
    public ResponseEntity<List<EmployeeTraining>> getAllEmployeeTrainings() {
        List<EmployeeTraining> employeeTrainings = employeeTrainingService.getAllEmployeeTrainings();
        return ResponseEntity.ok(employeeTrainings);
    }

    @GetMapping("/{id}")
    public ResponseEntity<EmployeeTraining> getEmployeeTrainingById(@PathVariable Long id) {
        Optional<EmployeeTraining> employeeTraining = employeeTrainingService.getEmployeeTrainingById(id);
        return employeeTraining.map(ResponseEntity::ok)
                              .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/employee/{employeeId}")
    public ResponseEntity<List<EmployeeTraining>> getTrainingsByEmployee(@PathVariable Long employeeId) {
        List<EmployeeTraining> trainings = employeeTrainingService.getTrainingsByEmployee(employeeId);
        return ResponseEntity.ok(trainings);
    }

    @GetMapping("/training/{trainingId}")
    public ResponseEntity<List<EmployeeTraining>> getEmployeesByTraining(@PathVariable Long trainingId) {
        List<EmployeeTraining> employees = employeeTrainingService.getEmployeesByTraining(trainingId);
        return ResponseEntity.ok(employees);
    }

    @GetMapping("/employee/{employeeId}/status/{status}")
    public ResponseEntity<List<EmployeeTraining>> getEmployeeTrainingsByStatus(
            @PathVariable Long employeeId,
            @PathVariable String status) {
        List<EmployeeTraining> trainings = employeeTrainingService.getEmployeeTrainingsByStatus(employeeId, status);
        return ResponseEntity.ok(trainings);
    }

    @GetMapping("/training/{trainingId}/status/{status}")
    public ResponseEntity<List<EmployeeTraining>> getTrainingEmployeesByStatus(
            @PathVariable Long trainingId,
            @PathVariable String status) {
        List<EmployeeTraining> employees = employeeTrainingService.getTrainingEmployeesByStatus(trainingId, status);
        return ResponseEntity.ok(employees);
    }

    @PostMapping
    public ResponseEntity<EmployeeTraining> assignTraining(@RequestBody EmployeeTraining employeeTraining) {
        EmployeeTraining createdAssignment = employeeTrainingService.assignTraining(employeeTraining);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdAssignment);
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<EmployeeTraining> updateTrainingStatus(
            @PathVariable Long id,
            @RequestParam String status,
            @RequestParam String updatedBy) {
        try {
            EmployeeTraining updatedTraining = employeeTrainingService.updateTrainingStatus(id, status, updatedBy);
            return ResponseEntity.ok(updatedTraining);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> removeTrainingAssignment(@PathVariable Long id) {
        try {
            employeeTrainingService.removeTrainingAssignment(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
