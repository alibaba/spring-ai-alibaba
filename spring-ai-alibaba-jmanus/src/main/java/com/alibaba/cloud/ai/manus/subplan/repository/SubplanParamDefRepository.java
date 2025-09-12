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
package com.alibaba.cloud.ai.manus.subplan.repository;

import com.alibaba.cloud.ai.manus.subplan.model.po.SubplanParamDef;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository interface for SubplanParamDef entity
 *
 * Provides data access methods for subplan parameter definitions
 */
@Repository
public interface SubplanParamDefRepository extends JpaRepository<SubplanParamDef, Long> {

	/**
	 * Find all parameters for a specific tool
	 * @param toolDefId the tool definition ID
	 * @return List of parameters for the tool
	 */
	List<SubplanParamDef> findByToolDefId(Long toolDefId);

	/**
	 * Find parameters by name for a specific tool
	 * @param toolDefId the tool definition ID
	 * @param name the parameter name
	 * @return List of parameters matching the criteria
	 */
	List<SubplanParamDef> findByToolDefIdAndName(Long toolDefId, String name);

	/**
	 * Find required parameters for a specific tool
	 * @param toolDefId the tool definition ID
	 * @return List of required parameters for the tool
	 */
	List<SubplanParamDef> findByToolDefIdAndRequiredTrue(Long toolDefId);

	/**
	 * Find parameters by type for a specific tool
	 * @param toolDefId the tool definition ID
	 * @param type the parameter type
	 * @return List of parameters of the specified type
	 */
	List<SubplanParamDef> findByToolDefIdAndType(Long toolDefId, String type);

	/**
	 * Find parameters by name pattern across all tools
	 * @param namePattern the parameter name pattern (e.g., "%data%")
	 * @return List of parameters matching the pattern
	 */
	@Query("SELECT p FROM SubplanParamDef p WHERE p.name LIKE %:namePattern%")
	List<SubplanParamDef> findByNamePattern(@Param("namePattern") String namePattern);

	/**
	 * Find parameters by description containing text
	 * @param description the description text to search for
	 * @return List of parameters with matching descriptions
	 */
	List<SubplanParamDef> findByDescriptionContainingIgnoreCase(String description);

	/**
	 * Count parameters for a specific tool
	 * @param toolDefId the tool definition ID
	 * @return count of parameters for the tool
	 */
	long countByToolDefId(Long toolDefId);

	/**
	 * Count required parameters for a specific tool
	 * @param toolDefId the tool definition ID
	 * @return count of required parameters for the tool
	 */
	long countByToolDefIdAndRequiredTrue(Long toolDefId);

	/**
	 * Delete all parameters for a specific tool
	 * @param toolDefId the tool definition ID
	 */
	void deleteByToolDefId(Long toolDefId);

	/**
	 * Find parameters by multiple types for a specific tool
	 * @param toolDefId the tool definition ID
	 * @param types list of parameter types
	 * @return List of parameters matching any of the specified types
	 */
	@Query("SELECT p FROM SubplanParamDef p WHERE p.toolDef.id = :toolDefId AND p.type IN :types")
	List<SubplanParamDef> findByToolDefIdAndTypeIn(@Param("toolDefId") Long toolDefId,
			@Param("types") List<String> types);

}
