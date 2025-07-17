package com.alibaba.cloud.ai.example.manus.config;

/**
 * Interface for Manus configuration properties
 */
public interface IManusProperties {

	/**
	 * Get browser headless setting
	 */
	Boolean getBrowserHeadless();

	/**
	 * Set browser headless setting
	 */
	void setBrowserHeadless(Boolean browserHeadless);

	/**
	 * Get browser request timeout
	 */
	Integer getBrowserRequestTimeout();

	/**
	 * Set browser request timeout
	 */
	void setBrowserRequestTimeout(Integer browserRequestTimeout);

	/**
	 * Get debug detail setting
	 */
	Boolean getDebugDetail();

	/**
	 * Set debug detail setting
	 */
	void setDebugDetail(Boolean debugDetail);

	/**
	 * Get open browser auto setting
	 */
	Boolean getOpenBrowserAuto();

	/**
	 * Set open browser auto setting
	 */
	void setOpenBrowserAuto(Boolean openBrowserAuto);

	/**
	 * Get max steps
	 */
	Integer getMaxSteps();

	/**
	 * Set max steps
	 */
	void setMaxSteps(Integer maxSteps);

	/**
	 * Get force override from yaml setting
	 */
	Boolean getForceOverrideFromYaml();

	/**
	 * Set force override from yaml setting
	 */
	void setForceOverrideFromYaml(Boolean forceOverrideFromYaml);

	/**
	 * Get user input timeout
	 */
	Integer getUserInputTimeout();

	/**
	 * Set user input timeout
	 */
	void setUserInputTimeout(Integer userInputTimeout);

	/**
	 * Get max memory
	 */
	Integer getMaxMemory();

	/**
	 * Set max memory
	 */
	void setMaxMemory(Integer maxMemory);

	/**
	 * Get parallel tool calls setting
	 */
	Boolean getParallelToolCalls();

	/**
	 * Set parallel tool calls setting
	 */
	void setParallelToolCalls(Boolean parallelToolCalls);

	/**
	 * Get base directory
	 */
	String getBaseDir();

	/**
	 * Set base directory
	 */
	void setBaseDir(String baseDir);

}
