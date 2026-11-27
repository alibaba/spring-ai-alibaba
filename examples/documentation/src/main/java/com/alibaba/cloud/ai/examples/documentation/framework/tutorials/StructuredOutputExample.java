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
package com.alibaba.cloud.ai.examples.documentation.framework.tutorials;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.exception.GraphRunnerException;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Objects;

/** Demonstrates validated structured output from ReactAgent and ChatClient. */
public class StructuredOutputExample {

	public static final String REVIEW_SCHEMA = """
			{
			  "type": "object",
			  "properties": {
			    "rating": {"type": "integer", "minimum": 1, "maximum": 5},
			    "sentiment": {"enum": ["POSITIVE", "NEUTRAL", "NEGATIVE"]},
			    "keyPoints": {"type": "array", "items": {"type": "string"}}
			  },
			  "required": ["rating", "sentiment", "keyPoints"],
			  "additionalProperties": false
			}
			""";

	public static ContactInfo extractContact(ChatModel model, String text) throws GraphRunnerException {
		var agent = ReactAgent.builder().name("contact_extractor").model(model)
				.outputType(ContactInfo.class).build();
		return agent.callStructured("提取姓名、电子邮件和电话号码。\n" + text, ContactInfo.class);
	}

	public static ProductReview analyzeReview(ChatModel model, String text) throws GraphRunnerException {
		var agent = ReactAgent.builder().name("review_analyzer").model(model)
				.outputSchema(REVIEW_SCHEMA).outputType(ProductReview.class)
				.build();
		return agent.callStructured("分析这条商品评价。\n" + text, ProductReview.class);
	}

	/** Uses schema feedback to correct a standalone extraction call. */
	public static ContactInfo validatedContact(ChatClient client, String input) {
		return client.prompt().user(input).call()
				.entity(ContactInfo.class, spec -> spec.validateSchema());
	}

	/** Requests native JSON Schema from a compatible provider and validates its response. */
	public static ContactInfo providerStructuredContact(ChatClient client, String input) {
		return client.prompt().user(input).call()
				.entity(ContactInfo.class, spec -> spec.useProviderStructuredOutput().validateSchema());
	}

	public static void main(String[] args) throws GraphRunnerException {
		String apiKey = System.getenv("AI_DASHSCOPE_API_KEY");
		if (!StringUtils.hasText(apiKey)) {
			System.err.println("请先设置 AI_DASHSCOPE_API_KEY");
			return;
		}
		var api = DashScopeApi.builder().apiKey(apiKey).build();
		var model = DashScopeChatModel.builder().dashScopeApi(api).build();
		System.out.println(extractContact(model, "联系人张三，zhangsan@example.com，13800138000"));
		System.out.println(analyzeReview(model, "5 星。配送很快，价格偏高。"));
	}

	public record ContactInfo(String name, String email, String phone) {

		public ContactInfo {
			if (!StringUtils.hasText(name)) {
				throw new IllegalArgumentException("A contact name is required");
			}
		}

	}

	public enum Sentiment {
		POSITIVE, NEUTRAL, NEGATIVE
	}

	public record ProductReview(Integer rating, Sentiment sentiment, List<String> keyPoints) {

		public ProductReview {
			if (rating == null || rating < 1 || rating > 5) {
				throw new IllegalArgumentException("Review rating must be between 1 and 5");
			}
			Objects.requireNonNull(sentiment, "Sentiment is required");
			keyPoints = List.copyOf(keyPoints);
		}

	}

}

