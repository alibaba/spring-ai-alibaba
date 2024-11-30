package com.alibaba.cloud.ai.graph;

import com.alibaba.cloud.ai.graph.checkpoint.Checkpoint;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MongoSaver;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;

public class StateGraphMongoPersistenceTest {
    @Test
    public void testMongoPersistenceApi() throws Exception {
        MongoClient client = MongoClients.create("mongodb://host:port");
        MongoSaver saver = new MongoSaver(client);
        RunnableConfig runnableConfig = RunnableConfig.builder().checkPointId("7faca71d-4014-44c8-ad07-fab5d5d9bb07").threadId("test-thread-3").build();
        Map<String, Object> message = Map.of("message", "hello world");
        Checkpoint checkPoint = Checkpoint.builder()
                .nodeId("agent_1")
                .state(message)
                .nextNodeId(StateGraph.END)
                .build();

        //put
        RunnableConfig put = saver.put(runnableConfig, checkPoint);
        System.out.println("put = " + put);

        //get
        Optional<Checkpoint> checkpoint = saver.get(runnableConfig);
        System.out.println("checkpoint = " + checkpoint);

        //list
        Collection<Checkpoint> list = saver.list(runnableConfig);
        System.out.println("list = " + list);

        //clear
        boolean clear = saver.clear(runnableConfig.withCheckPointId(checkPoint.getId()));
        System.out.println("clear = " + clear);
        Collection<Checkpoint> list1 = saver.list(runnableConfig);
        System.out.println("list1 = " + list1);
    }
}
