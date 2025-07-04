/*
 * Copyright 2025 the original author or authors.
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

// 计划模板相关类型定义

export interface PlanTemplate {
  id: string
  title?: string
  description?: string
  createTime: string
  updateTime?: string
  planJson?: string
  prompt?: string
  params?: string
}

export interface PlanTemplateListResponse {
  count: number
  templates: PlanTemplate[]
}

export interface PlanVersionsResponse {
  versions: string[]
}

export interface PlanTemplateEvents {
  planTemplateSelected: [payload: { templateId: string; template: PlanTemplate }]
  planTemplateDeleted: [payload: { templateId: string }]
  newTaskRequested: []
  planVersionsLoaded: [payload: { templateId: string; versions: string[] }]
  planParamsChanged: [payload: { prompt: string; params: string }]
  jsonContentSet: [payload: { content: string }]
  jsonContentClear: []
  planTemplateConfigRequested: [payload: { templateId: string; template: PlanTemplate }]
  configTabClosed: []
  planExecutionRequested: [payload: { title: string; planData: any; params?: string }]
}
