package com.alibaba.cloud.ai.service.impl;

import com.alibaba.cloud.ai.model.ChatClient;
import com.alibaba.cloud.ai.service.ChatClientDelegate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class ChatClientDelegateImpl implements ChatClientDelegate {

	@Override
	public List<ChatClient> list() {
		return new ArrayList<>();
	}

	@Override
	public ChatClient get(String clientName) {
		return null;
	}

}
