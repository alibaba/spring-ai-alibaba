package com.alibaba.cloud.ai.example.manus.event;

/**
 * @author dahua
 * @time 2025/7/15
 * @desc jmanus 事件监听器
 */
public interface JmanusListener<T extends JmanusEvent> {

	void onEvent(T event);

}
