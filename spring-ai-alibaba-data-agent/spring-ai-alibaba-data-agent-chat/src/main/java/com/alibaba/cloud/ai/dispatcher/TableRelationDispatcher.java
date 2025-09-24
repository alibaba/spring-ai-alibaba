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

package com.alibaba.cloud.ai.dispatcher;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.EdgeAction;
import com.alibaba.cloud.ai.util.StateUtils;

import java.util.Optional;

import static com.alibaba.cloud.ai.constant.Constant.*;
import static com.alibaba.cloud.ai.graph.StateGraph.END;

public class TableRelationDispatcher implements EdgeAction {

	private static final int MAX_RETRY_COUNT = 3;

	@Override
	public String apply(OverAllState state) throws Exception {

		String errorFlag = StateUtils.getStringValue(state, TABLE_RELATION_EXCEPTION_OUTPUT, null);
		Integer retryCount = StateUtils.getObjectValue(state, TABLE_RELATION_RETRY_COUNT, Integer.class, 0);

		if (errorFlag != null && !errorFlag.isEmpty()) {
			if (isRetryableError(errorFlag) && retryCount < MAX_RETRY_COUNT) {
				return TABLE_RELATION_NODE;
			}
			else {
				return END;
			}
		}

		Optional<String> tableRelationOutput = state.value(TABLE_RELATION_OUTPUT);
		if (tableRelationOutput.isPresent()) {
			return PLANNER_NODE; // next node is planner
		}

		// no output, end
		return END;
	}

	private boolean isRetryableError(String errorMessage) {
		return errorMessage.startsWith("RETRYABLE:");
	}

}
