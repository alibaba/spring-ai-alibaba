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
package com.alibaba.cloud.ai.studio.core.workflow;

import com.alibaba.cloud.ai.studio.runtime.domain.RequestContext;
import com.alibaba.cloud.ai.studio.runtime.domain.chat.Usage;
import com.alibaba.cloud.ai.studio.runtime.domain.workflow.InvokeSourceEnum;
import com.alibaba.cloud.ai.studio.runtime.domain.workflow.NodeResult;
import com.alibaba.cloud.ai.studio.runtime.utils.JsonUtils;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.collect.Maps;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import com.alibaba.cloud.ai.studio.runtime.domain.Error;

/**
 * Context for workflow execution and management
 *
 * @since 1.0.0.3
 */
@EqualsAndHashCode(callSuper = true)
@Slf4j
@Data
public class WorkflowContext extends RequestContext {

	/** Application identifier */
	private String appId;

	/** Task identifier */
	private String taskId;

	/** Conversation identifier */
	private String conversationId;

	/** Current status of the task */
	private String taskStatus;

	/** Result of the task execution */
	private String taskResult;

	/** Error code if task fails */
	private String errorCode;

	/** Error information if task fails */
	private String errorInfo;

	/** Error object containing detailed error information */
	private Error error;

	/** Map of sub-workflow contexts */
	private HashMap<String, WorkflowContext> subWorkflowContextMap = new HashMap<>();

	/** Source of invocation: api or console */
	private String invokeSource = InvokeSourceEnum.api.getCode();

	/** Collection of user input parameters, e.g., ${user.abc} */
	private Map<String, Object> userMap = Maps.newHashMap();

	/** Collection of system parameters, e.g., ${sys.abc} */
	private Map<String, Object> sysMap = Maps.newHashMap();

	/** Workflow configuration information */
	private WorkflowConfig workflowConfig;

	/** Cache for intermediate variables used in variable substitution */
	private ConcurrentHashMap<String, Object> variablesMap = new ConcurrentHashMap<>();

	/** Cache for node execution results, used for debugging and node evaluation */
	private ConcurrentHashMap<String, NodeResult> nodeResultMap = new ConcurrentHashMap<>();

	/** List of execution order */
	private CopyOnWriteArrayList<String> executeOrderList = new CopyOnWriteArrayList<>();

	/** Lock to ensure single execution of a node at a time */
	@JsonIgnore
	private transient Lock lock = new ReentrantLock();

	/** Set of sub-task IDs (currently only used by agentgroup) */
	private Set<String> subTaskIdSet = new CopyOnWriteArraySet<>();

	/** Usage statistics */
	private List<Usage> usages;

	/** API key identifier */
	private String apikeyId;

	/** Flag indicating if streaming is enabled */
	private boolean stream;

	/** End time of the workflow */
	private long endTime;

	/** Time of first response */
	private long firstResponseTime;

	/** Version number for conflict detection and merging */
	private long version = 1L;

	/**
	 * Creates a deep copy of the workflow context
	 * @param context The context to copy
	 * @return A deep copy of the context
	 */
	public static WorkflowContext deepCopy(WorkflowContext context) {
		try {
			ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
			ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream);
			objectOutputStream.writeObject(context);
			ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(byteArrayOutputStream.toByteArray());
			ObjectInputStream objectInputStream = new ObjectInputStream(byteArrayInputStream);
			WorkflowContext copy = (WorkflowContext) objectInputStream.readObject();
			// Reinitialize lock object after deserialization
			copy.lock = new ReentrantLock();
			// 确保版本号被正确复制
			if (copy.getVersion() == 0) {
				copy.setVersion(1L);
			}
			return copy;
		}
		catch (Exception e) {
			log.error("WorkflowContext deepCopy error:{}", JsonUtils.toJson(context), e);
			return null;
		}
	}

}
