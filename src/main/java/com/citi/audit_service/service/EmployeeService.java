package com.citi.audit_service.service;

import com.citi.audit_service.model.Employee;
import com.citi.audit_service.repository.EmployeeRepository;
import com.citi.audit_service.exception.EmployeeNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final AuditService auditService;

    public List<Employee> getAllEmployees() {
        return (List<Employee>) employeeRepository.findAll();
    }

    public Optional<Employee> getEmployeeById(Long id) {
        return employeeRepository.findById(id);
    }

    public Optional<Employee> getEmployeeByEmail(String email) {
        return employeeRepository.findByEmail(email);
    }

    public List<Employee> getEmployeesByDepartment(Long departmentId) {
        return employeeRepository.findByDepartmentId(departmentId);
    }

    public List<Employee> searchEmployeesByName(String name) {
        return employeeRepository.findByNameContaining(name);
    }

    public List<Employee> getEmployeesByTraining(Long trainingId) {
        return employeeRepository.findByTrainingId(trainingId);
    }

    public Employee createEmployee(Employee employee) {
        // Set timestamps
        employee.setCreatedTimestamp(LocalDateTime.now());
        employee.setUpdatedTimestamp(LocalDateTime.now());

        // Handle null/zero foreign keys - set to null if 0 to avoid FK constraint violation
        if (employee.getDepartmentId() != null && employee.getDepartmentId() == 0) {
            employee.setDepartmentId(null);
        }
        if (employee.getAddressId() != null && employee.getAddressId() == 0) {
            employee.setAddressId(null);
        }

        // Save employee (version will be automatically set to 0 by JPA)
        Employee savedEmployee = employeeRepository.save(employee);

        // Log audit event for creation
        String initiator = employee.getUpdatedBy() != null ? employee.getUpdatedBy() : "SYSTEM";
        auditService.logEmployeeCreated(savedEmployee, initiator);

        return savedEmployee;
    }

    public Employee updateEmployee(Long id, Employee employeeDetails) {
        Employee existingEmployee = employeeRepository.findById(id)
                .orElseThrow(() -> new EmployeeNotFoundException(id));

        // Create a copy of the old employee state for audit logging
        Employee oldEmployeeSnapshot = Employee.builder()
                .id(existingEmployee.getId())
                .firstName(existingEmployee.getFirstName())
                .lastName(existingEmployee.getLastName())
                .email(existingEmployee.getEmail())
                .phone(existingEmployee.getPhone())
                .departmentId(existingEmployee.getDepartmentId())
                .addressId(existingEmployee.getAddressId())
                .createdTimestamp(existingEmployee.getCreatedTimestamp())
                .updatedTimestamp(existingEmployee.getUpdatedTimestamp())
                .updatedBy(existingEmployee.getUpdatedBy())
                .version(existingEmployee.getVersion())
                .build();

        // Update employee fields
        existingEmployee.setFirstName(employeeDetails.getFirstName());
        existingEmployee.setLastName(employeeDetails.getLastName());
        existingEmployee.setEmail(employeeDetails.getEmail());
        existingEmployee.setPhone(employeeDetails.getPhone());
        existingEmployee.setDepartmentId(employeeDetails.getDepartmentId());
        existingEmployee.setAddressId(employeeDetails.getAddressId());
        existingEmployee.setUpdatedBy(employeeDetails.getUpdatedBy());
        existingEmployee.setUpdatedTimestamp(LocalDateTime.now());
        // Note: version is auto-incremented by JPA @Version annotation

        // Save updated employee
        Employee updatedEmployee = employeeRepository.save(existingEmployee);

        // Log audit event for update with old and new values
        String initiator = employeeDetails.getUpdatedBy() != null ? employeeDetails.getUpdatedBy() : "SYSTEM";
        auditService.logEmployeeUpdated(oldEmployeeSnapshot, updatedEmployee, initiator);

        return updatedEmployee;
    }

    public void deleteEmployee(Long id) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new EmployeeNotFoundException(id));

        // Create a snapshot before deletion for audit
        Employee employeeSnapshot = Employee.builder()
                .id(employee.getId())
                .firstName(employee.getFirstName())
                .lastName(employee.getLastName())
                .email(employee.getEmail())
                .phone(employee.getPhone())
                .departmentId(employee.getDepartmentId())
                .addressId(employee.getAddressId())
                .createdTimestamp(employee.getCreatedTimestamp())
                .updatedTimestamp(employee.getUpdatedTimestamp())
                .updatedBy(employee.getUpdatedBy())
                .version(employee.getVersion())
                .build();

        // Delete employee
        employeeRepository.delete(employee);

        // Log audit event for deletion
        String initiator = employee.getUpdatedBy() != null ? employee.getUpdatedBy() : "SYSTEM";
        auditService.logEmployeeDeleted(employeeSnapshot, initiator);
    }
}
