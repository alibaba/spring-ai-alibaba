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

import { ref, reactive } from 'vue'
import { PlanActApiService } from '@/api/plan-act-api-service'
import { DirectApiService } from '@/api/direct-api-service'
import { CommonApiService } from '@/api/common-api-service'

// 定义事件回调接口
interface EventCallbacks {
  onPlanUpdate?: (data: any) => void
  onPlanCompleted?: (data: any) => void
  onDialogRoundStart?: (data: any) => void
  onMessageUpdate?: (data: any) => void
  onChatInputUpdateState?: (data: any) => void
  onChatInputClear?: () => void
}

interface ExecutionState {
  activePlanId: string | null
  lastSequenceSize: number
  isPolling: boolean
  pollTimer: number | null
}

interface PlanDetails {
  planId: string
  title?: string
  steps?: any[]
  currentStepIndex?: number
  completed?: boolean
  summary?: string
}

export class PlanExecutionManager {
  private static instance: PlanExecutionManager | null = null
  private readonly POLL_INTERVAL = 5000

  // 响应式状态
  private state = reactive<ExecutionState>({
    activePlanId: null,
    lastSequenceSize: 0,
    isPolling: false,
    pollTimer: null
  })

  // 事件回调
  private callbacks: EventCallbacks = {}

  private constructor() {
    // 移除 window 事件监听器初始化
    console.log('[PlanExecutionManager] Initialized with callback-based event system')
  }

  /**
   * 获取单例实例
   */
  public static getInstance(): PlanExecutionManager {
    if (!PlanExecutionManager.instance) {
      PlanExecutionManager.instance = new PlanExecutionManager()
    }
    return PlanExecutionManager.instance
  }

  /**
   * 获取当前活动的计划ID
   */
  public getActivePlanId(): string | null {
    return this.state.activePlanId
  }

  /**
   * 获取当前状态（响应式）
   */
  public getState() {
    return this.state
  }

  /**
   * 设置事件回调
   */
  public setEventCallbacks(callbacks: EventCallbacks): void {
    this.callbacks = { ...this.callbacks, ...callbacks }
    console.log('[PlanExecutionManager] Event callbacks set:', Object.keys(callbacks))
  }

  /**
   * 处理用户消息发送请求
   */
  public async handleUserMessageSendRequested(query: string): Promise<void> {
    if (!this.validateAndPrepareUIForNewRequest(query)) {
      return
    }

    try {
      const response = await this.sendUserMessageAndSetPlanId(query)
      
      if (this.state.activePlanId) {
        this.initiatePlanExecutionSequence(query, this.state.activePlanId)
      } else {
        throw new Error('未能获取有效的计划ID')
      }
    } catch (error: any) {
      this.emitMessageUpdate({
        content: `发送失败: ${error.message}`,
        type: 'error',
        planId: this.state.activePlanId
      })
      this.emitChatInputUpdateState({ enabled: true })
      this.state.activePlanId = null
    }
  }

  /**
   * 处理计划执行请求
   */
  public handlePlanExecutionRequested(planId: string, query?: string): void {
    console.log('[PlanExecutionManager] Received plan execution request:', { planId, query })
    
    if (planId) {
      this.state.activePlanId = planId
      this.initiatePlanExecutionSequence(query || '执行计划', planId)
    } else {
      console.error('[PlanExecutionManager] Invalid plan execution request: missing planId')
    }
  }

  /**
   * 验证请求并准备UI
   */
  private validateAndPrepareUIForNewRequest(query: string): boolean {
    if (!query) {
      console.warn('[PlanExecutionManager] Query is empty')
      return false
    }

    if (this.state.activePlanId) {
      this.emitMessageUpdate({
        content: '当前有任务正在执行，请等待完成后再提交新任务',
        type: 'error',
        planId: this.state.activePlanId
      })
      return false
    }

    // 清空输入并设置为禁用状态
    this.emitChatInputClear()
    this.emitChatInputUpdateState({ enabled: false, placeholder: '处理中...' })
    
    return true
  }

  /**
   * 发送用户消息并设置计划ID
   */
  private async sendUserMessageAndSetPlanId(query: string): Promise<any> {
    try {
      // 使用直接执行模式API发送消息
      const response = await DirectApiService.sendMessage(query)
      
      if (response && response.planId) {
        this.state.activePlanId = response.planId
        return response
      } else if (response && response.planTemplateId) {
        // 如果响应中有planTemplateId而不是planId
        this.state.activePlanId = response.planTemplateId
        return { ...response, planId: response.planTemplateId }
      }
      
      console.error('[PlanExecutionManager] Failed to get planId from response:', response)
      throw new Error('未能从API响应中获取有效的 planId')
    } catch (error: any) {
      console.error('[PlanExecutionManager] API call failed:', error)
      throw error
    }
  }

  /**
   * 启动计划执行序列
   */
  public initiatePlanExecutionSequence(query: string, planId: string): void {
    this.emitDialogRoundStart({
      planId: planId,
      query: query
    })
    this.startPolling()
  }

  /**
   * 处理计划完成的通用逻辑
   */
  private handlePlanCompletion(details: PlanDetails): void {
    this.emitPlanCompleted({ ...details, planId: this.state.activePlanId })
    this.state.lastSequenceSize = 0
    this.stopPolling()

    // 延迟删除计划执行记录
    try {
      setTimeout(async () => {
        if (this.state.activePlanId) {
          try {
            await PlanActApiService.deletePlanTemplate(this.state.activePlanId)
            console.log(`[PlanExecutionManager] Plan template ${this.state.activePlanId} deleted successfully`)
          } catch (error: any) {
            console.log(`删除计划执行记录失败: ${error.message}`)
          }
        }
      }, 5000)
    } catch (error: any) {
      console.log(`删除计划执行记录失败: ${error.message}`)
    }

    if (details.completed) {
      this.state.activePlanId = null
      this.emitChatInputUpdateState({ enabled: true })
    }
  }

  /**
   * 轮询计划执行状态
   */
  private async pollPlanStatus(): Promise<void> {
    if (!this.state.activePlanId) return
    
    if (this.state.isPolling) {
      console.log('[PlanExecutionManager] Previous polling still in progress, skipping')
      return
    }

    try {
      this.state.isPolling = true
      
      // 这里需要实现获取计划详情的API调用
      // 由于原始代码调用 ManusAPI.getDetails，我们需要使用现有的API服务
      // 暂时使用 PlanActApiService 的相关方法
      const details = await this.getPlanDetails(this.state.activePlanId)
      
      if (!details) {
        console.warn('[PlanExecutionManager] No details received from API')
        return
      }

      if (!details.steps || details.steps.length === 0) {
        console.log('[PlanExecutionManager] Simple response without steps detected, handling as completed')
        // For simple responses, emit completion directly
        this.emitPlanUpdate({ ...details, planId: this.state.activePlanId, completed: true })
        this.handlePlanCompletion(details)
        return
      }

      this.emitPlanUpdate({ ...details, planId: this.state.activePlanId })

      if (details.completed) {
        this.handlePlanCompletion(details)
      }
    } catch (error: any) {
      console.error('[PlanExecutionManager] Failed to poll plan status:', error)
    } finally {
      this.state.isPolling = false
    }
  }

  /**
   * 获取计划详情（需要根据实际API调整）
   */
  private async getPlanDetails(planId: string): Promise<PlanDetails | null> {
    try {
      // 使用 CommonApiService 的 getDetails 方法
      const details = await CommonApiService.getDetails(planId)
      return details
    } catch (error: any) {
      console.error('[PlanExecutionManager] Failed to get plan details:', error)
      return null
    }
  }

  /**
   * 开始轮询计划执行状态
   */
  public startPolling(): void {
    if (this.state.pollTimer) {
      clearInterval(this.state.pollTimer)
    }
    
    this.state.pollTimer = window.setInterval(() => {
      this.pollPlanStatus()
    }, this.POLL_INTERVAL)
    
    console.log('[PlanExecutionManager] Started polling')
  }

  /**
   * 立即轮询计划执行状态（用于手动触发刷新）
   */
  public async pollPlanStatusImmediately(): Promise<void> {
    console.log('[PlanExecutionManager] Polling plan status immediately')
    await this.pollPlanStatus()
  }

  /**
   * 停止轮询
   */
  public stopPolling(): void {
    if (this.state.pollTimer) {
      clearInterval(this.state.pollTimer)
      this.state.pollTimer = null
    }
    console.log('[PlanExecutionManager] Stopped polling')
  }

  /**
   * 清理资源
   */
  public cleanup(): void {
    this.stopPolling()
    this.state.activePlanId = null
    this.state.lastSequenceSize = 0
    this.state.isPolling = false
  }

  // Event emission helpers - 使用回调函数替代 window 事件
  private emitMessageUpdate(data: any): void {
    if (this.callbacks.onMessageUpdate) {
      this.callbacks.onMessageUpdate(data)
    }
  }

  private emitChatInputClear(): void {
    if (this.callbacks.onChatInputClear) {
      this.callbacks.onChatInputClear()
    }
  }

  private emitChatInputUpdateState(data: any): void {
    if (this.callbacks.onChatInputUpdateState) {
      this.callbacks.onChatInputUpdateState(data)
    }
  }

  private emitDialogRoundStart(data: any): void {
    if (this.callbacks.onDialogRoundStart) {
      this.callbacks.onDialogRoundStart(data)
    }
  }

  private emitPlanUpdate(data: any): void {
    if (this.callbacks.onPlanUpdate) {
      this.callbacks.onPlanUpdate(data)
    }
  }

  private emitPlanCompleted(data: any): void {
    if (this.callbacks.onPlanCompleted) {
      this.callbacks.onPlanCompleted(data)
    }
  }
}

// 导出单例实例
export const planExecutionManager = PlanExecutionManager.getInstance()
