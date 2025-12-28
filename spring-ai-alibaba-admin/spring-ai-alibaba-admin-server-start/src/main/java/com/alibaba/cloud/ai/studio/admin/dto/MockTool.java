package com.alibaba.cloud.ai.studio.admin.dto;


import lombok.Data;
import org.springframework.ai.tool.definition.DefaultToolDefinition;

/**
 * @author zhuoguang
 */
@Data
public class MockTool {
    
    
    private MockToolDefinition toolDefinition;
    
    private String output;
    
}
