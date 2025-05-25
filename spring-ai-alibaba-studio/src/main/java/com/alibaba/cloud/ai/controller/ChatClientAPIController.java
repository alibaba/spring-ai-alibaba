/*
 * Copyright 2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.alibaba.cloud.ai.controller;

import com.alibaba.cloud.ai.api.ChatClientAPI;
import com.alibaba.cloud.ai.service.ChatClientDelegate;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@CrossOrigin
@RestController
@RequestMapping("studio/api/chat-clients")
public class ChatClientAPIController implements ChatClientAPI {

	private final ChatClientDelegate delegate;

	public ChatClientAPIController(@Autowired(required = false) ChatClientDelegate delegate) {
		this.delegate = Optional.ofNullable(delegate).orElse(new ChatClientDelegate() {
		});
	}

	@Override
	public ChatClientDelegate getDelegate() {
		return delegate;
	}

}
