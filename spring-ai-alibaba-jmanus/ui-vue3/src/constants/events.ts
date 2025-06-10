// // 统一管理全局和常用事件名
// // 参考 ui-event-common.js，提供统一的事件名和事件总线（发布订阅）实现

// export const EVENTS = {
//   MESSAGE_UPDATE: 'ui:message:update',
//   MESSAGE_COMPLETE: 'ui:message:complete',
//   SECTION_ADD: 'ui:section:add',
//   DIALOG_ROUND_START: 'ui:dialog:round:start',
//   PLAN_UPDATE: 'ui:plan:update',
//   PLAN_COMPLETED: 'ui:plan:completed',
//   USER_MESSAGE_SEND_REQUESTED: 'ui:user:message:send_requested',
//   CHAT_INPUT_CLEAR: 'ui:chatinput:clear',
//   CHAT_INPUT_UPDATE_STATE: 'ui:chatinput:updatestate',
//   CHAT_INPUT_FOCUS: 'ui:chatinput:focus',
//   PLAN_TEMPLATE_SELECTED: 'ui:plan_template:selected',
//   PLAN_EXECUTION_REQUESTED: 'ui:plan:execution_requested',
//   GENERATION_STATE_CHANGED: 'ui:generation:state_changed',
//   EXECUTION_STATE_CHANGED: 'ui:execution:state_changed',
//   CURRENT_PLAN_TEMPLATE_CHANGED: 'ui:current_plan_template:changed',
//   PLAN_PARAMS_CHANGED: 'ui:plan_params:changed',
//   JSON_CONTENT_SET: 'ui:json:content_set',
//   JSON_CONTENT_CLEAR: 'ui:json:content_clear',
//   VERSION_HISTORY_SET: 'ui:version_history:set',
// } as const

// export type EventKeys = keyof typeof EVENTS
// export type EventValue = typeof EVENTS[EventKeys]

// // 仅保留事件名常量和类型定义，无事件总线实现
