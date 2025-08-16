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
import { createApp } from 'vue'
import { createRouter, createWebHistory } from 'vue-router'
import App from './App.vue'

// 引入全局样式
import './styles/global.css'

// 引入页面组件
import Home from './views/Home.vue'
import HomeSimple from './views/HomeSimple.vue'
import BusinessKnowledge from './views/BusinessKnowledge.vue'
import BusinessKnowledgeSimple from './views/BusinessKnowledgeSimple.vue'
import SemanticModel from './views/SemanticModel.vue'
import SemanticModelSimple from './views/SemanticModelSimple.vue'
import AgentList from './views/AgentList.vue'
import AgentDetail from './views/AgentDetail.vue'
import CreateAgent from './views/CreateAgent.vue'
import AgentWorkspace from './views/AgentWorkspace.vue'
import AgentRun from './views/AgentRun.vue'

// 创建路由
const routes = [
  {
    path: '/',
    name: 'Home',
    component: AgentList  // 默认显示智能体列表
  },
  {
    path: '/home-full',
    name: 'HomeFull',
    component: Home
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
  // 智能体相关路由
  {
    path: '/agent',
    redirect: '/agents'  // 重定向到智能体列表
  },
  {
    path: '/agents',
    name: 'AgentList',
    component: AgentList
  },
  {
    path: '/agent/create',
    name: 'CreateAgent',
    component: CreateAgent
  },
  {
    path: '/agent/:id',
    name: 'AgentDetail',
    component: AgentDetail
  },
  {
    path: '/agent/:id/run',
    name: 'AgentRun',
    component: AgentRun
  },
  {
    path: '/workspace',
    name: 'AgentWorkspace',
    component: AgentWorkspace
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
