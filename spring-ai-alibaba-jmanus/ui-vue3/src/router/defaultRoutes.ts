import type { RouterMeta } from '@/router/RouterMeta'
import type { RouteRecordRaw } from 'vue-router'
import * as _ from 'lodash'

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
      skip: true,
    },
    children: [
      {
        path: '/conversation',
        name: 'conversation',
        component: () => import('../views/conversation/index.vue'),
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
