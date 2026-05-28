package com.trainsmart.repository;

import com.trainsmart.entity.TrainingPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TrainingPlanRepository extends JpaRepository<TrainingPlan, String> {
    Optional<TrainingPlan> findByShareCode(String shareCode);
}
