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

package com.alibaba.cloud.ai.studio.core.base.constants;

import java.time.Duration;

/**
 * Cache-related constants for the application.
 *
 * @since 1.0.0.3
 */
public interface CacheConstants {

	/** Default empty cache ID */
	Long CACHE_EMPTY_ID = -1L;

	/** Default cache TTL duration */
	Duration CACHE_EMPTY_TTL = Duration.ofMinutes(5);

	/** Cache key prefix for plugin and workspace */
	String CACHE_PLUGIN_WORKSPACE_ID_PREFIX = "plugin:%s:%s";

	/** Cache key prefix for tool and workspace */
	String CACHE_TOOL_WORKSPACE_ID_PREFIX = "tool:%s:%s";

	/** Cache key prefix for app workspace */
	String CACHE_APP_WORKSPACE_ID_PREFIX = "app:%s:%s";

	/** Cache key prefix for workspace */
	String CACHE_WORKSPACE_UID_PREFIX = "workspace:%s:%s";

	/** Cache key prefix for account */
	String CACHE_ACCOUNT_UID_PREFIX = "account:%s";

	/** Cache key prefix for API key */
	String CACHE_API_KEY_PREFIX = "api_key:%s";

	/** Cache key prefix for API key ID */
	String CACHE_API_KEY_ID_UID_PREFIX = "api_key_id:%s:%s";

	/** Cache key prefix for knowledge base workspace */
	String CACHE_KB_WORKSPACE_ID_PREFIX = "knowledge_base:%s:%s";

	/** Prefix for workflow task context */
	String WORKFLOW_TASK_CONTEXT_PREFIX = "workflow_task_context_";

	/** Prefix for workflow task execution flag */
	String WORKFLOW_TASK_EXECUTE_FLAG_PREFIX = "workflow_task_execute_flag_";

	/** Template for workflow session variable key (appcode,conversationId,key) */
	String WORKFLOW_SESSION_VARIABLE_KEY_TEMPLATE = "workflow_session_%s_%s_%s";

	/** Template for workflow node self short memory (appcode,conversationId,nodeId) */
	String WORKFLOW_NODE_SELF_SHORT_MEMORY_TEMPLATE = "workflow_node_self_short_memory_%s_%s_%s";

	/** Template for appcode session ID (appcode,conversationId) */
	String APPCODE_CONVERSATION_ID_TEMPLATE = "%s_%s";

	/** Prefix for provider list cache */
	String CACHE_PROVIDER_LIST_CACHE_PREFIX = "provider_list_cache_";

	/** Prefix for workspace provider */
	String CACHE_WORKSPACE_PROVIDER_PREFIX = "provider:%s:%s";

}
