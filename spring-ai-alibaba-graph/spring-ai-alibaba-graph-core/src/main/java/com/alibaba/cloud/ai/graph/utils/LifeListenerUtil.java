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
package com.alibaba.cloud.ai.graph.utils;

import com.alibaba.cloud.ai.graph.GraphLifecycleListener;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Deque;
import java.util.Map;

import static com.alibaba.cloud.ai.graph.StateGraph.*;
import static com.alibaba.cloud.ai.graph.StateGraph.NODE_AFTER;
import static com.alibaba.cloud.ai.graph.StateGraph.NODE_BEFORE;

/**
 * Utility class for processing graph lifecycle listeners. This class provides methods to
 * handle various lifecycle events such as start, end, error, and node before/after
 * events.
 */
public class LifeListenerUtil {

	private static final Logger log = LoggerFactory.getLogger(LifeListenerUtil.class);

	/**
	 * Process graph lifecycle listeners in LIFO (Last In, First Out) order. This method
	 * recursively processes all listeners in the deque, invoking the appropriate callback
	 * method based on the scene parameter.
	 * @param currentNodeId The ID of the current node being processed
	 * @param listeners The deque of listeners to process
	 * @param currentState The current state of the graph
	 * @param runnableConfig The configuration for the runnable task
	 * @param scene The scene or event type (START, END, ERROR, NODE_BEFORE, NODE_AFTER)
	 * @param e The exception object (used only in ERROR scene)
	 */
	public static void processListenersLIFO(String currentNodeId, Deque<GraphLifecycleListener> listeners,
			Map<String, Object> currentState, RunnableConfig runnableConfig, String scene, Throwable e) {
		// Base case: if no listeners remain, return
		if (listeners.isEmpty()) {
			return;
		}

		// Retrieve and remove the last listener from the deque (LIFO order)
		GraphLifecycleListener listener = listeners.pollLast();

		try {
			// Invoke the appropriate listener method based on the scene
			if (START.equals(scene)) {
				listener.onStart(START, currentState, runnableConfig);
			}
			else if (END.equals(scene)) {
				listener.onComplete(END, currentState, runnableConfig);
			}
			else if (ERROR.equals(scene)) {
				listener.onError(currentNodeId, currentState, e, runnableConfig);
			}
			else if (NODE_BEFORE.equals(scene)) {
				listener.before(currentNodeId, currentState, runnableConfig, SystemClock.now());
			}
			else if (NODE_AFTER.equals(scene)) {
				listener.after(currentNodeId, currentState, runnableConfig, SystemClock.now());
			}

			// Recursively process remaining listeners
			processListenersLIFO(currentNodeId, listeners, currentState, runnableConfig, scene, e);
		}
		catch (Exception ex) {
			// Log any exceptions that occur during listener processing
			// Note: 'log' should be properly defined in the actual class
			log.debug("Error occurred during listener processing: {}", ex.getMessage(), ex);
		}
	}

}
