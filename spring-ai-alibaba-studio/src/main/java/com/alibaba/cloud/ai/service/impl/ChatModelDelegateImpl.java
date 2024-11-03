package com.alibaba.cloud.ai.service.impl;

import com.alibaba.cloud.ai.model.ChatModel;
import com.alibaba.cloud.ai.param.RunActionParam;
import com.alibaba.cloud.ai.service.ChatModelDelegate;
import com.alibaba.cloud.ai.vo.ChatModelRunResult;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class ChatModelDelegateImpl implements ChatModelDelegate {

	@Override
	public List<ChatModel> list() {
		return new ArrayList<>();
	}

	@Override
	public ChatModel getByModelName(String modelName) {
		return null;
	}

	@Override
	public ChatModelRunResult run(RunActionParam runActionParam) {
		return ChatModelRunResult.builder().build();
	}

}
