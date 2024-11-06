package org.bsc.langgraph4j.jetty;

import com.alibaba.cloud.ai.graph.GraphRepresentation;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.ai.alibaba.samples.executor.AgentExecutor;
import dev.ai.alibaba.samples.executor.AgentService;
import dev.ai.alibaba.samples.executor.ToolService;
import org.bsc.langgraph4j.studio.jetty.LangGraphStreamingServerJetty;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Import;

@Import({AgentService.class, ToolService.class})
@SpringBootApplication
public class AgentExecutorStreamingServer {

    public static void main(String[] args) throws Exception {
        System.setProperty("server.port","8090");
        ConfigurableApplicationContext context = SpringApplication.run(AgentExecutorStreamingServer.class, args);
        AgentService agentService = context.getBean(AgentService.class);
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);

        var graph = new AgentExecutor(agentService).graphBuilder()
                .stateSerializer(AgentExecutor.Serializers.JSON.object() )
                .build();

        GraphRepresentation plantUml = graph.getGraph(GraphRepresentation.Type.PLANTUML, "Adaptive RAG");

        System.out.println(plantUml.getContent());

        var server = LangGraphStreamingServerJetty.builder()
                .port(8080)
                .objectMapper(objectMapper)
                .title("AGENT EXECUTOR")
                .addInputStringArg("input")
                .stateGraph(graph)
                .build();

        server.start().join();

    }

}
