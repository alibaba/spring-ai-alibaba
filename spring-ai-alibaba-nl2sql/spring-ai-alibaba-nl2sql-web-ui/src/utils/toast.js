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
import MessageToast from '../components/MessageToast.vue'

let toastContainer = null

const createToastContainer = () => {
  if (!toastContainer) {
    toastContainer = document.createElement('div')
    toastContainer.id = 'toast-container'
    document.body.appendChild(toastContainer)
  }
  return toastContainer
}

const showToast = (message, type = 'info', options = {}) => {
  const container = createToastContainer()
  const toastDiv = document.createElement('div')
  container.appendChild(toastDiv)

  const app = createApp(MessageToast, {
    message,
    type,
    duration: options.duration || 3000,
    closable: options.closable !== false,
    onClose: () => {
      app.unmount()
      if (container.contains(toastDiv)) {
        container.removeChild(toastDiv)
      }
    }
  })

  app.mount(toastDiv)
}

export const toast = {
  success: (message, options) => showToast(message, 'success', options),
  error: (message, options) => showToast(message, 'error', options),
  warning: (message, options) => showToast(message, 'warning', options),
  info: (message, options) => showToast(message, 'info', options)
}

export default toast
