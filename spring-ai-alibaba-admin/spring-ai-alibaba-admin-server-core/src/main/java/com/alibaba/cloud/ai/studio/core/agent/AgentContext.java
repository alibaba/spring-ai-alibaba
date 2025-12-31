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

package com.alibaba.cloud.ai.studio.core.agent;

import com.alibaba.cloud.ai.studio.runtime.enums.AppType;
import com.alibaba.cloud.ai.studio.runtime.domain.RequestContext;
import com.alibaba.cloud.ai.studio.runtime.domain.agent.AgentRequest;
import com.alibaba.cloud.ai.studio.runtime.domain.agent.AgentResponse;
import com.alibaba.cloud.ai.studio.runtime.domain.app.AgentConfig;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.EqualsAndHashCode;
import reactor.core.publisher.Sinks;

import java.util.Map;

/**
 * Context class for agent requests and responses. Contains all necessary information for
 * processing agent interactions.
 *
 * @since 1.0.0.3
 */

@EqualsAndHashCode(callSuper = true)
@Data
public class AgentContext extends RequestContext {

	public AgentContext() {
		startTime = System.currentTimeMillis();
	}

	/** Request source */
	private String source;

	/** Start time of the request */
	private long startTime;

	/** End time of the request */
	private long endTime;

	/** Time of first response */
	private long firstResponseTime;

	/** Application id */
	private String appId;

	/** Type of the application */
	private AppType appType;

	/** Id for the conversation */
	private String conversationId;

	/** Model id */
	private String model;

	/** Flag indicating if streaming is enabled */
	private Boolean stream = false;

	/** Sink for handling response events */
	@JsonIgnore
	private Sinks.Many<AgentResponse> eventSink;

	/** Flag indicating if memory is enabled */
	private boolean memoryEnabled;

	/** Agent configuration */
	private AgentConfig config;

	/** Original agent request */
	private AgentRequest request;

	/** Agent response */
	private AgentResponse response;

	/** Variables used in prompts */
	private Map<String, Object> promptVariables;

}
