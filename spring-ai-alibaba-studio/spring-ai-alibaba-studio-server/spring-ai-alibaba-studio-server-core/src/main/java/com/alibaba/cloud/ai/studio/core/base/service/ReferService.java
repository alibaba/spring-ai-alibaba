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

package com.alibaba.cloud.ai.studio.core.base.service;

import com.alibaba.cloud.ai.studio.runtime.domain.app.Application;
import com.alibaba.cloud.ai.studio.runtime.domain.refer.Refer;
import com.alibaba.cloud.ai.studio.core.base.entity.ReferEntity;

import java.util.List;

/**
 * Service interface for managing refer relationships
 *
 * @author guning.lt
 * @since 1.0.0.3
 */
public interface ReferService {

	/**
	 * Save a single refer entity
	 * @param refer the refer entity to save
	 * @return true if saved successfully
	 */
	Boolean saveRefer(ReferEntity refer);

	/**
	 * Save a list of refer entities
	 * @param refers list of refer entities to save
	 * @return true if all saved successfully
	 */
	Boolean saveReferList(List<ReferEntity> refers);

	/**
	 * Delete a refer entity by refer code and main code
	 * @param referCode the refer code
	 * @param mainCode the main code
	 * @return true if deleted successfully
	 */
	Boolean deleteRefer(String referCode, String mainCode);

	/**
	 * Delete refer entities by main code and refer type
	 * @param mainCode the main code
	 * @param referType the refer type
	 * @return true if all deleted successfully
	 */
	Boolean deleteReferList(String mainCode, Integer referType);

	/**
	 * Get refer list by main code
	 * @param mainCode the main code
	 * @return list of refers
	 */
	List<Refer> getReferListByMainCode(String mainCode);

	/**
	 * Get refer list by refer code
	 * @param referCode the refer code
	 * @return list of refers
	 */
	List<Refer> getReferListByReferCode(String referCode);

	/**
	 * Construct refer entities from application
	 * @param app the application
	 * @return list of constructed refer entities
	 */
	List<ReferEntity> constructRefers(Application app);

}
