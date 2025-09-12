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
package com.alibaba.cloud.ai.manus.planning.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.alibaba.cloud.ai.manus.planning.model.po.PlanTemplateVersion;

/**
 * The data access interface for the plan template version
 */
@Repository
public interface PlanTemplateVersionRepository extends JpaRepository<PlanTemplateVersion, Long> {

	/**
	 * Find all versions of the plan template by the plan template ID, sorted by the
	 * version index
	 * @param planTemplateId the plan template ID
	 * @return the list of versions
	 */
	List<PlanTemplateVersion> findByPlanTemplateIdOrderByVersionIndexAsc(String planTemplateId);

	/**
	 * Find the maximum version index of the plan template by the plan template ID
	 * @param planTemplateId the plan template ID
	 * @return the maximum version index, or null if there is no version
	 */
	@Query("SELECT MAX(v.versionIndex) FROM PlanTemplateVersion v WHERE v.planTemplateId = :planTemplateId")
	Integer findMaxVersionIndexByPlanTemplateId(@Param("planTemplateId") String planTemplateId);

	/**
	 * Find the specific version of the plan template by the plan template ID and the
	 * version index
	 * @param planTemplateId the plan template ID
	 * @param versionIndex the version index
	 * @return the plan template version entity
	 */
	PlanTemplateVersion findByPlanTemplateIdAndVersionIndex(String planTemplateId, Integer versionIndex);

	/**
	 * Delete all versions of the plan template by the plan template ID
	 * @param planTemplateId the plan template ID
	 */
	void deleteByPlanTemplateId(String planTemplateId);

}
