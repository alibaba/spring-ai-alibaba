package com.alibaba.cloud.ai.service.impl;

import com.alibaba.cloud.ai.common.ModelType;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.dashscope.image.DashScopeImageModel;
import com.alibaba.cloud.ai.exception.NotFoundException;
import com.alibaba.cloud.ai.model.ChatModel;
import com.alibaba.cloud.ai.param.RunActionParam;
import com.alibaba.cloud.ai.service.ChatModelDelegate;
import com.alibaba.cloud.ai.utils.SpringApplicationUtil;
import com.alibaba.cloud.ai.vo.ChatModelRunResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.image.ImageModel;
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
		Map<String, org.springframework.ai.chat.model.ChatModel> chatModelMap = SpringApplicationUtil
			.getBeans(org.springframework.ai.chat.model.ChatModel.class);
		for (Map.Entry<String, org.springframework.ai.chat.model.ChatModel> entry : chatModelMap.entrySet()) {
			org.springframework.ai.chat.model.ChatModel chatModel = entry.getValue();
			log.info("bean name:{}, bean Class:{}", entry.getKey(), chatModel.getClass());

			if (entry.getKey().equals(modelName)) {
				ChatModel model = ChatModel.builder()
					.name(entry.getKey())
					.model(chatModel.getDefaultOptions().getModel())
					.build();
				if (chatModel.getClass() == DashScopeChatModel.class) {
					DashScopeChatModel dashScopeChatModel = (DashScopeChatModel) chatModel;
					model.setChatOptions(dashScopeChatModel.getDashScopeChatOptions());
				}
				return model;
			}
		}

		Map<String, ImageModel> imageModelMap = SpringApplicationUtil.getBeans(ImageModel.class);
		for (Map.Entry<String, ImageModel> entry : imageModelMap.entrySet()) {
			ImageModel imageModel = entry.getValue();
			log.info("bean name:{}, bean Class:{}", entry.getKey(), imageModel.getClass());

			if (entry.getKey().equals(modelName)) {
				ChatModel model = ChatModel.builder().name(entry.getKey()).modelType(ModelType.IMAGE).build();
				if (imageModel.getClass() == DashScopeImageModel.class) {
					DashScopeImageModel dashScopeImageModel = (DashScopeImageModel) imageModel;
					model.setModel(dashScopeImageModel.getOptions().getModel());
					model.setImageOptions(dashScopeImageModel.getOptions());
				}
				return model;
			}
		}

		// TODO AudioModel

		log.error("can not find by bean name:{}", modelName);
		throw new NotFoundException();
	}

	@Override
	public ChatModelRunResult run(RunActionParam runActionParam) {
		return ChatModelRunResult.builder().build();
	}

}
