/*
 * Copyright 2024-2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.cloud.ai.graph.serializer.std;

import com.alibaba.cloud.ai.graph.serializer.Serializer;

import org.springframework.ai.chat.messages.ToolResponseMessage;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.List;
import java.util.Map;

class ToolResponseMessageSerializer implements Serializer<ToolResponseMessage> {

	@Override
	public void write(ToolResponseMessage object, ObjectOutput out) throws IOException {
		out.writeObject(object.getResponses());
		out.writeObject(object.getMetadata());
	}

	@Override
	@SuppressWarnings("unchecked")
	public ToolResponseMessage read(ObjectInput in) throws IOException, ClassNotFoundException {
		var response = (List<ToolResponseMessage.ToolResponse>) in.readObject();
		var metadata = (Map<String, Object>) in.readObject();
		return new ToolResponseMessage(response, metadata);
	}

}
