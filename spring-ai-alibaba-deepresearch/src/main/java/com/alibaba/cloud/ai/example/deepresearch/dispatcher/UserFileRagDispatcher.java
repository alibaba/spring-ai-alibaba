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

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.EdgeAction;

import static com.alibaba.cloud.ai.graph.StateGraph.END;

public class UserFileRagDispatcher implements EdgeAction {

	@Override
	public String apply(OverAllState state) {
		// 读取rewrite_multi_query_next_node的状态值，确保与前置节点决策一致
		// 如果前置节点决定进入user_file_rag，那么这里应该继续到background_investigator
		// 如果前置节点决定进入background_investigator，那么这里应该结束
		String previousDecision = state.value("rewrite_multi_query_next_node", END);

		// 如果前置节点决定进入user_file_rag，说明需要用户文件RAG处理
		// 处理完成后应该进入background_investigator进行后续调查
		if ("user_file_rag".equals(previousDecision)) {
			return "background_investigator";
		}

		// 其他情况直接结束
		return END;
	}

}
