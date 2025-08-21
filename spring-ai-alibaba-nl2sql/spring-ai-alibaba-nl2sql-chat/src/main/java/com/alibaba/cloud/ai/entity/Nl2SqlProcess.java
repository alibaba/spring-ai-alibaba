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

package com.alibaba.cloud.ai.entity;

import com.alibaba.cloud.ai.graph.StateGraph;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 用来记录NL2SQL中间过程的类
 *
 * @author vlsmb
 * @since 2025/8/14
 */
public class Nl2SqlProcess {

	/**
	 * 标识过程是否结束
	 */
	@JsonProperty("finished")
	Boolean finished;

	/**
	 * 标识是否成功
	 */
	@JsonProperty("succeed")
	Boolean succeed;

	/**
	 * 当isFinished为true时本字段有效。isSucceed为true则为生成SQL结果，为false则为错误原因
	 */
	@JsonProperty("result")
	String result;

	/**
	 * 当前运行节点名称
	 */
	@JsonProperty("current_node_name")
	String currentNodeName;

	/**
	 * 当前运行节点输出
	 */
	@JsonProperty("current_node_output")
	String currentNodeOutput;

	public Nl2SqlProcess() {

	}

	public Nl2SqlProcess(Boolean finished, Boolean succeed, String result, String currentNodeName,
			String currentNodeOutput) {
		this.finished = finished;
		this.succeed = succeed;
		this.result = result;
		this.currentNodeName = currentNodeName;
		this.currentNodeOutput = currentNodeOutput;
	}

	public static Nl2SqlProcess success(String result, String currentNodeName, String currentNodeOutput) {
		return new Nl2SqlProcess(true, true, result, currentNodeName, currentNodeOutput);
	}

	public static Nl2SqlProcess success(String result) {
		return success(result, StateGraph.END, "");
	}

	public static Nl2SqlProcess fail(String reason, String currentNodeName, String currentNodeOutput) {
		return new Nl2SqlProcess(true, false, reason, currentNodeName, currentNodeOutput);
	}

	public static Nl2SqlProcess fail(String reason) {
		return fail(reason, StateGraph.END, "");
	}

	public static Nl2SqlProcess processing(String currentNodeName, String currentNodeOutput) {
		return new Nl2SqlProcess(false, false, "", currentNodeName, currentNodeOutput);
	}

	public Boolean getFinished() {
		return finished;
	}

	public void setFinished(Boolean finished) {
		this.finished = finished;
	}

	public Boolean getSucceed() {
		return succeed;
	}

	public void setSucceed(Boolean succeed) {
		this.succeed = succeed;
	}

	public String getResult() {
		return result;
	}

	public void setResult(String result) {
		this.result = result;
	}

	public String getCurrentNodeName() {
		return currentNodeName;
	}

	public void setCurrentNodeName(String currentNodeName) {
		this.currentNodeName = currentNodeName;
	}

	public String getCurrentNodeOutput() {
		return currentNodeOutput;
	}

	public void setCurrentNodeOutput(String currentNodeOutput) {
		this.currentNodeOutput = currentNodeOutput;
	}

}
