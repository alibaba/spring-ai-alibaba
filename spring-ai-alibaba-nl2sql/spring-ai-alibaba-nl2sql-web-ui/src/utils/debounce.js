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

/**
 * 防抖函数
 * @param {Function} func 要防抖的函数
 * @param {number} delay 延迟时间（毫秒）
 * @param {boolean} immediate 是否立即执行
 * @returns {Function} 防抖后的函数
 */
export function debounce(func, delay = 300, immediate = false) {
  let timeoutId = null
  let lastCallTime = 0

  return function debounced(...args) {
    const now = Date.now()
    const timeSinceLastCall = now - lastCallTime

    const callNow = immediate && !timeoutId

    const executeFunction = () => {
      lastCallTime = Date.now()
      return func.apply(this, args)
    }

    if (timeoutId) {
      clearTimeout(timeoutId)
    }

    if (callNow) {
      return executeFunction()
    }

    timeoutId = setTimeout(() => {
      timeoutId = null
      if (!immediate) {
        return executeFunction()
      }
    }, delay)
  }
}

/**
 * 节流函数
 * @param {Function} func 要节流的函数
 * @param {number} delay 延迟时间（毫秒）
 * @returns {Function} 节流后的函数
 */
export function throttle(func, delay = 300) {
  let timeoutId = null
  let lastExecTime = 0

  return function throttled(...args) {
    const now = Date.now()

    if (!lastExecTime) {
      lastExecTime = now
      return func.apply(this, args)
    }

    if (timeoutId) {
      clearTimeout(timeoutId)
    }

    if (now - lastExecTime >= delay) {
      lastExecTime = now
      return func.apply(this, args)
    } else {
      timeoutId = setTimeout(() => {
        lastExecTime = Date.now()
        return func.apply(this, args)
      }, delay - (now - lastExecTime))
    }
  }
}

/**
 * Vue 3 组合式 API 防抖 Hook
 * @param {Function} fn 要防抖的函数
 * @param {number} delay 延迟时间
 * @param {boolean} immediate 是否立即执行
 * @returns {Function} 防抖后的函数
 */
export function useDebounce(fn, delay = 300, immediate = false) {
  return debounce(fn, delay, immediate)
}

/**
 * Vue 3 组合式 API 节流 Hook
 * @param {Function} fn 要节流的函数
 * @param {number} delay 延迟时间
 * @returns {Function} 节流后的函数
 */
export function useThrottle(fn, delay = 300) {
  return throttle(fn, delay)
}
