package com.alibaba.cloud.ai.service.impl;

import com.alibaba.cloud.ai.common.ModelType;
import com.alibaba.cloud.ai.dashscope.api.DashScopeImageApi;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import com.alibaba.cloud.ai.dashscope.image.DashScopeImageModel;
import com.alibaba.cloud.ai.dashscope.image.DashScopeImageOptions;
import com.alibaba.cloud.ai.exception.NotFoundException;
import com.alibaba.cloud.ai.model.ChatModel;
import com.alibaba.cloud.ai.param.RunActionParam;
import com.alibaba.cloud.ai.service.ChatModelDelegate;
import com.alibaba.cloud.ai.utils.SpringApplicationUtil;
import com.alibaba.cloud.ai.vo.ActionResult;
import com.alibaba.cloud.ai.vo.ChatModelRunResult;
import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.image.ImageModel;
import org.springframework.ai.image.ImageOptions;
import org.springframework.ai.image.ImageOptionsBuilder;
import org.springframework.ai.image.ImagePrompt;
import org.springframework.ai.image.ImageResponse;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class ChatModelDelegateImpl implements ChatModelDelegate {

	@Override
	public List<ChatModel> list() {
		List<ChatModel> res = new ArrayList<>();

		// ChatModel
		Map<String, org.springframework.ai.chat.model.ChatModel> chatModelMap = SpringApplicationUtil
			.getBeans(org.springframework.ai.chat.model.ChatModel.class);
		for (Map.Entry<String, org.springframework.ai.chat.model.ChatModel> entry : chatModelMap.entrySet()) {
			org.springframework.ai.chat.model.ChatModel chatModel = entry.getValue();
			log.info("bean name:{}, bean Class:{}", entry.getKey(), chatModel.getClass());

			ChatModel model = ChatModel.builder()
				.name(entry.getKey())
				.model(chatModel.getDefaultOptions().getModel())
				.modelType(ModelType.CHAT)
				.build();

			if (chatModel.getClass() == DashScopeChatModel.class) {
				DashScopeChatModel dashScopeChatModel = (DashScopeChatModel) chatModel;
				model.setChatOptions(dashScopeChatModel.getDashScopeChatOptions());
			}

			res.add(model);
		}

		// ImageModel
		Map<String, ImageModel> imageModelMap = SpringApplicationUtil.getBeans(ImageModel.class);
		for (Map.Entry<String, ImageModel> entry : imageModelMap.entrySet()) {
			ImageModel imageModel = entry.getValue();
			log.info("bean name:{}, bean Class:{}", entry.getKey(), imageModel.getClass());
			ChatModel model = ChatModel.builder().name(entry.getKey()).modelType(ModelType.IMAGE).build();

			if (imageModel.getClass() == DashScopeImageModel.class) {
				DashScopeImageModel dashScopeImageModel = (DashScopeImageModel) imageModel;
				model.setModel(dashScopeImageModel.getOptions().getModel());
				model.setImageOptions(dashScopeImageModel.getOptions());
			}
			res.add(model);
		}

		// TODO AudioModel
		return res;
	}

	@Override
	public ChatModel getByModelName(String modelName) {
		org.springframework.ai.chat.model.ChatModel chatModel = getChatModel(modelName);
		if (chatModel != null) {
			ChatModel model = ChatModel.builder()
				.name(modelName)
				.model(chatModel.getDefaultOptions().getModel())
				.build();
			if (chatModel.getClass() == DashScopeChatModel.class) {
				DashScopeChatModel dashScopeChatModel = (DashScopeChatModel) chatModel;
				model.setChatOptions(dashScopeChatModel.getDashScopeChatOptions());
			}
			return model;
		}

		ImageModel imageModel = getImageModel(modelName);
		if (imageModel != null) {
			ChatModel model = ChatModel.builder().name(modelName).modelType(ModelType.IMAGE).build();
			if (imageModel.getClass() == DashScopeImageModel.class) {
				DashScopeImageModel dashScopeImageModel = (DashScopeImageModel) imageModel;
				model.setModel(dashScopeImageModel.getOptions().getModel());
				model.setImageOptions(dashScopeImageModel.getOptions());
			}
			return model;
		}

		// TODO AudioModel

		log.error("can not find by bean name:{}", modelName);
		throw new NotFoundException();
	}

	@Override
	public ChatModelRunResult run(RunActionParam runActionParam) {
		String key = runActionParam.getKey();
		String input = runActionParam.getInput();
		DashScopeChatOptions chatOptions = runActionParam.getChatOptions();

		org.springframework.ai.chat.model.ChatModel chatModel = getChatModel(key);
		if (chatModel != null) {
			if (chatModel.getClass() == DashScopeChatModel.class) {
				DashScopeChatModel dashScopeChatModel = (DashScopeChatModel) chatModel;
				if (chatOptions != null) {
					log.info("set chat options, {}", JSON.toJSONString(chatOptions));
					dashScopeChatModel.setDashScopeChatOptions(chatOptions);
				}
			}
			ChatResponse response = chatModel.call(new Prompt(input));
			String resp = response.getResult().getOutput().getContent();
			return ChatModelRunResult.builder()
				.input(runActionParam)
				.result(ActionResult.builder().Response(resp).build())
				.build();
		}

		log.error("can not find by bean name:{}", key);
		throw new NotFoundException();
	}

	@Override
	public String runImageGenTask(RunActionParam runActionParam) {
		String key = runActionParam.getKey();
		String input = runActionParam.getInput();
		DashScopeImageOptions imageOptions = runActionParam.getImageOptions();

		ImageModel imageModel = getImageModel(key);
		if (imageModel != null) {
			if (imageModel.getClass() == DashScopeImageModel.class) {
				DashScopeImageModel dashScopeImageModel = (DashScopeImageModel) imageModel;
				if (imageOptions != null) {
					log.info("set image options, {}", JSON.toJSONString(imageOptions));
					dashScopeImageModel.setOptions(imageOptions);
				}
				else {
					imageOptions = DashScopeImageOptions.builder()
						.withModel(dashScopeImageModel.getOptions().getModel())
						.build();
				}
			}
			if (imageOptions == null) {
				imageOptions = DashScopeImageOptions.builder()
					.withModel(DashScopeImageApi.ImageModel.WANX_V1.getValue())
					.build();
			}
			ImageOptions options = ImageOptionsBuilder.builder()
				.withModel(imageOptions.getModel())
				.withN(imageOptions.getN())
				.withWidth(imageOptions.getWidth())
				.withHeight(imageOptions.getHeight())
				.withStyle(imageOptions.getStyle())
				.build();
			ImagePrompt imagePrompt = new ImagePrompt(input, options);
			ImageResponse imageResponse = imageModel.call(imagePrompt);
			return imageResponse.getResult().getOutput().getUrl();
		}

		log.error("can not find by bean name:{}", key);
		throw new NotFoundException();
	}

	private org.springframework.ai.chat.model.ChatModel getChatModel(String modelName) {
		Map<String, org.springframework.ai.chat.model.ChatModel> chatModelMap = SpringApplicationUtil
			.getBeans(org.springframework.ai.chat.model.ChatModel.class);
		for (Map.Entry<String, org.springframework.ai.chat.model.ChatModel> entry : chatModelMap.entrySet()) {
			org.springframework.ai.chat.model.ChatModel chatModel = entry.getValue();
			log.info("bean name:{}, bean Class:{}", entry.getKey(), chatModel.getClass());

			if (entry.getKey().equals(modelName)) {
				return chatModel;
			}
		}

		return null;
	}

	private ImageModel getImageModel(String modelName) {
		Map<String, ImageModel> imageModelMap = SpringApplicationUtil.getBeans(ImageModel.class);
		for (Map.Entry<String, ImageModel> entry : imageModelMap.entrySet()) {
			ImageModel imageModel = entry.getValue();
			log.info("bean name:{}, bean Class:{}", entry.getKey(), imageModel.getClass());

			if (entry.getKey().equals(modelName)) {
				return imageModel;
			}
		}

		return null;
	}

}
