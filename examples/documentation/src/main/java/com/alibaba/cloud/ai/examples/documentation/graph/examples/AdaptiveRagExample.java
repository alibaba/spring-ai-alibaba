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
package com.alibaba.cloud.ai.examples.documentation.graph.examples;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.PromptTemplate;

import java.util.Map;
import java.util.function.Function;

/**
 * Adaptive RAG 示例
 * 演示自适应检索增强生成
 */
public class AdaptiveRagExample {

	/**
	 * 测试示例
	 */
	public static void testAnswerGrader(ChatClient.Builder chatClientBuilder) {
		// 配置 ChatClient
		var grader = new AnswerGrader(chatClientBuilder);

		// 测试案例 1: 答案不相关
		var args = new AnswerGrader.Arguments(
				"What are the four operations?",
				"LLM means Large Language Model"
		);
		var result = grader.apply(args);
		System.out.println(result); // 输出: Score: no

		// 测试案例 2: 答案相关
		args = new AnswerGrader.Arguments(
				"What are the four operations",
				"There are four basic operations: addition, subtraction, multiplication, and division."
		);
		result = grader.apply(args);
		System.out.println(result); // 输出: Score: yes

		// 测试案例 3: NFL draft 问题
		args = new AnswerGrader.Arguments(
				"What player at the Bears expected to draft first in the 2024 NFL draft?",
				"The Bears selected USC quarterback Caleb Williams with the No. 1 pick in the 2024 NFL Draft."
		);
		result = grader.apply(args);
		System.out.println(result); // 输出: Score: yes
	}

	public static void main(String[] args) {
		System.out.println("=== Adaptive RAG 示例 ===\n");

		try {
			// 示例: 测试 AnswerGrader（需要 ChatClient）
			System.out.println("示例: 测试 AnswerGrader");
			System.out.println("注意: 此示例需要 ChatClient，跳过执行");
			// testAnswerGrader(ChatClient.builder(...));
			System.out.println();

			System.out.println("所有示例执行完成");
			System.out.println("提示: 请配置 ChatClient 后运行完整示例");
		}
		catch (Exception e) {
			System.err.println("执行示例时出错: " + e.getMessage());
			e.printStackTrace();
		}
	}

	/**
	 * AnswerGrader 实现
	 */
	public static class AnswerGrader implements Function<AnswerGrader.Arguments, AnswerGrader.Score> {

		private final ChatClient chatClient;

		public AnswerGrader(ChatClient.Builder chatClientBuilder) {
			this.chatClient = chatClientBuilder.build();
		}

		@Override
		public Score apply(Arguments args) {
			String systemPrompt = """
					You are a grader assessing whether an answer addresses and/or resolves a question.
					
					Give a binary score 'yes' or 'no'. Yes, means that the answer resolves the question otherwise return 'no'
					""";

			String userPrompt = """
					User question:
					
					{question}
					
					LLM generation:
					
					{generation}
					""";

			PromptTemplate promptTemplate = new PromptTemplate(userPrompt);
			Map<String, Object> params = Map.of(
					"question", args.question(),
					"generation", args.generation()
			);

			String response = chatClient.prompt()
					.system(systemPrompt)
					.user(promptTemplate.create(params).getContents())
					.call()
					.content();

			Score score = new Score();
			score.binaryScore = response.toLowerCase().contains("yes") ? "yes" : "no";

			return score;
		}

		/**
		 * Binary score to assess answer addresses question.
		 */
		public static class Score {
			public String binaryScore;

			@Override
			public String toString() {
				return "Score: " + binaryScore;
			}
		}

		public record Arguments(String question, String generation) {
		}
	}
}

