import { createRouter, createWebHashHistory } from 'vue-router'
import { routes } from '@/router/defaultRoutes'

const options = {
  history: createWebHashHistory('/ui'),
  routes,
}
const router = createRouter(options)

export default router
