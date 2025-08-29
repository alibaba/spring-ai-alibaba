package com.alibaba.cloud.ai.agent.nacos;

import com.alibaba.cloud.ai.agent.nacos.vo.McpServersVO;
import com.alibaba.cloud.ai.mcp.gateway.nacos.callback.NacosMcpGatewayToolCallback;
import com.alibaba.cloud.ai.mcp.gateway.nacos.definition.NacosMcpGatewayToolDefinition;
import com.alibaba.fastjson.JSON;
import com.alibaba.nacos.api.ai.model.mcp.McpServerDetailInfo;
import com.alibaba.nacos.api.ai.model.mcp.McpServerRemoteServiceConfig;
import com.alibaba.nacos.api.ai.model.mcp.McpTool;
import com.alibaba.nacos.api.ai.model.mcp.McpToolMeta;
import com.alibaba.nacos.api.ai.model.mcp.McpToolSpecification;
import com.alibaba.nacos.api.exception.NacosException;
import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.ToolCallback;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class NacosMcpToolsInjector {
    
    private static final Logger logger = LoggerFactory.getLogger(NacosMcpToolsInjector.class);
    
    public static List<ToolCallback> loadMcpTools(NacosOptions nacosOptions, String agentId) {
        McpServersVO mcpServersVO = getMcpServersVO(nacosOptions, agentId);
        if (mcpServersVO != null) {
            return convert(nacosOptions, mcpServersVO);
        }
        return null;
    }
    
    public static McpServersVO getMcpServersVO(NacosOptions nacosOptions, String agentId) {
        try {
            String config = nacosOptions.getNacosConfigService()
                    .getConfig(String.format("mcp-servers-%s.json", agentId), "nacos-ai-agent", 3000L);
            return JSON.parseObject(config, McpServersVO.class);
        } catch (NacosException e) {
            throw new RuntimeException(e);
        }
    }
    
    public static List<ToolCallback> convert(NacosOptions nacosOptions, McpServersVO mcpServersVO) {
        List<ToolCallback> toolCallbacks = new ArrayList<>();
        for (McpServersVO.McpServerVO mcpServerVO : mcpServersVO.getMcpServers()) {
            try {
                McpServerDetailInfo mcpServerDetail = nacosOptions.getNacosAiMaintainerService()
                        .getMcpServerDetail(nacosOptions.getMcpNamespace(), mcpServerVO.getMcpServerName(),null);
                List<ToolCallback> convert = convert(mcpServerDetail, mcpServerVO);
                if (convert != null) {
                    toolCallbacks.addAll(convert);
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            
        }
        return toolCallbacks;
    }
    
    private static List<ToolCallback> convert(McpServerDetailInfo serviceDetail, McpServersVO.McpServerVO mcpServerVO) {
        
        String protocol = serviceDetail.getProtocol();
        if ("mcp-sse".equalsIgnoreCase(protocol) || "mcp-streamable".equalsIgnoreCase(protocol)) {
            List<ToolCallback> tools = parseToolsFromMcpServerDetailInfo(serviceDetail);
            if (CollectionUtils.isEmpty(tools)) {
                logger.warn("No tools defined for service: {}", serviceDetail.getName());
                return null;
            }
            return tools.stream().filter(a -> (!mcpServerVO.getBlackTools().contains(a.getToolDefinition().name())))
                    .collect(Collectors.toList());
            
        }
        return null;
    }
    
    private static List<ToolCallback> parseToolsFromMcpServerDetailInfo(McpServerDetailInfo mcpServerDetailInfo) {
        try {
            McpToolSpecification toolSpecification = mcpServerDetailInfo.getToolSpec();
            String protocol = mcpServerDetailInfo.getProtocol();
            McpServerRemoteServiceConfig mcpServerRemoteServiceConfig = mcpServerDetailInfo.getRemoteServerConfig();
            List<ToolCallback> toolCallbacks = new ArrayList<>();
            if (toolSpecification != null) {
                List<McpTool> toolsList = toolSpecification.getTools();
                Map<String, McpToolMeta> toolsMeta = toolSpecification.getToolsMeta();
                if (toolsList == null || toolsMeta == null) {
                    return new ArrayList<>();
                }
                for (McpTool tool : toolsList) {
                    String toolName = tool.getName();
                    String toolDescription = tool.getDescription();
                    Map<String, Object> inputSchema = tool.getInputSchema();
                    McpToolMeta metaInfo = toolsMeta.get(toolName);
                    boolean enabled = metaInfo == null || metaInfo.isEnabled();
                    if (!enabled) {
                        logger.info("Tool {} is disabled by metaInfo, skipping.", toolName);
                        continue;
                    }
                    NacosMcpGatewayToolDefinition toolDefinition = NacosMcpGatewayToolDefinition.builder()
                            .name(mcpServerDetailInfo.getName() + "_tools_" + toolName).description(toolDescription)
                            .inputSchema(inputSchema).protocol(protocol)
                            .remoteServerConfig(mcpServerRemoteServiceConfig).toolsMeta(metaInfo).build();
                    toolCallbacks.add(new NacosMcpGatewayToolCallback(toolDefinition));
                }
            }
            return toolCallbacks;
        } catch (Exception e) {
            logger.warn("Failed to get or parse nacos mcp service tools info (mcpName {})",
                    mcpServerDetailInfo.getName() + mcpServerDetailInfo.getVersionDetail().getVersion(), e);
        }
        return null;
    }
}
