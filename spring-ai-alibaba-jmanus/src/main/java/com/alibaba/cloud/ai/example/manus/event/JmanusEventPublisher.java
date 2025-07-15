package com.alibaba.cloud.ai.example.manus.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class JmanusEventPublisher {

    private static final Logger logger = LoggerFactory.getLogger(JmanusEventPublisher.class);

    //监听器无法动态注册，无需线程安全
    private Map<Class<? extends JmanusEvent>, List<JmanusListener>> listeners = new HashMap<>();

    public void publish(JmanusEvent event) {
        List<JmanusListener> jmanusListeners = listeners.get(event.getClass());
        for (JmanusListener jmanusListener : jmanusListeners) {
            try {
                jmanusListener.onEvent(event);
            } catch (Exception e) {
                //这里忽略异常，避免影响其他监听器的执行
                logger.error("Error occurred while processing event: {}", e.getMessage());
            }
        }
    }

    void registerListener(Class<? extends JmanusEvent> eventClass, JmanusListener listener) {
        List<JmanusListener> jmanusListeners = listeners.get(eventClass);
        if (jmanusListeners == null) {
            listeners.put(eventClass, List.of(listener));
        } else {
            jmanusListeners.add(listener);
        }
    }
}
