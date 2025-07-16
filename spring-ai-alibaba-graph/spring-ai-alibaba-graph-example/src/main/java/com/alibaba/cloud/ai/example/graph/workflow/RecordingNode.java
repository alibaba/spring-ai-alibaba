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
package com.alibaba.cloud.ai.example.graph.workflow;

import java.util.HashMap;
import java.util.Map;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RecordingNode implements NodeAction {

	private static final Logger logger = LoggerFactory.getLogger(RecordingNode.class);

	@Override
	public Map<String, Object> apply(OverAllState state) throws Exception {

		String feedback = (String) state.value("classifier_output").get();

		Map<String, Object> updatedState = new HashMap<>();
		if (feedback.contains("positive")) {
			logger.info("Received positive feedback: {}", feedback);
			updatedState.put("solution", "Praise, no action taken.");
		}
		else {
			logger.info("Received negative feedback: {}", feedback);
			updatedState.put("solution", feedback);
		}

		return updatedState;
	}

}
