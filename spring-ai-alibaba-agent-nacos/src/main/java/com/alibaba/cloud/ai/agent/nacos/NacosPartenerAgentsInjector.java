package com.alibaba.cloud.ai.agent.nacos;

import com.alibaba.cloud.ai.agent.nacos.vo.PartnerAgentsVO;
import com.alibaba.cloud.ai.graph.node.LlmNode;
import com.alibaba.cloud.ai.graph.node.ToolNode;
import com.alibaba.fastjson.JSON;
import com.alibaba.nacos.api.config.listener.AbstractListener;
import com.alibaba.nacos.api.exception.NacosException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NacosPartenerAgentsInjector {

	private static final Logger logger = LoggerFactory.getLogger(NacosPartenerAgentsInjector.class);

	public static void registry(LlmNode llmNode, ToolNode toolNode, NacosOptions nacosOptions, String agentName) {

		try {
			nacosOptions.getNacosConfigService()
					.addListener("parterner-agents.json", "ai-agent-" + agentName, new AbstractListener() {
						@Override
						public void receiveConfigInfo(String configInfo) {
							PartnerAgentsVO partnerAgentsVO = JSON.parseObject(configInfo, PartnerAgentsVO.class);
							System.out.println(partnerAgentsVO);
						}
					});
		}
		catch (NacosException e) {
			throw new RuntimeException(e);
		}

	}

	public static PartnerAgentsVO getPartenerVO(NacosOptions nacosOptions, String agentName) {
		try {
			String config = nacosOptions.getNacosConfigService()
					.getConfig("parterner-agents.json", "ai-agent-" + agentName, 3000L);
			return JSON.parseObject(config, PartnerAgentsVO.class);
		}
		catch (NacosException e) {
			throw new RuntimeException(e);
		}
	}

}
