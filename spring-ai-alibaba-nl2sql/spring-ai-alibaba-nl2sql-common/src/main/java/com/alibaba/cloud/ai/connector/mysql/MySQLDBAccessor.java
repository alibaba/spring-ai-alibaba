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

package com.alibaba.cloud.ai.connector.mysql;

import com.alibaba.cloud.ai.connector.DBConnectionPool;
import com.alibaba.cloud.ai.connector.accessor.defaults.AbstractAccessor;
import com.alibaba.cloud.ai.connector.support.DdlFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

/**
 * @author yuluo
 * @author <a href="mailto:yuluo08290126@gmail.com">yuluo</a>
 */

@Service("mysqlAccessor")
public class MySQLDBAccessor extends AbstractAccessor {

	private final static String ACCESSOR_TYPE = "MySQL_Accessor";

	protected MySQLDBAccessor(DdlFactory ddlFactory,
			@Qualifier("mysqlJdbcConnectionPool") DBConnectionPool dbConnectionPool) {

		super(ddlFactory, dbConnectionPool);
	}

	@Override
	public String getDbAccessorType() {

		return ACCESSOR_TYPE;
	}

}
