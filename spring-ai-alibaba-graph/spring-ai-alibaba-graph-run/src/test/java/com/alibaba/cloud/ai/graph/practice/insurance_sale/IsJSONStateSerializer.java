package com.alibaba.cloud.ai.graph.practice.insurance_sale;

import com.alibaba.cloud.ai.graph.serializer.plain_text.PlainTextStateSerializer;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import dev.ai.alibaba.samples.executor.AgentAction;
import dev.ai.alibaba.samples.executor.AgentFinish;
import dev.ai.alibaba.samples.executor.AgentOutcome;
import dev.ai.alibaba.samples.executor.IntermediateStep;
import lombok.NonNull;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

public class IsJSONStateSerializer extends PlainTextStateSerializer<IsExecutor.State> {

	final ObjectMapper objectMapper;

	public IsJSONStateSerializer() {
		this(new ObjectMapper());
	}

	public IsJSONStateSerializer(@NonNull ObjectMapper objectMapper) {
		super(IsExecutor.State::new);
		this.objectMapper = objectMapper;
		this.objectMapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);

		var module = new SimpleModule();
		module.addDeserializer(IsExecutor.State.class, new IsStateDeserializer());
		module.addDeserializer(AgentOutcome.class, new IsAgentOutcomeDeserializer());
		module.addDeserializer(AgentAction.class, new IsAgentActionDeserializer());
		module.addDeserializer(AgentFinish.class, new IsAgentFinishDeserializer());
		module.addDeserializer(IntermediateStep.class, new IsIntermediateStepDeserializer());

		objectMapper.registerModule(module);
	}

	@Override
	public String mimeType() {
		return "application/json";
	}

	@Override
	public void write(IsExecutor.State object, ObjectOutput out) throws IOException {
		var json = objectMapper.writeValueAsString(object);
		out.writeUTF(json);
	}

	@Override
	public IsExecutor.State read(ObjectInput in) throws IOException, ClassNotFoundException {
		var json = in.readUTF();
		return objectMapper.readValue(json, IsExecutor.State.class);
	}

}
