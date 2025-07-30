/*
 * Copyright 2024-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import { createRouter, createWebHashHistory } from 'vue-router'
import { routes } from '@/router/defaultRoutes'

const options = {
  history: createWebHashHistory('/ui'),
  routes,
}
const router = createRouter(options)

// Global route guard for initialization check
router.beforeEach(async (to, _from, next) => {
  // Skip initialization check for the init page itself
  if (to.path === '/init') {
    next()
    return
  }

  try {
    // Check initialization status from server
    const response = await fetch('/api/init/status')
    const result = await response.json()
    
    if (result.success && !result.initialized) {
      // System not initialized, redirect to init page
      localStorage.removeItem('hasInitialized')
      next('/init')
      return
    } else if (result.success && result.initialized) {
      // System is initialized, save to localStorage
      localStorage.setItem('hasInitialized', 'true')
    }
  } catch (error) {
    console.warn('Failed to check initialization status:', error)
    // If check fails, rely on localStorage
    const hasInitialized = localStorage.getItem('hasInitialized') === 'true'
    if (!hasInitialized) {
      next('/init')
      return
    }
  }

  next()
})

export default router
