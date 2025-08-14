package com.alibaba.cloud.ai.studio.core.model.llm.impl;

import com.alibaba.cloud.ai.studio.runtime.domain.model.CredentialSpec;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Provider implementation for Tongyi LLM service
 */
@Component("TongyiProvider")
public class TongyiProvider extends OpenAIProvider {

	/**
	 * Returns the provider code
	 */
	@Override
	public String getCode() {
		return "Tongyi";
	}

	/**
	 * Returns the provider name
	 */
	@Override
	public String getName() {
		return "通义";
	}

	/**
	 * Returns the provider description
	 */
	@Override
	public String getDescription() {
		return "通义模型服务";
	}

	/**
	 * Returns the credential specifications required for authentication
	 * @return List of credential specifications
	 */
	@Override
	public List<CredentialSpec> getCredentialSpecs() {
		CredentialSpec credentialSpec = new CredentialSpec();
		credentialSpec.setCode("api_key");
		credentialSpec.setDisplayName("验证凭证");
		credentialSpec.setDescription("需要填写apiKey验证凭证才能调用远程模型服务");
		credentialSpec.setPlaceHolder("请填写apiKey");
		credentialSpec.setSensitive(true);
		return List.of(credentialSpec);
	}

}
