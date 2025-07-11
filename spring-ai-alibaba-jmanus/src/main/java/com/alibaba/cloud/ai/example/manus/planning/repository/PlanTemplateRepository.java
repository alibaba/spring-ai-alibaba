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
package com.alibaba.cloud.ai.example.manus.planning.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.alibaba.cloud.ai.example.manus.planning.model.po.PlanTemplate;

/**
 * The data access interface for the plan template
 */
@Repository
public interface PlanTemplateRepository extends JpaRepository<PlanTemplate, String> {

	/**
	 * Find the plan template by the plan template ID
	 * @param planTemplateId the plan template ID
	 * @return the plan template entity
	 */
	Optional<PlanTemplate> findByPlanTemplateId(String planTemplateId);

	/**
	 * Delete the plan template by the plan template ID
	 * @param planTemplateId the plan template ID
	 */
	void deleteByPlanTemplateId(String planTemplateId);

}
