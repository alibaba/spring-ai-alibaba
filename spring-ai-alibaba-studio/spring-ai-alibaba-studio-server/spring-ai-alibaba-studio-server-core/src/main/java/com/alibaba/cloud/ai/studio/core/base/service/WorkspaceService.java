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

import com.alibaba.cloud.ai.studio.runtime.domain.BaseQuery;
import com.alibaba.cloud.ai.studio.runtime.domain.PagingList;
import com.alibaba.cloud.ai.studio.runtime.domain.account.Workspace;

/**
 * Service interface for managing workspaces. Provides CRUD operations and workspace
 * listing functionality.
 *
 * @since 1.0.0.3
 */
public interface WorkspaceService {

	/**
	 * Creates a new workspace.
	 * @param workspace The workspace to create
	 * @return The ID of the created workspace
	 */
	String createWorkspace(Workspace workspace);

	/**
	 * Updates an existing workspace.
	 * @param workspace The workspace to update
	 */
	void updateWorkspace(Workspace workspace);

	/**
	 * Deletes a workspace by its ID.
	 * @param workspaceId The ID of the workspace to delete
	 */
	void deleteWorkspace(String workspaceId);

	/**
	 * Retrieves a workspace by its ID.
	 * @param workspaceId The ID of the workspace to retrieve
	 * @return The workspace
	 */
	Workspace getWorkspace(String workspaceId);

	/**
	 * Lists workspaces based on query parameters.
	 * @param query The query parameters for filtering and pagination
	 * @return A paged list of workspaces
	 */
	PagingList<Workspace> listWorkspaces(BaseQuery query);

	/**
	 * Retrieves a workspace by user ID and workspace ID.
	 * @param uid The user ID
	 * @param workspaceId The workspace ID
	 * @return The workspace
	 */
	Workspace getWorkspace(String uid, String workspaceId);

	/**
	 * Gets the default workspace for a user.
	 * @param uid The user ID
	 * @return The default workspace
	 */
	Workspace getDefaultWorkspace(String uid);

}
