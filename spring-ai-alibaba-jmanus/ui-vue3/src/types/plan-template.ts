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
