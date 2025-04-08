package com.alibaba.cloud.ai.example.manus.tool.textOperator;

public class FileState {

	private String currentFilePath = "";

	private String lastOperationResult = "";

	private final Object fileLock = new Object();

	public String getCurrentFilePath() {
		return currentFilePath;
	}

	public void setCurrentFilePath(String currentFilePath) {
		this.currentFilePath = currentFilePath;
	}

	public String getLastOperationResult() {
		return lastOperationResult;
	}

	public void setLastOperationResult(String lastOperationResult) {
		this.lastOperationResult = lastOperationResult;
	}

	public Object getFileLock() {
		return fileLock;
	}

}
