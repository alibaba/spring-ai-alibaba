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
package com.alibaba.cloud.ai.request;

import java.io.Serializable;
import java.util.Objects;

public class DeleteRequest implements Serializable {

	private String id;

	private String vectorType;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getVectorType() {
		return vectorType;
	}

	public void setVectorType(String vectorType) {
		this.vectorType = vectorType;
	}

	@Override
	public String toString() {
		return "DeleteRequest{" + "id='" + id + '\'' + ", vectorType='" + vectorType + '\'' + '}';
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		DeleteRequest that = (DeleteRequest) o;
		return Objects.equals(id, that.id) && Objects.equals(vectorType, that.vectorType);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, vectorType);
	}

}
