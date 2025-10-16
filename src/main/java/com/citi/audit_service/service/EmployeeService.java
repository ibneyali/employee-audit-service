package com.citi.audit_service.service;

import com.citi.audit_service.annotation.AuditAction;
import com.citi.audit_service.annotation.Auditable;
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

    @Auditable(action = AuditAction.CREATE)
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

        return savedEmployee;
    }

    @Auditable(action = AuditAction.UPDATE)
    public Employee updateEmployee(Long id, Employee employeeDetails) {
        Employee existingEmployee = employeeRepository.findById(id)
                .orElseThrow(() -> new EmployeeNotFoundException(id));

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

        return updatedEmployee;
    }

    @Auditable(action = AuditAction.DELETE)
    public void deleteEmployee(Long id) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new EmployeeNotFoundException(id));

        // Delete employee
        employeeRepository.delete(employee);
    }
}
