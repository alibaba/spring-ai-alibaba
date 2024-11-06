package dev.ai.alibaba.samples.graph;

import com.alibaba.cloud.ai.graph.GraphRepresentation;
import com.alibaba.cloud.ai.graph.StateGraph;
import dev.ai.alibaba.samples.adaptiverag.AdaptiveRag;
import org.junit.jupiter.api.Test;

public class AdaptiveRagTest {

    @Test
    public void getGraphTest() throws Exception {

        String OPENAI_API_KEY = System.getProperty("OPENAI_API_KEY");
        String TAVILY_API_KEY = System.getProperty("TAVILY_API_KEY");


        AdaptiveRag adaptiveRag = new AdaptiveRag(OPENAI_API_KEY, TAVILY_API_KEY);

        StateGraph<AdaptiveRag.State> graph = adaptiveRag.buildGraph();

        GraphRepresentation plantUml = graph.getGraph(GraphRepresentation.Type.PLANTUML, "Adaptive RAG");

        System.out.println(plantUml.getContent());

        GraphRepresentation mermaid = graph.getGraph(GraphRepresentation.Type.MERMAID, "Adaptive RAG");

        System.out.println(mermaid.getContent());
    }
}
