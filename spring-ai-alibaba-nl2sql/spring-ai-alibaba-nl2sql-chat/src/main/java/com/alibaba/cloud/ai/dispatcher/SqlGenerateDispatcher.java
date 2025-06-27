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
public class SqlGenerateDispatcher implements EdgeAction {

	private static final Logger logger = LoggerFactory.getLogger(SqlGenerateDispatcher.class);

	@Override
	public String apply(OverAllState state) {
		String sqlGenerateOutput = (String) state.value(SQL_GENERATE_OUTPUT).orElseThrow();
		logger.info("SQL 生成结果: {}", sqlGenerateOutput);
		switch (sqlGenerateOutput) {
			case END:
				logger.info("检测到流程结束标志: {}", END);
				return END;
			case SQL_GENERATE_SCHEMA_MISSING:
				logger.warn("SQL生成缺少Schema，跳转到{}节点", KEYWORD_EXTRACT_NODE);
				return KEYWORD_EXTRACT_NODE;
			default:
				logger.info("SQL生成成功，进入SQL校验节点: {}", SQL_VALIDATE_NODE);
				return SQL_VALIDATE_NODE;
		}
	}

}
