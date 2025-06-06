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
import { EVENTS } from '@/constants/events'

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

  private constructor() {
    this.initializeEventListeners()
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
   * 初始化事件监听器
   */
  private initializeEventListeners(): void {
    // 监听用户请求发送消息的事件
    window.addEventListener(EVENTS.USER_MESSAGE_SEND_REQUESTED, this.handleUserMessageSendRequested.bind(this))

    // 监听计划执行请求事件
    window.addEventListener(EVENTS.PLAN_EXECUTION_REQUESTED, this.handlePlanExecutionRequested.bind(this))

    console.log('[PlanExecutionManager] Event listeners initialized')
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
      // 使用计划模式API生成计划
      const response = await PlanActApiService.generatePlan(query)
      
      if (response && response.planId) {
        this.state.activePlanId = response.planId
        return response
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
   * 处理用户请求发送消息的事件
   */
  private async handleUserMessageSendRequested(event: any): Promise<void> {
    const { query } = event.detail || {}

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
   * 处理计划执行请求事件
   */
  private handlePlanExecutionRequested(event: any): void {
    const data = event.detail
    console.log('[PlanExecutionManager] Received PLAN_EXECUTION_REQUESTED event:', data)
    
    if (data && data.planId) {
      this.state.activePlanId = data.planId
      this.initiatePlanExecutionSequence(data.query || '执行计划', data.planId)
    } else {
      console.error('[PlanExecutionManager] Invalid plan execution request data:', data)
    }
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
        console.warn('[PlanExecutionManager] No steps found in plan details')
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

  // Event emission helpers
  private emitMessageUpdate(data: any): void {
    const event = new CustomEvent(EVENTS.MESSAGE_UPDATE, { detail: data })
    window.dispatchEvent(event)
  }

  private emitChatInputClear(): void {
    const event = new CustomEvent(EVENTS.CHAT_INPUT_CLEAR)
    window.dispatchEvent(event)
  }

  private emitChatInputUpdateState(data: any): void {
    const event = new CustomEvent(EVENTS.CHAT_INPUT_UPDATE_STATE, { detail: data })
    window.dispatchEvent(event)
  }

  private emitDialogRoundStart(data: any): void {
    const event = new CustomEvent(EVENTS.DIALOG_ROUND_START, { detail: data })
    window.dispatchEvent(event)
  }

  private emitPlanUpdate(data: any): void {
    const event = new CustomEvent(EVENTS.PLAN_UPDATE, { detail: data })
    window.dispatchEvent(event)
  }

  private emitPlanCompleted(data: any): void {
    const event = new CustomEvent(EVENTS.PLAN_COMPLETED, { detail: data })
    window.dispatchEvent(event)
  }
}

// 导出单例实例
export const planExecutionManager = PlanExecutionManager.getInstance()
