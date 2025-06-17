package com.alibaba.cloud.ai.example.deepresearch.repository;

import java.util.List;

public interface ModelParamRepository {

	/**
	 * Load model configuration list
	 */
	List<ModelParamRepositoryImpl.AgentModel> loadModels();

}
