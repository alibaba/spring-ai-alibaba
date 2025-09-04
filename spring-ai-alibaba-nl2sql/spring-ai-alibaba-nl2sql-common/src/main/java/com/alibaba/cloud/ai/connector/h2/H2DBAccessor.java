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

package com.alibaba.cloud.ai.connector.h2;

import com.alibaba.cloud.ai.connector.DBConnectionPool;
import com.alibaba.cloud.ai.connector.accessor.defaults.AbstractAccessor;
import com.alibaba.cloud.ai.connector.support.DdlFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

/**
 * @author HunterPorter
 * @author <a href="mailto:zongpeng_hzp@163.com">HunterPorter</a>
 */

@Service("h2Accessor")
public class H2DBAccessor extends AbstractAccessor {

	private final static String ACCESSOR_TYPE = "H2_Accessor";

	protected H2DBAccessor(DdlFactory ddlFactory,
			@Qualifier("h2JdbcConnectionPool") DBConnectionPool dbConnectionPool) {

		super(ddlFactory, dbConnectionPool);
	}

	@Override
	public String getDbAccessorType() {

		return ACCESSOR_TYPE;
	}

}
