/*
 * Copyright 2025 the original author or authors.
 *
 * Lic		// 使用新的有序通知方法，确保按顺序输出：开始消息 -> 主处理 -> 完成消息
		var generator = StreamingChatGeneratorUtil.createGeneratorWithOrderedNotifications(
			this.getClass(), state, response -> {
				String rewrite = response.getResult().getOutput().getText();
				return Map.of(QUERY_REWRITE_NODE_OUTPUT, rewrite, RESULT, rewrite);
			}, rewriteFlux, "开始进行问题重写...", "问题重写完成！");

		// 返回处理结果
		return Map.of(getStreamReturnKey(), generator, QUERY_REWRITE_NODE_OUTPUT, generator);

		// 备选方案：如果你想要更简单的实现，可以使用以下代码替换上面的部分：
		// 
		// AsyncGenerator<? extends NodeOutput> startGenerator = StreamingChatGeneratorUtil
		//     .createStreamPrintGenerator("开始进行问题重写...");
		// 
		// var mainGenerator = StreamingChatGeneratorUtil.createGeneratorWithComposeCompletion(
		//     this.getClass(), state, response -> {
		//         String rewrite = response.getResult().getOutput().getText();
		//         return Map.of(QUERY_REWRITE_NODE_OUTPUT, rewrite, RESULT, rewrite);
		//     }, rewriteFlux, "问题重写完成！");
		// 
		// return Map.of(
		//     getStreamReturnKey(), startGenerator, 
		//     QUERY_REWRITE_NODE_OUTPUT, mainGenerator
		// );er the Apache License, Version 2.0 (the "License");
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

package com.alibaba.cloud.ai.node;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.alibaba.cloud.ai.service.base.BaseNl2SqlService;
import com.alibaba.cloud.ai.util.ChatResponseUtil;
import com.alibaba.cloud.ai.util.StateUtils;
import com.alibaba.cloud.ai.util.StreamingChatGeneratorUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import reactor.core.publisher.Flux;

import java.util.Map;

import static com.alibaba.cloud.ai.constant.Constant.*;

/**
 * 问题重写与意图澄清，提升意图理解准确性。
 *
 * @author zhangshenghang
 */
public class QueryRewriteNode implements NodeAction {

	private static final Logger logger = LoggerFactory.getLogger(QueryRewriteNode.class);

	private final BaseNl2SqlService baseNl2SqlService;

	public QueryRewriteNode(ChatClient.Builder chatClientBuilder, BaseNl2SqlService baseNl2SqlService) {
		this.baseNl2SqlService = baseNl2SqlService;
	}

	@Override
	public Map<String, Object> apply(OverAllState state) throws Exception {
		logger.info("进入 {} 节点", this.getClass().getSimpleName());

		String input = StateUtils.getStringValue(state, INPUT_KEY);
		logger.info("[{}] 处理用户输入: {}", this.getClass().getSimpleName(), input);

		final StringBuilder rewriteResult = new StringBuilder();
		
		Flux<ChatResponse> rewriteFlux = Flux.create(emitter -> {
			try {
				emitter.next(ChatResponseUtil.createCustomStatusResponse("开始进行问题重写..."));
				
				baseNl2SqlService.rewriteStream(input)
					.doOnNext(response -> {
						String content = response.getResult().getOutput().getText();
						rewriteResult.append(content);
						// 输出到流中供用户查看
						emitter.next(response);
					})
					.doOnComplete(() -> {
						rewriteResult.append("\n");
						emitter.next(ChatResponseUtil.createCustomStatusResponse("问题重写完成！"));
						logger.info("[{}] 问题重写处理完成", this.getClass().getSimpleName());
						emitter.complete();
					})
					.doOnError(error -> {
						logger.error("问题重写出错", error);
						emitter.error(error);
					})
					.subscribe();
			} catch (Exception e) {
				logger.error("问题重写流程出错", e);
				emitter.error(e);
			}
		});

		var generator = StreamingChatGeneratorUtil.createGenerator(this.getClass(), state, response -> {
			String finalRewrite = rewriteResult.toString().trim();
			return Map.of(QUERY_REWRITE_NODE_OUTPUT, finalRewrite, RESULT, finalRewrite);
		}, rewriteFlux);

		return Map.of(QUERY_REWRITE_NODE_OUTPUT, generator);
	}

}
