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

import com.alibaba.cloud.ai.constant.Constant;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.action.EdgeAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author vlsmb
 * @since 2025/7/29
 */
public class PythonExecutorDispatcher implements EdgeAction {

    private static final Logger log = LoggerFactory.getLogger(PythonExecutorDispatcher.class);

    @Override
    public String apply(OverAllState state) throws Exception {
        // 节点内部会多次迭代运行代码，如果失败将记录日志
        boolean isSuccess = state.value(Constant.PYTHON_IS_SUCCESS, Boolean.class).orElse(false);
        if(!isSuccess) {
            String message = state.value(Constant.PYTHON_EXECUTE_NODE_OUTPUT).orElse("unknown").toString();
            log.error("Python Executor Node Error: {}", message);
            return StateGraph.END;
        }
        return Constant.PLAN_EXECUTOR_NODE;
    }
}
