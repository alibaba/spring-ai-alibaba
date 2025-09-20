// /*
// * Copyright 2025 the original author or authors.
// *
// * Licensed under the Apache License, Version 2.0 (the "License");
// * you may not use this file except in compliance with the License.
// * You may obtain a copy of the License at
// *
// * https://www.apache.org/licenses/LICENSE-2.0
// *
// * Unless required by applicable law or agreed to in writing, software
// * distributed under the License is distributed on an "AS IS" BASIS,
// * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// * See the License for the specific language governing permissions and
// * limitations under the License.
// */
// package com.alibaba.cloud.ai.manus.planning.factory;

// import com.alibaba.cloud.ai.manus.planning.model.vo.PlanType;
// import com.alibaba.cloud.ai.manus.tool.PlanningTool;
// import com.alibaba.cloud.ai.manus.tool.MapReducePlanningTool;
// import org.springframework.ai.tool.function.FunctionToolCallback;
// import org.springframework.ai.openai.api.OpenAiApi.FunctionTool;

// /**
// * Planning tool factory class
// */
// public class PlanningToolFactory {

// private static final PlanningTool simplePlanningTool = new PlanningTool();

// private static final MapReducePlanningTool mapReducePlanningTool = new
// MapReducePlanningTool();

// /**
// * Get corresponding planning tool based on plan type
// * @param planType Plan type
// * @return Corresponding planning tool
// */
// public static Object getPlanningTool(PlanType planType) {
// return switch (planType) {
// case SIMPLE -> simplePlanningTool;
// case MAPREDUCE -> mapReducePlanningTool;
// };
// }

// /**
// * Get corresponding function tool definition based on plan type
// * @param planType Plan type
// * @return Corresponding function tool definition
// */
// public static FunctionTool getFunctionToolDefinition(PlanType planType) {
// return switch (planType) {
// case SIMPLE -> simplePlanningTool.getToolDefinition();
// case MAPREDUCE -> mapReducePlanningTool.getToolDefinition();
// };
// }

// /**
// * Get corresponding function tool callback based on plan type
// * @param planType Plan type
// * @return Corresponding function tool callback
// */
// @SuppressWarnings("rawtypes")
// public static FunctionToolCallback getFunctionToolCallback(PlanType planType) {
// return switch (planType) {
// case SIMPLE -> simplePlanningTool.getFunctionToolCallback();
// case MAPREDUCE -> mapReducePlanningTool.getFunctionToolCallback();
// };
// }

// /**
// * Get simple plan tool instance
// * @return Simple plan tool
// */
// public static PlanningTool getSimplePlanningTool() {
// return simplePlanningTool;
// }

// /**
// * Get MapReduce plan tool instance
// * @return MapReduce plan tool
// */
// public static MapReducePlanningTool getMapReducePlanningTool() {
// return mapReducePlanningTool;
// }

// /**
// * Get all supported plan types
// * @return Plan type array
// */
// public static PlanType[] getSupportedPlanTypes() {
// return PlanType.values();
// }

// /**
// * Check if specified type has active plans
// * @param planType Plan type
// * @return Return true if there are active plans
// */
// public static boolean hasActivePlan(PlanType planType) {
// return switch (planType) {
// case SIMPLE -> simplePlanningTool.getCurrentPlanId() != null;
// case MAPREDUCE -> mapReducePlanningTool.getCurrentPlanId() != null;
// };
// }

// /**
// * Get detailed information string for plan type
// * @param planType Plan type
// * @return Detailed information string
// */
// public static String getPlanTypeInfo(PlanType planType) {
// return planType.getDisplayName() + ": " + planType.getDescription();
// }

// }
