package com.alibaba.cloud.ai.example.manus.dynamic.agent.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface DynamicAgentDefinition {
    String agentName();
    String agentDescription();
    String systemPrompt();
    String nextStepPrompt();
    String[] availableToolKeys();
}
