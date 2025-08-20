/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.cloud.ai.graph.agent.a2a;

import java.util.Map;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import io.a2a.spec.AgentCard;

public class A2aNode implements NodeAction {
	private AgentCard agentCard;
	private String inputKeyFromParent;
	private String outputKeyToParent;

	public A2aNode(AgentCard agentCard, String inputKeyFromParent, String outputKeyToParent) {
		this.outputKeyToParent = outputKeyToParent;
		this.inputKeyFromParent = inputKeyFromParent;
		this.agentCard = agentCard;
	}

	@Override
	public Map<String, Object> apply(OverAllState state) throws Exception {
		// remote call
		return Map.of();
	}
}
