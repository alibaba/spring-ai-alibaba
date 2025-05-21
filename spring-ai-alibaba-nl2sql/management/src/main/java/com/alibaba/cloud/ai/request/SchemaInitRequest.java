package com.alibaba.cloud.ai.request;

import com.alibaba.cloud.ai.dbconnector.DbConfig;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class SchemaInitRequest implements Serializable {

	private DbConfig dbConfig;

	private List<String> tables;

}