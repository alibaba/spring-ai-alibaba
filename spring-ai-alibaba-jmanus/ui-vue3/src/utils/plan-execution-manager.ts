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

import { reactive } from 'vue'
import { PlanActApiService } from '@/api/plan-act-api-service'
import { DirectApiService } from '@/api/direct-api-service'
import { CommonApiService } from '@/api/common-api-service'
import type { PlanExecutionRecord } from '@/types/plan-execution-record'

// Define event callback interface
interface EventCallbacks {
  onPlanUpdate?: (data: PlanExecutionRecord,activeId : string) => void
  onPlanCompleted?: (data: PlanExecutionRecord, activeId: string) => void
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


export class PlanExecutionManager {
  private static instance: PlanExecutionManager | null = null
  private readonly POLL_INTERVAL = 5000

  // Reactive state
  private state = reactive<ExecutionState>({
    activePlanId: null,
    lastSequenceSize: 0,
    isPolling: false,
    pollTimer: null
  })

  // Event callbacks
  private callbacks: EventCallbacks = {}

  private constructor() {
    // Remove window event listener initialization
    console.log('[PlanExecutionManager] Initialized with callback-based event system')
  }

  /**
   * Get singleton instance
   */
  public static getInstance(): PlanExecutionManager {
    if (!PlanExecutionManager.instance) {
      PlanExecutionManager.instance = new PlanExecutionManager()
    }
    return PlanExecutionManager.instance
  }

  /**
   * Get current active plan ID
   */
  public getActivePlanId(): string | null {
    return this.state.activePlanId
  }

  /**
   * Get current state (reactive)
   */
  public getState() {
    return this.state
  }

  /**
   * Set event callbacks
   */
  public setEventCallbacks(callbacks: EventCallbacks): void {
    this.callbacks = { ...this.callbacks, ...callbacks }
    console.log('[PlanExecutionManager] Event callbacks set:', Object.keys(callbacks))
  }

  /**
   * Handle user message send request
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
        throw new Error('Failed to get valid plan ID')
      }
    } catch (error: any) {
      this.emitMessageUpdate({
        content: `Send failed: ${error.message}`,
        type: 'error',
        planId: this.state.activePlanId
      })
      this.emitChatInputUpdateState({ enabled: true })
      this.state.activePlanId = null
    }
  }

  /**
   * Handle plan execution request
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
   * Validate request and prepare UI
   */
  private validateAndPrepareUIForNewRequest(query: string): boolean {
    if (!query) {
      console.warn('[PlanExecutionManager] Query is empty')
      return false
    }

    if (this.state.activePlanId) {
      this.emitMessageUpdate({
        content: 'There is a task currently executing, please wait for completion before submitting a new task',
        type: 'error',
        planId: this.state.activePlanId
      })
      return false
    }

    // Clear input and set to disabled state
    this.emitChatInputClear()
    this.emitChatInputUpdateState({ enabled: false, placeholder: 'Processing...' })

    return true
  }

  /**
   * Send user message and set plan ID
   */
  private async sendUserMessageAndSetPlanId(query: string): Promise<any> {
    try {
      // Use direct execution mode API to send message
      const response = await DirectApiService.sendMessage(query)

      if (response && response.planId) {
        this.state.activePlanId = response.planId
        return response
      } else if (response && response.planTemplateId) {
        // If response contains planTemplateId instead of planId
        this.state.activePlanId = response.planTemplateId
        return { ...response, planId: response.planTemplateId }
      }

      console.error('[PlanExecutionManager] Failed to get planId from response:', response)
      throw new Error('Failed to get valid planId from API response')
    } catch (error: any) {
      console.error('[PlanExecutionManager] API call failed:', error)
      throw error
    }
  }

  /**
   * Start plan execution sequence
   */
  public initiatePlanExecutionSequence(query: string, planId: string): void {
    this.emitDialogRoundStart({
      planId: planId,
      query: query
    })
    this.startPolling()
  }

  /**
   * Handle plan completion common logic
   */
  private handlePlanCompletion(details: PlanExecutionRecord): void {
    this.emitPlanCompleted(details, this.state.activePlanId || '')
    this.state.lastSequenceSize = 0
    this.stopPolling()

    // Delay deletion of plan execution record
    try {
      setTimeout(async () => {
        if (this.state.activePlanId) {
          try {
            await PlanActApiService.deletePlanTemplate(this.state.activePlanId)
            console.log(`[PlanExecutionManager] Plan template ${this.state.activePlanId} deleted successfully`)
          } catch (error: any) {
            console.log(`Delete plan execution record failed: ${error.message}`)
          }
        }
      }, 5000)
    } catch (error: any) {
      console.log(`Delete plan execution record failed: ${error.message}`)
    }

    if (details.completed) {
      this.state.activePlanId = null
      this.emitChatInputUpdateState({ enabled: true })
    }
  }

  /**
   * Poll plan execution status
   */
  private async pollPlanStatus(): Promise<void> {
    if (!this.state.activePlanId) return

    if (this.state.isPolling) {
      console.log('[PlanExecutionManager] Previous polling still in progress, skipping')
      return
    }

    try {
      this.state.isPolling = true

      // Here we need to implement the API call to get plan details
      // Since the original code calls ManusAPI.getDetails, we need to use the existing API services
      // For now, we'll use the relevant methods of PlanActApiService
      const details = await this.getPlanDetails(this.state.activePlanId)

      if (!details) {
        console.warn('[PlanExecutionManager] No details received from API')
        return
      }

      if (!details.steps || details.steps.length === 0) {
        console.log('[PlanExecutionManager] Simple response without steps detected, handling as completed')
        // For simple responses, emit completion directly
        this.emitPlanUpdate(details, this.state.activePlanId || '')
        this.handlePlanCompletion(details)
        return
      }

      this.emitPlanUpdate(details, this.state.activePlanId || '')

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
   * Get plan details (needs to be adjusted based on actual API)
   */
  private async getPlanDetails(planId: string): Promise<PlanExecutionRecord | null> {
    try {
      // Use CommonApiService's getDetails method
      const details = await CommonApiService.getDetails(planId)
      return details
    } catch (error: any) {
      console.error('[PlanExecutionManager] Failed to get plan details:', error)
      return null
    }
  }

  /**
   * Start polling plan execution status
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
   * Immediately poll plan execution status (for manual refresh trigger)
   */
  public async pollPlanStatusImmediately(): Promise<void> {
    console.log('[PlanExecutionManager] Polling plan status immediately')
    await this.pollPlanStatus()
  }

  /**
   * Stop polling
   */
  public stopPolling(): void {
    if (this.state.pollTimer) {
      clearInterval(this.state.pollTimer)
      this.state.pollTimer = null
    }
    console.log('[PlanExecutionManager] Stopped polling')
  }

  /**
   * Clean up resources
   */
  public cleanup(): void {
    this.stopPolling()
    this.state.activePlanId = null
    this.state.lastSequenceSize = 0
    this.state.isPolling = false
  }

  // Event emission helpers - Use callback functions instead of window events
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

  private emitPlanUpdate(data: PlanExecutionRecord, activeId: string): void {
    if (this.callbacks.onPlanUpdate) {
      this.callbacks.onPlanUpdate(data, activeId)
    }
  }

  private emitPlanCompleted(data: PlanExecutionRecord, activeId: string): void {
    if (this.callbacks.onPlanCompleted) {
      this.callbacks.onPlanCompleted(data, activeId)
    }
  }
}

// Export singleton instance
export const planExecutionManager = PlanExecutionManager.getInstance()
