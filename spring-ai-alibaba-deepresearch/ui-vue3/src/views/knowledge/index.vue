<!--
  ~ Licensed to the Apache Software Foundation (ASF) under one or more
  ~ contributor license agreements.  See the NOTICE file distributed with
  ~ this work for additional information regarding copyright ownership.
  ~ The ASF licenses this file to You under the Apache License, Version 2.0
  ~ (the "License"); you may not use this file except in compliance with
  ~ the License.  You may obtain a copy of the License at
  ~
  ~     http://www.apache.org/licenses/LICENSE-2.0
  ~
  ~ Unless required by applicable law or agreed to in writing, software
  ~ distributed under the License is distributed on an "AS IS" BASIS,
  ~ WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  ~ See the License for the specific language governing permissions and
  ~ limitations under the License.
-->
<template>
  <div class="knowledge-container">
    <div class="knowledge-header">
      <h2>{{ $t('knowledge_base') }}</h2>
      <p>正在努力实现中</p>
    </div>
    
    <div class="knowledge-content">
      <a-row :gutter="24">
        <a-col :span="8">
          <a-card :title="$t('knowledge_management')" class="feature-card">
            <FileTextOutlined class="feature-icon" />
            <p>管理您的知识库文档，组织和分类文档内容</p>
            <a-button type="primary" block @click="goManagement">进入管理</a-button>
          </a-card>
        </a-col>
        
        <a-col :span="8">
          <a-card :title="$t('document_upload')" class="feature-card">
            <UploadOutlined class="feature-icon" />
            <p>上传新的文档到知识库，支持多种文件格式</p>
            <a-button type="primary" block disabled>上传文档</a-button>
          </a-card>
        </a-col>
        
        <a-col :span="8">
          <a-card :title="$t('knowledge_search')" class="feature-card">
            <SearchOutlined class="feature-icon" />
            <p>在知识库中搜索相关信息和文档内容</p>
            <a-button type="primary" block disabled>开始搜索</a-button>
          </a-card>
        </a-col>
      </a-row>
      
      <div class="recent-documents">
        <h3>最近文档</h3>
        <a-list
          :data-source="recentDocuments"
          item-layout="horizontal"
        >
          <template #renderItem="{ item }">
            <a-list-item>
              <a-list-item-meta
                :title="item.title"
                :description="item.description"
              >
                <template #avatar>
                  <FileTextOutlined />
                </template>
              </a-list-item-meta>
              <template #actions>
                <a>查看</a>
                <a>编辑</a>
              </template>
            </a-list-item>
          </template>
        </a-list>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import {
  FileTextOutlined,
  UploadOutlined,
  SearchOutlined,
} from '@ant-design/icons-vue'

const router = useRouter()
const route = useRoute()
const goManagement = () => router.push('/knowledge/management')

const recentDocuments = ref([
  {
    title: '产品需求文档',
    description: '最新的产品功能需求和规格说明',
  },
  {
    title: '技术架构设计',
    description: '系统整体架构设计和技术选型',
  },
  {
    title: '用户手册',
    description: '面向最终用户的操作指南和说明',
  },
])
</script>

<style lang="less" scoped>
.knowledge-container {
  padding: 24px;
  height: 100%;
  overflow-y: auto;
  max-width: 1200px;
  margin: 0 auto;
}

.knowledge-header {
  margin-bottom: 32px;
  text-align: center;
  
  h2 {
    margin-bottom: 8px;
    color: #1890ff;
  }
  
  p {
    color: #666;
    font-size: 16px;
  }
}

.knowledge-content {
  .feature-card {
    text-align: center;
    height: 200px;
    
    .feature-icon {
      font-size: 48px;
      color: #1890ff;
      margin-bottom: 16px;
      display: block;
    }
    
    p {
      margin-bottom: 24px;
      color: #666;
    }
  }
  
  .recent-documents {
    margin-top: 48px;
    
    h3 {
      margin-bottom: 16px;
      color: #333;
    }
  }
}
</style>
