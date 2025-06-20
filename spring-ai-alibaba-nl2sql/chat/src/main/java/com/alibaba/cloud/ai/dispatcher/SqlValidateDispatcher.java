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

/**
 * @author zhangshenghang
 */
public class SqlValidateDispatcher implements EdgeAction {

	private static final Logger logger = LoggerFactory.getLogger(SqlValidateDispatcher.class);

	@Override
	public String apply(OverAllState state) {
		Boolean validate = (Boolean) state.value(SQL_VALIDATE_NODE_OUTPUT).orElseThrow();
		logger.info("SQL语法校验是否通过: {}", validate);
		if (validate) {
			logger.info("[SqlValidateDispatcher] SQL语法校验通过，跳转到节点: {}", SEMANTIC_CONSISTENC_NODE);
			return SEMANTIC_CONSISTENC_NODE;
		}
		else {
			logger.info("[SqlValidateDispatcher] SQL语法校验未通过，跳转到节点: {}", SQL_GENERATE_NODE);
			return SQL_GENERATE_NODE;
		}
	}

}
