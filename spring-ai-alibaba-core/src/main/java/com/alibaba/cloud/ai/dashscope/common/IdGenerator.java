package com.alibaba.cloud.ai.dashscope.common;

/**
 * @author nuocheng.lxm
 * @since 1.0.0-M2
 */
public interface IdGenerator {

	String generateId(Object... contents);

}
