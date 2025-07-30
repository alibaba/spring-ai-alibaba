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
    redirect: () => {
      // Check if system is initialized
      const hasInitialized = localStorage.getItem('hasInitialized') === 'true'
      if (!hasInitialized) {
        return '/init'
      }
      
      // Check if user has visited the homepage before
      const hasVisited = localStorage.getItem('hasVisitedHome') === 'true'
      return hasVisited ? '/direct' : '/home'
    },
    meta: {
      skip: true,
    },
    children: [
      {
        path: '/init',
        name: 'init',
        component: () => import('../views/init/index.vue'),
        meta: {
          fullscreen: true,
          skip: true,
        },
      },
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
        path: '/direct/:id?',
        name: 'direct',
        component: () => import('../views/direct/index.vue'),
        meta: {
          icon: 'carbon:chat',
          fullscreen: true,
        },
      },
      {
        path: '/configs/:category?',
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
      route.path = handlePath(parent.path, route.path)
    }
    if (route.redirect && typeof route.redirect === 'string') {
      route.redirect = handlePath(route.path, route.redirect || '')
    }

    handleRoutes(route.children, route)
  }
}

handleRoutes(routes, undefined)
