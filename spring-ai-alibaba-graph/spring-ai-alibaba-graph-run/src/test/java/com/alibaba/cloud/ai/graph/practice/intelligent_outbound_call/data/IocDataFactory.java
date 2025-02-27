package com.alibaba.cloud.ai.graph.practice.intelligent_outbound_call.data;

import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.alibaba.cloud.ai.graph.practice.intelligent_outbound_call.node.CustomerNode;
import com.alibaba.cloud.ai.graph.practice.intelligent_outbound_call.node.RobotNode;
import com.alibaba.cloud.ai.graph.state.NodeState;

import java.util.*;

import static com.alibaba.cloud.ai.graph.StateGraph.END;
import static com.alibaba.cloud.ai.graph.StateGraph.START;

public class IocDataFactory {
    private static final Map<String, NodeAction> NODES = new HashMap<>();
    private static final List<IocEdge> IOC_EDGES = new ArrayList<>();

    public static NodeAction getNode(String id) {
        return NODES.get(id);
    }

    public static List<IocEdge> getIocEdges() {
        return Collections.unmodifiableList(IOC_EDGES);
    }

    static {
        NODES.put("r_welcome", new RobotNode("喂，哎您好，我这边是xx街道社区卫生服务中心的家庭医生，我们正在对辖区居民的健康情况做了解和更新，有几个问题想问一下，请问您是xx本人吗？"));
        NODES.put("c_welcome", new CustomerNode());
        NODES.put("r_refusal", new RobotNode("那不好意思打扰了，再见？"));
        NODES.put("r_family_members", new RobotNode("那您是xx的家属吗？"));
        NODES.put("c_family_members", new CustomerNode());
        NODES.put("r_blood_pressure", new RobotNode("好的，请问您的血压和血糖都正常吗？"));
        NODES.put("c_blood_pressure", new CustomerNode());
        NODES.put("r_hight", new RobotNode("好的，您的身高是多少呢？"));
        NODES.put("c_hight", new CustomerNode());
        NODES.put("r_wight", new RobotNode("那您的体重是多少呢？"));
        NODES.put("c_wight", new CustomerNode());
        NODES.put("r_smoke", new RobotNode("您平时抽烟吗？"));
        NODES.put("c_smoke", new CustomerNode());
        NODES.put("r_smoke_yes", new RobotNode("一天大概抽几只呢？"));
        NODES.put("c_smoke_yes", new CustomerNode());
        NODES.put("r_drink", new RobotNode("您平时有喝酒吗？您可以说“不喝”“偶尔喝”“经常喝”“每天喝”"));
        NODES.put("c_drink", new CustomerNode());
        NODES.put("r_eye", new RobotNode("再问一下关于视力方面的，您有近视或者老花吗？度数是多少呢？"));
        NODES.put("c_eye", new CustomerNode());
        NODES.put("r_sport", new RobotNode("您平时都喜欢做哪些运动呢？"));
        NODES.put("c_sport", new CustomerNode());
        NODES.put("r_sport_times", new RobotNode("每周大概运动几次，每次大概多长时间呢？"));
        NODES.put("c_sport_times", new CustomerNode());

        IOC_EDGES.add(new IocEdge(START, "r_welcome"));
        IOC_EDGES.add(new IocEdge("r_welcome", "c_welcome"));
        IOC_EDGES.add(new IocEdge("c_welcome", "r_blood_pressure", "r_family_members", "r_refusal", "r_blood_pressure"));
        IOC_EDGES.add(new IocEdge("r_refusal", END));
        IOC_EDGES.add(new IocEdge("r_family_members", "c_family_members"));
        IOC_EDGES.add(new IocEdge("c_family_members", "r_blood_pressure", "r_refusal", "r_refusal", "r_blood_pressure"));
        IOC_EDGES.add(new IocEdge("r_blood_pressure", "c_blood_pressure"));
        IOC_EDGES.add(new IocEdge("c_blood_pressure", "r_hight", "r_hight", "r_refusal", "r_hight"));
        IOC_EDGES.add(new IocEdge("r_hight", "c_hight"));
        IOC_EDGES.add(new IocEdge("c_hight", "r_wight", "r_wight", "r_refusal", "r_wight"));
        IOC_EDGES.add(new IocEdge("r_wight", "c_wight"));
        IOC_EDGES.add(new IocEdge("c_wight", "r_smoke", "r_smoke", "r_refusal", "r_smoke"));
        IOC_EDGES.add(new IocEdge("r_smoke", "c_smoke"));
        IOC_EDGES.add(new IocEdge("c_smoke", "r_smoke_yes", "r_drink", "r_refusal", "r_drink"));
        IOC_EDGES.add(new IocEdge("r_smoke_yes", "c_smoke_yes"));
        IOC_EDGES.add(new IocEdge("c_smoke_yes", "r_drink", "r_drink", "r_refusal", "r_drink"));
        IOC_EDGES.add(new IocEdge("r_drink", "c_drink"));
        IOC_EDGES.add(new IocEdge("c_drink", "r_eye", "r_eye", "r_refusal", "r_eye"));
        IOC_EDGES.add(new IocEdge("r_eye", "c_eye"));
        IOC_EDGES.add(new IocEdge("c_eye", "r_sport", "r_sport", "r_refusal", "r_sport"));
        IOC_EDGES.add(new IocEdge("r_sport", "c_sport"));
        IOC_EDGES.add(new IocEdge("c_sport", "r_sport_times", END, "r_refusal", END));
        IOC_EDGES.add(new IocEdge("r_sport_times", "c_sport_times"));
        IOC_EDGES.add(new IocEdge("c_sport_times", END));
    }

    private static final List<String> affirmatives = new ArrayList<>();
    private static final List<String> negatives = new ArrayList<>();
    private static final List<String> refusals = new ArrayList<>();

    static {
        affirmatives.add("是的");
        affirmatives.add("好的");
        affirmatives.add("对的");

        negatives.add("不是");
        negatives.add("不行");
        negatives.add("不对");

        refusals.add("不需要");
        refusals.add("没空");
        refusals.add("在忙");
    }

    public static String generateCustomer(NodeState state) {
        var input = state.input()
                .filter(org.springframework.util.StringUtils::hasText)
                .orElseThrow(() -> new IllegalArgumentException("no input provided!"));
        if (affirmatives.contains(input)) {
            return "affirmative";
        } else if (negatives.contains(input)) {
            return "negative";
        } else if (refusals.contains(input)) {
            return "refusal";
        } else {
            return "default";
        }
    }

}