package com.alibaba.cloud.ai.graph.practice.insurance_sale;

import com.alibaba.cloud.ai.graph.GraphRepresentation;
import com.alibaba.cloud.ai.graph.studio.StreamingServerJetty;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.ai.alibaba.samples.executor.ToolService;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Import;

@Import({ IsAgentService.class, ToolService.class })
@SpringBootApplication
public class AiInsurance {

	public static void main(String[] args) throws Exception {
		// 因为studio的jetty占用了8080端口，修改springboot端口避免冲突
		System.setProperty("server.port", "8090");
		ConfigurableApplicationContext context = SpringApplication.run(AiInsurance.class, args);
		// AgentService注入了spring-ai的ChatClient，引入dashscope包后，默认注入百炼大模型
		IsAgentService agentService = context.getBean(IsAgentService.class);
		// 没有使用springmvc，所以手动用jackson进行参数映射
		ObjectMapper objectMapper = new ObjectMapper();
		objectMapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);

		// 核心步骤：构建graph对象
		// 1、注入llm
		// 2、注入序列化对象
		// 3、组装工作流流程
		// IsExecutor
		var graph = new IsExecutor(agentService).graphBuilder()
			.stateSerializer(IsExecutor.Serializers.JSON.object())
			.build();

		/*
		 * build(){ return new StateGraph<>(State.SCHEMA, stateSerializer)
		 * .addEdge(START,"agent") .addNode( "agent",
		 * node_async(IsExecutor.this::callAgent) ) .addNode( "action",
		 * IsExecutor.this::executeTools ) .addConditionalEdges( "agent",
		 * edge_async(IsExecutor.this::shouldContinue), Map.of("continue", "action",
		 * "end", END) ) .addEdge("action", "agent") ; }
		 */

		// 打印工作流内容，非json格式
		GraphRepresentation plantUml = graph.getGraph(GraphRepresentation.Type.PLANTUML, "Adaptive RAG");
		System.out.println(plantUml.getContent());
		/*
		 * @startuml unnamed.puml skinparam usecaseFontSize 14 skinparam
		 * usecaseStereotypeFontSize 12 skinparam hexagonFontSize 14 skinparam
		 * hexagonStereotypeFontSize 12 title "Adaptive RAG" footer
		 *
		 * powered by SpringAiGraph end footer circle start<<input>> circle stop as __END__
		 * usecase "agent"<<Node>> usecase "action"<<Node>> hexagon "check state" as
		 * condition1<<Condition>> start -down-> "agent" "agent" -down-> "condition1"
		 * "condition1" --> "action": "continue" '"agent" --> "action": "continue"
		 * "condition1" -down-> stop: "end" '"agent" -down-> stop: "end" "action" -down->
		 * "agent"
		 *
		 * @enduml
		 */

		var server = StreamingServerJetty.builder()
			.port(8080)
			.objectMapper(objectMapper)
			.title("AGENT EXECUTOR")
			.addInputStringArg("input")
			.stateGraph(graph)
			.build();

		// 启动jetty，访问studio: http://127.0.0.1:8080
		server.start().join();
	}

}
