/*
 * Copyright 2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import { ref } from 'vue'
import { defineStore } from 'pinia'

export interface TaskPayload {
  prompt: string
  timestamp: number
  processed?: boolean
}

export const useTaskStore = defineStore('task', () => {
  const currentTask = ref<TaskPayload | null>(null)
  const taskToInput = ref<string>('')
  const hasVisitedHome = ref(false)

  // Set new task
  const setTask = (prompt: string) => {
    console.log('[TaskStore] setTask called with prompt:', prompt)
    const newTask = {
      prompt,
      timestamp: Date.now(),
      processed: false
    }
    currentTask.value = newTask
    console.log('[TaskStore] Task set, currentTask.value:', currentTask.value)
  }

  // Set task to input (for pre-filling input without executing)
  const setTaskToInput = (prompt: string) => {
    console.log('[TaskStore] setTaskToInput called with prompt:', prompt)
    taskToInput.value = prompt
    console.log('[TaskStore] Task to input set:', taskToInput.value)
  }

  // Get and clear task to input
  const getAndClearTaskToInput = () => {
    const task = taskToInput.value
    taskToInput.value = ''
    console.log('[TaskStore] getAndClearTaskToInput returning:', task)
    return task
  }

  // Mark task as processed
  const markTaskAsProcessed = () => {
    console.log('[TaskStore] markTaskAsProcessed called, current task:', currentTask.value)
    if (currentTask.value) {
      currentTask.value.processed = true
      console.log('[TaskStore] Task marked as processed:', currentTask.value)
    } else {
      console.log('[TaskStore] No current task to mark as processed')
    }
  }

  // Clear task
  const clearTask = () => {
    currentTask.value = null
  }

  // Check if there are unprocessed tasks
  const hasUnprocessedTask = () => {
    const result = currentTask.value && !currentTask.value.processed
    console.log('[TaskStore] hasUnprocessedTask check - currentTask:', currentTask.value, 'result:', result)
    return result
  }

  // Set that home page has been visited
  const markHomeVisited = () => {
    hasVisitedHome.value = true
    // Save to localStorage
    localStorage.setItem('hasVisitedHome', 'true')
  }

  // Check if home page has been visited
  const checkHomeVisited = () => {
    const stored = localStorage.getItem('hasVisitedHome')
    hasVisitedHome.value = stored === 'true'
    return hasVisitedHome.value
  }

  // Reset visit status (for debugging or reset)
  const resetHomeVisited = () => {
    hasVisitedHome.value = false
    localStorage.removeItem('hasVisitedHome')
  }

  // Emit plan execution requested event
  const emitPlanExecutionRequested = (payload: { title: string; planData: any; params?: string }) => {
    console.log('[TaskStore] emitPlanExecutionRequested called with payload:', payload)

    // User is on direct page, send event directly
    window.dispatchEvent(new CustomEvent('plan-execution-requested', { detail: payload }))
  }

  return {
    currentTask,
    taskToInput,
    hasVisitedHome,
    setTask,
    setTaskToInput,
    getAndClearTaskToInput,
    markTaskAsProcessed,
    clearTask,
    hasUnprocessedTask,
    markHomeVisited,
    checkHomeVisited,
    resetHomeVisited,
    emitPlanExecutionRequested
  }
})
