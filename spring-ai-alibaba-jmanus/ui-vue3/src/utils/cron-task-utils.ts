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

import type { CronConfig } from '@/types/cron-task'
import { CronApiService } from '@/api/cron-api-service'

/**
 * CronTask utility functions collection
 * Provides common functionality related to scheduled tasks
 */
export const CronTaskUtils = {
  /**
   * Validate Cron expression format
   * @param cronExpr Cron expression
   * @returns Whether it's valid
   */
  validateCronExpression(cronExpr: string): boolean {
    // Simple validation: check if there are 5-6 space-separated parts
    const parts = cronExpr.trim().split(/\s+/)
    return parts.length >= 5 && parts.length <= 6
  },

  /**
   * Format time
   * @param timeString Time string
   * @returns Formatted time string
   */
  formatTime(timeString: string): string {
    return new Date(timeString).toLocaleString()
  },

  /**
   * Save scheduled task
   * @param task Task object
   * @returns Save result
   */
  async saveTask(task: CronConfig): Promise<CronConfig> {
    try {
      let result: CronConfig

      if (task.id) {
        // Update existing task
        result = await CronApiService.updateCronTask(Number(task.id), task)
      } else {
        // Create new task
        result = await CronApiService.createCronTask(task)
      }

      return result
    } catch (error) {
      console.error('Failed to save cron task:', error)
      throw error
    }
  },

  /**
   * Delete scheduled task
   * @param taskId Task ID
   */
  async deleteTask(taskId: string | number): Promise<void> {
    try {
      await CronApiService.deleteCronTask(String(taskId))
    } catch (error) {
      console.error('Failed to delete cron task:', error)
      throw error
    }
  },

  /**
   * Toggle task status
   * @param task Task object
   * @returns Updated task
   */
  async toggleTaskStatus(task: CronConfig): Promise<CronConfig> {
    if (!task.id) {
      throw new Error('Task ID is required')
    }

    const newStatus = task.status === 0 ? 1 : 0
    return await CronApiService.updateCronTask(Number(task.id), { ...task, status: newStatus })
  },

  /**
   * Prepare task execution data
   * @param task Task object
   * @returns Execution data object
   */
  prepareTaskExecution(task: CronConfig): { useTemplate: boolean; planData?: any; taskContent?: string } {
    if (task.planTemplateId) {
      // Execute using template
      return {
        useTemplate: true,
        planData: {
          title: task.cronName || 'Scheduled Task Execution',
          planData: {
            id: task.planTemplateId,
            planTemplateId: task.planTemplateId,
            planId: task.planTemplateId
          },
          params: task.executionParams || undefined
        }
      }
    } else {
      // Execute task content directly
      return {
        useTemplate: false,
        taskContent: task.planDesc || task.cronName || ''
      }
    }
  }
}
