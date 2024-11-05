package com.alibaba.cloud.ai.controller;

import com.alibaba.cloud.ai.api.ChatModelAPI;
import com.alibaba.cloud.ai.service.ChatModelDelegate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@CrossOrigin
@RestController
@RequestMapping("studio/api/chat-models")
public class ChatModelAPIController implements ChatModelAPI {

	private final ChatModelDelegate delegate;

	public ChatModelAPIController(@Autowired(required = false) ChatModelDelegate delegate) {
		this.delegate = Optional.ofNullable(delegate).orElse(new ChatModelDelegate() {
		});
	}

	@Override
	public ChatModelDelegate getDelegate() {
		return delegate;
	}

}
