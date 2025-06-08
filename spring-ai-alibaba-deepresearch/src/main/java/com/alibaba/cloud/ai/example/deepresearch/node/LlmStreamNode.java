package com.alibaba.cloud.ai.example.deepresearch.node;

import com.alibaba.cloud.ai.example.deepresearch.model.Plan;
import com.alibaba.cloud.ai.example.deepresearch.util.TemplateUtil;
import com.alibaba.cloud.ai.graph.NodeOutput;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.alibaba.cloud.ai.graph.streaming.StreamingChatGenerator;
import com.alibaba.fastjson.JSON;
import org.bsc.async.AsyncGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static org.springframework.ai.chat.memory.ChatMemory.CONVERSATION_ID;

/**
 * @Author: XiaoYunTao
 * @Date: 2025/6/7
 */
public class LlmStreamNode implements NodeAction {

	private static final Logger logger = LoggerFactory.getLogger(LlmStreamNode.class);

	private final String REQUEST_SPEC = "requestSpec";

	private final ChatClient chatClient;

	private final ToolCallback[] toolCallbacks;

	private final BeanOutputConverter<Plan> converter;

	private final InMemoryChatMemoryRepository chatMemoryRepository = new InMemoryChatMemoryRepository();

	private final int MAX_MESSAGES = 100;

	private final MessageWindowChatMemory messageWindowChatMemory = MessageWindowChatMemory.builder()
		.chatMemoryRepository(chatMemoryRepository)
		.maxMessages(MAX_MESSAGES)
		.build();

	public LlmStreamNode(ChatClient.Builder chatClientBuilder, ToolCallback[] toolCallbacks) {
		this.chatClient = chatClientBuilder
			.defaultAdvisors(MessageChatMemoryAdvisor.builder(messageWindowChatMemory).build())
			.build();
		this.toolCallbacks = toolCallbacks;
		this.converter = new BeanOutputConverter<>(new ParameterizedTypeReference<Plan>() {
		});
	}

	@Override
	public Map<String, Object> apply(OverAllState state) throws Exception {
		logger.info("LlmStreamNode is running.");
		List<Message> messages = TemplateUtil.applyPromptTemplate("planner", state);
		// add human feedback content
		if (StringUtils.hasText(state.data().getOrDefault("feed_back_content", "").toString())) {
			messages.add(new UserMessage(state.data().getOrDefault("feed_back_content", "").toString()));
		}
		Integer planIterations = state.value("plan_iterations", 0);
		Integer maxStepNum = state.value("max_step_num", 3);
		String threadId = state.value("thread_id", "__default__");

		Boolean enableBackgroundInvestigation = state.value("enable_background_investigation", false);
		List<String> backgroundInvestigationResults = state.value("background_investigation_results",
				new ArrayList<>());

		if (planIterations == 0 && enableBackgroundInvestigation && !backgroundInvestigationResults.isEmpty()) {
			messages.add(SystemMessage.builder()
				.text("background investigation results of user query:\n" + backgroundInvestigationResults + "\n")
				.build());
		}
		String nextStep = "reporter";
		Map<String, Object> updated = new HashMap<>();
		logger.info("planIterations:{}", planIterations);
		if (planIterations > maxStepNum) {
			logger.info("planIterations reaches the upper limit");
			updated.put("planner_next_node", nextStep);
			return updated;
		}

		Flux<ChatResponse> streamResult = chatClient.prompt(converter.getFormat())
			.advisors(a -> a.param(CONVERSATION_ID, threadId))
			.options(ToolCallingChatOptions.builder().toolCallbacks(toolCallbacks).build())
			.messages(messages)
			.stream()
			.chatResponse();

		var generator = StreamingChatGenerator.builder()
			.startingNode("llm_stream")
			.startingState(state)
			.mapResult(response -> Map.of("llm_node_generator",
					Objects.requireNonNull(response.getResult().getOutput().getText())))
			.build(streamResult);

		return Map.of("llm_node_generator", generator);
	}

}
