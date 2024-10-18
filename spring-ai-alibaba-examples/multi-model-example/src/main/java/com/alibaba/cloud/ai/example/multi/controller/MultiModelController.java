package com.alibaba.cloud.ai.example.multi.controller;

import java.net.URI;
import java.util.List;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import com.alibaba.cloud.ai.dashscope.chat.MessageFormat;
import jakarta.annotation.Resource;

import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.Media;
import org.springframework.core.io.ResourceLoader;
import org.springframework.util.MimeType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author yuluo
 * @author <a href="mailto:yuluo08290126@gmail.com">yuluo</a>
 */

@RestController
@RequestMapping("/ai/multi")
public class MultiModelController {

	@Resource
	private ChatModel chatModel;

	@Resource
	private ResourceLoader resourceLoader;

	private static final String DEFAULT_PROMPT = "这些是什么？";

	private static final String DEFAULT_MODEL = "qwen-vl-max-latest";

	@GetMapping("/image")
	public String image(
			@RequestParam(value = "prompt", required = false, defaultValue = DEFAULT_PROMPT)
			String prompt
	) throws Exception {

		List<Media> mediaList = List.of(
				new Media(
						MimeType.valueOf("image/png"),
						new URI("https://dashscope.oss-cn-beijing.aliyuncs.com/images/dog_and_girl.jpeg").toURL()
				)
		);

		UserMessage message = new UserMessage(prompt, mediaList);
		ChatResponse response = chatModel.call(
				new Prompt(
						message,
						DashScopeChatOptions.builder()
								.withModel(DEFAULT_MODEL)
								.withMultiModel(true)
								.build()
				)
		);

		return response.getResult().getOutput().getContent();
	}

	@GetMapping("/video")
	public String video(
			@RequestParam(value = "prompt", required = false, defaultValue = DEFAULT_PROMPT)
			String prompt
	) throws Exception {

		List<Media> mediaList = List.of(

				new Media(
						MimeType.valueOf("image/png"),
						new URI("https://img.alicdn.com/imgextra/i3/O1CN01K3SgGo1eqmlUgeE9b_!!6000000003923-0-tps-3840-2160.jpg").toURL()
				),
				new Media(
						MimeType.valueOf("image/png"),
						new URI("https://img.alicdn.com/imgextra/i4/O1CN01BjZvwg1Y23CF5qIRB_!!6000000003000-0-tps-3840-2160.jpg").toURL()
				),
				new Media(
						MimeType.valueOf("image/png"),
						new URI("https://img.alicdn.com/imgextra/i4/O1CN01Ib0clU27vTgBdbVLQ_!!6000000007859-0-tps-3840-2160.jpg").toURL()
				),
				new Media(
						MimeType.valueOf("image/png"),
						new URI("https://img.alicdn.com/imgextra/i1/O1CN01aygPLW1s3EXCdSN4X_!!6000000005710-0-tps-3840-2160.jpg").toURL()
				));

		UserMessage message = new UserMessage(prompt, mediaList);
		message.getMetadata().put(DashScopeChatModel.MESSAGE_FORMAT, MessageFormat.VIDEO);

		ChatResponse response = chatModel.call(
				new Prompt(
						message,
						DashScopeChatOptions.builder()
								.withModel(DEFAULT_MODEL)
								.withMultiModel(true)
								.build()
				)
		);

		return response.getResult().getOutput().getContent();
	}

	@GetMapping("/image/bin")
	public String imagesBinary(
			@RequestParam(value = "prompt", required = false, defaultValue = DEFAULT_PROMPT)
			String prompt
	) {

		UserMessage message = new UserMessage(
				prompt,
				new Media(
						MimeType.valueOf("image/jpeg"),
						resourceLoader.getResource("classpath:/multimodel/dog_and_girl.jpeg")
				));
		message.getMetadata().put(DashScopeChatModel.MESSAGE_FORMAT, MessageFormat.VIDEO);

		ChatResponse response = chatModel.call(
				new Prompt(
						message,
						DashScopeChatOptions.builder()
								.withModel(DEFAULT_MODEL)
								.withMultiModel(true)
								.build()
				)
		);

		return response.getResult().getOutput().getContent();
	}

	@GetMapping("/stream/image")
	public String streamImage(
			@RequestParam(value = "prompt", required = false, defaultValue = DEFAULT_PROMPT)
			String prompt
	) {

		UserMessage message = new UserMessage(
				prompt,
				new Media(
						MimeType.valueOf("image/jpeg"),
						resourceLoader.getResource("classpath:/multimodel/dog_and_girl.jpeg")
				));

		message.getMetadata().put(DashScopeChatModel.MESSAGE_FORMAT, MessageFormat.VIDEO);
		List<ChatResponse> response = chatModel.stream(
				new Prompt(
						message,
						DashScopeChatOptions.builder()
								.withModel(DEFAULT_MODEL)
								.withMultiModel(true)
								.build()
				)
		).collectList().block();

		StringBuilder result = new StringBuilder();
		if (response != null) {
			for (ChatResponse chatResponse : response) {
				String outputContent = chatResponse.getResult().getOutput().getContent();
				result.append(outputContent);
			}
		}

		return result.toString();
	}

}
