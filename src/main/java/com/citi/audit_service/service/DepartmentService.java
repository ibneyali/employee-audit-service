package com.citi.audit_service.service;

import com.citi.audit_service.annotation.AuditAction;
import com.citi.audit_service.annotation.Auditable;
import com.citi.audit_service.model.Department;
import com.citi.audit_service.repository.DepartmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class DepartmentService {

    private final DepartmentRepository departmentRepository;

    public List<Department> getAllDepartments() {
        return (List<Department>) departmentRepository.findAll();
    }

    public Optional<Department> getDepartmentById(Long id) {
        return departmentRepository.findById(id);
    }

    public Optional<Department> getDepartmentByName(String name) {
        return departmentRepository.findByName(name);
    }

    @Auditable(action = AuditAction.CREATE, domain = "HR", entity = "DEPARTMENT")
    public Department createDepartment(Department department) {
        department.setCreatedTimestamp(LocalDateTime.now());
        department.setUpdatedTimestamp(LocalDateTime.now());
        return departmentRepository.save(department);
    }

    @Auditable(action = AuditAction.UPDATE, domain = "HR", entity = "DEPARTMENT")
    public Department updateDepartment(Long id, Department departmentDetails) {
        Department department = departmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Department not found with id: " + id));

        department.setName(departmentDetails.getName());
        department.setUpdatedBy(departmentDetails.getUpdatedBy());
        department.setUpdatedTimestamp(LocalDateTime.now());

        return departmentRepository.save(department);
    }

    @Auditable(action = AuditAction.DELETE, domain = "HR", entity = "DEPARTMENT")
    public void deleteDepartment(Long id) {
        Department department = departmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Department not found with id: " + id));
        departmentRepository.delete(department);
    }
}
