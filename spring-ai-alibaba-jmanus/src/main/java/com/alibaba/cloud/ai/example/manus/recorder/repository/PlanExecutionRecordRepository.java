package com.alibaba.cloud.ai.example.manus.recorder.repository;

import com.alibaba.cloud.ai.example.manus.recorder.entity.PlanExecutionRecordEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PlanExecutionRecordRepository extends JpaRepository<PlanExecutionRecordEntity, String> {

	PlanExecutionRecordEntity findByPlanId(String planId);

	PlanExecutionRecordEntity deleteByPlanId(String planId);

}
