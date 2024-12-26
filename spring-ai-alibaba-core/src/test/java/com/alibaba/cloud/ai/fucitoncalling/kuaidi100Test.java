package com.alibaba.cloud.ai.fucitoncalling;

import com.alibaba.cloud.ai.dashscope.DashscopeAiTestConfiguration;
import com.alibaba.cloud.ai.functioncalling.kuaidi100.Kuaidi100AutoConfiguration;
import com.alibaba.cloud.ai.functioncalling.kuaidi100.Kuaidi100Properties;
import com.alibaba.cloud.ai.functioncalling.kuaidi100.Kuaidi100Service;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * @Author: XiaoYunTao
 * @Date: 2024/12/26
 */
@SpringBootTest(classes = { Kuaidi100AutoConfiguration.class, DashscopeAiTestConfiguration.class })
@EnabledIfEnvironmentVariable(named = "DASHSCOPE_API_KEY", matches = ".+")
public class kuaidi100Test {

	@Autowired
	ChatModel dashscopeChatModel;

	@Autowired
	Kuaidi100Properties kuaidi100Properties;

	@Test
	public void test() {
		ChatClient chatClient = ChatClient.builder(dashscopeChatModel).build();
		String content = chatClient.prompt()
			.function("queryTrackFunction", "查询快递", new Kuaidi100Service(kuaidi100Properties))
			.user("查询一下单号为：，的快递")
			.call()
			.content();
		System.out.println(content);
	}

}
