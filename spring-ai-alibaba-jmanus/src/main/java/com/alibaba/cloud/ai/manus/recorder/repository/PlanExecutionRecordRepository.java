/*
 * Copyright 2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.cloud.ai.manus.recorder.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.alibaba.cloud.ai.manus.recorder.entity.po.PlanExecutionRecordEntity;

import java.util.List;
import java.util.Optional;

@Repository
public interface PlanExecutionRecordRepository extends JpaRepository<PlanExecutionRecordEntity, Long> {

	/**
	 * Find plan execution record by current plan ID
	 */
	Optional<PlanExecutionRecordEntity> findByCurrentPlanId(String currentPlanId);

	/**
	 * Find all plan execution records by parent plan ID
	 */
	List<PlanExecutionRecordEntity> findByParentPlanId(String parentPlanId);

	/**
	 * Find all plan execution records by root plan ID
	 */
	List<PlanExecutionRecordEntity> findByRootPlanId(String rootPlanId);

	/**
	 * Check if a plan execution record exists by current plan ID
	 */
	boolean existsByCurrentPlanId(String currentPlanId);

	/**
	 * Delete plan execution record by current plan ID
	 */
	void deleteByCurrentPlanId(String currentPlanId);

}
