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
package com.alibaba.cloud.ai.graph.stream;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.alibaba.cloud.ai.graph.async.AsyncGenerator;
import com.alibaba.cloud.ai.graph.async.AsyncGeneratorQueue;
import com.alibaba.cloud.ai.graph.streaming.StreamingOutput;
import org.apache.commons.collections4.CollectionUtils;
import org.reactivestreams.Subscription;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import reactor.core.CoreSubscriber;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;

public class LLmNodeAction implements NodeAction {

	private DashScopeChatModel chatModel;

	public LLmNodeAction(DashScopeChatModel chatModel) {
		this.chatModel = chatModel;
	}

	@Override
	public Map<String, Object> apply(OverAllState t) {
		BlockingQueue<AsyncGenerator.Data<StreamingOutput>> queue = new ArrayBlockingQueue<>(2000);
		AsyncGenerator.WithResult<StreamingOutput> it = new AsyncGenerator.WithResult<>(
				new AsyncGeneratorQueue.Generator<>(queue));
		final CountDownLatch streamStarted = new CountDownLatch(1);
		// Create prompt with user message
		UserMessage message = new UserMessage((String) t.value(OverAllState.DEFAULT_INPUT_KEY).get());
		Prompt prompt = new Prompt(message);
		chatModel.stream(prompt).subscribe(new CoreSubscriber<>() {
			@Override
			public void onSubscribe(Subscription subscription) {
				subscription.request(Long.MAX_VALUE); // Request all items
				streamStarted.countDown();
			}

			@Override
			public void onNext(ChatResponse chatResponse) {
				try {
					if (chatResponse == null || CollectionUtils.isEmpty(chatResponse.getResults())) {
						queue.add(AsyncGenerator.Data.done());
						return;
					}

					Generation generation = chatResponse.getResults().get(0);
					String content = generation.getOutput().getText();

					if (content == null || content.isEmpty()) {
						queue.add(AsyncGenerator.Data.done());
						return;
					}

					t.updateState(Map.of("llm_result", content));
					queue.add(AsyncGenerator.Data.of(new StreamingOutput(content, "llmNode", t)));
				}
				catch (Exception e) {
					onError(e);
				}
			}

			@Override
			public void onError(Throwable throwable) {
				System.err.println("Stream error: " + throwable.getMessage());
				queue.add(AsyncGenerator.Data.error(throwable));
			}

			@Override
			public void onComplete() {
				System.out.println("Stream completed");
				queue.add(AsyncGenerator.Data.done());
			}
		});
		return Map.of("messages", it);
	}

}
