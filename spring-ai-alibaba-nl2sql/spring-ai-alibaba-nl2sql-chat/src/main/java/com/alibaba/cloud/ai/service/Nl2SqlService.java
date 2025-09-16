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
package com.alibaba.cloud.ai.service;

import com.alibaba.cloud.ai.entity.Nl2SqlProcess;
import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.NodeOutput;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.exception.GraphRunnerException;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import com.alibaba.cloud.ai.graph.streaming.StreamingOutput;
import org.springframework.beans.factory.annotation.Qualifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import static com.alibaba.cloud.ai.constant.Constant.AGENT_ID;
import static com.alibaba.cloud.ai.constant.Constant.INPUT_KEY;
import static com.alibaba.cloud.ai.constant.Constant.IS_ONLY_NL2SQL;
import static com.alibaba.cloud.ai.constant.Constant.ONLY_NL2SQL_OUTPUT;

/**
 * NL2SQL接口预留
 *
 * @author vlsmb
 * @since 2025/7/27
 */
@Service
public class Nl2SqlService {

	private static final Logger logger = LoggerFactory.getLogger(Nl2SqlService.class);

	private final CompiledGraph nl2sqlGraph;

	public Nl2SqlService(@Qualifier("nl2sqlGraph") StateGraph stateGraph) throws GraphStateException {
		this.nl2sqlGraph = stateGraph.compile();
		this.nl2sqlGraph.setMaxIterations(100);
	}

	/**
	 * 自然语言转SQL，仅返回SQL代码结果
	 * @param naturalQuery 自然语言
	 * @param agentId Agent Id
	 * @return SQL结果
	 * @throws GraphRunnerException 图运行异常
	 */
	public String nl2sql(String naturalQuery, String agentId) throws GraphRunnerException {
		if (agentId == null) {
			agentId = "";
		}
		Map<String, Object> stateMap = Map.of(IS_ONLY_NL2SQL, true, INPUT_KEY, naturalQuery, AGENT_ID, agentId);
		Optional<OverAllState> call = this.nl2sqlGraph.call(stateMap);
		OverAllState state = call.orElseThrow(() -> {
			logger.error("Nl2SqlService call fail, stateMap: {}", stateMap);
			return new GraphRunnerException("图运行失败");
		});
		return state.value(ONLY_NL2SQL_OUTPUT, "");
	}

	/**
	 * 自然语言转SQL，仅返回SQL代码结果
	 * @param naturalQuery 自然语言
	 * @return SQL结果
	 * @throws GraphRunnerException 图运行异常
	 */
	public String nl2sql(String naturalQuery) throws GraphRunnerException {
		return this.nl2sql(naturalQuery, "");
	}

	/**
	 * 自然语言转SQL，允许记录中间执行过程
	 * @param nl2SqlProcessConsumer 处理节点运行结果的Consumer
	 * @param naturalQuery 自然语言
	 * @param agentId Agent Id
	 * @param runnableConfig Runnable Config
	 * @return CompletableFuture
	 * @throws GraphRunnerException 图运行异常
	 */
	public CompletableFuture<Void> nl2sqlWithProcess(Consumer<Nl2SqlProcess> nl2SqlProcessConsumer, String naturalQuery,
			String agentId, RunnableConfig runnableConfig) throws GraphRunnerException {
		Map<String, Object> stateMap = Map.of(IS_ONLY_NL2SQL, true, INPUT_KEY, naturalQuery, AGENT_ID, agentId);
		Consumer<NodeOutput> consumer = (output) -> {
			Nl2SqlProcess sqlProcess = this.nodeOutputToNl2sqlProcess(output);
			nl2SqlProcessConsumer.accept(sqlProcess);
		};
		return this.nl2sqlGraph.fluxStream(stateMap, runnableConfig).doOnNext(consumer::accept).then().toFuture();
	}

	/**
	 * 自然语言转SQL，允许记录中间执行过程
	 * @param nl2SqlProcessConsumer 处理节点运行结果的Consumer
	 * @param naturalQuery 自然语言
	 * @param agentId Agent Id
	 * @return CompletableFuture
	 * @throws GraphRunnerException 图运行异常
	 */
	public CompletableFuture<Void> nl2sqlWithProcess(Consumer<Nl2SqlProcess> nl2SqlProcessConsumer, String naturalQuery,
			String agentId) throws GraphRunnerException {
		return this.nl2sqlWithProcess(nl2SqlProcessConsumer, naturalQuery, agentId, RunnableConfig.builder().build());
	}

	/**
	 * 自然语言转SQL，允许记录中间执行过程
	 * @param nl2SqlProcessConsumer 处理节点运行结果的Consumer
	 * @param naturalQuery 自然语言
	 * @return CompletableFuture
	 * @throws GraphRunnerException 图运行异常
	 */
	public CompletableFuture<Void> nl2sqlWithProcess(Consumer<Nl2SqlProcess> nl2SqlProcessConsumer, String naturalQuery)
			throws GraphRunnerException {
		return this.nl2sqlWithProcess(nl2SqlProcessConsumer, naturalQuery, "");
	}

	/**
	 * 将NodeOutput转为NlSqlProcess实体类（用于nl2sqlWithProcess的consumer中记录转化过程）
	 * @param output NodeOutput
	 * @return NlSqlProcess
	 */
	private Nl2SqlProcess nodeOutputToNl2sqlProcess(NodeOutput output) {
		// 将节点运行结果进行包装
		String nodeRes = "";
		if (output instanceof StreamingOutput streamingOutput) {
			nodeRes = streamingOutput.chunk();
		}
		else {
			nodeRes = output.toString();
		}

		// 如果是结束节点，取出最终生成结果
		if (StateGraph.END.equals(output.node())) {
			String result = output.state().value(ONLY_NL2SQL_OUTPUT, "");
			return Nl2SqlProcess.success(result, output.node(), nodeRes);
		}
		return Nl2SqlProcess.processing(output.node(), nodeRes);
	}

}
