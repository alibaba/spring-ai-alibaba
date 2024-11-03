package com.alibaba.cloud.ai.service;

import com.alibaba.cloud.ai.model.ChatModel;
import com.alibaba.cloud.ai.param.RunActionParam;
import com.alibaba.cloud.ai.vo.ChatModelRunResult;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

public interface ChatModelDelegate {

	default List<ChatModel> list() {
		return null;
	}

	default ChatModel getByModelName(String modelName) {
		return null;
	}

	default ChatModelRunResult run(RunActionParam runActionParam) {
		return null;
	}

}
