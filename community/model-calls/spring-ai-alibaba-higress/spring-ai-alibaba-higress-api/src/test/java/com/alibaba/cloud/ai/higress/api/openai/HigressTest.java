package com.alibaba.cloud.ai.higress.api.openai;

import org.junit.Test;
import org.springframework.ai.chat.client.ChatClient;

/**
 * @Author NGshiyu
 * @Description test
 * @CreateTime 2026/1/29 15:17
 */
public class HigressTest {

	@Test
	public void testHigressByApiKey() {
		String higressAiGatewayBaseUrl = System.getenv("AI_GATEWAY_BASE_URL");
		String apiKey = System.getenv("HIGRESS_AI_GATEWAY_API_KEY");
		String jobDescription = "你是一个智能问答助手";
		String question = "给我讲个笑话";

		HigressOpenAiApi openAiApi = HigressOpenAiApi.builder().apiKey(apiKey).baseUrl(higressAiGatewayBaseUrl).build();
		HigressOpenAiChatOptions openAiChatOptions = HigressOpenAiChatOptions.builder().model("qwen-plus").build();
		HigressOpenAiChatModel higressOpenAiChatModel = HigressOpenAiChatModel.builder()
			.higressOpenAiApi(openAiApi)
			.defaultOptions(openAiChatOptions)
			.build();
		ChatClient client = ChatClient.builder(higressOpenAiChatModel).build();
		// CallResponse是一个Spring Ai通用的返回模型
		ChatClient.ChatClientRequestSpec doChat = client.prompt(question).system(systemSpec -> {
			// 设置系统角色
			systemSpec.text(jobDescription);
		});
		StringBuffer answer = new StringBuffer();
		doChat.stream().chatResponse().doOnNext(data -> {
			answer.append(data.getResult().getOutput().getText());
			System.out.printf("😀😀😀😀answer : %s\n", answer.toString());
			System.out.println("============================================================");
		}).blockLast();
	}

	private static final String ACCESS_KEY = System.getenv("ACCESS_KEY");// AI网关上的消费者HMAC凭证的Access
																			// Key

	private static final String SECRET_KEY = System.getenv("SECRET_KEY");// AI网关上的消费者HMAC凭证的Secret
																			// Key

	private static final String AKSK_BASE_URL = System.getenv("AI_GATEWAY_HMAC_BASE_URL_API");// AI网关上的消费者HMAC凭证的Secret
																								// Key

	@Test
	public void testHigressByHmac() {
		String jobDescription = "你是一个智能问答助手";
		String question = "给我讲个笑话";

		HigressOpenAiApi openAiApi = HigressOpenAiApi.builder()
			.higressHmac(ACCESS_KEY, SECRET_KEY)
			.baseUrl(AKSK_BASE_URL)
			.build();
		HigressOpenAiChatOptions openAiChatOptions = HigressOpenAiChatOptions.builder().model("qwen-plus").build();
		HigressOpenAiChatModel higressOpenAiChatModel = HigressOpenAiChatModel.builder()
			.higressOpenAiApi(openAiApi)
			.defaultOptions(openAiChatOptions)
			.build();
		ChatClient client = ChatClient.builder(higressOpenAiChatModel).build();
		// CallResponse是一个Spring Ai通用的返回模型
		ChatClient.ChatClientRequestSpec doChat = client.prompt(question).system(systemSpec -> {
			// 设置系统角色
			systemSpec.text(jobDescription);
		});
		StringBuffer answer = new StringBuffer();
		doChat.stream().chatResponse().doOnNext(data -> {
			answer.append(data.getResult().getOutput().getText());
			System.out.printf("😀😀😀😀answer : %s\n", answer);
			System.out.println("============================================================");
		}).blockLast();
	}

}
