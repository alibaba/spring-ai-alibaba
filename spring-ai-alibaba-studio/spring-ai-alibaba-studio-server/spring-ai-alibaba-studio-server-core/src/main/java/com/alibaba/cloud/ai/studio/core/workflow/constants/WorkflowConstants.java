/*
* Copyright 2024 the original author or authors.
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

package com.alibaba.cloud.ai.studio.core.workflow.constants;

/**
 * Title Workflow constants.<br>
 * Description Workflow constants.<br>
 *
 * @since 1.0.0.3
 */

public interface WorkflowConstants {

	/** Flag indicating completion of workflow task */
	String WORKFLOW_TASK_FINISH_FLAG = "workflowTask_Finish";

	/** Code representing array type */
	String ARRAY_CODE = "Array";

	/** Key for system history list */
	String SYS_HISTORY_LIST_KEY = "history_list";

	/** Key for system query */
	String SYS_QUERY_KEY = "query";

	// Node configuration keys
	String NODE_CONFIG_RETRY = "retry_config";

	String NODE_CONFIG_TRY_CATCH = "try_catch_config";

	String NODE_CONFIG_SHORT_MEMORY = "short_memory";

	// Node branch types
	String NODE_BRANCH_FAIL = "fail";

	String NODE_BRANCH_DEFAULT = "default";

	// Try-catch strategy types
	String TRY_CATCH_STRATEGY_DEFAULT_VALUE = "defaultValue";

	String TRY_CATCH_STRATEGY_FAIL_BRANCH = "failBranch";

	String TRY_CATCH_STRATEGY_NOOP = "noop";

	// Memory types
	String MEMORY_TYPE_CUSTOM = "custom";

	String MEMORY_TYPE_SELF = "self";

	// Parameter types
	String PARAM_TYPE_STRING_LOWER_CASE = "string";

	String PARAM_TYPE_NUMBER_LOWER_CASE = "number";

	String PARAM_TYPE_BOOLEAN_LOWER_CASE = "boolean";

	String PARAM_TYPE_ARRAY_OBJECT_LOWER_CASE = "array<object>";

	String PARAM_TYPE_OBJECT_LOWER_CASE = "object";

}
