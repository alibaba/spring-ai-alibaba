package com.alibaba.cloud.ai.graph.serializer.std;

import java.io.IOException;
import java.io.ObjectOutput;
import java.util.Objects;
import java.util.Optional;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import com.alibaba.cloud.ai.graph.serializer.Serializer;

@Slf4j
class ObjectOutputWithMapper implements ObjectOutput {

	private final ObjectOutput out;

	private final SerializerMapper mapper;

	public ObjectOutputWithMapper(@NonNull ObjectOutput out, @NonNull SerializerMapper mapper) {
		this.out = out;
		this.mapper = mapper;
	}

	@Override
	public void writeObject(Object obj) throws IOException {
		Objects.requireNonNull(obj, "object to serialize cannot be null");

		Optional<Serializer<Object>> serializer = mapper.getSerializer(obj.getClass());

		if (serializer.isPresent()) {
			out.writeObject(obj.getClass());
			serializer.get().write(obj, this);
		}
		else {
			out.writeObject(obj);
		}
		// check if written by serializer
		out.flush();
	}

	@Override
	public void write(int b) throws IOException {
		out.write(b);
	}

	@Override
	public void write(byte[] b) throws IOException {
		out.write(b);
	}

	@Override
	public void write(byte[] b, int off, int len) throws IOException {
		out.write(b, off, len);
	}

	@Override
	public void flush() throws IOException {
		out.flush();
	}

	@Override
	public void close() throws IOException {
		out.close();
	}

	@Override
	public void writeBoolean(boolean v) throws IOException {
		out.writeBoolean(v);
	}

	@Override
	public void writeByte(int v) throws IOException {
		out.writeByte(v);
	}

	@Override
	public void writeShort(int v) throws IOException {
		out.writeShort(v);
	}

	@Override
	public void writeChar(int v) throws IOException {
		out.writeChar(v);
	}

	@Override
	public void writeInt(int v) throws IOException {
		out.writeInt(v);
	}

	@Override
	public void writeLong(long v) throws IOException {
		out.writeLong(v);
	}

	@Override
	public void writeFloat(float v) throws IOException {
		out.writeFloat(v);
	}

	@Override
	public void writeDouble(double v) throws IOException {
		out.writeDouble(v);
	}

	@Override
	public void writeBytes(String s) throws IOException {
		out.writeBytes(s);
	}

	@Override
	public void writeChars(String s) throws IOException {
		out.writeChars(s);
	}

	@Override
	public void writeUTF(String s) throws IOException {
		out.writeUTF(s);
	}

}
