package com.alibaba.cloud.ai.autoconfigure.memory.redis;

import org.springframework.boot.autoconfigure.data.redis.RedisConnectionDetails;

import java.util.List;

/**
 * Adapts RedisChatMemoryProperties to RedisChatMemoryConnectionDetails.
 * 
 * @author benym
 * @date 2025/7/30 20:39
 */
public class RedisChatMemoryConnectionDetails implements RedisConnectionDetails {

    private final RedisChatMemoryProperties properties;

    public RedisChatMemoryConnectionDetails(RedisChatMemoryProperties properties) {
        this.properties = properties;
    }

    @Override
    public String getUsername() {
        return properties.getUsername();
    }

    @Override
    public String getPassword() {
        return properties.getPassword();
    }

    @Override
    public Standalone getStandalone() {
        return Standalone.of(properties.getHost(), properties.getPort());
    }

    @Override
    public Cluster getCluster() {
        RedisChatMemoryProperties.Cluster cluster = this.properties.getCluster();
        List<Node> nodes = (cluster != null) ? cluster.getNodes().stream().map(this::asNode).toList() : null;
        return (nodes != null) ? () -> nodes : null;
    }

    private Node asNode(String node) {
        int portSeparatorIndex = node.lastIndexOf(':');
        String host = node.substring(0, portSeparatorIndex);
        int port = Integer.parseInt(node.substring(portSeparatorIndex + 1));
        return new Node(host, port);
    }
}
