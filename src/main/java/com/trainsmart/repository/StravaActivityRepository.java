package com.trainsmart.repository;

import com.trainsmart.entity.StravaActivity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StravaActivityRepository extends JpaRepository<StravaActivity, Long> {
    List<StravaActivity> findByPlanId(String planId);
    List<StravaActivity> findByPlanIdAndMatched(String planId, Boolean matched);
}
