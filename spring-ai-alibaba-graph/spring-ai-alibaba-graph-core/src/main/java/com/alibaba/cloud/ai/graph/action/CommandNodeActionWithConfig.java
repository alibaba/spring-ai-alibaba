package com.alibaba.cloud.ai.graph.action;

import com.alibaba.cloud.ai.graph.Command;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.RunnableConfig;

/**
 * The interface Command node action with config.
 *
 */
@FunctionalInterface
public interface CommandNodeActionWithConfig {

	/**
	 * Apply command.
	 * @param t the t
	 * @param config the config
	 * @return the command
	 * @throws Exception the exception
	 */
	Command apply(OverAllState t, RunnableConfig config) throws Exception;

}
