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

  // Only switch frontend language, do not reset backend prompt language
  console.log(`Successfully switched frontend language to: ${locale}`)
}

/**
 * Change language during initialization and reset all agents and prompts
 * This function is used during the initial setup process
 */
export const changeLanguageWithAgentReset = async (locale: string) => {
  // First change the frontend language
  await changeLanguage(locale)

  try {
    // Reset prompts to the new language
    const promptResponse = await fetch(`/admin/prompts/switch-language?language=${locale}`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
    })

    if (promptResponse.ok) {
      console.log(`Successfully reset prompts to language: ${locale}`)
    } else {
      const promptError = await promptResponse.text()
      console.error(`Failed to reset prompts to language: ${locale}`, promptError)
      // Continue with agent initialization even if prompt reset fails
    }

    // Initialize agents with the new language (used during initial setup)
    const agentResponse = await fetch('/api/agent-management/initialize', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({ language: locale }),
    })

    if (agentResponse.ok) {
      const result = await agentResponse.json()
      console.log(`Successfully initialized agents with language: ${locale}`, result)
    } else {
      const error = await agentResponse.json()
      console.error(`Failed to initialize agents with language: ${locale}`, error)
      throw new Error(error.error || 'Failed to initialize agents')
    }
  } catch (error) {
    console.error('Error initializing agents and prompts during language change:', error)
    throw error
  }
}
