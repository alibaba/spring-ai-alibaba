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

export class CronApiService {
  private static readonly BASE_URL = '/api/cron-tasks'

  /**
   * 获取所有定时任务
   */
  static async getAllCronTasks(): Promise<CronConfig[]> {
    try {
      const response = await fetch(this.BASE_URL)
      const result = await this.handleResponse(response)
      return await result.json()
    } catch (error) {
      console.error('Failed to get cron tasks:', error)
      throw error
    }
  }

  /**
   * 根据ID获取定时任务
   */
  static async getCronTaskById(id: string): Promise<CronConfig> {
    try {
      const response = await fetch(`${this.BASE_URL}/${id}`)
      const result = await this.handleResponse(response)
      return await result.json()
    } catch (error) {
      console.error('Failed to get cron task by id:', error)
      throw error
    }
  }

  /**
   * 创建定时任务
   */
  static async createCronTask(cronConfig: CronConfig): Promise<CronConfig> {
    try {
      const response = await fetch(this.BASE_URL, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify(cronConfig),
      })
      const result = await this.handleResponse(response)
      return await result.json()
    } catch (error) {
      console.error('Failed to create cron task:', error)
      throw error
    }
  }

  /**
   * 更新定时任务
   */
  static async updateCronTask(id: number, cronConfig: CronConfig): Promise<CronConfig> {
    try {
      const response = await fetch(`${this.BASE_URL}/${id}`, {
        method: 'PUT',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify(cronConfig),
      })
      const result = await this.handleResponse(response)
      return await result.json()
    } catch (error) {
      console.error('Failed to update cron task:', error)
      throw error
    }
  }

  /**
   * 更新任务状态
   */
  static async updateTaskStatus(id: string, status: number): Promise<void> {
    try {
      const response = await fetch(`${this.BASE_URL}/${id}/status?status=${status}`, {
        method: 'PUT',
      })
      await this.handleResponse(response)
    } catch (error) {
      console.error('Failed to update task status:', error)
      throw error
    }
  }

  /**
   * 删除定时任务
   */
  static async deleteCronTask(id: string): Promise<void> {
    try {
      const response = await fetch(`${this.BASE_URL}/${id}`, {
        method: 'DELETE',
      })
      await this.handleResponse(response)
    } catch (error) {
      console.error('Failed to delete cron task:', error)
      throw error
    }
  }

  private static async handleResponse(response: Response) {
    if (!response.ok) {
      try {
        const errorData = await response.json()
        throw new Error(errorData.message || `API request failed: ${response.status}`)
      } catch {
        throw new Error(`API request failed: ${response.status} ${response.statusText}`)
      }
    }
    return response
  }
}

