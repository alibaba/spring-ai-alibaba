package com.alibaba.cloud.ai.graph;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.graph.node.llm.LLMNodeAction;
import com.alibaba.cloud.ai.graph.state.KeyStrategy;
import com.alibaba.cloud.ai.graph.state.NodeState;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Description;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author 北极星 replace the DASHSCOPE_APIKEY to yours or set ENV
 */
@SpringBootTest(classes = { LLMNodeActionTest.class, LLMNodeActionTest.MockLawyer.class })
public class LLMNodeActionTest {

	@Autowired
	private ApplicationContext applicationContext;


	@TestConfiguration
	static class MockLawyer {

		record ConsultRequest(String caseDetail) {
		}

		record ConsultResponse(String consultResult) {
		}

		@Bean("consultLawyer")
		@Description("Judge the case is guilty or innocent")
		public Function<ConsultRequest, ConsultResponse> consultLawyer() {
			return consultRequest -> {
				System.out.println("call the tool");
				return new ConsultResponse("guilty, because you're challenging the law!");
			};
		}

	}

	private OverAllState mockState() {
		return new OverAllState()
				.inputs(Map.of("type", "law", "llmName", "Law-Breaker"))
				.addKeyAndStrategy("type", (o, o2) -> o2)
				.addKeyAndStrategy("llmName", (o, o2) -> o2);
	}

	@Test
	public void testVariableRender() throws Exception {
		LLMNodeAction node = LLMNodeAction.builder(new DashScopeChatModel(new DashScopeApi("${API_KEY}")))
			.withPromptTemplates(List.of(new SystemPromptTemplate("You're a helpful {type} assistant"),
					new PromptTemplate("If I step on an ant and kill it, am I breaking the law?")))
			.build();
		Map<String, Object> result = node.apply(mockState());
		assertEquals(1, result.size());
		System.out.println(result);

	}

	@Test
	public void testInputOutput() throws Exception {
		LLMNodeAction nodeAction = LLMNodeAction.builder(new DashScopeChatModel(new DashScopeApi("${API_KEY}")))
			.withPromptTemplates(List.of(new SystemPromptTemplate("Your name is {llmName}"),
					new PromptTemplate("What's your name?")))
			// if the input Key is `type`, will throw an exception, indicating that
			// `Missing variable names are [llmName]`
			.withInputKey("llmName")
			.withOutputKey("llmText")
			.build();
		Map<String, Object> result = nodeAction.apply(mockState());
		assertEquals(1, result.size());
		for (String outputKey : result.keySet()) {
			assertEquals("llmText", outputKey);
		}
		System.out.println(result);
	}

	@Test
	public void testFunctionCall() throws Exception {
		assert applicationContext.containsBean("consultLawyer")
				: "Bean 'consultLawyer' not found in application context.";
		// fixme this kind of function calling have issues: functionCallbackContext will
		// be null dashscope
		LLMNodeAction node = LLMNodeAction.builder(new DashScopeChatModel(new DashScopeApi("${API_KEY}")))
			.withPromptTemplates(List.of(
					new SystemPromptTemplate("You're a assistant of a lower, you need to consult lawyer at first"),
					new PromptTemplate("If I step on an ant and kill it, am I guilty?")))
			.withFunctions("consultLawyer")
			.build();

		Map<String, Object> result = node.apply(mockState());
		assertEquals(1, result.size());
		System.out.println(result);
	}

}
