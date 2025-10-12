package com.citi.audit_service.repository;

import com.citi.audit_service.model.Training;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TrainingRepository extends JpaRepository<Training, Long> {

    Optional<Training> findByName(String name);

    List<Training> findByNameContaining(String name);
}
