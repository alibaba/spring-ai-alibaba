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

package com.alibaba.cloud.ai.manus.planning.service;

import com.alibaba.cloud.ai.manus.planning.exception.ParameterValidationException;
import com.alibaba.cloud.ai.manus.planning.model.vo.ParameterValidationResult;

import java.util.List;
import java.util.Map;

/**
 * Plan parameter mapping service interface providing functionality for handling parameter
 * placeholders in plan templates
 */
public interface IPlanParameterMappingService {

	/**
	 * Validate whether all parameter placeholders in plan template can be found in raw
	 * parameters. Throws detailed exception information if validation fails
	 * @param planJson plan template JSON string
	 * @param rawParams raw parameters dictionary
	 * @return validation result containing list of missing parameters
	 * @throws ParameterValidationException thrown when parameter validation fails
	 */
	ParameterValidationResult validateParameters(String planJson, Map<String, Object> rawParams)
			throws ParameterValidationException;

	/**
	 * Extract all parameter placeholders from plan template
	 * @param planJson plan template JSON string
	 * @return parameter placeholder list
	 */
	List<String> extractParameterPlaceholders(String planJson);

	/**
	 * Replace parameters in JSON and return the replaced plan JSON If validation fails,
	 * throws detailed exception information
	 * @param planJson Plan template JSON string
	 * @param rawParams Raw parameters dictionary
	 * @return Replaced plan JSON string
	 * @throws ParameterValidationException when parameter validation fails
	 */
	String replaceParametersInJson(String planJson, Map<String, Object> rawParams) throws ParameterValidationException;

	/**
	 * Validate parameter completeness before parameter replacement. Throws detailed
	 * exception information if validation fails
	 * @param planJson plan template JSON
	 * @param rawParams raw parameters
	 * @throws ParameterValidationException thrown when parameter validation fails
	 */
	void validateParametersBeforeReplacement(String planJson, Map<String, Object> rawParams)
			throws ParameterValidationException;

	/**
	 * Safely replace parameters, throws exception if validation fails
	 * @param planJson plan template JSON
	 * @param rawParams raw parameters
	 * @return replaced plan template
	 * @throws ParameterValidationException thrown when parameter validation fails
	 */
	String replaceParametersSafely(String planJson, Map<String, Object> rawParams) throws ParameterValidationException;

	/**
	 * Get parameter requirements information for plan template to help users understand
	 * what parameters need to be provided
	 * @param planJson plan template JSON
	 * @return parameter requirements information
	 */
	String getParameterRequirements(String planJson);

}
