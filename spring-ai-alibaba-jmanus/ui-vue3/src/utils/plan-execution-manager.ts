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
import type { UIStateData } from '@/types/cache-data'

// Define event callback interface
interface EventCallbacks {
  onPlanUpdate?: (rootPlanId: string) => void
  onPlanCompleted?: (rootPlanId: string) => void
  onDialogRoundStart?: (rootPlanId: string) => void
  onChatInputUpdateState?: (rootPlanId: string) => void
  onChatInputClear?: () => void
  onPlanError?: (message: string) => void
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

  // Cache for PlanExecutionRecord by rootPlanId
  private planExecutionCache = new Map<string, PlanExecutionRecord>()

  // Cache for UI state by rootPlanId
  private uiStateCache = new Map<string, UIStateData>()

  /**
   * Get cached plan execution record by rootPlanId
   */
  getCachedPlanRecord(rootPlanId: string): PlanExecutionRecord | undefined {
    return this.planExecutionCache.get(rootPlanId)
  }

  /**
   * Get cached UI state by rootPlanId
   */
  getCachedUIState(rootPlanId: string): UIStateData | undefined {
    return this.uiStateCache.get(rootPlanId)
  }

  /**
   * Set cached UI state by rootPlanId
   */
  setCachedUIState(rootPlanId: string, uiState: UIStateData): void {
    this.uiStateCache.set(rootPlanId, uiState)
    console.log(`[PlanExecutionManager] Cached UI state for rootPlanId: ${rootPlanId}`)
  }

  /**
   * Get all cached plan execution records
   */
  getAllCachedRecords(): Map<string, PlanExecutionRecord> {
    return new Map(this.planExecutionCache)
  }

  /**
   * Check if a plan execution record exists in cache
   */
  hasCachedPlanRecord(rootPlanId: string): boolean {
    return this.planExecutionCache.has(rootPlanId)
  }

  /**
   * Set cached plan execution record by rootPlanId
   */
  setCachedPlanRecord(rootPlanId: string, record: PlanExecutionRecord): void {
    this.planExecutionCache.set(rootPlanId, record)
    console.log(`[PlanExecutionManager] Cached plan execution record for rootPlanId: ${rootPlanId}`)
  }

  /**
   * Clear cached plan execution record by rootPlanId
   */
  clearCachedPlanRecord(rootPlanId: string): boolean {
    const deleted = this.planExecutionCache.delete(rootPlanId)
    if (deleted) {
      console.log(`[PlanExecutionManager] Cleared cached plan execution record for rootPlanId: ${rootPlanId}`)
    }
    return deleted
  }

  /**
   * Clear all cached plan execution records
   */
  clearAllCachedRecords(): void {
    const planCacheSize = this.planExecutionCache.size
    const uiStateCacheSize = this.uiStateCache.size

    this.planExecutionCache.clear()
    this.uiStateCache.clear()

    console.log(`[PlanExecutionManager] Cleared all caches - Plans: ${planCacheSize}, UI States: ${uiStateCacheSize}`)
  }

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
      await this.sendUserMessageAndSetPlanId(query)

      if (this.state.activePlanId) {
        this.initiatePlanExecutionSequence(query, this.state.activePlanId)
      } else {
        throw new Error('Failed to get valid plan ID')
      }
    } catch (error: any) {
      console.error('[PlanExecutionManager] Failed to send user message:', error)
      // Set UI state to enabled for error recovery
      const errorPlanId = this.state.activePlanId ?? 'error'
      this.setCachedUIState(errorPlanId, { enabled: true })

      this.emitChatInputUpdateState(errorPlanId)
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
      this.initiatePlanExecutionSequence(query ?? 'Execute Plan', planId)
    } else {
      console.error('[PlanExecutionManager] Invalid plan execution request: missing planId')
    }
  }

  /**
   * Handle plan execution request with cache lookup by rootPlanId
   */
  public handleCachedPlanExecution(rootPlanId: string, query?: string): boolean {
    const cachedRecord = this.getCachedPlanRecord(rootPlanId)

    if (cachedRecord?.currentPlanId) {
      console.log(`[PlanExecutionManager] Found cached plan execution record for rootPlanId: ${rootPlanId}`)
      this.handlePlanExecutionRequested(cachedRecord.currentPlanId, query)
      return true
    } else {
      console.log(`[PlanExecutionManager] No cached plan execution record found for rootPlanId: ${rootPlanId}`)
      return false
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
      // There is already an active plan, cannot start new request
      return false
    }

    // Clear input and set to disabled state
    this.emitChatInputClear()

    // Cache UI state data first
    const uiStatePlanId = this.state.activePlanId ?? 'ui-state'
    this.setCachedUIState(uiStatePlanId, { enabled: false, placeholder: 'Processing...' })
    this.emitChatInputUpdateState(uiStatePlanId)

    return true
  }

  /**
   * Send user message and set plan ID
   */
  private async sendUserMessageAndSetPlanId(query: string): Promise<any> {
    try {
      // Use direct execution mode API to send message
      const response = await DirectApiService.sendMessage({
        input: query
      })

      if (response?.planId) {
        this.state.activePlanId = response.planId
        return response
      } else if (response?.planTemplateId) {
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
    console.log(`[PlanExecutionManager] Starting plan execution sequence for query: "${query}", planId: ${planId}`)

    // Use planId as rootPlanId for now (assume they are the same initially)
    const rootPlanId = planId

    // Try to emit dialog start
    this.emitDialogRoundStart(rootPlanId)

    this.startPolling()
  }

  /**
   * Handle plan completion common logic
   */
  private handlePlanCompletion(details: PlanExecutionRecord): void {
    this.emitPlanCompleted(details.rootPlanId ?? "");
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
      this.emitChatInputUpdateState(details.rootPlanId ?? "");
    }
  }

  private handlePlanError(details: PlanExecutionRecord): void {
    this.emitPlanError(details.message ?? "");
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
        console.warn('[PlanExecutionManager] No details received from API - this might be a temporary network issue')
        return
      }

      // Only handle actual plan execution failures, not network errors
      if(details.status && details.status === 'failed' && details.message && !details.message.includes('Failed to get detailed information')){
        this.handlePlanError(details)
        return;
      }

      // Update cache with latest plan details if rootPlanId exists
      if (details.rootPlanId) {
        this.setCachedPlanRecord(details.rootPlanId, details)
      }



      this.emitPlanUpdate(details.rootPlanId ?? "");

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

      // Cache the plan execution record by rootPlanId if it exists
      if (details?.rootPlanId) {
        this.planExecutionCache.set(details.rootPlanId, details)
        console.log(`[PlanExecutionManager] Cached plan execution record for rootPlanId: ${details.rootPlanId}`)
      }

      return details
    } catch (error: any) {
      console.error('[PlanExecutionManager] Failed to get plan details:', error)
      return {
        currentPlanId: planId,
        status: 'failed',
        message: error instanceof Error ? error.message : 'Failed to get plan'
      }
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

    // Clear all cached plan execution records
    this.clearAllCachedRecords()
  }

  // Event emission helpers - Use callback functions instead of window events
  private emitChatInputClear(): void {
    if (this.callbacks.onChatInputClear) {
      this.callbacks.onChatInputClear()
    }
  }

  private emitChatInputUpdateState(rootPlanId: string): void {
    if (this.callbacks.onChatInputUpdateState) {
      this.callbacks.onChatInputUpdateState(rootPlanId)
    }
  }

  private emitDialogRoundStart(rootPlanId: string): void {
    if (this.callbacks.onDialogRoundStart) {
      this.callbacks.onDialogRoundStart(rootPlanId)
    }
  }

  private emitPlanUpdate(rootPlanId: string): void {
    if (this.callbacks.onPlanUpdate) {
      this.callbacks.onPlanUpdate(rootPlanId)
    }
  }

  private emitPlanCompleted(rootPlanId: string): void {
    if (this.callbacks.onPlanCompleted) {
      this.callbacks.onPlanCompleted(rootPlanId)
    }
  }

  private emitPlanError(message: string): void {
    if (this.callbacks.onPlanError) {
      this.callbacks.onPlanError(message)
    }
  }
}

// Export singleton instance
export const planExecutionManager = PlanExecutionManager.getInstance()
