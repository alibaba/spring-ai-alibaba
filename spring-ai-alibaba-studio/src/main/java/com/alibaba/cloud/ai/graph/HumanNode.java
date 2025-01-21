/*
 * Copyright 2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.cloud.ai.graph;

import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.alibaba.cloud.ai.graph.state.NodeState;

import java.util.HashMap;
import java.util.Map;

import static com.alibaba.cloud.ai.service.impl.GraphServiceImpl.USER_INPUT;

public class HumanNode implements NodeAction {

	@Override
	public Map<String, Object> apply(NodeState state) {
		USER_INPUT.clear();
		synchronized (USER_INPUT) {
			try {
				USER_INPUT.wait();
			}
			catch (InterruptedException e) {
				throw new RuntimeException(e);
			}

		}
		USER_INPUT.remove(NodeState.OUTPUT);
		return new HashMap<>(USER_INPUT);
	}

}
