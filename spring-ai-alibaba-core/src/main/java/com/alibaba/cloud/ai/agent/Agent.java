package com.alibaba.cloud.ai.agent;

import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import reactor.core.publisher.Flux;

/**
 * Title base agent.<br>
 * Description base agent.<br>
 *
 * @author yuanci.ytb
 * @since 1.0.0-M2
 */
public abstract class Agent {

	/**
	 * call with chat model
	 * @param prompt user prompt
	 * @return chat response
	 */
	public abstract ChatResponse call(Prompt prompt);

	/**
	 * stream call with chat model
	 * @param prompt user prompt
	 * @return streaming chat response
	 */
	public abstract Flux<ChatResponse> stream(Prompt prompt);

}
