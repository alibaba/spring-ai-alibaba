package com.alibaba.cloud.ai.example.graph.stream;

import com.alibaba.cloud.ai.graph.*;
import com.alibaba.cloud.ai.graph.state.strategy.AppendStrategy;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import org.bsc.async.AsyncGenerator;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletionException;

import static com.alibaba.cloud.ai.graph.StateGraph.END;
import static com.alibaba.cloud.ai.graph.StateGraph.START;
import static com.alibaba.cloud.ai.graph.action.AsyncNodeAction.node_async;

@RestController
@RequestMapping("/http-stream")
public class HttpStreamController {

	private StateGraph workflow;

	private ObjectMapper objectMapper = new ObjectMapper();

	@PostConstruct
	public void init() throws GraphStateException {
		// 定义工作流
		// 创建状态和策略
		workflow = new StateGraph(() -> new OverAllState().registerKeyAndStrategy("messages", new AppendStrategy())
			.registerKeyAndStrategy("count", (oldValue, newValue) -> oldValue == null ? newValue : 1))
			.addNode("collectInput", node_async(s -> {
				// 处理输入
				String input = s.value("input", "");
				return Map.of("messages", "Received: " + input, "count", 1);
			}))
			.addNode("processData", node_async(s -> {
				// 处理数据 - 这里可以是耗时操作，会以流式方式返回结果
				return Map.of("messages", "Processing...", "count", 1);
			}))
			.addNode("generateResponse", node_async(s -> {
				// 生成最终响应
				int count = s.value("count", 0);
				return Map.of("messages", "Response generated (processed " + count + " items)", "result", "Success");
			}))
			.addEdge(START, "collectInput")
			.addEdge("collectInput", "processData")
			.addEdge("processData", "generateResponse")
			.addEdge("generateResponse", END);

	}

	@PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
	public Flux<ServerSentEvent<String>> stream(HttpServletRequest request, @RequestBody Map<String, Object> inputData)
			throws Exception {
		// 编译工作流
		CompiledGraph compiledGraph = workflow.compile();

		// 初始化输入
		OverAllState overAllState = compiledGraph.overAllState();
		overAllState.input(inputData);

		// 从请求中获取输入
		String threadId = UUID.randomUUID().toString();

		// 创建 Sink 用于发送事件
		Sinks.Many<ServerSentEvent<String>> sink = Sinks.many().unicast().onBackpressureBuffer();
		Flux<ServerSentEvent<String>> flux = sink.asFlux();

		// 使用 CompiledGraph 的流式功能
		AsyncGenerator<NodeOutput> generator = compiledGraph.stream(inputData,
				RunnableConfig.builder().threadId(threadId).build());

		// 处理流式输出
		generator.forEachAsync(output -> {
			try {
				Map<String, Object> data = output.state().data();
				System.out.println("data = " + data);
				Object messages = data.get("messages");
				sink.tryEmitNext(ServerSentEvent.builder(JSON.toJSONString(messages)).build());
			}
			catch (Exception e) {
				throw new CompletionException(e);
			}
		}).thenAccept(v -> sink.tryEmitComplete()).exceptionally(e -> {
			sink.tryEmitError(e);
			return null;
		});

		return flux;
	}

}