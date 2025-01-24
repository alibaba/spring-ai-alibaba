//package com.alibaba.cloud.ai.graph.practice.insurance_sale;
//
//import com.alibaba.cloud.ai.graph.CompileConfig;
//import com.alibaba.cloud.ai.graph.GraphStateException;
//import com.alibaba.cloud.ai.graph.OverAllState;
//import com.alibaba.cloud.ai.graph.StateGraph;
//import com.alibaba.cloud.ai.graph.action.EdgeAction;
//import com.alibaba.cloud.ai.graph.checkpoint.config.SaverConfig;
//import com.alibaba.cloud.ai.graph.checkpoint.constant.SaverConstant;
//import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;
//import com.alibaba.cloud.ai.graph.practice.insurance_sale.node.HumanNode;
//import com.alibaba.cloud.ai.graph.practice.insurance_sale.node.PromptNode;
//import com.alibaba.cloud.ai.graph.serializer.StateSerializer;
//import com.alibaba.cloud.ai.graph.serializer.agent.AgentAction;
//import com.alibaba.cloud.ai.graph.serializer.agent.AgentFinish;
//import com.alibaba.cloud.ai.graph.serializer.agent.AgentOutcome;
//import com.alibaba.cloud.ai.graph.serializer.agent.JSONStateSerializer;
//import com.alibaba.cloud.ai.graph.state.NodeState;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.stereotype.Service;
//import org.springframework.util.StringUtils;
//
//import java.util.Map;
//import java.util.Random;
//
//import static com.alibaba.cloud.ai.graph.StateGraph.END;
//import static com.alibaba.cloud.ai.graph.StateGraph.START;
//import static com.alibaba.cloud.ai.graph.action.AsyncEdgeAction.edge_async;
//import static com.alibaba.cloud.ai.graph.action.AsyncNodeAction.node_async;
//
//@Slf4j
//@Service
//public class IsExecutor {
//
//	public class GraphBuilder {
//
//		private StateSerializer stateSerializer;
//
//		public GraphBuilder stateSerializer(StateSerializer stateSerializer) {
//			this.stateSerializer = stateSerializer;
//			return this;
//		}
//
//		public StateGraph build() throws GraphStateException {
//			if (stateSerializer == null) {
//				stateSerializer = new JSONStateSerializer();
//			}
//			MemorySaver saver = new MemorySaver();
//			SaverConfig saverConfig = SaverConfig.builder()
//				.type(SaverConstant.MEMORY)
//				.register(SaverConstant.MEMORY, saver)
//				.build();
//			CompileConfig compileConfig = CompileConfig.builder().saverConfig(saverConfig).build();
//			var subGraph = new StateGraph(stateSerializer).addEdge(START, "select_prompt")
//				.addNode("select_prompt", node_async(new PromptNode("请选择：1-人寿保险：100元/年，2-健康险：200元/年")))
//				.addEdge("select_prompt", "user_select")
//				.addNode("user_select", node_async(new HumanNode()))
//				.addConditionalEdges( // 条件边，在agent节点之后
//						"user_select", edge_async(new EdgeAction() {
//							@Override
//							public String apply(OverAllState t) throws Exception {
//								return null;
//							}
//						}),
//						Map.of("continue", "do_purchase", "error", "error_prompt"))
//				.addNode("do_purchase", node_async(new PromptNode("请使用付款链接#{url}付款，成功后确认", input -> {
//					var url = "www.bill.com?money=" + Integer.parseInt(input) * 100;
//					return Map.of("url", url);
//				})))
//				.addNode("error_prompt", node_async(new PromptNode("输入错误，请重新选择")))
//				.addEdge("error_prompt", "select_prompt")
//
//				.addEdge("do_purchase", "user_confirm")
//				.addNode("user_confirm", node_async(new HumanNode()))
//
//				.addConditionalEdges( // 条件边，在agent节点之后
//						"user_confirm", edge_async(new EdgeAction() {
//							@Override
//							public String apply(OverAllState t) throws Exception {
//								return null;
//							}
//						}),
//						Map.of("continue", "pay_success", "error", END, "prompt", "error_prompt1"))
//				.addNode("error_prompt1", node_async(new PromptNode("参数错误，请选择确认或者取消")))
//				.addEdge("error_prompt1", "user_confirm")
//
//				.addNode("pay_success", node_async(new PromptNode("付款成功")))
//				.addEdge("pay_success", END)
//
//				.compile(compileConfig);
//
//			return null /*new StateGraph(stateSerializer).addEdge(START, "welcome")
//				.addNode("welcome",
//						node_async(new PromptNode(
//								"您好！我是您的保险助手popo。无论您是在寻找保障、规划未来，还是需要专业的保险建议，我都在这里为您提供帮助。请告诉我您的保险需求，让我们开始吧！"))) // 调用llm
//				.addEdge("welcome", "input_customer_wish")// 下一个节点
//				.addNode("input_customer_wish", node_async(new HumanNode()))
//
//				.addConditionalEdges( // 条件边，在agent节点之后
//						"input_customer_wish", edge_async(IsExecutor.this::questionEnough),
//						Map.of("input_enough", "llm_answer", "input_not_enough", "prompt"))
//				.addNode("prompt", node_async(new PromptNode("请填写年龄、性别、学历等完整信息")))
//				.addEdge("prompt", "llm_answer")// 下一个节点
//				// .addEdge("human", "agent")// 下一个节点
//				.addNode("llm_answer", node_async(IsExecutor.this::callAgent)) // 调用llm
//				.addNode("input_customer_purchase_intention", node_async(new HumanNode()))
//				.addEdge("llm_answer", "input_customer_purchase_intention")// 下一个节点
//
//				.addConditionalEdges( // 条件边，在agent节点之后
//						"input_customer_purchase_intention", edge_async(IsExecutor.this::purchaseIntention),
//						Map.of("want_purchase", "purchaseGraph", "not_want_purchase", "want_feedback"))
//
//				.addNode("want_feedback", node_async(new PromptNode("感谢您的咨询，欢迎向我们反馈任何建议")))
//				.addEdge("want_feedback", END)
//
//				.addSubgraph("purchaseGraph", subGraph)
//				.addEdge("purchaseGraph", END)*/
//
//			// .addNode("select_prompt", node_async(new
//			// PromptNode("请选择：1-人寿保险：100元/年，2-健康险：200元/年")))
//			// .addEdge("select_prompt", "user_select")
//			// .addNode("user_select", node_async(new HumanNode()))
//			// .addConditionalEdges( // 条件边，在agent节点之后
//			// "user_select", edge_async(IsExecutor.this::generateBills),
//			// Map.of("continue", "do_purchase", "error", "error_prompt"))
//			// .addNode("do_purchase", node_async(new PromptNode("请使用付款链接#{url}付款，成功后确认",
//			// input -> {
//			// var url = "www.bill.com?money=" + Integer.parseInt(input) * 100;
//			// return Map.of("url", url);
//			// })))
//			// .addNode("error_prompt", node_async(new PromptNode("输入错误，请重新选择")))
//			// .addEdge("error_prompt", "select_prompt")
//			//
//			// .addEdge("do_purchase", "user_confirm")
//			// .addNode("user_confirm", node_async(new HumanNode()))
//			//
//			// .addConditionalEdges( // 条件边，在agent节点之后
//			// "user_confirm", edge_async(IsExecutor.this::payFinish),
//			// Map.of("continue", "pay_success", "error", END, "prompt", "error_prompt1"))
//			// .addNode("error_prompt1", node_async(new PromptNode("参数错误，请选择确认或者取消")))
//			// .addEdge("error_prompt1", "user_confirm")
//			//
//			// .addNode("pay_success", node_async(new PromptNode("付款成功")))
//			// .addEdge("pay_success", END)
//
//			;
//
//			// 提供报价、用户选择、提供付费单据、用户付费、提示购买成功
//
//		}
//
//	}
//
//	public final GraphBuilder graphBuilder() {
//		return new GraphBuilder();
//	}
//
//	private final IsAgentService agentService;
//
//	public IsExecutor(IsAgentService agentService) {
//		this.agentService = agentService;
//	}
//
//	Map<String, Object> callAgent(NodeState state) {
//		log.info("callAgent");
//
//		var input = state.input()
//			.filter(StringUtils::hasText)
//			.orElseThrow(() -> new IllegalArgumentException("no input provided!"));
//
//		var response = agentService.execute(input);
//
//		var output = response.getResult().getOutput();
//
//		if (output.hasToolCalls()) {
//			var action = new AgentAction(output.getToolCalls().get(0), "");
//			return Map.of(NodeState.OUTPUT, new AgentOutcome(action, null));
//
//		}
//		else {
//			var finish = new AgentFinish(Map.of("returnValues", output.getContent()), output.getContent());
//
//			return Map.of(NodeState.OUTPUT, new AgentOutcome(null, finish));
//		}
//	}
//
//	String questionEnough(NodeState state) {
//
//		Map<String, Object> returnValues = state.data();
//		if (!returnValues.containsKey("input")) {
//			return "return";
//		}
//		String input = (String) returnValues.get("input");
//		// 判断用户输入内容是否包括全部所需要信息（年龄、性别、学历……）
//		if (input.contains("年龄") && input.contains("性别") && input.contains("学历")) {
//			return "input_enough";
//		}
//		else {
//			return "input_not_enough";
//		}
//	}
//
//	String purchaseIntention(NodeState state) {
//
//		var input = state.input()
//			.filter(StringUtils::hasText)
//			.orElseThrow(() -> new IllegalArgumentException("no input provided!"));
//
//		var response = agentService.executeByPrompt("判断用户是否有购买保险意愿，只返回是或者否。比如用户输入我要买，返回是。" + input,
//				"判断用户是否有购买保险意愿，只返回是或者否。比如用户输入我要买，返回是");
//
//		var output = response.getResult().getOutput();
//		log.info("agent:{}", output.getContent());
//		// 判断用户输入内容是否包括全部所需要信息（年龄、性别、学历……）
//		if (output.getContent().equals("是")) {
//			return "want_purchase";
//		}
//		else {
//			return "not_want_purchase";
//		}
//	}
//
//	String generateBills(NodeState state) {
//
//		var input = state.input()
//			.filter(StringUtils::hasText)
//			.orElseThrow(() -> new IllegalArgumentException("no input provided!"));
//
//		if ("1".equals(input) || "2".equals(input)) {
//			return "continue";
//		}
//		else {
//			return "error";
//		}
//	}
//
//	String payFinish(NodeState state) {
//
//		var input = state.input()
//			.filter(StringUtils::hasText)
//			.orElseThrow(() -> new IllegalArgumentException("no input provided!"));
//
//		// 1 确认 0 取消
//		if ("1".equals(input)) {
//			// 查询是否付款，假设已付款
//			if (new Random().nextInt(100) < 50) {
//				return "continue";
//			}
//			else {
//				return "error";
//			}
//		}
//		else if ("0".equals(input)) {
//			return "error";
//		}
//		else {
//			return "prompt";
//		}
//	}
//
//}
