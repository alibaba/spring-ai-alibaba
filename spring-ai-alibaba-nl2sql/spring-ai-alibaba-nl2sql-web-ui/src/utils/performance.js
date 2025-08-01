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
 * 性能监控工具
 */

class PerformanceMonitor {
  constructor() {
    this.marks = new Map()
    this.measures = new Map()
    this.observers = new Map()
    this.isEnabled = process.env.NODE_ENV === 'development'
  }

  /**
   * 标记性能时间点
   * @param {string} name 标记名称
   */
  mark(name) {
    if (!this.isEnabled) return

    const timestamp = performance.now()
    this.marks.set(name, timestamp)
    
    if (performance.mark) {
      performance.mark(name)
    }
    
    console.log(`[Performance] Mark: ${name} at ${timestamp.toFixed(2)}ms`)
  }

  /**
   * 测量两个时间点之间的性能
   * @param {string} name 测量名称
   * @param {string} startMark 开始标记
   * @param {string} endMark 结束标记
   */
  measure(name, startMark, endMark) {
    if (!this.isEnabled) return

    const startTime = this.marks.get(startMark)
    const endTime = this.marks.get(endMark)
    
    if (!startTime || !endTime) {
      console.warn(`[Performance] Cannot measure ${name}: missing marks`)
      return
    }

    const duration = endTime - startTime
    this.measures.set(name, duration)
    
    if (performance.measure) {
      performance.measure(name, startMark, endMark)
    }
    
    console.log(`[Performance] Measure: ${name} took ${duration.toFixed(2)}ms`)
    return duration
  }

  /**
   * 测量函数执行时间
   * @param {string} name 测量名称
   * @param {Function} fn 要测量的函数
   * @returns {Promise|any} 函数执行结果
   */
  async measureFunction(name, fn) {
    if (!this.isEnabled) {
      return await fn()
    }

    const startTime = performance.now()
    
    try {
      const result = await fn()
      const endTime = performance.now()
      const duration = endTime - startTime
      
      console.log(`[Performance] Function ${name} took ${duration.toFixed(2)}ms`)
      return result
    } catch (error) {
      const endTime = performance.now()
      const duration = endTime - startTime
      
      console.log(`[Performance] Function ${name} failed after ${duration.toFixed(2)}ms`)
      throw error
    }
  }

  /**
   * 监控长任务
   */
  observeLongTasks() {
    if (!this.isEnabled || !window.PerformanceObserver) return

    try {
      const observer = new PerformanceObserver((list) => {
        for (const entry of list.getEntries()) {
          console.warn(`[Performance] Long task detected: ${entry.duration.toFixed(2)}ms`)
        }
      })
      
      observer.observe({ entryTypes: ['longtask'] })
      this.observers.set('longtask', observer)
    } catch (error) {
      console.warn('[Performance] Long task observation not supported')
    }
  }

  /**
   * 监控资源加载性能
   */
  observeResources() {
    if (!this.isEnabled || !window.PerformanceObserver) return

    try {
      const observer = new PerformanceObserver((list) => {
        for (const entry of list.getEntries()) {
          if (entry.duration > 1000) { // 超过1秒的资源加载
            console.warn(`[Performance] Slow resource: ${entry.name} took ${entry.duration.toFixed(2)}ms`)
          }
        }
      })
      
      observer.observe({ entryTypes: ['resource'] })
      this.observers.set('resource', observer)
    } catch (error) {
      console.warn('[Performance] Resource observation not supported')
    }
  }

  /**
   * 监控导航性能
   */
  observeNavigation() {
    if (!this.isEnabled || !window.PerformanceObserver) return

    try {
      const observer = new PerformanceObserver((list) => {
        for (const entry of list.getEntries()) {
          console.log('[Performance] Navigation timing:', {
            domContentLoaded: entry.domContentLoadedEventEnd - entry.domContentLoadedEventStart,
            loadComplete: entry.loadEventEnd - entry.loadEventStart,
            totalTime: entry.loadEventEnd - entry.fetchStart
          })
        }
      })
      
      observer.observe({ entryTypes: ['navigation'] })
      this.observers.set('navigation', observer)
    } catch (error) {
      console.warn('[Performance] Navigation observation not supported')
    }
  }

  /**
   * 获取内存使用情况
   */
  getMemoryUsage() {
    if (!this.isEnabled || !performance.memory) return null

    return {
      used: Math.round(performance.memory.usedJSHeapSize / 1024 / 1024),
      total: Math.round(performance.memory.totalJSHeapSize / 1024 / 1024),
      limit: Math.round(performance.memory.jsHeapSizeLimit / 1024 / 1024)
    }
  }

  /**
   * 获取页面加载性能指标
   */
  getPageMetrics() {
    if (!this.isEnabled) return null

    const navigation = performance.getEntriesByType('navigation')[0]
    if (!navigation) return null

    return {
      dns: navigation.domainLookupEnd - navigation.domainLookupStart,
      tcp: navigation.connectEnd - navigation.connectStart,
      request: navigation.responseStart - navigation.requestStart,
      response: navigation.responseEnd - navigation.responseStart,
      dom: navigation.domContentLoadedEventEnd - navigation.domContentLoadedEventStart,
      load: navigation.loadEventEnd - navigation.loadEventStart,
      total: navigation.loadEventEnd - navigation.fetchStart
    }
  }

  /**
   * 启动所有性能监控
   */
  startMonitoring() {
    if (!this.isEnabled) return

    this.observeLongTasks()
    this.observeResources()
    this.observeNavigation()
    
    console.log('[Performance] Monitoring started')
  }

  /**
   * 停止所有性能监控
   */
  stopMonitoring() {
    this.observers.forEach(observer => observer.disconnect())
    this.observers.clear()
    
    console.log('[Performance] Monitoring stopped')
  }

  /**
   * 生成性能报告
   */
  generateReport() {
    if (!this.isEnabled) return null

    const report = {
      timestamp: new Date().toISOString(),
      marks: Object.fromEntries(this.marks),
      measures: Object.fromEntries(this.measures),
      memory: this.getMemoryUsage(),
      pageMetrics: this.getPageMetrics()
    }

    console.log('[Performance] Report:', report)
    return report
  }

  /**
   * 清除所有性能数据
   */
  clear() {
    this.marks.clear()
    this.measures.clear()
    
    if (performance.clearMarks) {
      performance.clearMarks()
    }
    
    if (performance.clearMeasures) {
      performance.clearMeasures()
    }
  }
}

// 创建全局实例
const performanceMonitor = new PerformanceMonitor()

/**
 * Vue 3 组合式 API Hook
 */
export function usePerformance() {
  const mark = (name) => performanceMonitor.mark(name)
  const measure = (name, start, end) => performanceMonitor.measure(name, start, end)
  const measureFunction = (name, fn) => performanceMonitor.measureFunction(name, fn)
  const getReport = () => performanceMonitor.generateReport()
  
  return {
    mark,
    measure,
    measureFunction,
    getReport
  }
}

/**
 * 装饰器：自动测量方法执行时间
 */
export function measureTime(name) {
  return function(target, propertyKey, descriptor) {
    const originalMethod = descriptor.value
    
    descriptor.value = async function(...args) {
      const measureName = name || `${target.constructor.name}.${propertyKey}`
      return await performanceMonitor.measureFunction(measureName, () => {
        return originalMethod.apply(this, args)
      })
    }
    
    return descriptor
  }
}

/**
 * 监控组件渲染性能
 */
export function measureComponentRender(componentName) {
  return {
    beforeMount() {
      performanceMonitor.mark(`${componentName}-mount-start`)
    },
    mounted() {
      performanceMonitor.mark(`${componentName}-mount-end`)
      performanceMonitor.measure(
        `${componentName}-mount`,
        `${componentName}-mount-start`,
        `${componentName}-mount-end`
      )
    },
    beforeUpdate() {
      performanceMonitor.mark(`${componentName}-update-start`)
    },
    updated() {
      performanceMonitor.mark(`${componentName}-update-end`)
      performanceMonitor.measure(
        `${componentName}-update`,
        `${componentName}-update-start`,
        `${componentName}-update-end`
      )
    }
  }
}

export default performanceMonitor
