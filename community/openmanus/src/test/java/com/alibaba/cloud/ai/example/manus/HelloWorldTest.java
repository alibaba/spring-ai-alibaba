package com.alibaba.cloud.ai.example.manus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(classes = OpenManusSpringBootApplication.class)
public class HelloWorldTest {

	@Test
	@DisplayName("测试Hello World")
	void testHelloWorld() {
		String expected = "Hello World";
		String actual = "Hello World";
		assertEquals(expected, actual, "字符串应该相等");
	}

}
