/*
 * Copyright 2024-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.alibaba.cloud.ai.model.workflow;

import com.alibaba.cloud.ai.model.VariableSelector;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * A Case represents a condition in ConditionalEdge
 */
@Data
@Accessors(chain = true)
public class Case {

	private String id;

	private String logicalOperator;

	private List<Condition> conditions;

	@Data
	@Accessors(chain = true)
	public static class Condition {

		private String value;

		private String varType;

		// TODO comparison operator enum
		private String comparisonOperator;

		private VariableSelector variableSelector;

	}

}
