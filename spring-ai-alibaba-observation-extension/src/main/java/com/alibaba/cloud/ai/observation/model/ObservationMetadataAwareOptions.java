package com.alibaba.cloud.ai.observation.model;

import java.util.Map;

public interface ObservationMetadataAwareOptions {

	/**
	 * Gets observation metadata.
	 * @return the observation metadata
	 */
	public Map<String, String> getObservationMetadata();

	/**
	 * Sets observation metadata.
	 * @param observationMetadata the observation metadata
	 */
	public void setObservationMetadata(Map<String, String> observationMetadata);

}
