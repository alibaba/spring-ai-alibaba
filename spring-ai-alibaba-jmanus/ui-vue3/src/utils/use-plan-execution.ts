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

import { computed,  onUnmounted } from 'vue'
import { planExecutionManager } from './plan-execution-manager'

/**
 * Vue composable for plan execution management
 * Provides reactive plan execution status and control methods
 */
export function usePlanExecution() {
  const manager = planExecutionManager

  // Reactive state
  const activePlanId = computed(() => manager.getActivePlanId())
  const state = computed(() => manager.getState())
  const isPolling = computed(() => state.value.isPolling)
  const hasActivePlan = computed(() => !!activePlanId.value)

  /**
   * Start plan execution
   */
  const startExecution = (query: string, planId: string) => {
    manager.initiatePlanExecutionSequence(query, planId)
  }

  /**
   * Stop polling
   */
  const stopPolling = () => {
    manager.stopPolling()
  }

  /**
   * Start polling
   */
  const startPolling = () => {
    manager.startPolling()
  }

  /**
   * Clean up resources
   */
  const cleanup = () => {
    manager.cleanup()
  }

  // Clean up resources when component is unmounted
  onUnmounted(() => {
    cleanup()
  })

  return {
    // State
    activePlanId,
    state,
    isPolling,
    hasActivePlan,
    
    // Methods
    startExecution,
    stopPolling,
    startPolling,
    cleanup
  }
}
