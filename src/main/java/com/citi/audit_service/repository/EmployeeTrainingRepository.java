package com.citi.audit_service.repository;

import com.citi.audit_service.model.EmployeeTraining;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EmployeeTrainingRepository extends JpaRepository<EmployeeTraining, Long> {

    List<EmployeeTraining> findByEmpId(Long empId);

    List<EmployeeTraining> findByTrainingId(Long trainingId);

    List<EmployeeTraining> findByEmpIdAndStatus(Long empId, String status);

    List<EmployeeTraining> findByTrainingIdAndStatus(Long trainingId, String status);
}
