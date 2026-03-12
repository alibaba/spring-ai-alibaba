/*
 * Copyright 2025-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.cloud.ai.examples.multiagents.handoffs.state;

/**
 * State keys and agent names for the multi-agent handoffs workflow (sales + support).
 */
public final class MultiAgentStateConstants {

	private MultiAgentStateConstants() {
	}

	public static final String ACTIVE_AGENT = "active_agent";

	public static final String SALES_AGENT = "sales_agent";
	public static final String SUPPORT_AGENT = "support_agent";

}
