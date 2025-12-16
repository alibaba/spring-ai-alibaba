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

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.streaming.OutputType;
import com.alibaba.cloud.ai.graph.streaming.StreamingOutput;
import org.springframework.ai.chat.messages.Message;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

public class StreamingOutputSerializer implements NullableObjectSerializer<StreamingOutput> {

	private final MessageSerializer messageSerializer = new MessageSerializer();

	@Override
	public void write(StreamingOutput object, ObjectOutput out) throws IOException {
		// Write base NodeOutput fields
		writeNullableUTF(object.node(), out);
		writeNullableUTF(object.agent(), out);
		writeNullableObject(object.state(), out);
		out.writeBoolean(object.isSubGraph());

		// Write StreamingOutput specific fields
		writeNullableUTF(object.chunk(), out);
		
		// Write message using MessageSerializer if present
		Message message = object.message();
		if (message != null) {
			out.writeByte(1);
			messageSerializer.write(message, out);
		} else {
			out.writeByte(0);
		}
		
		// Write outputType if present
		if (object.getOutputType() != null) {
			out.writeByte(1);
			writeNullableUTF(object.getOutputType().name(), out);
		} else {
			out.writeByte(0);
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	public StreamingOutput read(ObjectInput in) throws IOException, ClassNotFoundException {
		// Read base NodeOutput fields
		String node = readNullableUTF(in).orElse(null);
		String agent = readNullableUTF(in).orElse(null);
		OverAllState state = (OverAllState) readNullableObject(in).orElse(null);
		boolean subGraph = in.readBoolean();

		// Read StreamingOutput specific fields
		String chunk = readNullableUTF(in).orElse(null);
		
		// Read message using MessageSerializer if present
		Message message = null;
		byte hasMessage = in.readByte();
		if (hasMessage == 1) {
			message = messageSerializer.read(in);
		}
		
		// Read outputType if present
		OutputType outputType = null;
		byte hasOutputType = in.readByte();
		if (hasOutputType == 1) {
			String outputTypeStr = readNullableUTF(in).orElse(null);
			if (outputTypeStr != null) {
				try {
					outputType = OutputType.valueOf(outputTypeStr);
				} catch (IllegalArgumentException e) {
					// If enum value is not found, outputType remains null
				}
			}
		}

		// Create StreamingOutput with all available fields
		// Prefer constructor with message if message is present
		StreamingOutput<?> output;
		if (message != null) {
			output = new StreamingOutput<>(message, node, agent, state, outputType);
		} else {
			// Fallback to deprecated constructor with chunk (for backward compatibility)
			output = new StreamingOutput<>(chunk, node, agent, state);
		}
		
		output.setSubGraph(subGraph);
		return output;
	}

}

