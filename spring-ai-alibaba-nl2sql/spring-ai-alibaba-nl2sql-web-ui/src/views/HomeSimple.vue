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
  <div>
    <HeaderComponent 
      title="NL2SQL 自然语言转SQL演示"
      subtitle="输入自然语言问题，系统将自动转换为SQL查询语句"
      icon="bi bi-database"
    />

    <div class="container">
      <div class="search-container">
        <input 
          type="text" 
          v-model="query"
          class="search-input" 
          placeholder="例如：查询销售额前10的产品..." 
          autofocus
        >
        <button class="search-button" @click="performSearch">
          <i class="bi bi-search"></i> 查询
        </button>
        <button class="init-button" @click="initializeDataSource">
          <i class="bi bi-database-add"></i> 初始化数据源
        </button>
      </div>

      <div class="result-container">
        <div class="result-header">
          <div class="result-title">
            <i class="bi bi-file-earmark-text"></i> 查询结果
          </div>
        </div>
        <div class="result-content">
          <div v-if="showEmptyState" class="empty-state">
            <div class="empty-icon">
              <i class="bi bi-chat-square-text"></i>
            </div>
            <div class="empty-text">
              输入自然语言问题，查看系统如何将其转换为SQL查询语句
            </div>
            <div class="example-queries">
              <div 
                v-for="example in exampleQueries" 
                :key="example"
                class="example-query"
                @click="useExampleQuery(example)"
              >
                {{ example }}
              </div>
            </div>
          </div>
          
          <div v-if="result" class="result-text">
            {{ result }}
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { ref } from 'vue'
import HeaderComponent from '../components/HeaderComponent.vue'

export default {
  name: 'Home',
  components: {
    HeaderComponent
  },
  setup() {
    const query = ref('')
    const showEmptyState = ref(true)
    const result = ref('')
    
    const exampleQueries = [
      '查询销售额最高的5个产品',
      '分析2025年6月的销售情况',
      '查询每个分类下已成交商品中销量最高的商品'
    ]

    const useExampleQuery = (example) => {
      query.value = example
      performSearch()
    }

    const performSearch = () => {
      if (!query.value) {
        alert('请输入查询内容')
        return
      }
      
      showEmptyState.value = false
      result.value = `正在处理查询: ${query.value}`
      
      // 模拟API调用
      setTimeout(() => {
        result.value = `查询结果: ${query.value} - 这是一个模拟的查询结果`
      }, 1000)
    }

    const initializeDataSource = () => {
      result.value = '正在初始化数据源...'
      showEmptyState.value = false
      
      setTimeout(() => {
        result.value = '数据源初始化完成！'
      }, 2000)
    }

    return {
      query,
      showEmptyState,
      result,
      exampleQueries,
      useExampleQuery,
      performSearch,
      initializeDataSource
    }
  }
}
</script>

<style scoped>
.container {
  max-width: 100%;
  margin: 0 auto;
  padding: 1rem 2rem;
}

.search-container {
  display: flex;
  gap: 0.5rem;
  margin-bottom: 1rem;
}

.search-input {
  flex: 1;
  padding: 0.75rem 1rem;
  font-size: 1rem;
  border: 1px solid var(--border-color);
  border-radius: var(--radius);
  transition: all 0.3s;
  outline: none;
}

.search-input:focus {
  border-color: var(--primary-color);
  box-shadow: 0 0 0 2px rgba(24, 144, 255, 0.2);
}

.search-button {
  padding: 0.75rem 1.5rem;
  background-color: var(--primary-color);
  color: white;
  border: none;
  border-radius: var(--radius);
  font-size: 1rem;
  cursor: pointer;
  transition: all 0.3s;
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.search-button:hover {
  background-color: #40a9ff;
}

.init-button {
  padding: 0.75rem 1.5rem;
  background-color: var(--secondary-color);
  color: white;
  border: none;
  border-radius: var(--radius);
  font-size: 1rem;
  cursor: pointer;
  transition: all 0.3s;
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.init-button:hover {
  background-color: #73d13d;
}

.result-container {
  background-color: var(--card-bg);
  border-radius: var(--radius);
  box-shadow: var(--shadow);
  overflow: hidden;
  margin-bottom: 1.5rem;
}

.result-header {
  padding: 1rem 1.5rem;
  background-color: #fafafa;
  border-bottom: 1px solid var(--border-color);
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.result-title {
  font-size: 1.1rem;
  font-weight: 500;
  color: #333;
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.result-content {
  padding: 1.5rem;
  min-height: 200px;
  max-height: 600px;
  overflow-y: auto;
  line-height: 1.7;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 3rem 1rem;
  color: #999;
  text-align: center;
}

.empty-icon {
  font-size: 3rem;
  margin-bottom: 1rem;
  color: #ccc;
}

.empty-text {
  font-size: 1rem;
  max-width: 400px;
}

.example-queries {
  margin-top: 1.5rem;
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
}

.example-query {
  padding: 0.5rem 1rem;
  background-color: #f0f5ff;
  border-radius: 20px;
  font-size: 0.9rem;
  cursor: pointer;
  transition: all 0.2s;
  border: 1px solid #d6e4ff;
}

.example-query:hover {
  background-color: #d6e4ff;
}

.result-text {
  padding: 1rem;
  background-color: #f9f9f9;
  border-radius: var(--radius);
  border: 1px solid var(--border-color);
}

@media (max-width: 768px) {
  .container {
    padding: 1rem;
  }

  .search-container {
    flex-direction: column;
  }

  .search-button,
  .init-button {
    width: 100%;
    justify-content: center;
  }
}
</style>
