package com.alibaba.cloud.ai.service;

import com.alibaba.cloud.ai.model.ChatClient;

import java.util.ArrayList;
import java.util.List;

public interface ChatClientDelegate {

	default List<ChatClient> list() {
		return new ArrayList<>();
	}

	default ChatClient get(String clientName) {
		return null;
	}

}
