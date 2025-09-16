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
package com.alibaba.cloud.ai.example.manus.task.example;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.cloud.ai.example.manus.context.ContextKey;
import com.alibaba.cloud.ai.example.manus.context.JManusExecutionContext;
import com.alibaba.cloud.ai.example.manus.task.StatefulJManusStep;
import com.alibaba.cloud.ai.example.manus.task.TaskExecutionException;
import com.alibaba.cloud.ai.example.manus.task.example.DatabaseQueryStep.User;

public class UserFilterStep implements StatefulJManusStep {

	private static final Logger logger = LoggerFactory.getLogger(UserFilterStep.class);

	// Define context keys for filtered results
	public static final ContextKey<List<User>> ACTIVE_USERS_KEY = ContextKey.ofGeneric("filtered.active_users",
			List.class);

	public static final ContextKey<Integer> ACTIVE_USER_COUNT_KEY = ContextKey.of("filtered.active_user_count",
			Integer.class);

	@Override
	public void execute(JManusExecutionContext context) throws TaskExecutionException {
		logger.info("Executing user filter step for plan: {}", context.getPlanId());

		try {
			// Retrieve structured data from previous step
			List<User> allUsers = getPreviousStepResult(context, DatabaseQueryStep.USER_LIST_KEY,
					Collections.emptyList());

			if (allUsers.isEmpty()) {
				logger.warn("No users found from previous step, skipping filtering");
				setStepResult(context, ACTIVE_USERS_KEY, Collections.emptyList());
				setStepResult(context, ACTIVE_USER_COUNT_KEY, 0);
				return;
			}

			// Filter for active users only
			List<User> activeUsers = allUsers.stream().filter(User::isActive).collect(Collectors.toList());

			// Store filtered results for subsequent steps
			setStepResult(context, ACTIVE_USERS_KEY, activeUsers);
			setStepResult(context, ACTIVE_USER_COUNT_KEY, activeUsers.size());

			// Store processing metadata
			context.putMetadata("filter_timestamp", java.time.LocalDateTime.now().toString());
			context.putMetadata("original_user_count", allUsers.size());
			context.putMetadata("filtered_user_count", activeUsers.size());
			context.putMetadata("filter_criteria", "active=true");

			logger.info("Filtered {} users down to {} active users", allUsers.size(), activeUsers.size());

			// Log filtered user details for demonstration
			if (logger.isDebugEnabled()) {
				activeUsers.forEach(user -> logger.debug("Active user: {}", user));
			}

		}
		catch (Exception e) {
			String error = "Failed to filter users: " + e.getMessage();
			logger.error(error, e);
			throw new TaskExecutionException(error, e);
		}
	}

	@Override
	public String getName() {
		return "UserFilterStep";
	}

	@Override
	public String getDescription() {
		return "Filters users from the database query results to keep only active users";
	}

	@Override
	public boolean validateContext(JManusExecutionContext context) {
		// Validate that the required data from previous step is available
		return context.get(DatabaseQueryStep.USER_LIST_KEY).isPresent();
	}

}
