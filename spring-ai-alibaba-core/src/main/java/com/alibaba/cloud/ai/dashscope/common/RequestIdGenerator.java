package com.alibaba.cloud.ai.dashscope.common;

import java.util.UUID;

/**
 * @author nuocheng.lxm
 * @since 1.0.0-M2
 */
public class RequestIdGenerator implements IdGenerator {

	@Override
	public String generateId(Object... contents) {
		return UUID.randomUUID().toString();
	}

}
