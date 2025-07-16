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
package com.alibaba.cloud.ai.model.workflow;

import com.alibaba.cloud.ai.model.Variable;

import java.util.List;

public class Workflow {

	private Graph graph;

	private List<Variable> workflowVars;

	private List<Variable> envVars;

	public Graph getGraph() {
		return graph;
	}

	public Workflow setGraph(Graph graph) {
		this.graph = graph;
		return this;
	}

	public List<Variable> getWorkflowVars() {
		return workflowVars;
	}

	public Workflow setWorkflowVars(List<Variable> workflowVars) {
		this.workflowVars = workflowVars;
		return this;
	}

	public List<Variable> getEnvVars() {
		return envVars;
	}

	public Workflow setEnvVars(List<Variable> envVars) {
		this.envVars = envVars;
		return this;
	}

}
