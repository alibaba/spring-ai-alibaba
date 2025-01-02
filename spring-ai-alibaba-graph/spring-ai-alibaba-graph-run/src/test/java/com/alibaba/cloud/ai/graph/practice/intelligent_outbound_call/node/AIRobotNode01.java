package com.alibaba.cloud.ai.graph.practice.intelligent_outbound_call.node;

import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.alibaba.cloud.ai.graph.practice.intelligent_outbound_call.IsExecutor;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AIRobotNode01 implements NodeAction<IsExecutor.State> {

    //可以通过前端映射
    private String template =
            "是这样的，公司主要提供阿里云产品分销业务，通过我们您可以享受比官网更优惠的价格和返佣哦，您这边有兴趣了解下吗？";

    @Override
    public Map<String, Object> apply(IsExecutor.State agentState) {
        Pattern pattern = Pattern.compile("#\\{(.*?)\\}");
        Matcher matcher = pattern.matcher(template);
        StringBuilder sb = new StringBuilder();
        boolean anyFind = false;
        while (matcher.find()) {
            anyFind = true;
            String key = matcher.group(1);
            if (agentState.data().containsKey(key)) {
                String replacement = agentState.data().get(key).toString();
                matcher.appendReplacement(sb, replacement != null ? replacement : "");
            }
        }
        matcher.appendTail(sb);
        String content = anyFind ? sb.toString() : template;
        var finish = new IsExecutor.Finish(Map.of("returnValues", content), content);
        return Map.of(IsExecutor.State.AGENT_OUTCOME, new IsExecutor.Outcome(null, finish));
    }

}
