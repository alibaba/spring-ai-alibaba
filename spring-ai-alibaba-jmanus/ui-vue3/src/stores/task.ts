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
  const hasVisitedHome = ref(false)

  // 设置新任务
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

  // 标记任务为已处理
  const markTaskAsProcessed = () => {
    console.log('[TaskStore] markTaskAsProcessed called, current task:', currentTask.value)
    if (currentTask.value) {
      currentTask.value.processed = true
      console.log('[TaskStore] Task marked as processed:', currentTask.value)
    } else {
      console.log('[TaskStore] No current task to mark as processed')
    }
  }

  // 清空任务
  const clearTask = () => {
    currentTask.value = null
  }

  // 检查是否有未处理的任务
  const hasUnprocessedTask = () => {
    const result = currentTask.value && !currentTask.value.processed
    console.log('[TaskStore] hasUnprocessedTask check - currentTask:', currentTask.value, 'result:', result)
    return result
  }

  // 设置已访问过 home 页面
  const markHomeVisited = () => {
    hasVisitedHome.value = true
    // 保存到 localStorage
    localStorage.setItem('hasVisitedHome', 'true')
  }

  // 检查是否访问过 home 页面
  const checkHomeVisited = () => {
    const stored = localStorage.getItem('hasVisitedHome')
    hasVisitedHome.value = stored === 'true'
    return hasVisitedHome.value
  }

  // 重置访问状态（用于调试或重置）
  const resetHomeVisited = () => {
    hasVisitedHome.value = false
    localStorage.removeItem('hasVisitedHome')
  }

  return {
    currentTask,
    hasVisitedHome,
    setTask,
    markTaskAsProcessed,
    clearTask,
    hasUnprocessedTask,
    markHomeVisited,
    checkHomeVisited,
    resetHomeVisited
  }
})
