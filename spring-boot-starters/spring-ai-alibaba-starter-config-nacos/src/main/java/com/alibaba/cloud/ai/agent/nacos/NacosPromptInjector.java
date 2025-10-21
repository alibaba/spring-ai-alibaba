/*
 * Copyright 2024-2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.alibaba.cloud.ai.agent.nacos;

import com.alibaba.cloud.ai.agent.nacos.vo.PromptVO;
import com.alibaba.fastjson.JSON;

public class NacosPromptInjector {
	
	/**
	 * load promot by prompt key.
	 */
	public static PromptVO getPromptByKey(NacosOptions nacosOptions, String promptKey) {

		try {
			String dataId = (nacosOptions.isPromptEncrypted() ? "cipher-kms-aes-256-" : "") + String.format("prompt-%s.json", promptKey);
			String promptConfig = nacosOptions.getNacosConfigService()
					.getConfig(dataId, "nacos-ai-meta",
							3000L);
			PromptVO promptVO = JSON.parseObject(promptConfig, PromptVO.class);
			promptVO.setPromptKey(promptKey);
			return promptVO;
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

}
