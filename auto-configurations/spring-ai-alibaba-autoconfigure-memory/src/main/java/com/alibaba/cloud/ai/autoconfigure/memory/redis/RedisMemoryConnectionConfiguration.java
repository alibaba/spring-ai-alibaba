package com.alibaba.cloud.ai.autoconfigure.memory.redis;

import org.springframework.boot.autoconfigure.data.redis.RedisConnectionDetails;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Base Redis Memory Connection Configuration
 *
 * @author benym
 * @date 2025/7/30 21:24
 */
public abstract class RedisMemoryConnectionConfiguration {

    private final RedisChatMemoryProperties properties;

    private final RedisChatMemoryConnectionDetails connectionDetails;

    public RedisMemoryConnectionConfiguration(RedisChatMemoryProperties properties, RedisChatMemoryConnectionDetails connectionDetails) {
        this.properties = properties;
        this.connectionDetails = connectionDetails;
    }

    protected final RedisMemoryStandaloneConfiguration getStandaloneConfiguration() {
        RedisConnectionDetails.Standalone standalone = connectionDetails.getStandalone();
        return new RedisMemoryStandaloneConfiguration(standalone.getHost(), standalone.getPort(), connectionDetails.getUsername(),
                connectionDetails.getPassword(), properties.getTimeout());
    }


    protected final RedisMemoryClusterConfiguration getClusterConfiguration() {
        if (CollectionUtils.isEmpty(properties.getCluster().getNodes())) {
            return null;
        }
        List<String> nodes = getNodes(connectionDetails.getCluster());
        return new RedisMemoryClusterConfiguration(nodes, connectionDetails.getUsername(), connectionDetails.getPassword(), properties.getTimeout());
    }

    private List<String> getNodes(RedisConnectionDetails.Cluster cluster) {
        List<String> clusterNodes = new ArrayList<>();
        for (RedisConnectionDetails.Node node : cluster.getNodes()) {
            clusterNodes.add("redis://" + node.host() + ":" + node.port());
        }
        return clusterNodes;
    }
}
