package com.alibaba.cloud.ai.graph.practice.intelligent_outbound_call;

import com.alibaba.cloud.ai.graph.GraphRepresentation;
import com.alibaba.cloud.ai.graph.serializer.agent.JSONStateSerializer;
import com.alibaba.cloud.ai.graph.studio.StreamingServerJetty;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.ai.alibaba.samples.executor.ToolService;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Import;

@Import({ IocAgentService.class, ToolService.class })
@SpringBootApplication
public class IntelligentOutboundCall {

	public static void main(String[] args) throws Exception {
		// 因为studio的jetty占用了8080端口，修改springboot端口避免冲突
		System.setProperty("server.port", "8090");
		ConfigurableApplicationContext context = SpringApplication.run(IntelligentOutboundCall.class, args);
		// AgentService注入了spring-ai的ChatClient，引入dashscope包后，默认注入百炼大模型
		IocAgentService agentService = context.getBean(IocAgentService.class);
		// 没有使用springmvc，所以手动用jackson进行参数映射
		ObjectMapper objectMapper = new ObjectMapper();
		objectMapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);

		// 核心步骤：构建graph对象
		// 1、注入llm
		// 2、注入序列化对象
		// 3、组装工作流流程
		// IsExecutor
		var graph = new IocExecutor(agentService).graphBuilder().stateSerializer(JSONStateSerializer.INSTANCE).build();

		// 打印工作流内容，非json格式
		GraphRepresentation plantUml = graph.getGraph(GraphRepresentation.Type.PLANTUML, "Adaptive RAG");
		System.out.println(plantUml.getContent());

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
