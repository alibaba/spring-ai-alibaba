package com.alibaba.cloud.ai.graph.serializer.plain_text;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Reader;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import com.alibaba.cloud.ai.graph.OverAllState;
import lombok.NonNull;
import com.alibaba.cloud.ai.graph.serializer.StateSerializer;
import com.alibaba.cloud.ai.graph.state.AgentState;
import com.alibaba.cloud.ai.graph.state.AgentStateFactory;

public abstract class PlainTextStateSerializer extends StateSerializer<OverAllState> {

	protected PlainTextStateSerializer(@NonNull AgentStateFactory<OverAllState> stateFactory) {
		super(stateFactory);
	}

	@Override
	public String mimeType() {
		return "plain/text";
	}

	@SuppressWarnings("unchecked")
	public Class<OverAllState> getStateType() {
		Type superClass = getClass().getGenericSuperclass();
		if (superClass instanceof ParameterizedType) {
			ParameterizedType parameterizedType = (ParameterizedType) superClass;
			Type[] typeArguments = parameterizedType.getActualTypeArguments();
			if (typeArguments.length > 0) {
				return (Class<OverAllState>) typeArguments[0];
			}
		}
		throw new IllegalStateException("Unable to determine state type");
	}

	public OverAllState read(String data) throws IOException, ClassNotFoundException {
		ByteArrayOutputStream bytesStream = new ByteArrayOutputStream();

		try (ObjectOutputStream out = new ObjectOutputStream(bytesStream)) {
			out.writeUTF(data);
			out.flush();
		}

		try (ObjectInput in = new ObjectInputStream(new ByteArrayInputStream(bytesStream.toByteArray()))) {
			return read(in);
		}

	}

	public OverAllState read(Reader reader) throws IOException, ClassNotFoundException {
		StringBuilder sb = new StringBuilder();
		try (BufferedReader bufferedReader = new BufferedReader(reader)) {
			String line;
			while ((line = bufferedReader.readLine()) != null) {
				sb.append(line).append(System.lineSeparator());
			}
		}
		return read(sb.toString());
	}

}
