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

import type { RouterMeta } from '@/router/RouterMeta'
import type { RouteRecordRaw } from 'vue-router'
import LayoutTab from '../layout/tab/layout_tab.vue'
import * as _ from 'lodash'
import AppTabHeaderSlot from '@/views/resources/applications/slots/AppTabHeaderSlot.vue'
import ServiceTabHeaderSlot from '@/views/resources/services/slots/ServiceTabHeaderSlot.vue'
import InstanceTabHeaderSlot from '@/views/resources/instances/slots/InstanceTabHeaderSlot.vue'

export declare type RouteRecordType = RouteRecordRaw & {
  key?: string
  name: string
  children?: RouteRecordType[]
  meta?: RouterMeta
}

export const routes: Readonly<RouteRecordType[]> = [
  {
    path: '/',
    name: 'Root',
    redirect: '/conversation',
    component: () => import('../layout/index.vue'),
    meta: {
      skip: true
    },
    children: [
      {
        path: '/conversation',
        name: 'menu.conversation',
        component: () => import('../views/conversation/index.vue'),
        meta: {
          icon: 'carbon:web-services-cluster'
        }
      },
      {
        path: '/plan',
        name: 'menu.planExecution',
        component: () => import('../views/plan/index.vue'),
        meta: {
          icon: 'carbon:web-services-cluster'
        }
      },
      {
        path: '/dashboard',
        name: 'menu.configsRoot',
        meta: {
          icon: 'carbon:web-services-cluster',
          title: '配置管理'
        },
        children: [
          {
            path: '/dashboard/basic',
            name: 'configs.basicConfig.title',
            component: LayoutTab,
            meta: {
              tab_parent: true,
              title: 'configs.basicConfig.title',
              slots: {
                header: InstanceTabHeaderSlot
              }
            },
            children: [
              {
                path: '/',
                name: 'configs.basicConfig.detail',
                component: () => import('../views/dashboard/basicConfig/index.vue'),
                meta: {
                  hidden: true,
                  title: 'configs.basicConfig.title'
                }
              }
            ]
          },
          {
            path: '/dashboard/agent',
            name: 'configs.agentConfig.title',
            component: LayoutTab,
            meta: {
              tab_parent: true,
              title: 'configs.agentConfig.title',
              slots: {
                header: ServiceTabHeaderSlot
              }
            },
            children: [
              {
                path: '/',
                name: 'configs.agentConfig.detail',
                component: () => import('../views/dashboard/agentConfig/index.vue'),
                meta: {
                  hidden: true,
                  title: 'configs.agentConfig.title'
                }
              }
            ]
          },
          {
            path: '/dashboard/mcp',
            name: 'configs.mcpConfig.title',
            component: LayoutTab,
            meta: {
              tab_parent: true,
              title: 'configs.mcpConfig.title',
              slots: {
                header: ServiceTabHeaderSlot
              }
            },
            children: [
              {
                path: '/',
                name: 'configs.mcpConfig.detail',
                component: () => import('../views/dashboard/mcpConfig/index.vue'),
                meta: {
                  hidden: true,
                  title: 'configs.mcpConfig.title'
                }
              }
            ]
          }
        ]
      }
    ]
  },
  {
    path: '/:catchAll(.*)',
    name: 'notFound',
    component: () => import('../views/error/notFound.vue'),
    meta: {
      skip: true
    }
  }
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

    if (route.meta) {
      route.meta._router_key = _.uniqueId('__router_key')
      route.meta.parent = parent
      // fixme, its really useful for tab_router judging how to  show tab
      route.meta.skip = route.meta.skip === true ? true : false
    } else {
      route.meta = {
        _router_key: _.uniqueId('__router_key'),
        skip: false
      }
    }
    handleRoutes(route.children, route)
  }
}

handleRoutes(routes, undefined)
