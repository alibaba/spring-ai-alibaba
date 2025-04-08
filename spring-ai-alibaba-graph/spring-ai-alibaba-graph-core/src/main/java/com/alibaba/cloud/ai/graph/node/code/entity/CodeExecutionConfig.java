package com.alibaba.cloud.ai.graph.node.code.entity;

public class CodeExecutionConfig {

	private String workDir = "extensions";

	/**
	 * the docker image to use for code execution.
	 */
	private String docker;

	private int timeout = 600;

	private int lastMessagesNumber = 1;

	private int codeMaxDepth = 5;

	public String getWorkDir() {
		return workDir;
	}

	public CodeExecutionConfig setWorkDir(String workDir) {
		this.workDir = workDir;
		return this;
	}

	public String getDocker() {
		return docker;
	}

	public CodeExecutionConfig setDocker(String docker) {
		this.docker = docker;
		return this;
	}

	public int getTimeout() {
		return timeout;
	}

	public CodeExecutionConfig setTimeout(int timeout) {
		this.timeout = timeout;
		return this;
	}

	public int getLastMessagesNumber() {
		return lastMessagesNumber;
	}

	public CodeExecutionConfig setLastMessagesNumber(int lastMessagesNumber) {
		this.lastMessagesNumber = lastMessagesNumber;
		return this;
	}

	public int getCodeMaxDepth() {
		return codeMaxDepth;
	}

	public CodeExecutionConfig setCodeMaxDepth(int codeMaxDepth) {
		this.codeMaxDepth = codeMaxDepth;
		return this;
	}

}
