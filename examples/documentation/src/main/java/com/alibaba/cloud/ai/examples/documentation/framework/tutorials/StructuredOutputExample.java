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
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;
import com.alibaba.cloud.ai.graph.exception.GraphRunnerException;

import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.converter.BeanOutputConverter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Structured Output Tutorial - 完整代码示例
 * 展示如何让Agent返回特定格式的结构化数据
 *
 * 来源：structured-output.md
 */
public class StructuredOutputExample {

	// ==================== 基础类定�?====================

	/**
	 * 示例1：基�?JSON Schema
	 */
	public static void basicJsonSchema() throws GraphRunnerException {
		DashScopeApi dashScopeApi = DashScopeApi.builder()
				.apiKey(System.getenv("AI_DASHSCOPE_API_KEY"))
				.build();

		ChatModel chatModel = DashScopeChatModel.builder()
				.dashScopeApi(dashScopeApi)
				.build();

		// Use BeanOutputConverter to generate outputSchema
		BeanOutputConverter<ContactInfo> outputConverter = new BeanOutputConverter<>(ContactInfo.class);
		String format = outputConverter.getFormat();

		ReactAgent agent = ReactAgent.builder()
				.name("contact_extractor")
				.model(chatModel)
				.outputSchema(format)
				.build();

		AssistantMessage result = agent.call(
				"从以下信息提取联系方式：张三，zhangsan@example.com�?555) 123-4567"
		);

		System.out.println(result.getText());
		// 输出: {"name": "张三", "email": "zhangsan@example.com", "phone": "(555) 123-4567"}
	}

	/**
	 * 示例2：复杂嵌�?Schema
	 */
	public static void complexNestedSchema() throws GraphRunnerException {
		DashScopeApi dashScopeApi = DashScopeApi.builder()
				.apiKey(System.getenv("AI_DASHSCOPE_API_KEY"))
				.build();

		ChatModel chatModel = DashScopeChatModel.builder()
				.dashScopeApi(dashScopeApi)
				.build();

		// Use BeanOutputConverter to generate outputSchema
		BeanOutputConverter<ProductReview> outputConverter = new BeanOutputConverter<>(ProductReview.class);
		String format = outputConverter.getFormat();

		ReactAgent agent = ReactAgent.builder()
				.name("review_analyzer")
				.model(chatModel)
				.outputSchema(format)
				.build();

		AssistantMessage result = agent.call(
				"分析评价：这个产品很棒，5星好评。配送快速，但价格稍贵�?
		);

		System.out.println(result.getText());
		// 输出: {"rating": 5, "sentiment": "正面", "keyPoints": [...], "details": {...}}
	}

	/**
	 * 示例3：结构化分析 Schema
	 */
	public static void structuredAnalysisSchema() throws GraphRunnerException {
		DashScopeApi dashScopeApi = DashScopeApi.builder()
				.apiKey(System.getenv("AI_DASHSCOPE_API_KEY"))
				.build();

		ChatModel chatModel = DashScopeChatModel.builder()
				.dashScopeApi(dashScopeApi)
				.build();

		// Use BeanOutputConverter to generate outputSchema
		BeanOutputConverter<TextAnalysis> outputConverter = new BeanOutputConverter<>(TextAnalysis.class);
		String format = outputConverter.getFormat();

		ReactAgent agent = ReactAgent.builder()
				.name("text_analyzer")
				.model(chatModel)
				.outputSchema(format)
				.build();

		AssistantMessage result = agent.call(
				"分析这段文字：昨天，李明在北京参加了阿里巴巴公司的技术大会，感受到了创新的力量�?
		);

		System.out.println(result.getText());
	}

	// ==================== 输出 Schema 策略 ====================

	/**
	 * 示例4：使�?outputType - ContactInfo
	 */
	public static void outputTypeContactInfo() throws GraphRunnerException {
		DashScopeApi dashScopeApi = DashScopeApi.builder()
				.apiKey(System.getenv("AI_DASHSCOPE_API_KEY"))
				.build();

		ChatModel chatModel = DashScopeChatModel.builder()
				.dashScopeApi(dashScopeApi)
				.build();

		ReactAgent agent = ReactAgent.builder()
				.name("contact_extractor")
				.model(chatModel)
				.outputType(ContactInfo.class)
				.saver(new MemorySaver())
				.build();

		AssistantMessage result = agent.call(
				"从以下信息提取联系方式：张三，zhangsan@example.com�?555) 123-4567"
		);

		System.out.println(result.getText());
	}

	/**
	 * 示例5：使�?outputType - ProductReview
	 */
	public static void outputTypeProductReview() throws GraphRunnerException {
		DashScopeApi dashScopeApi = DashScopeApi.builder()
				.apiKey(System.getenv("AI_DASHSCOPE_API_KEY"))
				.build();

		ChatModel chatModel = DashScopeChatModel.builder()
				.dashScopeApi(dashScopeApi)
				.build();

		ReactAgent agent = ReactAgent.builder()
				.name("review_analyzer")
				.model(chatModel)
				.outputType(ProductReview.class)
				.saver(new MemorySaver())
				.build();

		AssistantMessage result = agent.call(
				"分析评价：这个产品很棒，5星好评。配送快速，但价格稍贵�?
		);

		System.out.println(result.getText());
	}

	/**
	 * 示例6：使�?outputType - TextAnalysis
	 */
	public static void outputTypeTextAnalysis() throws GraphRunnerException {
		DashScopeApi dashScopeApi = DashScopeApi.builder()
				.apiKey(System.getenv("AI_DASHSCOPE_API_KEY"))
				.build();

		ChatModel chatModel = DashScopeChatModel.builder()
				.dashScopeApi(dashScopeApi)
				.build();

		ReactAgent agent = ReactAgent.builder()
				.name("text_analyzer")
				.model(chatModel)
				.outputType(TextAnalysis.class)
				.saver(new MemorySaver())
				.build();

		AssistantMessage result = agent.call(
				"分析这段文字：昨天，李明在北京参加了阿里巴巴公司的技术大会，感受到了创新的力量�?
		);

		System.out.println(result.getText());
	}

	// ==================== 输出类型策略 ====================

	/**
	 * 示例7：Try-Catch 模式
	 */
	public static void tryCatchPattern() {
		DashScopeApi dashScopeApi = DashScopeApi.builder()
				.apiKey(System.getenv("AI_DASHSCOPE_API_KEY"))
				.build();

		ChatModel chatModel = DashScopeChatModel.builder()
				.dashScopeApi(dashScopeApi)
				.build();

		ReactAgent agent = ReactAgent.builder()
				.name("data_extractor")
				.model(chatModel)
				.outputType(ContactInfo.class)
				.build();

		try {
			AssistantMessage result = agent.call("提取数据");
			ObjectMapper mapper = new ObjectMapper();
			ContactInfo data = mapper.readValue(result.getText(), ContactInfo.class);
			// 处理数据
			System.out.println("Name: " + data.getName());
		}
		catch (JsonProcessingException | GraphRunnerException e) {
			System.err.println("JSON解析失败: " + e.getMessage());
			// 回退处理
		}
	}

	/**
	 * 示例8：验证模�?
	 */
	public static void validationPattern() {
		DashScopeApi dashScopeApi = DashScopeApi.builder()
				.apiKey(System.getenv("AI_DASHSCOPE_API_KEY"))
				.build();

		ChatModel chatModel = DashScopeChatModel.builder()
				.dashScopeApi(dashScopeApi)
				.build();

		ReactAgent agent = ReactAgent.builder()
				.name("validated_agent")
				.model(chatModel)
				.outputType(ValidatedOutput.class)
				.build();

		try {
			AssistantMessage result = agent.call("生成评价");
			ObjectMapper mapper = new ObjectMapper();
			ValidatedOutput output = mapper.readValue(result.getText(), ValidatedOutput.class);
			output.validate();  // 如果无效则抛出异�?
			System.out.println("Valid output: " + output.getTitle());
		}
		catch (Exception e) {
			System.err.println("Validation failed: " + e.getMessage());
		}
	}

	/**
	 * 示例9：重试模�?
	 */
	public static void retryPattern() {
		DashScopeApi dashScopeApi = DashScopeApi.builder()
				.apiKey(System.getenv("AI_DASHSCOPE_API_KEY"))
				.build();

		ChatModel chatModel = DashScopeChatModel.builder()
				.dashScopeApi(dashScopeApi)
				.build();

		ReactAgent agent = ReactAgent.builder()
				.name("retry_agent")
				.model(chatModel)
				.outputType(ContactInfo.class)
				.build();

		int maxRetries = 3;
		ContactInfo data = null;
		ObjectMapper mapper = new ObjectMapper();

		for (int i = 0; i < maxRetries; i++) {
			try {
				AssistantMessage result = agent.call("提取数据");
				data = mapper.readValue(result.getText(), ContactInfo.class);
				break;  // 成功
			}
			catch (Exception e) {
				if (i == maxRetries - 1) {
					throw new RuntimeException("多次尝试后仍然失�?, e);
				}
				System.out.println("�? + (i + 1) + "次尝试失败，重试�?..");
			}
		}

		if (data != null) {
			System.out.println("Successfully extracted: " + data.getName());
		}
	}

	// ==================== 错误处理 ====================

	/**
	 * 示例10：完整的结构化输出示�?
	 */
	public static void comprehensiveExample() throws GraphRunnerException {
		DashScopeApi dashScopeApi = DashScopeApi.builder()
				.apiKey(System.getenv("AI_DASHSCOPE_API_KEY"))
				.build();

		ChatModel chatModel = DashScopeChatModel.builder()
				.dashScopeApi(dashScopeApi)
				.build();

		// 使用 outputType
		ReactAgent typeAgent = ReactAgent.builder()
				.name("type_agent")
				.model(chatModel)
				.outputType(ContactInfo.class)
				.saver(new MemorySaver())
				.build();

		// 使用 outputSchema (通过 BeanOutputConverter 生成)
		BeanOutputConverter<ContactInfo> outputConverter = new BeanOutputConverter<>(ContactInfo.class);
		String format = outputConverter.getFormat();

		ReactAgent schemaAgent = ReactAgent.builder()
				.name("schema_agent")
				.model(chatModel)
				.outputSchema(format)
				.saver(new MemorySaver())
				.build();

		String input = "联系人：王五，wangwu@example.com�?3800138000";

		// 使用 outputType
		AssistantMessage typeResult = typeAgent.call(input);
		System.out.println("Type-based: " + typeResult.getText());

		// 使用 outputSchema
		AssistantMessage schemaResult = schemaAgent.call(input);
		System.out.println("Schema-based: " + schemaResult.getText());
	}

	public static void main(String[] args) {
		System.out.println("=== Structured Output Tutorial Examples ===");
		System.out.println("注意：需要设�?AI_DASHSCOPE_API_KEY 环境变量\n");

		try {
			System.out.println("\n--- 示例1：基础 JSON Schema ---");
			basicJsonSchema();

			System.out.println("\n--- 示例2：复杂嵌�?Schema ---");
			complexNestedSchema();

			System.out.println("\n--- 示例3：结构化分析 Schema ---");
			structuredAnalysisSchema();

			System.out.println("\n--- 示例4：OutputType - 联系信息 ---");
			outputTypeContactInfo();

			System.out.println("\n--- 示例5：OutputType - 产品评论 ---");
			outputTypeProductReview();

			System.out.println("\n--- 示例6：OutputType - 文本分析 ---");
			outputTypeTextAnalysis();

			System.out.println("\n--- 示例7：Try-Catch 模式 ---");
			tryCatchPattern();

			System.out.println("\n--- 示例8：验证模�?---");
			validationPattern();

			System.out.println("\n--- 示例9：重试模�?---");
			retryPattern();

			System.out.println("\n--- 示例10：综合示�?---");
			comprehensiveExample();

			System.out.println("\n=== 所有示例执行完�?===");
		}
		catch (Exception e) {
			System.err.println("执行示例时发生错�? " + e.getMessage());
			e.printStackTrace();
		}
	}

	/**
	 * 联系信息输出�?
	 */
	public static class ContactInfo {
		private String name;
		private String email;
		private String phone;

		// Getters and Setters
		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public String getEmail() {
			return email;
		}

		public void setEmail(String email) {
			this.email = email;
		}

		public String getPhone() {
			return phone;
		}

		public void setPhone(String phone) {
			this.phone = phone;
		}
	}

	/**
	 * 产品评价输出�?
	 */
	public static class ProductReview {
		private int rating;
		private String sentiment;
		private String[] keyPoints;
		private ReviewDetails details;

		public int getRating() {
			return rating;
		}

		public void setRating(int rating) {
			this.rating = rating;
		}

		public String getSentiment() {
			return sentiment;
		}

		public void setSentiment(String sentiment) {
			this.sentiment = sentiment;
		}

		public String[] getKeyPoints() {
			return keyPoints;
		}

		public void setKeyPoints(String[] keyPoints) {
			this.keyPoints = keyPoints;
		}

		public ReviewDetails getDetails() {
			return details;
		}

		public void setDetails(ReviewDetails details) {
			this.details = details;
		}

		public static class ReviewDetails {
			private String[] pros;
			private String[] cons;

			public String[] getPros() {
				return pros;
			}

			public void setPros(String[] pros) {
				this.pros = pros;
			}

			public String[] getCons() {
				return cons;
			}

			public void setCons(String[] cons) {
				this.cons = cons;
			}
		}
	}

	// ==================== 综合示例 ====================

	/**
	 * 文本分析输出�?
	 */
	public static class TextAnalysis {
		private String summary;
		private String[] keywords;
		private String sentiment;
		private Entities entities;

		public String getSummary() {
			return summary;
		}

		public void setSummary(String summary) {
			this.summary = summary;
		}

		public String[] getKeywords() {
			return keywords;
		}

		public void setKeywords(String[] keywords) {
			this.keywords = keywords;
		}

		public String getSentiment() {
			return sentiment;
		}

		public void setSentiment(String sentiment) {
			this.sentiment = sentiment;
		}

		public Entities getEntities() {
			return entities;
		}

		public void setEntities(Entities entities) {
			this.entities = entities;
		}

		public static class Entities {
			private String[] persons;
			private String[] locations;
			private String[] organizations;

			public String[] getPersons() {
				return persons;
			}

			public void setPersons(String[] persons) {
				this.persons = persons;
			}

			public String[] getLocations() {
				return locations;
			}

			public void setLocations(String[] locations) {
				this.locations = locations;
			}

			public String[] getOrganizations() {
				return organizations;
			}

			public void setOrganizations(String[] organizations) {
				this.organizations = organizations;
			}
		}
	}

	// ==================== Main 方法 ====================

	/**
	 * 验证输出�?
	 */
	public static class ValidatedOutput {
		private String title;
		private Integer rating;

		public void validate() throws IllegalArgumentException {
			if (title == null || title.isEmpty()) {
				throw new IllegalArgumentException("标题不能为空");
			}
			if (rating != null && (rating < 1 || rating > 5)) {
				throw new IllegalArgumentException("评分必须�?-5之间");
			}
		}

		// Getter �?Setter 方法
		public String getTitle() {
			return title;
		}

		public void setTitle(String title) {
			this.title = title;
		}

		public Integer getRating() {
			return rating;
		}

		public void setRating(Integer rating) {
			this.rating = rating;
		}
	}
}

