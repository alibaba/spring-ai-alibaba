package com.alibaba.cloud.ai.example.manus.prompt;

/**
 * Interface for prompt loader that handles prompt template loading and caching
 */
public interface IPromptLoader {

	/**
	 * Load prompt template from file
	 */
	String loadPrompt(String promptPath);

	/**
	 * Clear prompt cache
	 */
	void clearCache();

}
