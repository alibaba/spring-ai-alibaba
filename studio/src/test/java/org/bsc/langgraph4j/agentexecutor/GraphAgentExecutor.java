package org.bsc.langgraph4j.agentexecutor;

import com.alibaba.cloud.ai.graph.GraphStateException;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.serializer.StateSerializer;
import com.alibaba.cloud.ai.graph.state.AgentState;
import com.alibaba.cloud.ai.graph.state.AppenderChannel;
import com.alibaba.cloud.ai.graph.state.Channel;
import com.alibaba.cloud.ai.tool.ToolNode;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.output.FinishReason;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.agentexecutor.serializer.json.JSONStateSerializer;
import org.bsc.langgraph4j.agentexecutor.serializer.std.STDStateSerializer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static com.alibaba.cloud.ai.graph.StateGraph.END;
import static com.alibaba.cloud.ai.graph.StateGraph.START;
import static com.alibaba.cloud.ai.graph.action.AsyncEdgeAction.edge_async;
import static com.alibaba.cloud.ai.graph.action.AsyncNodeAction.node_async;
import static java.util.Optional.ofNullable;

@Slf4j
public class GraphAgentExecutor {
    public enum Serializers {

        STD( new STDStateSerializer() ),
        JSON( new JSONStateSerializer() );

        private final StateSerializer<State> serializer;

        Serializers(StateSerializer<State> serializer) {
            this.serializer = serializer;
        }

        public StateSerializer<State> object() {
            return serializer;
        }

    }
    public class GraphBuilder {
        private ChatLanguageModel chatLanguageModel;
        private List<Object> objectsWithTools;
        private StateSerializer<State> stateSerializer;

        public GraphBuilder chatLanguageModel(ChatLanguageModel chatLanguageModel) {
            this.chatLanguageModel = chatLanguageModel;
            return this;
        }
        public GraphBuilder objectsWithTools(List<Object> objectsWithTools) {
            this.objectsWithTools = objectsWithTools;
            return this;
        }

        public GraphBuilder stateSerializer( StateSerializer<State> stateSerializer) {
            this.stateSerializer = stateSerializer;
            return this;
        }

        public StateGraph<State> build() throws GraphStateException {
            Objects.requireNonNull(objectsWithTools, "objectsWithTools is required!");
            Objects.requireNonNull(chatLanguageModel, "chatLanguageModel is required!");

            var toolNode = ToolNode.of( objectsWithTools );

            final List<ToolSpecification> toolSpecifications = toolNode.toolSpecifications();

            var agentRunnable = Agent.builder()
                    .chatLanguageModel(chatLanguageModel)
                    .tools( toolSpecifications )
                    .build();

            if( stateSerializer == null ) {
                stateSerializer = Serializers.STD.object();
            }

            return new StateGraph<>(State.SCHEMA, stateSerializer)
                    .addEdge(START,"agent")
                    .addNode( "agent", node_async( state ->
                            callAgent(agentRunnable, state))
                    )
                    .addNode( "action", node_async( state ->
                            executeTools(toolNode, state))
                    )
                    .addConditionalEdges(
                            "agent",
                            edge_async(GraphAgentExecutor.this::shouldContinue),
                            Map.of("continue", "action", "end", END)
                    )
                    .addEdge("action", "agent")
                    ;

        }
    }

    public final GraphBuilder graphBuilder() {
        return new GraphBuilder();
    }

    public static class State extends AgentState {
        static Map<String, Channel<?>> SCHEMA = Map.of(
            "intermediate_steps", AppenderChannel.<IntermediateStep>of(ArrayList::new)
        );

        public State(Map<String, Object> initData) {
            super(initData);
        }

        Optional<String> input() {
            return value("input");
        }
        Optional<AgentOutcome> agentOutcome() {
            return value("agent_outcome");
        }
        List<IntermediateStep> intermediateSteps() {
            return this.<List<IntermediateStep>>value("intermediate_steps").orElseGet(ArrayList::new);
        }

    }

    Map<String,Object> callAgent(Agent agentRunnable, State state )  {
        log.trace( "callAgent" );
        var input = state.input()
                        .orElseThrow(() -> new IllegalArgumentException("no input provided!"));

        var intermediateSteps = state.intermediateSteps();

        var response = agentRunnable.execute( input, intermediateSteps );

        if( response.finishReason() == FinishReason.TOOL_EXECUTION ) {

            var toolExecutionRequests = response.content().toolExecutionRequests();
            var action = new AgentAction( toolExecutionRequests.get(0), "");

            return Map.of("agent_outcome", new AgentOutcome( action, null ) );

        }
        else {
            var result = response.content().text();
            var finish = new AgentFinish( Map.of("returnValues", result), result );

            return Map.of("agent_outcome", new AgentOutcome( null, finish ) );
        }

    }

    Map<String,Object> executeTools( ToolNode toolNode, State state )  {
        log.trace( "executeTools" );

        var agentOutcome = state.agentOutcome().orElseThrow(() -> new IllegalArgumentException("no agentOutcome provided!"));

        var toolExecutionRequest = ofNullable(agentOutcome.action())
                                        .map(AgentAction::toolExecutionRequest)
                                        .orElseThrow(() -> new IllegalStateException("no action provided!" ))
                                        ;
        var result = toolNode.execute( toolExecutionRequest )
                .map( ToolExecutionResultMessage::text )
                .orElseThrow(() -> new IllegalStateException("no tool found for: " + toolExecutionRequest.name()));

        return Map.of("intermediate_steps", new IntermediateStep( agentOutcome.action(), result ) );

    }

    String shouldContinue(State state) {

        return state.agentOutcome()
                .map(AgentOutcome::finish)
                .map( finish -> "end" )
                .orElse("continue");
    }

}
