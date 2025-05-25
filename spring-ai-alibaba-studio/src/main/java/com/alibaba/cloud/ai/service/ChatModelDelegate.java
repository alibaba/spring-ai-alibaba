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

package com.alibaba.cloud.ai.service;

import com.alibaba.cloud.ai.common.ModelType;
import com.alibaba.cloud.ai.model.ChatModel;
import com.alibaba.cloud.ai.param.ModelRunActionParam;
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

	default ChatModelRunResult run(ModelRunActionParam runActionParam) {
		return null;
	}

	default String runImageGenTask(ModelRunActionParam modelRunActionParam) {
		return null;
	}

	default ChatModelRunResult runImageGenTaskAndGetUrl(ModelRunActionParam modelRunActionParam) {
		return null;
	}

	default List<String> listModelNames(ModelType modelType) {
		return new ArrayList<>();
	}

}
