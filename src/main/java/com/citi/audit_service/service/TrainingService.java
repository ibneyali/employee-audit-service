package com.citi.audit_service.service;

import com.citi.audit_service.model.Training;
import com.citi.audit_service.repository.TrainingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class TrainingService {

    private final TrainingRepository trainingRepository;

    public List<Training> getAllTrainings() {
        return (List<Training>) trainingRepository.findAll();
    }

    public Optional<Training> getTrainingById(Long id) {
        return trainingRepository.findById(id);
    }

    public Optional<Training> getTrainingByName(String name) {
        return trainingRepository.findByName(name);
    }

    public List<Training> searchTrainingsByName(String name) {
        return trainingRepository.findByNameContaining(name);
    }

    public Training createTraining(Training training) {
        return trainingRepository.save(training);
    }

    public Training updateTraining(Long id, Training trainingDetails) {
        Training training = trainingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Training not found with id: " + id));

        training.setName(trainingDetails.getName());
        training.setDescription(trainingDetails.getDescription());
        training.setUpdatedBy(trainingDetails.getUpdatedBy());

        return trainingRepository.save(training);
    }

    public void deleteTraining(Long id) {
        Training training = trainingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Training not found with id: " + id));
        trainingRepository.delete(training);
    }
}
