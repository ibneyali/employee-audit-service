package com.citi.audit_service.repository;

import com.citi.audit_service.model.Employee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, Long> {

    Optional<Employee> findByEmail(String email);

    List<Employee> findByDepartmentId(Long departmentId);

    @Query("SELECT e FROM Employee e WHERE e.firstName LIKE CONCAT('%', :name, '%') OR e.lastName LIKE CONCAT('%', :name, '%')")
    List<Employee> findByNameContaining(@Param("name") String name);

    @Query("SELECT e FROM Employee e WHERE e.id IN (SELECT et.empId FROM EmployeeTraining et WHERE et.trainingId = :trainingId)")
    List<Employee> findByTrainingId(@Param("trainingId") Long trainingId);
}
