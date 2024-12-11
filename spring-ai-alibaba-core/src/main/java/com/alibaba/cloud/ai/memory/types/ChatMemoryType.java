package com.alibaba.cloud.ai.memory.types;

import java.util.List;

import org.springframework.ai.chat.messages.Message;

/**
 * @author yuluo
 * @author <a href="mailto:yuluo08290126@gmail.com">yuluo</a>
 */

public interface ChatMemoryType {

	String getName();

	List<Message> findSystemMessages();

	void add(Message e);

	void clear(Message e);

	List<Message> message();

}
