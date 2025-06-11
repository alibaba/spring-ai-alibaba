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

import { computed, onMounted, onUnmounted } from 'vue'
import { planExecutionManager } from './plan-execution-manager'

/**
 * Vue composable for plan execution management
 * 提供响应式的计划执行状态和控制方法
 */
export function usePlanExecution() {
  const manager = planExecutionManager

  // 响应式状态
  const activePlanId = computed(() => manager.getActivePlanId())
  const state = computed(() => manager.getState())
  const isPolling = computed(() => state.value.isPolling)
  const hasActivePlan = computed(() => !!activePlanId.value)

  /**
   * 启动计划执行
   */
  const startExecution = (query: string, planId: string) => {
    manager.initiatePlanExecutionSequence(query, planId)
  }

  /**
   * 停止轮询
   */
  const stopPolling = () => {
    manager.stopPolling()
  }

  /**
   * 开始轮询
   */
  const startPolling = () => {
    manager.startPolling()
  }

  /**
   * 清理资源
   */
  const cleanup = () => {
    manager.cleanup()
  }

  // 组件卸载时清理资源
  onUnmounted(() => {
    cleanup()
  })

  return {
    // 状态
    activePlanId,
    state,
    isPolling,
    hasActivePlan,
    
    // 方法
    startExecution,
    stopPolling,
    startPolling,
    cleanup
  }
}
