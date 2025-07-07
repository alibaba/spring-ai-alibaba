// // /*
// //  * Copyright 2025 the original author or authors.
// //  *
// //  * Licensed under the Apache License, Version 2.0 (the "License");
// //  * you may not use this file except in compliance with the License.
// //  * You may obtain a copy of the License at
// //  *
// //  *      https://www.apache.org/licenses/LICENSE-2.0
// //  *
// //  * Unless required by applicable law or agreed to in writing, software
// //  * distributed under the License is distributed on an "AS IS" BASIS,
// //  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// //  * See the License for the specific language governing permissions and
// //  * limitations under the License.
// //  */

// import { defineStore } from 'pinia'
// import { ref, reactive, computed, nextTick } from 'vue'
// import { planExecutionManager } from '@/utils/plan-execution-manager'

// export const useRightPanelStore = defineStore('rightPanel', () => {
//   // Basic state
//   const activeTab = ref('details')


//   const chatBubbles = ref([
//     {
//       id: '1',
//       type: 'thinking',
//       icon: 'carbon:thinking',
//       title: '分析需求',
//       content:
//         '将您的请求分解为可操作的步骤：1) 创建用户实体，2) 实现用户服务，3) 构建 REST 端点，4) 添加验证和错误处理。',
//       timestamp: '2 分钟前',
//     },
//     {
//       id: '2',
//       type: 'progress',
//       icon: 'carbon:in-progress',
//       title: '生成代码',
//       content:
//         '创建具有用户管理 CRUD 操作的 Spring Boot REST API。包括正确的 HTTP 状态代码和错误处理。',
//       timestamp: '1 分钟前',
//     },
//     {
//       id: '3',
//       type: 'success',
//       icon: 'carbon:checkmark',
//       title: '代码已生成',
//       content:
//         '成功生成具有所有 CRUD 操作的 UserController。代码包含正确的 REST 约定、错误处理，并遵循 Spring Boot 最佳实践。',
//       timestamp: '刚刚',
//     },
//   ])

//   // Preview tab configuration
//   const previewTabs = [
//     { id: 'details', name: '步骤执行详情', icon: 'carbon:events' },
//     { id: 'chat', name: 'Chat', icon: 'carbon:chat' },
//     { id: 'code', name: 'Code', icon: 'carbon:code' },
//   ]
//   const stepStatusText = computed(() => {
//     if (!selectedStep.value) return ''
//     if (selectedStep.value.completed) return '已完成'
//     if (selectedStep.value.current) return '执行中'
//     return '等待执行'
//   })

//     // Construct step details object, similar to right-sidebar.js logic
//     selectedStep.value = {
//       planId: planId, // Ensure planId is included
//       index: stepIndex,
//       title:
//         typeof step === 'string'
//           ? step
//           : step.title || step.description || step.name || `步骤 ${stepIndex + 1}`,
//       description: typeof step === 'string' ? step : step.description || step,
//       agentExecution: agentExecution,
//       completed: isStepCompleted,
//       current: isCurrent,
//     }
//  })
