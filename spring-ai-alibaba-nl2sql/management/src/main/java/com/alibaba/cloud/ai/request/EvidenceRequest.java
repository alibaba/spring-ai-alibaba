package com.alibaba.cloud.ai.request;

import lombok.Data;

import java.io.Serializable;

@Data
public class EvidenceRequest implements Serializable {

	private String content;

	private Integer type;

}