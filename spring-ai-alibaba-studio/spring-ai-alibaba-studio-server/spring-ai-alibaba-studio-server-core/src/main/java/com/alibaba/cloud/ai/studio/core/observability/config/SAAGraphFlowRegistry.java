/*
 * Copyright 2024-2025 the original author or authors.
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
package com.alibaba.cloud.ai.studio.core.observability.config;

import com.alibaba.cloud.ai.studio.core.observability.model.SAAGraphFlow;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import javax.annotation.PostConstruct;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Registry service for managing Spring AI Alibaba Graph Flows
 * 
 * <p>This service acts as a central registry for all graph flows in the system.
 * It automatically discovers and registers graph flow beans from the Spring
 * application context during initialization, providing efficient lookup and
 * management capabilities.</p>
 * 
 * <p>The registry uses a thread-safe concurrent map to store flows, indexed by
 * their unique flow IDs for fast retrieval. It supports various query operations
 * including finding flows by owner ID and checking existence.</p>
 * 
 * <h2>Key Features:</h2>
 * <ul>
 *   <li>Automatic discovery of graph flow beans</li>
 *   <li>Thread-safe concurrent access</li>
 *   <li>Efficient lookup by flow ID and owner ID</li>
 *   <li>Duplicate flow ID detection</li>
 *   <li>Complete flow lifecycle management</li>
 * </ul>
 * 
 */
@Service
public class SAAGraphFlowRegistry {

	private final ApplicationContext applicationContext;

	// Thread-safe registry with flowId as key.
	private final Map<String, SAAGraphFlow> flowRegistry = new ConcurrentHashMap<>();

	public SAAGraphFlowRegistry(ApplicationContext applicationContext) {
		this.applicationContext = applicationContext;
	}

	/**
	 * Initializes the registry by discovering and registering all SAAGraphFlow beans
	 * from the Spring application context.
	 * 
	 * <p>This method is automatically called after dependency injection is complete.
	 * It scans for all SAAGraphFlow beans and registers them in the internal registry
	 * map, ensuring no duplicate flow IDs exist.</p>
	 * 
	 * @throws IllegalStateException if duplicate flow IDs are detected
	 */
	@PostConstruct
	public void init() {
		Collection<SAAGraphFlow> flows = applicationContext.getBeansOfType(SAAGraphFlow.class).values();

		// Register all discovered flows in our internal map.
		flows.forEach(flow -> {
			if (flowRegistry.containsKey(flow.graphId())) {
				throw new IllegalStateException("Duplicate Flow ID found: " + flow.graphId());
			}
			flowRegistry.put(flow.graphId(), flow);
		});

		System.out.println("Initialized GraphFlowRegistry. Found " + flowRegistry.size() + " flows.");
	}

	/**
	 * Finds and returns all flows owned by a specific user.
	 *
	 * @param ownerID The unique identifier of the owner.
	 * @return List of SAAGraphFlow objects owned by the specified user, or empty list if
	 * none found.
	 */
	public List<SAAGraphFlow> findByOwnerID(String ownerID) {
		if (ownerID == null || ownerID.isBlank()) {
			return List.of();
		}

		return flowRegistry.values()
			.stream()
			.filter(flow -> ownerID.equals(flow.ownerID()))
			.collect(Collectors.toList());
	}

	/**
	 * Finds and returns a specific flow by its unique identifier.
	 *
	 * @param flowId The unique identifier of the flow to search for.
	 * @return The matching SAAGraphFlow, or null if not found.
	 */
	public SAAGraphFlow findById(String flowId) {
		if (flowId == null || flowId.isBlank()) {
			return null;
		}
		return flowRegistry.get(flowId);
	}

	/**
	 * Checks if a flow with the specified ID exists in the registry.
	 *
	 * @param flowId The unique identifier of the flow to check.
	 * @return true if the flow exists, false otherwise.
	 */
	public boolean existsById(String flowId) {
		if (flowId == null || flowId.isBlank()) {
			return false;
		}
		return flowRegistry.containsKey(flowId);
	}

	/**
	 * Returns all registered flows in the system.
	 *
	 * @return An immutable list of all registered SAAGraphFlow objects.
	 */
	public List<SAAGraphFlow> findAll() {
		return List.copyOf(flowRegistry.values());
	}

}
