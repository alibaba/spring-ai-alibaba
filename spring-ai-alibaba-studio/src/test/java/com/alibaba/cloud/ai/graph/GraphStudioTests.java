package com.alibaba.cloud.ai.graph;

import java.io.IOException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.TestPropertySource;

//@TestPropertySource("classpath:application.yml")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
//@ComponentScan({"com.alibaba.cloud.ai"})
class GraphStudioTests {

	@LocalServerPort
	private int port;

	@Test
	@EnabledIfEnvironmentVariable(named = "AI_DASHSCOPE_API_KEY" , matches = ".+")
	void contextLoads() throws IOException {
		System.in.read();
	}

}
