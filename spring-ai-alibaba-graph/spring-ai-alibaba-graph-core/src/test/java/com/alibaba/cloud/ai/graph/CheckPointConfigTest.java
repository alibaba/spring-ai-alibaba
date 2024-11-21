package com.alibaba.cloud.ai.graph;

import com.alibaba.cloud.ai.graph.checkpoint.BaseCheckpointSaver;
import com.alibaba.cloud.ai.graph.checkpoint.Checkpoint;
import com.alibaba.cloud.ai.graph.checkpoint.config.SaverConfig;
import com.alibaba.cloud.ai.graph.checkpoint.constant.SaverConstant;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;
import org.junit.jupiter.api.Test;

public class CheckPointConfigTest {

    @Test
    public void testConfig() {
        SaverConfig saverConfig = SaverConfig.builder()
                .register(SaverConstant.MEMORY, new MemorySaver())
                .build();
        BaseCheckpointSaver baseCheckpointSaver = saverConfig.get("disaster");
        BaseCheckpointSaver baseCheckpointSaver1 = saverConfig.get();
        System.out.println("baseCheckpointSaver = " + baseCheckpointSaver);
        System.out.println("baseCheckpointSaver1 = " + baseCheckpointSaver1);
        //test error scene
        saverConfig.get("");
        saverConfig.get(null);
    }
}
