package com.alibaba.cloud.ai.graph.practice.insurance_sale.node;

import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.alibaba.cloud.ai.graph.practice.insurance_sale.IsExecutor;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WelcomeNode implements NodeAction<IsExecutor.State> {

    //可以通过前端映射
    private String template =
            "您好！我是您的保险助手popo。无论您是在寻找保障、规划未来，还是需要专业的保险建议，我都在这里为您提供帮助。请告诉我您的保险需求，让我们开始吧！\n";

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
