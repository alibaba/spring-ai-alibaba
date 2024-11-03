package com.alibaba.cloud.ai.service;

import com.alibaba.cloud.ai.model.ChatModel;
import com.alibaba.cloud.ai.param.RunActionParam;
import com.alibaba.cloud.ai.vo.ChatModelRunResult;

import java.util.ArrayList;
import java.util.List;

public interface ChatModelDelegate {

	default List<ChatModel> list() {
		return new ArrayList<>();
	}

	default ChatModel getByModelName(String modelName) {
		return null;
	}

	default ChatModelRunResult run(RunActionParam runActionParam) {
		return null;
	}

}
