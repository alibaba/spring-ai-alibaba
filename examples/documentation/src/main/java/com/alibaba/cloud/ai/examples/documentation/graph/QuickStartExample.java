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
package com.alibaba.cloud.ai.examples.documentation.graph;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.graph.CompileConfig;
import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.KeyStrategy;
import com.alibaba.cloud.ai.graph.KeyStrategyFactory;
import com.alibaba.cloud.ai.graph.NodeOutput;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.alibaba.cloud.ai.graph.checkpoint.config.SaverConfig;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import com.alibaba.cloud.ai.graph.state.strategy.AppendStrategy;
import com.alibaba.cloud.ai.graph.state.strategy.ReplaceStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.alibaba.cloud.ai.graph.StateGraph.END;
import static com.alibaba.cloud.ai.graph.StateGraph.START;
import static com.alibaba.cloud.ai.graph.action.AsyncEdgeAction.edge_async;
import static com.alibaba.cloud.ai.graph.action.AsyncNodeAction.node_async;

/**
 * Graph 工作流编排快速入门示�?
 * 
 * 本示例演示如何通过将客服邮件处理流程分解为离散步骤来使�?Spring AI Alibaba Graph 构建智能工作流�?
 * 
 * 示例包含�?
 * 1. 状态定义（EmailClassification�?
 * 2. 节点实现（读取邮件、分类意图、搜索文档、Bug跟踪、起草回复、人工审核、发送回复）
 * 3. Graph 组装和配�?
 * 4. 测试执行
 */
public class QuickStartExample {

	private static final Logger log = LoggerFactory.getLogger(QuickStartExample.class);

	// ==================== 状态定�?====================

	/**
	 * 邮件分类结构
	 */
	public static class EmailClassification {
		private String intent;      // "question", "bug", "billing", "feature", "complex"
		private String urgency;     // "low", "medium", "high", "critical"
		private String topic;
		private String summary;

		public EmailClassification() {
		}

		public EmailClassification(String intent, String urgency, String topic, String summary) {
			this.intent = intent;
			this.urgency = urgency;
			this.topic = topic;
			this.summary = summary;
		}

		public String getIntent() {
			return intent;
		}

		public void setIntent(String intent) {
			this.intent = intent;
		}

		public String getUrgency() {
			return urgency;
		}

		public void setUrgency(String urgency) {
			this.urgency = urgency;
		}

		public String getTopic() {
			return topic;
		}

		public void setTopic(String topic) {
			this.topic = topic;
		}

		public String getSummary() {
			return summary;
		}

		public void setSummary(String summary) {
			this.summary = summary;
		}

		@Override
		public String toString() {
			return String.format("EmailClassification{intent='%s', urgency='%s', topic='%s', summary='%s'}", 
					intent, urgency, topic, summary);
		}
	}

	/**
	 * 配置状态键策略
	 */
	public static KeyStrategyFactory createKeyStrategyFactory() {
		return () -> {
			HashMap<String, KeyStrategy> strategies = new HashMap<>();
			strategies.put("email_content", new ReplaceStrategy());
			strategies.put("sender_email", new ReplaceStrategy());
			strategies.put("email_id", new ReplaceStrategy());
			strategies.put("classification", new ReplaceStrategy());
			strategies.put("search_results", new ReplaceStrategy());
			strategies.put("customer_history", new ReplaceStrategy());
			strategies.put("draft_response", new ReplaceStrategy());
			strategies.put("messages", new AppendStrategy());
			strategies.put("next_node", new ReplaceStrategy());
			strategies.put("status", new ReplaceStrategy());
			strategies.put("review_data", new ReplaceStrategy());
			return strategies;
		};
	}

	// ==================== 节点实现 ====================

	/**
	 * 读取邮件节点
	 */
	public static class ReadEmailNode implements NodeAction {

		@Override
		public Map<String, Object> apply(OverAllState state) throws Exception {
			// 在生产环境中，这将连接到您的邮件服务
			String emailContent = state.value("email_content")
					.map(v -> (String) v)
					.orElse("");

			log.info("Processing email: {}", emailContent);

			List<String> messages = new ArrayList<>();
			messages.add("Processing email: " + emailContent);

			return Map.of("messages", messages);
		}
	}

	/**
	 * 分类意图节点
	 */
	public static class ClassifyIntentNode implements NodeAction {

		private final ChatClient chatClient;

		public ClassifyIntentNode(ChatClient.Builder chatClientBuilder) {
			this.chatClient = chatClientBuilder.build();
		}

		@Override
		public Map<String, Object> apply(OverAllState state) throws Exception {
			String emailContent = state.value("email_content")
					.map(v -> (String) v)
					.orElseThrow(() -> new IllegalStateException("No email content"));
			String senderEmail = state.value("sender_email")
					.map(v -> (String) v)
					.orElse("unknown");

			// 按需格式化提示，不存储在状态中
			String classificationPrompt = String.format("""
					分析这封客户邮件并进行分类：

					邮件: %s
					发件�? %s

					提供分类，包括意图、紧急程度、主题和摘要�?

					意图应该是以下之一: question, bug, billing, feature, complex
					紧急程度应该是以下之一: low, medium, high, critical

					以JSON格式返回: {"intent": "...", "urgency": "...", "topic": "...", "summary": "..."}
					""", emailContent, senderEmail);

			// 获取结构化响�?
			String response = chatClient.prompt()
					.user(classificationPrompt)
					.call()
					.content();

			// 解析�?EmailClassification 对象
			EmailClassification classification = parseClassification(response);

			// 根据分类确定下一个节�?
			String nextNode;
			if ("billing".equals(classification.getIntent()) ||
					"critical".equals(classification.getUrgency())) {
				nextNode = "human_review";
			} else if (List.of("question", "feature").contains(classification.getIntent())) {
				nextNode = "search_documentation";
			} else if ("bug".equals(classification.getIntent())) {
				nextNode = "bug_tracking";
			} else {
				nextNode = "draft_response";
			}

			// 将分类作为单个对象存储在状态中
			return Map.of(
					"classification", classification,
					"next_node", nextNode
			);
		}

		/**
		 * 简化的JSON解析（实际应用中使用Jackson或Gson�?
		 */
		private EmailClassification parseClassification(String jsonResponse) {
			EmailClassification classification = new EmailClassification();

			// 简单的正则表达式解�?
			Pattern intentPattern = Pattern.compile("\"intent\"\\s*:\\s*\"([^\"]+)\"");
			Pattern urgencyPattern = Pattern.compile("\"urgency\"\\s*:\\s*\"([^\"]+)\"");
			Pattern topicPattern = Pattern.compile("\"topic\"\\s*:\\s*\"([^\"]+)\"");
			Pattern summaryPattern = Pattern.compile("\"summary\"\\s*:\\s*\"([^\"]+)\"");

			Matcher matcher = intentPattern.matcher(jsonResponse);
			if (matcher.find()) {
				classification.setIntent(matcher.group(1));
			}

			matcher = urgencyPattern.matcher(jsonResponse);
			if (matcher.find()) {
				classification.setUrgency(matcher.group(1));
			}

			matcher = topicPattern.matcher(jsonResponse);
			if (matcher.find()) {
				classification.setTopic(matcher.group(1));
			}

			matcher = summaryPattern.matcher(jsonResponse);
			if (matcher.find()) {
				classification.setSummary(matcher.group(1));
			}

			// 如果解析失败，设置默认�?
			if (classification.getIntent() == null) {
				classification.setIntent("question");
			}
			if (classification.getUrgency() == null) {
				classification.setUrgency("medium");
			}
			if (classification.getTopic() == null) {
				classification.setTopic("general");
			}
			if (classification.getSummary() == null) {
				classification.setSummary("需要处理的客户邮件");
			}

			return classification;
		}
	}

	/**
	 * 文档搜索节点
	 */
	public static class SearchDocumentationNode implements NodeAction {

		@Override
		public Map<String, Object> apply(OverAllState state) throws Exception {
			// 从分类构建搜索查�?
			EmailClassification classification = state.value("classification")
					.map(v -> (EmailClassification) v)
					.orElse(new EmailClassification());
			String query = classification.getIntent() + " " + classification.getTopic();

			try {
				// 实现您的搜索逻辑
				// 存储原始搜索结果，而不是格式化的文�?
				List<String> searchResults = List.of(
						"通过设置 > 安全 > 更改密码重置密码",
						"密码必须至少12个字�?,
						"包含大写字母、小写字母、数字和符号"
				);

				log.info("Searching documentation for: {}", query);

				return Map.of(
						"search_results", searchResults,
						"next_node", "draft_response"
				);
			} catch (Exception e) {
				// 对于可恢复的搜索错误，存储错误并继续
				log.warn("Search error: {}", e.getMessage());
				List<String> errorResult = List.of("搜索暂时不可�? " + e.getMessage());
				return Map.of(
						"search_results", errorResult,
						"next_node", "draft_response"
				);
			}
		}
	}

	/**
	 * Bug跟踪节点
	 */
	public static class BugTrackingNode implements NodeAction {

		@Override
		public Map<String, Object> apply(OverAllState state) throws Exception {
			// 在您的bug跟踪系统中创建票�?
			String ticketId = "BUG-12345";  // 将通过API创建

			log.info("Created bug ticket: {}", ticketId);

			return Map.of(
					"search_results", List.of("已创建Bug票据 " + ticketId),
					"current_step", "bug_tracked",
					"next_node", "draft_response"
			);
		}
	}

	/**
	 * 起草回复节点
	 */
	public static class DraftResponseNode implements NodeAction {

		private final ChatClient chatClient;

		public DraftResponseNode(ChatClient.Builder chatClientBuilder) {
			this.chatClient = chatClientBuilder.build();
		}

		@Override
		public Map<String, Object> apply(OverAllState state) throws Exception {
			EmailClassification classification = state.value("classification")
					.map(v -> (EmailClassification) v)
					.orElse(new EmailClassification());
			String emailContent = state.value("email_content")
					.map(v -> (String) v)
					.orElse("");

			// 从原始状态数据按需格式化上下文
			List<String> contextSections = new ArrayList<>();

			Optional<List<String>> searchResults = state.value("search_results")
					.map(v -> (List<String>) v);
			if (searchResults.isPresent()) {
				// 为提示格式化搜索结果
				List<String> docs = searchResults.get();
				String formattedDocs = docs.stream()
						.map(doc -> "- " + doc)
						.collect(Collectors.joining("\n"));
				contextSections.add("相关文档:\n" + formattedDocs);
			}

			Optional<Map<String, Object>> customerHistory = state.value("customer_history")
					.map(v -> (Map<String, Object>) v);
			if (customerHistory.isPresent()) {
				// 为提示格式化客户数据
				Map<String, Object> history = customerHistory.get();
				contextSections.add("客户等级: " + history.getOrDefault("tier", "standard"));
			}

			// 使用格式化的上下文构建提�?
			String draftPrompt = String.format("""
					为这封客户邮件起草回�?
					%s

					邮件意图: %s
					紧急程�? %s

					%s

					指南:
					- 专业且有帮助
					- 解决他们的具体问�?
					- 在相关时使用提供的文�?
					""",
					emailContent,
					classification.getIntent(),
					classification.getUrgency(),
					String.join("\n", contextSections)
			);

			String response = chatClient.prompt()
					.user(draftPrompt)
					.call()
					.content();

			// 根据紧急程度和意图确定是否需要人工审�?
			boolean needsReview =
					List.of("high", "critical").contains(classification.getUrgency()) ||
							"complex".equals(classification.getIntent());

			// 路由到适当的下一个节�?
			String nextNode = needsReview ? "human_review" : "send_reply";

			return Map.of(
					"draft_response", response,  // 仅存储原始响�?
					"next_node", nextNode
			);
		}
	}

	/**
	 * 人工审核节点
	 * 
	 * 注意：在 interruptBefore 模式下，中断是在编译配置中设置的（见 createEmailAgentGraph 方法）�?
	 * 节点本身不需要做任何特殊处理，只需要正常返回状态即可�?
	 * 当执行到此节点前时，Graph 会自动中断，等待人工输入�?
	 */
	public static class HumanReviewNode implements NodeAction {

		@Override
		public Map<String, Object> apply(OverAllState state) throws Exception {
			EmailClassification classification = state.value("classification")
					.map(v -> (EmailClassification) v)
					.orElse(new EmailClassification());

			// 准备审核数据
			Map<String, Object> reviewData = Map.of(
					"email_id", state.value("email_id").map(v -> (String) v).orElse(""),
					"original_email", state.value("email_content").map(v -> (String) v).orElse(""),
					"draft_response", state.value("draft_response").map(v -> (String) v).orElse(""),
					"urgency", classification.getUrgency(),
					"intent", classification.getIntent(),
					"action", "请审核并批准/编辑此响�?
			);

			log.info("Waiting for human review: {}", reviewData);

			// 返回审核数据和下一个节�?
			// 注意：在 interruptBefore 模式下，此节点在人工输入后才会执�?
			return Map.of(
					"review_data", reviewData,
					"status", "waiting_for_review",
					"next_node", "send_reply"
			);
		}
	}

	/**
	 * 发送回复节�?
	 */
	public static class SendReplyNode implements NodeAction {

		@Override
		public Map<String, Object> apply(OverAllState state) throws Exception {
			String draftResponse = state.value("draft_response")
					.map(v -> (String) v)
					.orElse("");

			// 与邮件服务集�?
			log.info("Sending reply: {}...", 
					draftResponse.length() > 100 
							? draftResponse.substring(0, 100) 
							: draftResponse);

			return Map.of("status", "sent");
		}
	}

	// ==================== Graph 组装 ====================

	/**
	 * 创建邮件处理 Graph
	 */
	public static CompiledGraph createEmailAgentGraph(ChatModel chatModel) throws GraphStateException {
		// 配置 ChatClient
		ChatClient.Builder chatClientBuilder = ChatClient.builder(chatModel);

		// 创建节点
		var readEmail = node_async(new ReadEmailNode());
		var classifyIntent = node_async(new ClassifyIntentNode(chatClientBuilder));
		var searchDocumentation = node_async(new SearchDocumentationNode());
		var bugTracking = node_async(new BugTrackingNode());
		var draftResponse = node_async(new DraftResponseNode(chatClientBuilder));
		var humanReview = node_async(new HumanReviewNode());
		var sendReply = node_async(new SendReplyNode());

		// 创建�?
		StateGraph workflow = new StateGraph(createKeyStrategyFactory())
				.addNode("read_email", readEmail)
				.addNode("classify_intent", classifyIntent)
				.addNode("search_documentation", searchDocumentation)
				.addNode("bug_tracking", bugTracking)
				.addNode("draft_response", draftResponse)
				.addNode("human_review", humanReview)
				.addNode("send_reply", sendReply);

		// 添加基本�?
		workflow.addEdge(START, "read_email");
		workflow.addEdge("read_email", "classify_intent");
		workflow.addEdge("send_reply", END);

		// 添加条件边（基于节点返回�?next_node�?
		workflow.addConditionalEdges("classify_intent",
				edge_async(state -> {
					return (String) state.value("next_node").orElse("draft_response");
				}),
				Map.of(
						"search_documentation", "search_documentation",
						"bug_tracking", "bug_tracking",
						"human_review", "human_review",
						"draft_response", "draft_response"
				));

		workflow.addConditionalEdges("draft_response",
				edge_async(state -> {
					return (String) state.value("next_node").orElse("send_reply");
				}),
				Map.of(
						"human_review", "human_review",
						"send_reply", "send_reply"
				));

		workflow.addConditionalEdges("human_review",
				edge_async(state -> {
					return (String) state.value("next_node").orElse("send_reply");
				}),
				Map.of(
						"send_reply", "send_reply"
				));

		workflow.addEdge("search_documentation", "draft_response");
		workflow.addEdge("bug_tracking", "draft_response");

		// 配置持久�?
		var memory = new MemorySaver();
		var compileConfig = CompileConfig.builder()
				.saverConfig(SaverConfig.builder()
						.register(memory)
						.build())
				.interruptBefore("human_review")  // 在人工审核前中断
				.build();

		return workflow.compile(compileConfig);
	}

	// ==================== 测试方法 ====================

	/**
	 * 测试紧急账单问�?
	 */
	public static void testBillingIssue(CompiledGraph app) throws Exception {
		log.info("=== 测试紧急账单问�?===");

		// 测试紧急账单问�?
		Map<String, Object> initialState = Map.of(
				"email_content", "我的订阅被收费两次了！这很紧急！",
				"sender_email", "customer@example.com",
				"email_id", "email_123",
				"messages", new ArrayList<String>()
		);

		// 使用 thread_id 运行以实现持久化
		var config = RunnableConfig.builder()
				.threadId("customer_123")
				.build();

		// 使用 stream 执行，直到中断点（human_review�?
		// 图将�?human_review 处暂停（因为配置�?interruptBefore�?
		Flux<NodeOutput> stream = app.stream(initialState, config);
		stream
				.doOnNext(output -> log.info("节点输出: {}", output))
				.doOnError(error -> log.error("执行错误: {}", error.getMessage()))
				.doOnComplete(() -> log.info("流完�?))
				.blockLast();

		// 获取当前状态，检查是否有草稿回复
		var currentState = app.getState(config);
		Map<String, Object> stateData = currentState.state().data();
		String draftResponse = (String) stateData.get("draft_response");
		if (draftResponse != null) {
			log.info("Draft ready for review: {}...", 
					draftResponse.length() > 100 
							? draftResponse.substring(0, 100) 
							: draftResponse);
		}

		// 准备好后，提供人工输入以恢复
		// 使用 updateState 更新状态（interruptBefore 模式下，传入 null 作为节点 ID�?
		var updatedConfig = app.updateState(config, Map.of(
				"approved", true,
				"edited_response", "我们对重复收费深表歉意。我已经立即启动了退�?.."
		), null);

		// 继续执行（input �?null，使用之前的状态）
		app.stream(null, updatedConfig)
				.doOnNext(output -> log.info("节点输出: {}", output))
				.doOnError(error -> log.error("执行错误: {}", error.getMessage()))
				.doOnComplete(() -> log.info("流完�?))
				.blockLast();

		// 获取最终状�?
		var finalState = app.getState(updatedConfig);
		String status = (String) finalState.state().data().get("status");
		log.info("Email sent successfully! Status: {}", status);
	}

	/**
	 * 测试简单问�?
	 */
	public static void testSimpleQuestion(CompiledGraph app) {
		log.info("=== 测试简单问�?===");

		Map<String, Object> initialState = Map.of(
				"email_content", "如何重置我的密码�?,
				"sender_email", "user@example.com",
				"email_id", "email_456",
				"messages", new ArrayList<String>()
		);

		var config = RunnableConfig.builder()
				.threadId("user_456")
				.build();

		// invoke 返回 Optional<OverAllState>，需要使�?orElseThrow() 获取结果
		var result = app.invoke(initialState, config).orElseThrow();
		log.info("Simple question processed. Status: {}", result.data().get("status"));
	}

	/**
	 * 主方�?
	 */
	public static void main(String[] args) throws Exception {
		log.info("========================================");
		log.info("Graph 工作流编排快速入门示�?);
		log.info("========================================\n");

		// 注意：实际使用时需要提�?ChatModel 实例
		// 创建 DashScope API 实例
		DashScopeApi dashScopeApi = DashScopeApi.builder()
				.apiKey(System.getenv("AI_DASHSCOPE_API_KEY"))
				.build();

		// 创建 ChatModel
		ChatModel chatModel = DashScopeChatModel.builder()
				.dashScopeApi(dashScopeApi)
				.build();

		CompiledGraph app = createEmailAgentGraph(chatModel);

		testBillingIssue(app);
//		testSimpleQuestion(app);
	}
}

