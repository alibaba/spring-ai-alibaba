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
// package com.alibaba.cloud.ai.example.manus.planning.factory;

// import com.alibaba.cloud.ai.example.manus.planning.model.vo.PlanType;
// import com.alibaba.cloud.ai.example.manus.tool.PlanningTool;
// import com.alibaba.cloud.ai.example.manus.tool.MapReducePlanningTool;
// import org.springframework.ai.tool.function.FunctionToolCallback;
// import org.springframework.ai.openai.api.OpenAiApi.FunctionTool;

// /**
// * 计划工具工厂类
// */
// public class PlanningToolFactory {

// private static final PlanningTool simplePlanningTool = new PlanningTool();

// private static final MapReducePlanningTool mapReducePlanningTool = new
// MapReducePlanningTool();

// /**
// * 根据计划类型获取相应的计划工具
// * @param planType 计划类型
// * @return 对应的计划工具
// */
// public static Object getPlanningTool(PlanType planType) {
// return switch (planType) {
// case SIMPLE -> simplePlanningTool;
// case MAPREDUCE -> mapReducePlanningTool;
// };
// }

// /**
// * 根据计划类型获取相应的功能工具定义
// * @param planType 计划类型
// * @return 对应的功能工具定义
// */
// public static FunctionTool getFunctionToolDefinition(PlanType planType) {
// return switch (planType) {
// case SIMPLE -> simplePlanningTool.getToolDefinition();
// case MAPREDUCE -> mapReducePlanningTool.getToolDefinition();
// };
// }

// /**
// * 根据计划类型获取相应的功能工具回调
// * @param planType 计划类型
// * @return 对应的功能工具回调
// */
// @SuppressWarnings("rawtypes")
// public static FunctionToolCallback getFunctionToolCallback(PlanType planType) {
// return switch (planType) {
// case SIMPLE -> simplePlanningTool.getFunctionToolCallback();
// case MAPREDUCE -> mapReducePlanningTool.getFunctionToolCallback();
// };
// }

// /**
// * 获取简单计划工具实例
// * @return 简单计划工具
// */
// public static PlanningTool getSimplePlanningTool() {
// return simplePlanningTool;
// }

// /**
// * 获取MapReduce计划工具实例
// * @return MapReduce计划工具
// */
// public static MapReducePlanningTool getMapReducePlanningTool() {
// return mapReducePlanningTool;
// }

// /**
// * 获取所有支持的计划类型
// * @return 计划类型数组
// */
// public static PlanType[] getSupportedPlanTypes() {
// return PlanType.values();
// }

// /**
// * 检查指定类型是否有活跃的计划
// * @param planType 计划类型
// * @return 如果有活跃计划则返回true
// */
// public static boolean hasActivePlan(PlanType planType) {
// return switch (planType) {
// case SIMPLE -> simplePlanningTool.getCurrentPlanId() != null;
// case MAPREDUCE -> mapReducePlanningTool.getCurrentPlanId() != null;
// };
// }

// /**
// * 获取计划类型的详细信息字符串
// * @param planType 计划类型
// * @return 详细信息字符串
// */
// public static String getPlanTypeInfo(PlanType planType) {
// return planType.getDisplayName() + ": " + planType.getDescription();
// }

// }
