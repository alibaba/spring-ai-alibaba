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

import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.cloud.ai.example.manus.context.ContextKey;
import com.alibaba.cloud.ai.example.manus.context.JManusExecutionContext;
import com.alibaba.cloud.ai.example.manus.task.StatefulJManusStep;
import com.alibaba.cloud.ai.example.manus.task.TaskExecutionException;


public class DatabaseQueryStep implements StatefulJManusStep {

	private static final Logger logger = LoggerFactory.getLogger(DatabaseQueryStep.class);

	// Define context keys for type-safe data storage
	public static final ContextKey<List<User>> USER_LIST_KEY = ContextKey.ofGeneric("database.users", List.class);

	public static final ContextKey<Integer> USER_COUNT_KEY = ContextKey.of("database.user_count", Integer.class);

	@Override
	public void execute(JManusExecutionContext context) throws TaskExecutionException {
		logger.info("Executing database query step for plan: {}", context.getPlanId());

		try {
			// Simulate database query
			List<User> users = simulateUserQuery();

			// Store structured data in context for subsequent steps
			setStepResult(context, USER_LIST_KEY, users);
			setStepResult(context, USER_COUNT_KEY, users.size());

			// Store additional metadata
			context.putMetadata("query_timestamp", java.time.LocalDateTime.now().toString());
			context.putMetadata("data_source", "user_database");

			logger.info("Successfully retrieved {} users from database", users.size());

		}
		catch (Exception e) {
			String error = "Failed to execute database query: " + e.getMessage();
			logger.error(error, e);
			throw new TaskExecutionException(error, e);
		}
	}

	@Override
	public String getName() {
		return "DatabaseQueryStep";
	}

	@Override
	public String getDescription() {
		return "Queries the database for user information and stores results in structured format";
	}

	/**
	 * Simulates a database query that returns user data. In a real implementation, this
	 * would connect to an actual database.
	 */
	private List<User> simulateUserQuery() {
		return Arrays.asList(new User(1L, "Alice Johnson", "alice@example.com", true),
				new User(2L, "Bob Smith", "bob@example.com", false),
				new User(3L, "Carol Davis", "carol@example.com", true),
				new User(4L, "David Wilson", "david@example.com", true),
				new User(5L, "Eve Brown", "eve@example.com", false));
	}

	/**
	 * Simple User data class for demonstration.
	 */
	public static class User {

		private final Long id;

		private final String name;

		private final String email;

		private final boolean active;

		public User(Long id, String name, String email, boolean active) {
			this.id = id;
			this.name = name;
			this.email = email;
			this.active = active;
		}

		// Getters
		public Long getId() {
			return id;
		}

		public String getName() {
			return name;
		}

		public String getEmail() {
			return email;
		}

		public boolean isActive() {
			return active;
		}

		@Override
		public String toString() {
			return String.format("User{id=%d, name='%s', email='%s', active=%s}", id, name, email, active);
		}

	}

}
