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
package com.alibaba.cloud.ai.studio.admin.builder.generator.service.generator.workflow.sections;

import com.alibaba.cloud.ai.studio.admin.builder.generator.model.workflow.Case;
import com.alibaba.cloud.ai.studio.admin.builder.generator.model.workflow.Edge;
import com.alibaba.cloud.ai.studio.admin.builder.generator.model.workflow.LogicalOperatorType;
import com.alibaba.cloud.ai.studio.admin.builder.generator.model.workflow.nodedata.BranchNodeData;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BranchNodeSectionTest {

    @Test
    void testRenderEdges_Parallel() {
        BranchNodeSection section = new BranchNodeSection();
        BranchNodeData nodeData = new BranchNodeData();
        nodeData.setVarName("test_parallel_node");
        nodeData.setDefaultCase("default");

        Case falseCase = new Case();
        falseCase.setId("false");
        falseCase.setLogicalOperator(LogicalOperatorType.OR);
        falseCase.setConditions(Collections.emptyList());
        nodeData.setCases(List.of(falseCase));

        List<Edge> edges = new ArrayList<>();

        // Edge 1: false -> node_A
        Edge edge1 = new Edge();
        edge1.setSourceHandle("false");
        edge1.setTarget("node_A");
        edges.add(edge1);

        // Edge 2: false -> node_B (同一 Handle，不同 Target -> 触发并行)
        Edge edge2 = new Edge();
        edge2.setSourceHandle("false");
        edge2.setTarget("node_B");
        edges.add(edge2);

        String code = section.renderEdges(nodeData, edges);

        assertTrue(code.contains("stateGraph.addParallelConditionalEdges"));
        assertTrue(code.contains("return completedFuture(List.of("));
        assertTrue(code.contains("\"node_A\""));
        assertTrue(code.contains("\"node_B\""));
        assertTrue(code.contains("Map.entry(\"node_A\", \"node_A\")"));
    }
}
