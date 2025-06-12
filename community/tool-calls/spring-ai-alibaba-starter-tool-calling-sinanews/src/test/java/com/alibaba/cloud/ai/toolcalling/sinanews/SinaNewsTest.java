package com.alibaba.cloud.ai.toolcalling.sinanews;

import com.alibaba.cloud.ai.toolcalling.common.CommonToolCallAutoConfiguration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.logging.Logger;

@SpringBootTest(classes = {CommonToolCallAutoConfiguration.class, SinaNewsAutoConfiguration.class})
@DisplayName("Sina News Test")
class SinaNewsTest {
    @Autowired
    private SinaNewsService sinaNewsService;

    private static final Logger log = Logger.getLogger(SinaNewsTest.class.getName());

    @Test
    @DisplayName("Tool-Calling Test")
    public void testGetHotEventFromSinaNews() {
        var resp = sinaNewsService.apply(new SinaNewsService.Request());
        assert resp != null && resp.events() != null;
        log.info("results: " + resp.events());
    }

}
