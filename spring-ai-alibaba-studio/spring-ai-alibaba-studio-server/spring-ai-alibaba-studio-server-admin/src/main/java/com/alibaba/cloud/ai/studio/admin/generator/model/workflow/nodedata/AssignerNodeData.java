/*
 * Copyright 2024-2026 the original author or authors.
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

package com.alibaba.cloud.ai.studio.admin.generator.model.workflow.nodedata;

import com.alibaba.cloud.ai.studio.admin.generator.model.VariableSelector;
import com.alibaba.cloud.ai.studio.admin.generator.model.workflow.NodeData;
import com.alibaba.cloud.ai.studio.admin.generator.service.dsl.DSLDialectType;
import com.alibaba.cloud.ai.studio.admin.generator.utils.ObjectToCodeUtil;

import java.util.List;
import java.util.function.Function;

public class AssignerNodeData extends NodeData {

	private List<AssignItem> items;

	public List<AssignItem> getItems() {
		return items;
	}

	public void setItems(List<AssignItem> items) {
		this.items = items;
	}

	public record AssignItem(VariableSelector targetSelector, VariableSelector inputSelector, WriteMode writeMode,
			String inputConst) {
		@Override
		public String toString() {
			return String.format("new AssignerNode.AssignItem(%s, %s, %s, %s)",
					ObjectToCodeUtil
						.toCode(this.targetSelector() != null ? this.targetSelector().getNameInCode() : null),
					ObjectToCodeUtil.toCode(this.inputSelector() != null ? this.inputSelector().getNameInCode() : null),
					ObjectToCodeUtil.toCode(this.writeMode()), ObjectToCodeUtil.toCode(this.inputConst()));
		}
	}

	private static final String UNSUPPORTED = "UNSUPPORTED";

	public enum WriteMode {

		OVER_WRITE(type -> switch (type) {
			case DIFY -> "over-write";
			case STUDIO -> "refer";
			default -> UNSUPPORTED;
		}),

		APPEND(type -> UNSUPPORTED),

		CLEAR(type -> switch (type) {
			case DIFY, STUDIO -> "clear";
			default -> UNSUPPORTED;
		}),

		INPUT_CONSTANT(type -> switch (type) {
			case DIFY -> "set";
			case STUDIO -> "input";
			default -> UNSUPPORTED;
		});

		private final Function<DSLDialectType, String> dslValue;

		WriteMode(Function<DSLDialectType, String> dslValue) {
			this.dslValue = dslValue;
		}

		public static WriteMode fromDslValue(DSLDialectType dialectType, String dslValue) {
			for (WriteMode mode : WriteMode.values()) {
				if (mode.dslValue.apply(dialectType).equals(dslValue)) {
					return mode;
				}
			}
			throw new IllegalArgumentException("Invalid write mode: " + dslValue);
		}

		@Override
		public String toString() {
			return "AssignerNode.WriteMode." + this.name();
		}

	}

}
