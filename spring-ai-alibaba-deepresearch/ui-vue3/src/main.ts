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

import { createApp } from 'vue'
import { createPinia } from 'pinia'
import piniaPluginPersistedstate from 'pinia-plugin-persistedstate'
import Antd from 'ant-design-vue'

import router from './router'
import App from './App.vue'
import 'ant-design-vue/dist/reset.css'
import { i18n } from '@/base/i18n'

import Vue3ColorPicker from 'vue3-colorpicker'
import 'vue3-colorpicker/style.css'
import { useAuthStore } from '@/store/AuthStore'
import { useRouterStore } from '@/store/RouterStore'

const app = createApp(App)
const pinia = createPinia()
pinia.use(piniaPluginPersistedstate)

app.use(pinia).use(Antd).use(Vue3ColorPicker).use(i18n).use(router).mount('#app')

const authStore = useAuthStore()
const routerStore = useRouterStore()
router.beforeEach((to, from, next) => {
  if (to.path === '/login') {
    next()
  }
  // todo 模拟登录
  const token = authStore.token
  if (null == token) {
    next(`/login?redirect=${to.fullPath}`)
    return
  }
  if (
    routerStore.needRecordPath.includes(to.name) &&
    !routerStore.needRecordPath.includes(from.name)
  ) {
    console.log(from.name, to.name)
    routerStore.push(from.fullPath)
  }
  next()
})
