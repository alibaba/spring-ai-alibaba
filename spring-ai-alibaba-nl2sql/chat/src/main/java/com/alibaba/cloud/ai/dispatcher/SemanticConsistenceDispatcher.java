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
public class SemanticConsistenceDispatcher implements EdgeAction {

	private static final Logger logger = LoggerFactory.getLogger(SemanticConsistenceDispatcher.class);

	@Override
	public String apply(OverAllState state) {
		Boolean validate = (Boolean) state.value(SEMANTIC_CONSISTENC_NODE_OUTPUT).orElseThrow();
		logger.info("语义一致性校验结果: {}，跳转节点配置", validate);
		if (validate) {
			logger.info("语义一致性校验通过，跳转到结束节点。");
			return END;
		}
		else {
			logger.info("语义一致性校验未通过，跳转到SQL生成节点：{}", SQL_GENERATE_NODE);
			return SQL_GENERATE_NODE;
		}
	}

}
