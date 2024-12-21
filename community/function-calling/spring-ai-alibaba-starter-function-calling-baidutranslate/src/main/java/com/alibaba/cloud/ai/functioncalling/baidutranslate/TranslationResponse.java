package com.alibaba.cloud.ai.functioncalling.baidutranslate;

import java.util.List;

class TranslationResponse {

	private String from;

	private String to;

	private List<TranslationResult> trans_result;

	public String getFrom() {
		return from;
	}

	public void setFrom(String from) {
		this.from = from;
	}

	public String getTo() {
		return to;
	}

	public void setTo(String to) {
		this.to = to;
	}

	public List<TranslationResult> getTransResult() {
		return trans_result;
	}

	public void setTransResult(List<TranslationResult> trans_result) {
		this.trans_result = trans_result;
	}

}