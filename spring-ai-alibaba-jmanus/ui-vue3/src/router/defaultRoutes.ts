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

import type { RouteRecordRaw } from 'vue-router'
import * as _ from 'lodash'

export declare type RouteRecordType = RouteRecordRaw & {
  key?: string
  name: string
  children?: RouteRecordType[]
  meta?: {
    icon?: string
  }
}

export const routes: Readonly<RouteRecordType[]> = [
  {
    path: '/',
    name: 'Root',
    redirect: '/home',
    meta: {
      skip: true,
    },
    children: [
      {
        path: '/home',
        name: 'conversation',
        component: () => import('../views/home/index.vue'),
        meta: {
          icon: 'carbon:chat',
          fullscreen: true,
        },
      },
      {
        path: '/plan/:id?',
        name: 'plan',
        component: () => import('../views/plan/index.vue'),
        meta: {
          icon: 'carbon:plan',
          fullscreen: true,
        },
      },
      {
        path: '/configs',
        name: 'configs',
        component: () => import('../views/configs/index.vue'),
        meta: {
          icon: 'carbon:settings-adjust',
        },
      },
    ],
  },
  {
    path: '/:catchAll(.*)',
    name: 'notFound',
    component: () => import('../views/error/notFound.vue'),
    meta: {
      skip: true,
    },
  },
]

function handlePath(...paths: any[]) {
  return paths.join('/').replace(/\/+/g, '/')
}

function handleRoutes(
  routes: readonly RouteRecordType[] | undefined,
  parent: RouteRecordType | undefined
) {
  if (!routes) return
  for (const route of routes) {
    if (parent) {
      route.path = handlePath(parent?.path, route.path)
    }
    if (route.redirect) {
      route.redirect = handlePath(route.path, route.redirect || '')
    }

    handleRoutes(route.children, route)
  }
}

handleRoutes(routes, undefined)
