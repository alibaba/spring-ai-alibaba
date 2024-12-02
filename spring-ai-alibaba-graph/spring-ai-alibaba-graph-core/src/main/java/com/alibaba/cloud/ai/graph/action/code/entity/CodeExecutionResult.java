package com.alibaba.cloud.ai.graph.action.code.entity;

/**
 * Represents the result of code execution.
 *
 * @param exitCode 0 if the code executes successfully.
 * @param logs the error message if the code fails to execute, the stdout otherwise.
 * @param extra commandLine code_file or the docker image name after container run when
 * docker is used.
 * @author HeYQ
 * @since 0.0.1
 */
public record CodeExecutionResult(int exitCode, String logs, String extra) {

	public CodeExecutionResult(int exitCode, String logs) {
		this(exitCode, logs, null);
	}
}
