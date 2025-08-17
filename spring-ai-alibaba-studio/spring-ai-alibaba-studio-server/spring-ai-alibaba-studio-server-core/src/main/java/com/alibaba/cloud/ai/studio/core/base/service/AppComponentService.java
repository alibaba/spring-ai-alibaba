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

import com.alibaba.cloud.ai.studio.runtime.domain.PagingList;
import com.alibaba.cloud.ai.studio.runtime.domain.Result;
import com.alibaba.cloud.ai.studio.runtime.domain.component.AppComponent;
import com.alibaba.cloud.ai.studio.runtime.domain.component.AppComponentQuery;

import java.util.List;

/**
 * Service interface for managing application components. Provides CRUD operations and
 * query capabilities for app components.
 *
 * @author guning.lt
 * @since 1.0.0.3
 */
public interface AppComponentService {

	/**
	 * Get a paginated list of app components based on query conditions.
	 * @param request Query request object
	 * @return Paginated list of app components
	 */
	PagingList<AppComponent> getAppComponentList(AppComponentQuery request);

	/**
	 * Get all app components matching the given type and status.
	 * @param type Component type filter
	 * @param status Component status filter
	 * @return List of app components
	 */
	List<AppComponent> getAppComponentListAll(String type, Integer status);

	/**
	 * Get app components by their codes.
	 * @param codes List of component codes
	 * @return List of matched app components
	 */
	List<AppComponent> getAppComponentListByCodes(List<String> codes);

	/**
	 * Get an app component by application code and status.
	 * @param appId Application code
	 * @param status Component status
	 * @return App component object
	 */
	AppComponent getAppComponentByAppId(String appId, Integer status);

	/**
	 * Get an app component by component code and status.
	 * @param code Internal component code
	 * @param status Component status
	 * @return App component object
	 */
	AppComponent getAppComponentByCode(String code, Integer status);

	/**
	 * Create a new app component.
	 * @param component App component to create
	 * @return Result indicating success or failure
	 */
	Result<String> createAppComponent(AppComponent component);

	/**
	 * Update an existing app component.
	 * @param component Updated app component data
	 * @return Result indicating success or failure
	 */
	Result<Integer> updateAppComponent(AppComponent component);

	/**
	 * Delete an app component by marking it as deleted.
	 * @param code component code
	 * @return Result indicating success or failure
	 */
	Result<Void> deleteAppComponent(String code);

}
