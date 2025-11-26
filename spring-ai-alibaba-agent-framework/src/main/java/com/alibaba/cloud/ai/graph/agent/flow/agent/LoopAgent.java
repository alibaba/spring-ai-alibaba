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

package com.alibaba.cloud.ai.graph.agent.flow.agent;

import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.agent.Agent;
import com.alibaba.cloud.ai.graph.agent.flow.agent.loop.LoopStrategy;
import com.alibaba.cloud.ai.graph.agent.flow.builder.FlowAgentBuilder;
import com.alibaba.cloud.ai.graph.agent.flow.builder.FlowGraphBuilder;
import com.alibaba.cloud.ai.graph.agent.flow.enums.FlowAgentEnum;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;

import java.util.List;

/**
 * Loop Agent that supports multiple loop modes:
 * <ul>
 * <li><b>COUNT</b>: Execute a fixed number of loops</li>
 * <li><b>CONDITION</b>: Continue looping based on a condition, similar to a do-while
 * structure, but when the condition is true, terminate the loop</li>
 * <li><b>JSON_ARRAY</b>: Parse a JSON array and iterate over its elements</li>
 * <li><b>Other Loop Strategy</b>: Users can implement the LoopStrategy interface according to their needs.</li>
 * </ul>
 *
 * <p>
 * <b>Note:</b> The LoopAgent must have a subAgent, which is the agent that will be executed in each loop.
 * </p>
 *
 * <p>
 * Usage example:
 * </p>
 * <pre>{@code
 * LoopAgent loopAgent = LoopAgent.builder()
 *     .name("example-loop-agent")
 *     .description("Example loop agent")
 *     .loopStrategy(LoopMode.condition(messagePredicate))
 *     .subAgent(subAgent)
 *     .build();
 * }</pre>
 *
 * @author vlsmb
 * @since 2025/8/25
 */
public class LoopAgent extends FlowAgent {

    private final LoopStrategy loopStrategy;

    public static final String LOOP_STRATEGY = "loopStrategy";

    private LoopAgent(LoopAgentBuilder builder) throws GraphStateException {
        super(builder.name, builder.description, builder.compileConfig, builder.subAgents, builder.stateSerializer, builder.executor);
        this.loopStrategy = builder.loopStrategy;
    }

    @Override
    protected StateGraph buildSpecificGraph(FlowGraphBuilder.FlowGraphConfig config) throws GraphStateException {
        config.customProperty(LOOP_STRATEGY, loopStrategy);
        return FlowGraphBuilder.buildGraph(FlowAgentEnum.LOOP.getType(), config);
    }

    public static LoopAgentBuilder builder() {
        return new LoopAgentBuilder();
    }

    public static class LoopAgentBuilder extends FlowAgentBuilder<LoopAgent, LoopAgentBuilder> {

        private LoopStrategy loopStrategy = null;

        @Override
        protected LoopAgentBuilder self() {
            return this;
        }

        public LoopAgentBuilder subAgent(Agent subAgent) {
            this.subAgents = List.of(subAgent);
            return self();
        }

        @Override
        public LoopAgentBuilder subAgents(List<Agent> subAgents) {
            throw new UnsupportedOperationException("LoopAgent must have only one subAgent, please use subAgent() method.");
        }

        public LoopAgentBuilder loopStrategy(LoopStrategy loopStrategy) {
            this.loopStrategy = loopStrategy;
            return self();
        }

        @Override
        protected void validate() {
            super.validate();
            if (this.loopStrategy == null) {
                throw new IllegalArgumentException("LoopAgent must have a loopStrategy.");
            }
        }

        @Override
        public LoopAgent build() throws GraphStateException {
            validate();
            return new LoopAgent(this);
        }
    }
}
