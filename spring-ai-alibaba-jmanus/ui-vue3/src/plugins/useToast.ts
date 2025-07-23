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
import { createApp } from 'vue'
import Toast from '@/components/toast/Toast.vue'

type ToastInstance = InstanceType<typeof Toast>

let toastInstance: ToastInstance | null = null
export const useToast = () => {
  if (!toastInstance) {
    const app = createApp(Toast)
    const container = document.createElement('div')
    document.body.appendChild(container)
    const instance = app.mount(container)

    // Assign mounted instance to toastInstance
    toastInstance = instance as ToastInstance
  }

  return {
    // Show success message
    success: (msg: string, duration?: number) => {
      toastInstance?.show(msg, 'success', duration)
    },

    // Show error message
    error: (msg: string, duration?: number) => {
      toastInstance?.show(msg, 'error', duration)
    },
  }
}
