package com.alibaba.cloud.ai.agent.nacos;

import io.micrometer.observation.ObservationRegistry;
import lombok.Data;

import org.springframework.ai.chat.client.observation.ChatClientObservationConvention;
import org.springframework.ai.chat.observation.ChatModelObservationConvention;
import org.springframework.ai.model.tool.ToolCallingManager;

@Data
public class ObservationConfigration {

	private ObservationRegistry observationRegistry;

	private ToolCallingManager toolCallingManager;

	private ChatModelObservationConvention chatModelObservationConvention;

	private ChatClientObservationConvention chatClientObservationConvention;

	public ObservationConfigration(ObservationRegistry observationRegistry, ToolCallingManager toolCallingManager,
			ChatModelObservationConvention chatModelObservationConvention,
			ChatClientObservationConvention chatClientObservationConvention) {
		this.observationRegistry = observationRegistry;
		this.toolCallingManager = toolCallingManager;
		this.chatModelObservationConvention = chatModelObservationConvention;
		this.chatClientObservationConvention = chatClientObservationConvention;
	}
}
