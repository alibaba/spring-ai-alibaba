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
 * CronTask 工具函数集合
 * 提供定时任务相关的通用功能
 */
export const CronTaskUtils = {
  /**
   * 验证 Cron 表达式格式
   * @param cronExpr Cron 表达式
   * @returns 是否有效
   */
  validateCronExpression(cronExpr: string): boolean {
    // 简单验证：检查是否有 5-6 个由空格分隔的部分
    const parts = cronExpr.trim().split(/\s+/)
    return parts.length >= 5 && parts.length <= 6
  },

  /**
   * 格式化时间
   * @param timeString 时间字符串
   * @returns 格式化后的时间字符串
   */
  formatTime(timeString: string): string {
    return new Date(timeString).toLocaleString()
  },

  /**
   * 保存定时任务
   * @param task 任务对象
   * @returns 保存结果
   */
  async saveTask(task: CronConfig): Promise<CronConfig> {
    try {
      let result: CronConfig

      if (task.id) {
        // 更新现有任务
        result = await CronApiService.updateCronTask(Number(task.id), task)
      } else {
        // 创建新任务
        result = await CronApiService.createCronTask(task)
      }

      return result
    } catch (error) {
      console.error('Failed to save cron task:', error)
      throw error
    }
  },

  /**
   * 删除定时任务
   * @param taskId 任务ID
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
   * 切换任务状态
   * @param task 任务对象
   * @returns 更新后的任务
   */
  async toggleTaskStatus(task: CronConfig): Promise<CronConfig> {
    if (!task.id) {
      throw new Error('Task ID is required')
    }

    const newStatus = task.status === 0 ? 1 : 0
    return await CronApiService.updateCronTask(Number(task.id), { ...task, status: newStatus })
  },

  /**
   * 准备任务执行数据
   * @param task 任务对象
   * @returns 执行数据对象
   */
  prepareTaskExecution(task: CronConfig): { useTemplate: boolean; planData?: any; taskContent?: string } {
    if (task.planTemplateId) {
      // 使用模板执行
      return {
        useTemplate: true,
        planData: {
          title: task.cronName || '定时任务执行',
          planData: {
            id: task.planTemplateId,
            planTemplateId: task.planTemplateId,
            planId: task.planTemplateId
          },
          params: task.executionParams || undefined
        }
      }
    } else {
      // 直接执行任务内容
      return {
        useTemplate: false,
        taskContent: task.planDesc || task.cronName || ''
      }
    }
  }
}
