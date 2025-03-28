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
package com.alibaba.cloud.ai.graph.node;

import java.util.Map;

import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.action.NodeAction;

public class StateAdaptorNode implements NodeAction {

	NodeAction nodeAction;

	CompiledGraph compiledGraph;

	public StateAdaptorNode(NodeAction nodeAction) {
		this.nodeAction = nodeAction;
	}

	@Override
	public Map<String, Object> apply(OverAllState t) throws Exception {
		// parent state to current state;
		OverAllState subState = t;

		Map<String, Object> updatedSubState = nodeAction.apply(subState);

		return updatedSubState;
	}

}
