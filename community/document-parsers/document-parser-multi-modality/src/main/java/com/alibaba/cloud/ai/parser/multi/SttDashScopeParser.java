package com.alibaba.cloud.ai.parser.multi;

import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import com.alibaba.cloud.ai.document.DocumentParser;
import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversation;
import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversationParam;
import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversationResult;
import com.alibaba.dashscope.common.MultiModalMessage;
import com.alibaba.dashscope.common.Role;
import com.alibaba.dashscope.exception.ApiException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.alibaba.dashscope.exception.UploadFileException;
import org.springframework.ai.document.Document;

/**
 * @author HeYQ
 * @since 2025-02-06 23:38
 */
public class SttDashScopeParser implements DocumentParser {

	private final String model;

	private final String apiKey;

	private final String text;

	public SttDashScopeParser(String apiKey) {
		this(apiKey, "qwen-audio-turbo-latest", "这段音频在说什么?");
	}

	public SttDashScopeParser(String apiKey, String model) {
		this(apiKey, model, "这段音频在说什么?");
	}

	public SttDashScopeParser(String apiKey, String model, String text) {
		this.apiKey = apiKey;
		this.model = model;
		this.text = text;
	}

	public List<Document> parse(String path) {
		try {
			MultiModalConversation conversation = new MultiModalConversation();
			String filePath = "";
			if (path.startsWith("http")) {
				filePath = path;
			}
			else {
				String os = System.getProperty("os.name").toLowerCase();

				if (os.contains("win")) {
					filePath = "file:///" + path;
				}
				else if (os.contains("nix") || os.contains("nux") || os.contains("mac")) {
					filePath = "file://" + path;
				}
			}
			String finalFilePath = filePath;
			MultiModalMessage userMessage = MultiModalMessage.builder()
				.role(Role.USER.getValue())
				.content(Arrays.asList(new HashMap<String, Object>() {
					{
						put("audio", finalFilePath);
					}
				}, new HashMap<String, Object>() {
					{
						put("text", text);
					}
				}))
				.build();
			MultiModalConversationParam param = MultiModalConversationParam.builder()
				.apiKey(apiKey)
				.model(model)
				.message(userMessage)
				.build();
			MultiModalConversationResult result = conversation.call(param);
			return List.of(new Document(
					result.getOutput().getChoices().get(0).getMessage().getContent().get(0).get("text").toString()));

		}
		catch (ApiException | NoApiKeyException | UploadFileException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public List<Document> parse(InputStream inputStream) {
		return List.of();
	}

}