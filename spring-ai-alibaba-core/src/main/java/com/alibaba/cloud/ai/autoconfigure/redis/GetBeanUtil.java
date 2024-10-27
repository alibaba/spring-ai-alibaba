package com.alibaba.cloud.ai.autoconfigure.redis;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

/**
 * @author HeYQ
 * @version 1.0
 * @date 2024-10-27 17:12
 * @describe
 */
@Component
public class GetBeanUtil implements ApplicationContextAware {
    private static ApplicationContext applicationContext = null;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        if (GetBeanUtil.applicationContext == null) {
            GetBeanUtil.applicationContext = applicationContext;
        }
    }

    /**
     * @return ApplicationContext
     */
    public static ApplicationContext getApplicationContext() {
        return applicationContext;
    }

    /**
     * @param beanName beanName
     * @return bean
     */
    public static Object getBean(String beanName) {
        return applicationContext.getBean(beanName);
    }

    /**
     * @param c c
     * @param <T> 泛型
     * @return bean
     */
    public static <T> T getBean(Class<T> c) {
        return applicationContext.getBean(c);
    }

    /**
     * @param c c
     * @param  name
     * @param <T>
     * @return T
     */
    public static <T> T getBean(String name, Class<T> c) {
        return getApplicationContext().getBean(name, c);
    }
}
