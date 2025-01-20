package com.alibaba.cloud.ai.graph.practice.intelligent_outbound_call.data;

import lombok.Data;

@Data
public class IocEdge {
    private String id;
    private String affirmativeNode;
    private String negativeNode;
    private String refusalNode;
    private String defaultNode;
    private String nextNode;

    public IocEdge(String id, String nextNode) {
        this.id = id;
        this.nextNode = nextNode;
    }

    public IocEdge(String id, String affirmativeNode, String negativeNode, String refusalNode, String defaultNode) {
        this.id = id;
        this.affirmativeNode = affirmativeNode;
        this.negativeNode = negativeNode;
        this.refusalNode = refusalNode;
        this.defaultNode = defaultNode;
    }

}