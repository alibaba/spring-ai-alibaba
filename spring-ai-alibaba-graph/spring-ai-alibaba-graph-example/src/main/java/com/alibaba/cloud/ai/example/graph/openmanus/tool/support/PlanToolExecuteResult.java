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
package com.alibaba.cloud.ai.example.graph.openmanus.tool.support;

import com.alibaba.cloud.ai.example.graph.openmanus.tool.Plan;

public class PlanToolExecuteResult extends ToolExecuteResult {

	private String id;

	private Plan plan;

	public PlanToolExecuteResult(String output, String id) {
		super(output);
		this.id = id;
	}

	public PlanToolExecuteResult(Plan plan, String output, String id) {
		super(output);
		this.id = id;
		this.plan = plan;
	}

	String getId() {
		return id;
	}

	void setId(String id) {
		this.id = id;
	}

}
