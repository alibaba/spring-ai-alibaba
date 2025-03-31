package com.alibaba.cloud.ai.example.manus.tool;

import java.util.function.BiFunction;

import org.springframework.ai.chat.model.ToolContext;

import com.alibaba.cloud.ai.example.manus.tool.support.ToolExecuteResult;

/**
 * Tool 定义的接口，提供统一的工具定义方法
 */
public interface ToolDefinition extends BiFunction<String, ToolContext, ToolExecuteResult>  {
    
    /**
     * 获取工具的名称
     */
    String getName();
    
    /**
     * 获取工具的描述信息
     */
    String getDescription();
    
    /**
     * 获取工具的参数定义 schema
     */
    String getParameters();
    
    /**
     * 获取工具的输入类型
     */
    Class<?> getInputType();

    boolean isReturnDirect();
}
