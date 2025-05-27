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

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.alibaba.cloud.ai.graph.streaming.StreamingOutput;
import com.theokanning.openai.completion.chat.ChatCompletionChoice;
import com.theokanning.openai.completion.chat.ChatCompletionChunk;
import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.SystemMessage;
import com.theokanning.openai.service.OpenAiService;
import io.reactivex.Flowable;
import io.reactivex.rxjava3.subscribers.DisposableSubscriber;
import org.apache.commons.collections4.CollectionUtils;
import org.bsc.async.AsyncGenerator;
import org.bsc.async.AsyncGeneratorQueue;

import java.time.Duration;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class LLmNodeAction implements NodeAction {
    private OpenAiService service;

    public LLmNodeAction(OpenAiService service) {
        this.service = service;
    }

    @Override
    public Map<String, Object> apply(OverAllState t) {
        BlockingQueue<AsyncGenerator.Data<StreamingOutput>> queue = new ArrayBlockingQueue<>(2000);
        AsyncGenerator.WithResult<StreamingOutput> it = new AsyncGenerator.WithResult<>(new AsyncGeneratorQueue.Generator<>(queue));
        final CountDownLatch streamStarted = new CountDownLatch(1);

        Flowable<ChatCompletionChunk> responseStream = service.streamChatCompletion(
                ChatCompletionRequest.builder()
                        .model("gpt-4o-mini")
                        .messages(Collections.singletonList(
                                new SystemMessage((String) t.value(OverAllState.DEFAULT_INPUT_KEY).get())))
                        .n(1)
                        .maxTokens(50)
                        .build());


        try {
            responseStream.serialize().subscribe(new DisposableSubscriber<>() {
                @Override
                protected void onStart() {
                    streamStarted.countDown();
                    request(1);
                }

                @Override
                public void onNext(ChatCompletionChunk chunk) {
                    try {
                        if (chunk == null || CollectionUtils.isEmpty(chunk.getChoices())) {
                            queue.add(AsyncGenerator.Data.done());
                            return;
                        }

                        ChatCompletionChoice choice = chunk.getChoices().get(0);
                        if (choice.getMessage() == null) {
                            queue.add(AsyncGenerator.Data.done());
                            return;
                        }

                        String content = choice.getMessage().getContent();
                        if (content == null) {
                            queue.add(AsyncGenerator.Data.done());
                            return;
                        }
                        t.updateState(Map.of("llm_result", content));
                        queue.add(AsyncGenerator.Data.of(new StreamingOutput(content, "llmNode", t)));
                        request(1);
                    } catch (Exception e) {
                        onError(e);
                    }
                }

                @Override
                public void onError(Throwable t) {
                    System.err.println("Stream error: " + t.getMessage());
                    queue.add(AsyncGenerator.Data.error(t));
                    dispose();
                }

                @Override
                public void onComplete() {
                    System.out.println("Stream completed");
                    queue.add(AsyncGenerator.Data.done());
                    dispose();
                }
            });
            streamStarted.await(5, TimeUnit.SECONDS);
        } catch (Exception e) {
            queue.add(AsyncGenerator.Data.error(e));
        }
        return Map.of("messages", it);
    }
}

