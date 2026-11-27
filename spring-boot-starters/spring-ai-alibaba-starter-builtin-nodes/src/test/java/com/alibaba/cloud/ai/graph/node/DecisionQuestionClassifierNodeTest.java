/*
 * Copyright 2024-2026 the original author or authors.
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

package com.alibaba.cloud.ai.graph.node;

import java.util.List;
import java.util.Map;

import com.alibaba.cloud.ai.graph.OverAllState;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.decision.DecisionModel;
import org.springframework.ai.decision.DecisionResponse;
import org.springframework.ai.decision.answer.ChoiceAnswer;
import org.springframework.ai.decision.answer.RefusalAnswer;
import org.springframework.ai.decision.question.ChoiceQuestion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DecisionQuestionClassifierNodeTest {

	@Test
	void routesByStableCategoryIdAndRendersLabelsAndInstructions() throws Exception {
		var node = node(prompt -> {
			var question = assertInstanceOf(ChoiceQuestion.class, prompt.getQuestions().get("category"));
			assertEquals(Map.of("sales", "New sales", "support", "Technical support"), question.getOptions());
			assertTrue(question.getText().contains("Use the support queue"));
			return DecisionResponse.builder(prompt).answer("category", ChoiceAnswer.builder().value("support")
					.probabilities(Map.of("sales", 0.1, "support", 0.9)).build()).build();
		});
		var result = node.apply(state());
		assertEquals("support", result.get("route"));
		assertEquals("Technical support", assertInstanceOf(AssistantMessage.class, result.get("messages")).getText());
	}

	@Test
	void refusesToRouteWhenTheDecisionModelRefuses() {
		var node = node(prompt -> DecisionResponse.builder(prompt).answer("category",
				RefusalAnswer.builder().reason("filtered").build()).build());
		assertThrows(IllegalStateException.class, () -> node.apply(state()));
	}

	@Test
	void preservesTheChatClientOption() throws Exception {
		var client = ChatClient.builder(prompt -> new ChatResponse(
				List.of(new Generation(new AssistantMessage("{\"category_name\":\"Technical support\"}"))))).build();
		var node = QuestionClassifierNode.builder().chatClient(client).inputTextKey("input")
				.categories(Map.of("sales", "New sales", "support", "{team} support")).outputKey("route").build();

		assertEquals("support", node.apply(state()).get("route"));
	}

	private static QuestionClassifierNode node(DecisionModel model) {
		return QuestionClassifierNode.builder().decisionModel(model).inputTextKey("input")
				.categories(Map.of("sales", "New sales", "support", "{team} support"))
				.classificationInstructions(List.of("Use the {queue} queue")).outputKey("route").build();
	}

	private static OverAllState state() {
		return new OverAllState(Map.of("input", "The device is broken", "team", "Technical", "queue", "support",
				"messages", List.of()));
	}

}
