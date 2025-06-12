package com.alibaba.cloud.ai.toolcalling.toutiaonews;

import com.alibaba.cloud.ai.toolcalling.common.CommonToolCallAutoConfiguration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.logging.Logger;

@SpringBootTest(classes = { CommonToolCallAutoConfiguration.class, ToutiaoNewsAutoConfiguration.class })
@DisplayName("Toutiao News Test")
class ToutiaoNewsSearchHotEventsTest {

	@Autowired
	private ToutiaoNewsSearchHotEventsService toutiaoNewsSearchHotEventsService;

	private static final Logger log = Logger.getLogger(ToutiaoNewsSearchHotEventsTest.class.getName());

	@Test
	@DisplayName("Tool-Calling Test")
	public void testGetHotEventFromToutiaoNews() {
		var resp = toutiaoNewsSearchHotEventsService.apply(new ToutiaoNewsSearchHotEventsService.Request());
		assert resp != null && resp.events() != null;
		log.info("results: " + resp.events());
	}

}
