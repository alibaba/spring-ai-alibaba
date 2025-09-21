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

import { ref, nextTick, onMounted, onUnmounted, readonly } from 'vue'
import type { Ref } from 'vue'

/**
 * Scroll behavior management for chat messages
 */
export function useScrollBehavior(containerRef: Ref<HTMLElement | null>) {
  const showScrollToBottom = ref(false)
  const isAutoScrollEnabled = ref(true)
  const scrollThreshold = 150 // pixels from bottom to show scroll button

  /**
   * Scroll to bottom of container
   */
  const scrollToBottom = async (smooth = true) => {
    if (!containerRef.value) return
    
    await nextTick()
    
    containerRef.value.scrollTo({
      top: containerRef.value.scrollHeight,
      behavior: smooth ? 'smooth' : 'auto'
    })
  }

  /**
   * Check if user is near bottom of container
   */
  const isNearBottom = (): boolean => {
    if (!containerRef.value) return false
    
    const { scrollTop, scrollHeight, clientHeight } = containerRef.value
    return scrollHeight - scrollTop - clientHeight < scrollThreshold
  }

  /**
   * Check scroll position and update scroll button visibility
   */
  const checkScrollPosition = () => {
    if (!containerRef.value) return
    
    const nearBottom = isNearBottom()
    showScrollToBottom.value = !nearBottom && containerRef.value.scrollHeight > containerRef.value.clientHeight
    
    // Enable auto-scroll when user scrolls to bottom
    if (nearBottom) {
      isAutoScrollEnabled.value = true
    }
  }

  /**
   * Handle scroll event
   */
  const handleScroll = () => {
    checkScrollPosition()
    
    // Disable auto-scroll when user manually scrolls up
    if (!isNearBottom()) {
      isAutoScrollEnabled.value = false
    }
  }

  /**
   * Auto scroll to bottom when new messages arrive
   */
  const autoScrollToBottom = async () => {
    if (isAutoScrollEnabled.value) {
      await scrollToBottom()
    }
  }

  /**
   * Force scroll to bottom and enable auto-scroll
   */
  const forceScrollToBottom = async () => {
    isAutoScrollEnabled.value = true
    await scrollToBottom()
  }

  /**
   * Scroll to a specific message element
   */
  const scrollToMessage = (messageId: string, smooth = true) => {
    if (!containerRef.value) return
    
    const messageElement = containerRef.value.querySelector(`[data-message-id="${messageId}"]`)
    if (messageElement) {
      messageElement.scrollIntoView({
        behavior: smooth ? 'smooth' : 'auto',
        block: 'center'
      })
    }
  }

  /**
   * Get scroll position info
   */
  const getScrollInfo = () => {
    if (!containerRef.value) {
      return {
        scrollTop: 0,
        scrollHeight: 0,
        clientHeight: 0,
        isAtTop: true,
        isAtBottom: true,
        scrollPercentage: 0
      }
    }

    const { scrollTop, scrollHeight, clientHeight } = containerRef.value
    const isAtTop = scrollTop === 0
    const isAtBottom = scrollTop + clientHeight >= scrollHeight - 5
    const scrollPercentage = scrollHeight > clientHeight 
      ? (scrollTop / (scrollHeight - clientHeight)) * 100 
      : 100

    return {
      scrollTop,
      scrollHeight,
      clientHeight,
      isAtTop,
      isAtBottom,
      scrollPercentage
    }
  }

  /**
   * Add scroll event listener
   */
  const addScrollListener = () => {
    if (containerRef.value) {
      containerRef.value.addEventListener('scroll', handleScroll, { passive: true })
    }
  }

  /**
   * Remove scroll event listener
   */
  const removeScrollListener = () => {
    if (containerRef.value) {
      containerRef.value.removeEventListener('scroll', handleScroll)
    }
  }

  /**
   * Initialize scroll behavior
   */
  const initializeScrollBehavior = () => {
    addScrollListener()
    checkScrollPosition()
  }

  /**
   * Clean up scroll behavior
   */
  const cleanupScrollBehavior = () => {
    removeScrollListener()
  }

  // Auto-initialize on mount
  onMounted(() => {
    nextTick(() => {
      initializeScrollBehavior()
    })
  })

  // Cleanup on unmount
  onUnmounted(() => {
    cleanupScrollBehavior()
  })

  return {
    // State
    showScrollToBottom: readonly(showScrollToBottom),
    isAutoScrollEnabled: readonly(isAutoScrollEnabled),

    // Methods
    scrollToBottom,
    autoScrollToBottom,
    forceScrollToBottom,
    scrollToMessage,
    isNearBottom,
    checkScrollPosition,
    getScrollInfo,
    addScrollListener,
    removeScrollListener,
    initializeScrollBehavior,
    cleanupScrollBehavior
  }
}

/**
 * Smooth scroll behavior with easing
 */
export function useSmoothScroll() {
  const scrollWithEasing = (
    element: HTMLElement,
    targetPosition: number,
    duration: number = 300
  ) => {
    const startPosition = element.scrollTop
    const distance = targetPosition - startPosition
    let startTime: number | null = null

    const animation = (currentTime: number) => {
      if (startTime === null) startTime = currentTime
      const timeElapsed = currentTime - startTime
      const run = easeInOutQuad(timeElapsed, startPosition, distance, duration)
      
      element.scrollTop = run
      
      if (timeElapsed < duration) {
        requestAnimationFrame(animation)
      }
    }

    requestAnimationFrame(animation)
  }

  // Easing function
  const easeInOutQuad = (t: number, b: number, c: number, d: number): number => {
    t /= d / 2
    if (t < 1) return c / 2 * t * t + b
    t--
    return -c / 2 * (t * (t - 2) - 1) + b
  }

  return {
    scrollWithEasing
  }
}
