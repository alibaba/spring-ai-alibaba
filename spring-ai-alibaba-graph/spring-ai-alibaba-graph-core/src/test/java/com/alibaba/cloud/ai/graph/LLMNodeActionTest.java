package com.alibaba.cloud.ai.graph;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.graph.node.LLMNodeAction;
import com.alibaba.cloud.ai.graph.state.AgentState;
import com.alibaba.cloud.ai.graph.state.AppenderChannel;
import com.alibaba.cloud.ai.graph.state.Channel;
import com.alibaba.cloud.ai.graph.utils.CollectionsUtils;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.UserMessage;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author 北极星
 */

public class LLMNodeActionTest {

	@Test
	void init_llmNode() throws Exception {

		LLMNodeAction node = LLMNodeAction.builder(new DashScopeChatModel(new DashScopeApi("${DashScope_API-Key}")))
			// .withSysPrompt("You're a helpful assistant")
			// .withFunctions("LarkSuiteDocService")
			.build();
		Map message = node.apply(new MessagesState(
				Map.of("messages", List.of(new UserMessage("How do I use larksuite to create a doc ? ")))));
		assertEquals(1, message.size());

	}

	static class MessagesState extends AgentState {

		static Map<String, Channel<?>> SCHEMA = CollectionsUtils.mapOf("messages",
				AppenderChannel.<String>of(ArrayList::new));

		public MessagesState(Map<String, Object> initData) {
			super(initData);
		}

		int steps() {
			return value("steps", 0);
		}

		List<String> messages() {
			return this.<List<String>>value("messages").orElseThrow(() -> new RuntimeException("messages not found"));
		}

	}

}
