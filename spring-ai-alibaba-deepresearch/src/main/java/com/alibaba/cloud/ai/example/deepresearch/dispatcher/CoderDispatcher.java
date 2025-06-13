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
package com.alibaba.cloud.ai.example.deepresearch.dispatcher;

import com.alibaba.cloud.ai.example.deepresearch.model.dto.Plan;
import com.alibaba.cloud.ai.example.deepresearch.node.CoderNode;
import com.alibaba.cloud.ai.example.deepresearch.util.StateUtil;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.EdgeAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author xiaoyuntao
 * @since 2025/06/11
 */
public class CoderDispatcher implements EdgeAction {

	private static final Logger logger = LoggerFactory.getLogger(CoderNode.class);

	@Override
	public String apply(OverAllState state) throws Exception {
		String coderContent = state.value("coder_content", "");
		Plan currentPlan = StateUtil.getPlan(state);
		List<String> observations = StateUtil.getMessagesByType(state, "observations");
		Plan.Step unexecutedStep = null;
		for (Plan.Step step : currentPlan.getSteps()) {
			if (step.stepType().equals(Plan.StepType.PROCESSING) && step.executionRes() == null) {
				unexecutedStep = step.mutate().build();
				break;
			}
		}
		if (unexecutedStep == null) {
			logger.info("all coder node is finished.");
			return "research_team";
		}
		unexecutedStep = unexecutedStep.mutate().executionRes(coderContent).build();

        logger.info("coder Node result: {}", coderContent);
		Map<String, Object> updated = new HashMap<>();
		observations.add(coderContent);
		updated.put("observations", observations);
		state.input(updated);
		return "research_team";
	}

}
