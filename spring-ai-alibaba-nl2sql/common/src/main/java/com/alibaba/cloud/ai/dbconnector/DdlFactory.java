package com.alibaba.cloud.ai.dbconnector;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@AllArgsConstructor
public class DdlFactory {

	private static Map<String, AbstractDdl> ddlExecutorSet = new ConcurrentHashMap<>();

	public static void registry(AbstractDdl ddlExecutor) {
		ddlExecutorSet.put(getConstraint(ddlExecutor.getType()), ddlExecutor);
	}

	public AbstractDdl getDdlExecutor(DbConfig dbConfig) {
		BizDataSourceTypeEnum type = BizDataSourceTypeEnum.fromTypeName(dbConfig.getDialectType());
		if (type == null) {
			throw new RuntimeException("unknown db type");
		}
		return getDdlExecutor(type);
	}

	public AbstractDdl getDdlExecutor(BizDataSourceTypeEnum type) {
		return ddlExecutorSet.get(getConstraint(type));
	}

	private static String getConstraint(BizDataSourceTypeEnum type) {
		return type.getProtocol() + "@" + type.getDialect();
	}

}
