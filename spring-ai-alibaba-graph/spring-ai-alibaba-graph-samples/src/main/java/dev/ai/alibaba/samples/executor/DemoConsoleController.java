package dev.ai.alibaba.samples.executor;

import com.alibaba.cloud.ai.graph.GraphRepresentation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Controller;

import java.util.Map;

@Slf4j
@Controller
public class DemoConsoleController implements CommandLineRunner {

	private final AgentExecutor agentExecutor;

	public DemoConsoleController(AgentExecutor agentExecutor) {
		this.agentExecutor = agentExecutor;
	}

	@Override
	public void run(String... args) throws Exception {

		log.info("Welcome to the Spring Boot CLI application!");

		var graph = agentExecutor.graphBuilder().build();

		var plantUml = graph.getGraph(GraphRepresentation.Type.PLANTUML, "Agent Executor");

		System.out.println("----begin-----");
		System.out.println(plantUml.getContent());
		System.out.println("----end-----");

		var workflow = graph.compile();

		var result = workflow.invoke(Map.of(AgentExecutor.State.INPUT, "what is the weather in Napoli ?"));

		var finish = result.flatMap(AgentExecutor.State::agentOutcome).map(AgentExecutor.Outcome::finish).orElseThrow();

		log.info("result: {}", finish.returnValues());
	}

}
