import { createApp } from 'vue'
import { createRouter, createWebHistory } from 'vue-router'
import App from './App.vue'

// 引入页面组件
import Home from './views/Home.vue'
import HomeSimple from './views/HomeSimple.vue'
import BusinessKnowledge from './views/BusinessKnowledge.vue'
import BusinessKnowledgeSimple from './views/BusinessKnowledgeSimple.vue'
import SemanticModel from './views/SemanticModel.vue'
import SemanticModelSimple from './views/SemanticModelSimple.vue'
import TestPage from './views/TestPage.vue'

// 创建路由
const routes = [
  {
    path: '/',
    name: 'Home',
    component: Home  // 恢复使用完整版本
  },
  {
    path: '/home-full',
    name: 'HomeFull',
    component: Home
  },
  {
    path: '/test',
    name: 'TestPage',
    component: TestPage
  },
  {
    path: '/nl2sql',
    name: 'NL2SQL',
    component: Home  // 使用完整版本
  },
  {
    path: '/business-knowledge',
    name: 'BusinessKnowledge',
    component: BusinessKnowledge
  },
  {
    path: '/semantic-model',
    name: 'SemanticModel',
    component: SemanticModel
  },
  // 简化版本的测试路由
  {
    path: '/simple/home',
    name: 'HomeSimple',
    component: HomeSimple
  },
  {
    path: '/simple/business-knowledge',
    name: 'BusinessKnowledgeSimple',
    component: BusinessKnowledgeSimple
  },
  {
    path: '/simple/semantic-model',
    name: 'SemanticModelSimple',
    component: SemanticModelSimple
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 创建应用实例
const app = createApp(App)
app.use(router)
app.mount('#app')
