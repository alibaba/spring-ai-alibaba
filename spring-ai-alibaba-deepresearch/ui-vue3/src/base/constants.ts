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

import type { Component } from 'vue'
import { computed, h, reactive, ref } from 'vue'
import type { RouteRecordType } from '@/router/defaultRoutes'
import type { RouteLocationNormalizedLoaded } from 'vue-router'

// 2aacb8
export const PRIMARY_COLOR_DEFAULT = '#2356F6'

export const LOCAL_STORAGE_LOCALE = 'LOCAL_STORAGE_LOCALE'
export const LOCAL_STORAGE_THEME = 'LOCAL_STORAGE_THEME'

let item = localStorage.getItem(LOCAL_STORAGE_THEME)

export const PRIMARY_COLOR = ref(item || PRIMARY_COLOR_DEFAULT)
export const PRIMARY_COLOR_T = (percent: string) => computed(() => PRIMARY_COLOR.value + percent)

/**
 * 'Running','Pending', 'Terminating', 'Crashing'
 */
export const INSTANCE_DEPLOY_COLOR: { [key: string]: string } = {
  RUNNING: 'green',
  PENDING: 'yellow',
  TERMINATING: 'red',
  CRASHING: 'darkRed',
}
