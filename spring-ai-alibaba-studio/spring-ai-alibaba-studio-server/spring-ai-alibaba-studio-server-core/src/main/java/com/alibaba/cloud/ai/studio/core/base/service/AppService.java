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
import com.alibaba.cloud.ai.studio.runtime.domain.app.AppQuery;
import com.alibaba.cloud.ai.studio.runtime.domain.app.Application;
import com.alibaba.cloud.ai.studio.runtime.domain.app.ApplicationVersion;
import com.alibaba.cloud.ai.studio.runtime.domain.component.AppComponentQuery;

import java.util.List;

/**
 * Service interface for managing applications and their versions. Provides CRUD
 * operations and version management for applications.
 *
 * @since 1.0.0.3
 */
public interface AppService {

	/**
	 * Creates a new application
	 * @param application Application details
	 * @return ID of the created application
	 */
	String createApp(Application application);

	/**
	 * Updates an existing application
	 * @param application Updated application details
	 */
	void updateApp(Application application);

	/**
	 * Deletes an application by ID
	 * @param appId Application ID to delete
	 */
	void deleteApp(String appId);

	/**
	 * Retrieves an application by ID
	 * @param appId Application ID to retrieve
	 * @return Application details
	 */
	Application getApp(String appId);

	/**
	 * Lists applications based on query criteria
	 * @param query Search criteria
	 * @return Paged list of applications
	 */
	PagingList<Application> listApps(AppQuery query);

	/**
	 * Publishes an application
	 * @param appId Application ID to publish
	 */
	void publishApp(String appId);

	/**
	 * Lists application versions based on query criteria
	 * @param query Search criteria
	 * @return Paged list of application versions
	 */
	PagingList<ApplicationVersion> listAppVersions(AppQuery query);

	/**
	 * Retrieves a specific version of an application
	 * @param appId Application ID
	 * @param versionId Version ID
	 * @return Application version details
	 */
	ApplicationVersion getAppVersion(String appId, String versionId);

	/**
	 * Gets list of published applications by type and name
	 * @param type Application type
	 * @param appName Application name
	 * @param codes List of codes
	 * @return List of application IDs
	 */
	List<Long> getApplicationPublished(String type, String appName, List<String> codes);

	/**
	 * Gets list of published applications that are not components
	 * @param request Component query parameters
	 * @param codes List of codes
	 * @param ids List of application IDs
	 * @return Paged list of applications
	 */
	PagingList<Application> getApplicationPublishedAndNotComponentList(AppComponentQuery request, List<String> codes,
			List<Long> ids);

	/**
	 * Creates a copy of an existing application
	 * @param appId ID of the application to copy
	 * @return ID of the newly created application
	 */
	String copyApp(String appId);

}
