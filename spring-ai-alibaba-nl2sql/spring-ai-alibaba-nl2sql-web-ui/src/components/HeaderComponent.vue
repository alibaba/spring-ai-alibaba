<!--
 * Copyright 2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
-->
<template>
  <header class="modern-header">
    <div class="header-container">
      <!-- 品牌区域 -->
      <div class="brand-section">
        <div class="brand-icon">
          <i :class="icon"></i>
        </div>
        <div class="brand-info">
          <h1 class="brand-title">{{ title }}</h1>
          <p class="brand-subtitle">{{ subtitle }}</p>
        </div>
      </div>

      <!-- 导航区域 -->
      <nav class="main-navigation">
        <router-link 
          to="/" 
          class="nav-item" 
          :class="{ active: $route.name === 'Home' || $route.name === 'NL2SQL' }"
        >
          <div class="nav-icon">
            <i class="bi bi-house"></i>
          </div>
          <div class="nav-content">
            <span class="nav-label">首页</span>
            <span class="nav-desc">NL2SQL转换</span>
          </div>
        </router-link>
        
        <router-link 
          to="/business-knowledge" 
          class="nav-item"
          :class="{ active: $route.name === 'BusinessKnowledge' }"
        >
          <div class="nav-icon">
            <i class="bi bi-book"></i>
          </div>
          <div class="nav-content">
            <span class="nav-label">知识管理</span>
            <span class="nav-desc">业务知识库</span>
          </div>
        </router-link>
        
        <router-link 
          to="/semantic-model" 
          class="nav-item"
          :class="{ active: $route.name === 'SemanticModel' }"
        >
          <div class="nav-icon">
            <i class="bi bi-diagram-3"></i>
          </div>
          <div class="nav-content">
            <span class="nav-label">语义模型</span>
            <span class="nav-desc">模型配置</span>
          </div>
        </router-link>
        
        <router-link 
          to="/agents" 
          class="nav-item"
          :class="{ active: $route.name === 'AgentList' }"
        >
          <div class="nav-icon">
            <i class="bi bi-robot"></i>
          </div>
          <div class="nav-content">
            <span class="nav-label">智能体</span>
            <span class="nav-desc">AI助手管理</span>
          </div>
        </router-link>
      </nav>

      <!-- 操作区域 -->
      <div class="header-actions">
        <button class="action-btn" data-tooltip="帮助文档">
          <i class="bi bi-question-circle"></i>
        </button>
        <button class="action-btn" data-tooltip="设置">
          <i class="bi bi-gear"></i>
        </button>
        <div class="user-menu">
          <div class="user-avatar">
            <i class="bi bi-person-circle"></i>
          </div>
        </div>
      </div>
    </div>
  </header>
</template>

<script>
export default {
  name: 'HeaderComponent',
  props: {
    title: {
      type: String,
      required: true
    },
    subtitle: {
      type: String,
      required: true
    },
    icon: {
      type: String,
      required: true
    }
  }
}
</script>

<style scoped>
.modern-header {
  background: var(--bg-primary);
  border-bottom: 1px solid var(--border-secondary);
  box-shadow: var(--shadow-sm);
  position: sticky;
  top: 0;
  z-index: var(--z-sticky);
  backdrop-filter: blur(8px);
}

.header-container {
  max-width: 1600px;
  margin: 0 auto;
  padding: 0 var(--space-xl);
  display: flex;
  align-items: center;
  justify-content: space-between;
  min-height: 80px;
}

/* 品牌区域 */
.brand-section {
  display: flex;
  align-items: center;
  gap: var(--space-lg);
  flex-shrink: 0;
}

.brand-icon {
  width: 48px;
  height: 48px;
  background: linear-gradient(135deg, var(--primary-color), var(--accent-color));
  border-radius: var(--radius-lg);
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--bg-primary);
  font-size: var(--font-size-xl);
  box-shadow: var(--shadow-sm);
  position: relative;
  overflow: hidden;
}

.brand-icon::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: linear-gradient(135deg, rgba(255,255,255,0.2), rgba(255,255,255,0));
  pointer-events: none;
}

.brand-info {
  display: flex;
  flex-direction: column;
  gap: var(--space-xs);
}

.brand-title {
  font-size: var(--font-size-xl);
  font-weight: var(--font-weight-semibold);
  color: var(--text-primary);
  margin: 0;
  background: linear-gradient(135deg, var(--primary-color), var(--accent-color));
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.brand-subtitle {
  font-size: var(--font-size-sm);
  color: var(--text-secondary);
  margin: 0;
  font-weight: var(--font-weight-medium);
}

/* 导航区域 */
.main-navigation {
  display: flex;
  align-items: center;
  gap: var(--space-sm);
  flex: 1;
  justify-content: center;
  max-width: 800px;
}

.nav-item {
  display: flex;
  align-items: center;
  gap: var(--space-sm);
  padding: var(--space-sm) var(--space-md);
  text-decoration: none;
  color: var(--text-secondary);
  border-radius: var(--radius-md);
  transition: all var(--transition-base);
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-medium);
  border: 1px solid transparent;
  position: relative;
  overflow: hidden;
  min-width: 120px;
}

.nav-item::before {
  content: '';
  position: absolute;
  top: 0;
  left: -100%;
  width: 100%;
  height: 100%;
  background: linear-gradient(90deg, transparent, rgba(95, 112, 225, 0.1), transparent);
  transition: left var(--transition-slow);
}

.nav-item:hover::before {
  left: 100%;
}

.nav-item:hover {
  background: var(--bg-secondary);
  color: var(--text-primary);
  border-color: var(--border-primary);
  transform: translateY(-1px);
}

.nav-item.active {
  background: linear-gradient(135deg, var(--primary-light), var(--accent-light));
  color: var(--primary-color);
  border-color: var(--primary-color);
  box-shadow: var(--shadow-sm);
}

.nav-item.active::before {
  display: none;
}

.nav-icon {
  width: 24px;
  height: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: var(--font-size-base);
  flex-shrink: 0;
}

.nav-content {
  display: flex;
  flex-direction: column;
  gap: 1px;
  min-width: 0;
}

.nav-label {
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-semibold);
  line-height: 1.2;
}

.nav-desc {
  font-size: var(--font-size-xs);
  color: var(--text-tertiary);
  line-height: 1.2;
  font-weight: var(--font-weight-normal);
}

.nav-item.active .nav-desc {
  color: var(--primary-color);
  opacity: 0.8;
}

/* 操作区域 */
.header-actions {
  display: flex;
  align-items: center;
  gap: var(--space-md);
  flex-shrink: 0;
}

.action-btn {
  width: 40px;
  height: 40px;
  border: 1px solid var(--border-primary);
  background: var(--bg-primary);
  border-radius: var(--radius-base);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all var(--transition-base);
  color: var(--text-secondary);
  font-size: var(--font-size-base);
  position: relative;
}

.action-btn:hover {
  border-color: var(--primary-color);
  color: var(--primary-color);
  background: var(--primary-light);
  transform: translateY(-1px);
  box-shadow: var(--shadow-sm);
}

.action-btn::before,
.action-btn::after {
  position: absolute;
  opacity: 0;
  pointer-events: none;
  transition: all var(--transition-base);
  z-index: var(--z-tooltip);
}

.action-btn::before {
  content: attr(data-tooltip);
  bottom: 100%;
  left: 50%;
  transform: translateX(-50%) translateY(-4px);
  background: var(--text-primary);
  color: var(--bg-primary);
  padding: var(--space-xs) var(--space-sm);
  border-radius: var(--radius-sm);
  font-size: var(--font-size-xs);
  white-space: nowrap;
  box-shadow: var(--shadow-md);
}

.action-btn::after {
  content: '';
  bottom: 100%;
  left: 50%;
  transform: translateX(-50%);
  border: 4px solid transparent;
  border-top-color: var(--text-primary);
}

.action-btn:hover::before,
.action-btn:hover::after {
  opacity: 1;
  transform: translateX(-50%) translateY(0);
}

.user-menu {
  margin-left: var(--space-sm);
}

.user-avatar {
  width: 40px;
  height: 40px;
  background: linear-gradient(135deg, var(--accent-color), var(--primary-color));
  border-radius: var(--radius-full);
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--bg-primary);
  font-size: var(--font-size-lg);
  cursor: pointer;
  transition: all var(--transition-base);
  box-shadow: var(--shadow-sm);
  position: relative;
  overflow: hidden;
}

.user-avatar::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: linear-gradient(135deg, rgba(255,255,255,0.2), rgba(255,255,255,0));
  pointer-events: none;
}

.user-avatar:hover {
  transform: scale(1.05);
  box-shadow: var(--shadow-md);
}

/* 响应式设计 */
@media (max-width: 1200px) {
  .header-container {
    padding: 0 var(--space-lg);
  }
  
  .main-navigation {
    gap: var(--space-xs);
  }
  
  .nav-item {
    min-width: 100px;
    padding: var(--space-sm);
  }
  
  .nav-content {
    display: none;
  }
  
  .nav-icon {
    margin: 0;
  }
}

@media (max-width: 768px) {
  .header-container {
    padding: 0 var(--space-md);
    min-height: 64px;
    flex-wrap: wrap;
    gap: var(--space-md);
  }
  
  .brand-section {
    gap: var(--space-md);
  }
  
  .brand-icon {
    width: 40px;
    height: 40px;
    font-size: var(--font-size-lg);
  }
  
  .brand-title {
    font-size: var(--font-size-lg);
  }
  
  .brand-subtitle {
    display: none;
  }
  
  .main-navigation {
    order: 3;
    width: 100%;
    justify-content: flex-start;
    overflow-x: auto;
    padding: var(--space-sm) 0;
    margin: 0 calc(-1 * var(--space-md));
    padding-left: var(--space-md);
    padding-right: var(--space-md);
  }
  
  .nav-item {
    flex-shrink: 0;
    min-width: 80px;
    padding: var(--space-xs) var(--space-sm);
  }
  
  .header-actions {
    gap: var(--space-sm);
  }
  
  .action-btn {
    width: 36px;
    height: 36px;
    font-size: var(--font-size-sm);
  }
  
  .user-avatar {
    width: 36px;
    height: 36px;
    font-size: var(--font-size-base);
  }
}

@media (max-width: 480px) {
  .header-container {
    padding: 0 var(--space-sm);
    min-height: 56px;
  }
  
  .brand-section {
    gap: var(--space-sm);
  }
  
  .brand-icon {
    width: 36px;
    height: 36px;
    font-size: var(--font-size-base);
  }
  
  .brand-title {
    font-size: var(--font-size-base);
  }
  
  .main-navigation {
    margin: 0 calc(-1 * var(--space-sm));
    padding-left: var(--space-sm);
    padding-right: var(--space-sm);
  }
  
  .nav-item {
    min-width: 60px;
    padding: var(--space-xs);
  }
  
  .nav-icon {
    width: 20px;
    height: 20px;
    font-size: var(--font-size-sm);
  }
  
  .header-actions {
    gap: var(--space-xs);
  }
  
  .action-btn {
    width: 32px;
    height: 32px;
    font-size: var(--font-size-xs);
  }
  
  .user-avatar {
    width: 32px;
    height: 32px;
    font-size: var(--font-size-sm);
  }
}

/* 滚动条样式 */
.main-navigation::-webkit-scrollbar {
  height: 2px;
}

.main-navigation::-webkit-scrollbar-track {
  background: var(--bg-secondary);
}

.main-navigation::-webkit-scrollbar-thumb {
  background: var(--primary-color);
  border-radius: var(--radius-sm);
}

.main-navigation::-webkit-scrollbar-thumb:hover {
  background: var(--primary-hover);
}
</style>
