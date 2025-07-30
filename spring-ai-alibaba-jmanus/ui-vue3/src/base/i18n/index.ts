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

import { createI18n } from 'vue-i18n'
import { reactive } from 'vue'
import en from './en'
import zh from './zh'

export const LOCAL_STORAGE_LOCALE = 'LOCAL_STORAGE_LOCALE'

export const localeConfig = reactive({
  locale: localStorage.getItem(LOCAL_STORAGE_LOCALE) ?? 'en',
  opts: [
    {
      value: 'en',
      title: 'English',
    },
    {
      value: 'zh',
      title: '中文',
    },
  ],
})

export const i18n = createI18n({
  legacy: false,
  locale: localeConfig.locale,
  fallbackLocale: 'en',
  messages: {
    en: en,
    zh: zh,
  },
})

export const changeLanguage = async (locale: string) => {
  localStorage.setItem(LOCAL_STORAGE_LOCALE, locale)
  i18n.global.locale.value = locale as 'zh' | 'en'
  localeConfig.locale = locale

  // 同时切换后端PromptService的语言配置
  try {
    const { PromptApiService } = await import('@/api/prompt-api-service')
    await PromptApiService.importAllPromptsFromLanguage(locale)
    console.log(`Successfully switched PromptService to language: ${locale}`)
  } catch (error) {
    console.warn(`Failed to switch PromptService language to ${locale}:`, error)
    // 不阻断前端语言切换，只记录警告
  }
}
