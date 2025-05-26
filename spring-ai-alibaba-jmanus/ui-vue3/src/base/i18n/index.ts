/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import { createI18n } from 'vue-i18n'
import { LOCAL_STORAGE_LOCALE } from '@/base/constants'
import { reactive } from 'vue'
import en from './en'
import zh from './zh'

export const localeConfig = reactive({
  locale: localStorage.getItem(LOCAL_STORAGE_LOCALE) || 'zh',
  opts: [
    {
      value: 'en',
      title: 'English'
    },
    {
      value: 'zh',
      title: '中文'
    }
  ]
})

export const i18n = createI18n({
  legacy: false,
  locale: localeConfig.locale,
  fallbackLocale: 'zh-CN',
  messages: {
    en: en,
    zh: zh
  }
})

export const changeLanguage = (l: string) => {
  const locale = l === 'en' ? 'en-US' : 'zh-CN'
  localStorage.setItem(LOCAL_STORAGE_LOCALE, locale)
  // @ts-ignore
  i18n.global.locale.value = locale
}
