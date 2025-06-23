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
package com.alibaba.cloud.ai.example.graph.stream;

import com.alibaba.cloud.ai.example.graph.stream.node.BaiduSearchNode;
import com.alibaba.cloud.ai.example.graph.stream.node.LLmNode;
import com.alibaba.cloud.ai.example.graph.stream.node.ResultNode;
import com.alibaba.cloud.ai.example.graph.stream.node.TavilySearchNode;
import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.NodeOutput;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.async.AsyncGenerator;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import com.alibaba.cloud.ai.graph.state.strategy.AppendStrategy;
import com.alibaba.cloud.ai.graph.streaming.StreamingOutput;
import com.alibaba.fastjson.JSON;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.AsyncContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.io.PrintWriter;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import static com.alibaba.cloud.ai.graph.StateGraph.END;
import static com.alibaba.cloud.ai.graph.StateGraph.START;
import static com.alibaba.cloud.ai.graph.action.AsyncNodeAction.node_async;

@RestController
@RequestMapping("/llm-stream")
public class LLmSearchStreamController {

	private StateGraph workflow;

	@Autowired
	private LLmNode lLmNode;

	@Autowired
	private BaiduSearchNode baiduSearchNode;

	@Autowired
	private TavilySearchNode tavilySearchNode;

	@Autowired
	private ResultNode resultNode;

	@PostConstruct
	public void init() throws GraphStateException {
		workflow = new StateGraph(
				() -> new OverAllState().registerKeyAndStrategy("parallel_result", new AppendStrategy())
					.registerKeyAndStrategy("messages1", new AppendStrategy())
					.registerKeyAndStrategy("messages", new AppendStrategy()))
			.addNode("baiduSearchNode", node_async(baiduSearchNode))
			.addNode("tavilySearchNode", node_async(tavilySearchNode))
			.addNode("resultNode", node_async(resultNode))
			.addNode("llmNode", node_async(lLmNode))
			.addEdge(START, "baiduSearchNode")
			.addEdge(START, "tavilySearchNode")
			.addEdge("baiduSearchNode", "llmNode")
			.addEdge("tavilySearchNode", "llmNode")
			.addEdge("llmNode", "resultNode")
			.addEdge("resultNode", END);

	}

	@PostMapping("/search/chat")
	public void searchChat(HttpServletRequest request, HttpServletResponse response,
			@RequestBody Map<String, Object> inputData) throws Exception {

		AsyncContext asyncContext = request.startAsync();
		response.setContentType(MediaType.TEXT_EVENT_STREAM_VALUE + ";charset=UTF-8");
		response.setCharacterEncoding("UTF-8");

		response.setHeader("Cache-Control", "no-cache");
		response.setHeader("Connection", "keep-alive");

		CompiledGraph compiledGraph = workflow.compile();
		AsyncGenerator<NodeOutput> generator = compiledGraph.stream(inputData,
				RunnableConfig.builder().threadId(UUID.randomUUID().toString()).build());

		CompletableFuture.runAsync(() -> {
			try (PrintWriter writer = response.getWriter()) {
				generator.forEachAsync(output -> {
					System.out.println("output = " + output);
					try {
						if (output instanceof StreamingOutput) {
							writer.write("data: " + ((StreamingOutput) output).chunk() + "\n\n");
						}
						else {
							Optional<List> value = output.state().value("messages", List.class);
							value.ifPresent(v -> writer.write("data: " + v.get(0) + "\n\n"));
						}
						writer.flush();
					}
					catch (Exception e) {
						asyncContext.complete();
					}
				}).thenRun(() -> {
					writer.write("event: done\ndata: \n\n");
					writer.flush();
					asyncContext.complete();
				});
			}
			catch (Exception e) {
				asyncContext.complete();
			}
		});
	}

	@PostMapping(value = "/search/chat/v2", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
	public Flux<ServerSentEvent<String>> stream(HttpServletRequest request, @RequestBody Map<String, Object> inputData)
			throws Exception {
		CompiledGraph compiledGraph = workflow.compile();
		String threadId = UUID.randomUUID().toString();

		Sinks.Many<ServerSentEvent<String>> sink = Sinks.many().unicast().onBackpressureBuffer();

		AsyncGenerator<NodeOutput> generator = compiledGraph.stream(inputData,
				RunnableConfig.builder().threadId(threadId).build());

		CompletableFuture.runAsync(() -> {
			generator.forEachAsync(output -> {
				try {
					System.out.println("output = " + output);
					if (output instanceof StreamingOutput) {
						StreamingOutput streamingOutput = (StreamingOutput) output;
						sink.tryEmitNext(ServerSentEvent.builder(JSON.toJSONString(streamingOutput.chunk())).build());
					}
					else {
						sink.tryEmitNext(
								ServerSentEvent.builder(JSON.toJSONString(output.state().value("messages"))).build());
					}
				}
				catch (Exception e) {
					throw new CompletionException(e);
				}
			}).thenRun(() -> sink.tryEmitComplete()).exceptionally(ex -> {
				sink.tryEmitError(ex);
				return null;
			});
		});

		return sink.asFlux()
			.doOnCancel(() -> System.out.println("Client disconnected from stream"))
			.doOnError(e -> System.err.println("Error occurred during streaming: " + e));
	}

}
