/*
 * Copyright 2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.cloud.ai.graph;

import com.alibaba.cloud.ai.graph.serializer.agent.AgentAction;
import com.alibaba.cloud.ai.graph.serializer.agent.AgentFinish;
import com.alibaba.cloud.ai.graph.serializer.agent.AgentOutcome;
import com.alibaba.cloud.ai.graph.state.NodeState;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

import java.util.Map;
import java.util.Random;

@Slf4j
public class AiInsurance {

    private final IsAgentService agentService;

    public AiInsurance(IsAgentService agentService) {
        this.agentService = agentService;
    }

    public Map<String, Object> callAgent(NodeState state) {
        log.info("callAgent");

        var input = state.input()
                .filter(StringUtils::hasText)
                .orElseThrow(() -> new IllegalArgumentException("no input provided!"));

        var response = agentService.execute(input);

        var output = response.getResult().getOutput();

        if (output.hasToolCalls()) {
            var action = new AgentAction(output.getToolCalls().get(0), "");
            return Map.of(NodeState.OUTPUT, new AgentOutcome(action, null));

        } else {
            var finish = new AgentFinish(Map.of("returnValues", output.getContent()), output.getContent());

            return Map.of(NodeState.OUTPUT, new AgentOutcome(null, finish));
        }
    }

    public String questionEnough(NodeState state) {

        Map<String, Object> returnValues = state.data();
        if (!returnValues.containsKey("input")) {
            return "return";
        }
        String input = (String) returnValues.get("input");
        // 判断用户输入内容是否包括全部所需要信息（年龄、性别、学历……）
        if (input.contains("年龄") && input.contains("性别") && input.contains("学历")) {
            return "input_enough";
        } else {
            return "input_not_enough";
        }
    }

    public String purchaseIntention(NodeState state) {

        var input = state.input()
                .filter(StringUtils::hasText)
                .orElseThrow(() -> new IllegalArgumentException("no input provided!"));

        var response = agentService.executeByPrompt(
                "判断用户是否有购买保险意愿，只返回是或者否。比如用户输入我要买，返回是" + input, "判断用户是否有购买保险意愿，只返回是或者否。比如用户输入我要买，返回是");

        var output = response.getResult().getOutput();
        log.info("agent:{}", output.getContent());
        // 判断用户输入内容是否包括全部所需要信息（年龄、性别、学历……）
        if (output.getContent().equals("是")) {
            return "want_purchase";
        } else {
            return "not_want_purchase";
        }
    }

    public String generateBills(NodeState state) {

        var input = state.input()
                .filter(StringUtils::hasText)
                .orElseThrow(() -> new IllegalArgumentException("no input provided!"));

        if ("1".equals(input) || "2".equals(input)) {
            return "continue";
        } else {
            return "error";
        }
    }

    public String payFinish(NodeState state) {

        var input = state.input()
                .filter(StringUtils::hasText)
                .orElseThrow(() -> new IllegalArgumentException("no input provided!"));

        // 1 确认 0 取消
        if ("1".equals(input)) {
            // 查询是否付款，假设已付款
            if (new Random().nextInt(100) < 50) {
                return "continue";
            } else {
                return "error";
            }
        } else if ("0".equals(input)) {
            return "error";
        } else {
            return "prompt";
        }
    }
}
