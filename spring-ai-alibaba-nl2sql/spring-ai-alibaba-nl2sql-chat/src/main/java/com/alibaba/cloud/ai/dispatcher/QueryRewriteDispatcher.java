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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.alibaba.cloud.ai.constant.Constant.*;
import static com.alibaba.cloud.ai.graph.StateGraph.END;

/**
 * @author zhangshenghang
 */
public class QueryRewriteDispatcher implements EdgeAction {

	private static final Logger logger = LoggerFactory.getLogger(QueryRewriteDispatcher.class);

	@Override
	public String apply(OverAllState state) {
		String value = state.value(QUERY_REWRITE_NODE_OUTPUT, END);
		logger.debug("[QueryRewriteDispatcher]apply方法被调用，参数value: {}", value);

		switch (value) {
			case INTENT_UNCLEAR:
			case SMALL_TALK_REJECT:
				logger.info("[QueryRewriteDispatcher]意图不明确或闲聊被拒绝，返回END节点");
				return END;
			default:
				logger.info("[QueryRewriteDispatcher]进入KEYWORD_EXTRACT_NODE节点");
				return KEYWORD_EXTRACT_NODE;
		}
	}

}
