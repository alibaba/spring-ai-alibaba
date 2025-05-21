package com.alibaba.cloud.ai.request;

import lombok.Data;

import java.io.Serializable;

@Data
public class SearchRequest implements Serializable {

	private String query;

	private int topK;

	private String vectorType;

	private String filterFormatted;

}
