package com.citi.audit_service.service;

import com.citi.audit_service.model.EmployeeTraining;
import com.citi.audit_service.repository.EmployeeTrainingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class EmployeeTrainingService {

    private final EmployeeTrainingRepository employeeTrainingRepository;

    public List<EmployeeTraining> getAllEmployeeTrainings() {
        return (List<EmployeeTraining>) employeeTrainingRepository.findAll();
    }

    public Optional<EmployeeTraining> getEmployeeTrainingById(Long id) {
        return employeeTrainingRepository.findById(id);
    }

    public List<EmployeeTraining> getTrainingsByEmployee(Long employeeId) {
        return employeeTrainingRepository.findByEmpId(employeeId);
    }

    public List<EmployeeTraining> getEmployeesByTraining(Long trainingId) {
        return employeeTrainingRepository.findByTrainingId(trainingId);
    }

    public List<EmployeeTraining> getEmployeeTrainingsByStatus(Long employeeId, String status) {
        return employeeTrainingRepository.findByEmpIdAndStatus(employeeId, status);
    }

    public List<EmployeeTraining> getTrainingEmployeesByStatus(Long trainingId, String status) {
        return employeeTrainingRepository.findByTrainingIdAndStatus(trainingId, status);
    }

    public EmployeeTraining assignTraining(EmployeeTraining employeeTraining) {
        return employeeTrainingRepository.save(employeeTraining);
    }

    public EmployeeTraining updateTrainingStatus(Long id, String status, String updatedBy) {
        EmployeeTraining employeeTraining = employeeTrainingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Employee Training not found with id: " + id));

        employeeTraining.setStatus(status);
        employeeTraining.setUpdatedBy(updatedBy);

        return employeeTrainingRepository.save(employeeTraining);
    }

    public void removeTrainingAssignment(Long id) {
        EmployeeTraining employeeTraining = employeeTrainingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Employee Training not found with id: " + id));
        employeeTrainingRepository.delete(employeeTraining);
    }
}
