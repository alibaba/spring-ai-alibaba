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

import { defineStore } from 'pinia'
import { reactive } from 'vue'
import { v4 as uuidv4 } from 'uuid'

export interface KnowledgeFile {
  id: string
  name: string
  size: number
  uploadTime: string
  status: 'uploading' | 'success' | 'error'
}

export interface KnowledgeBase {
  kb_id: string
  kb_name: string
  kb_description: string
  category: string
  files: KnowledgeFile[]
  createTime: string
  updateTime: string
}

export const useKnowledgeStore = () =>
  defineStore('knowledgeStore', {
    state() {
      return reactive({
        knowledgeBases: [] as KnowledgeBase[],
      })
    },
    getters: {
      getKnowledgeBaseById: (state) => (id: string) => {
        return state.knowledgeBases.find(kb => kb.kb_id === id)
      },
      getKnowledgeBasesByCategory: (state) => (category: string) => {
        return state.knowledgeBases.filter(kb => kb.category === category)
      },
    },
    actions: {
      addKnowledgeBase(kb: Omit<KnowledgeBase, 'kb_id' | 'files' | 'createTime' | 'updateTime'>) {
        const newKb: KnowledgeBase = {
          ...kb,
          kb_id: uuidv4(),
          files: [],
          createTime: new Date().toISOString(),
          updateTime: new Date().toISOString(),
        }
        this.knowledgeBases.push(newKb)
        return newKb
      },
      
      updateKnowledgeBase(id: string, updates: Partial<Omit<KnowledgeBase, 'kb_id' | 'createTime'>>) {
        const index = this.knowledgeBases.findIndex(kb => kb.kb_id === id)
        if (index !== -1) {
          this.knowledgeBases[index] = {
            ...this.knowledgeBases[index],
            ...updates,
            updateTime: new Date().toISOString(),
          }
          return this.knowledgeBases[index]
        }
        return null
      },
      
      deleteKnowledgeBase(id: string) {
        const index = this.knowledgeBases.findIndex(kb => kb.kb_id === id)
        if (index !== -1) {
          this.knowledgeBases.splice(index, 1)
          return true
        }
        return false
      },
      
      addFileToKnowledgeBase(kbId: string, file: Omit<KnowledgeFile, 'id' | 'status'>) {
        const kb = this.getKnowledgeBaseById(kbId)
        if (kb) {
          const newFile: KnowledgeFile = {
            ...file,
            id: uuidv4(),
            uploadTime: new Date().toISOString(),
            status: 'uploading',
          }
          kb.files.push(newFile)
          kb.updateTime = new Date().toISOString()
          return newFile
        }
        return null
      },
      
      updateFileStatus(kbId: string, fileId: string, status: KnowledgeFile['status']) {
        const kb = this.getKnowledgeBaseById(kbId)
        if (kb) {
          const file = kb.files.find(f => f.id === fileId)
          if (file) {
            file.status = status
            kb.updateTime = new Date().toISOString()
            return file
          }
        }
        return null
      },
      
      removeFileFromKnowledgeBase(kbId: string, fileId: string) {
        const kb = this.getKnowledgeBaseById(kbId)
        if (kb) {
          const index = kb.files.findIndex(f => f.id === fileId)
          if (index !== -1) {
            kb.files.splice(index, 1)
            kb.updateTime = new Date().toISOString()
            return true
          }
        }
        return false
      },
      
      initSampleData() {
        if (this.knowledgeBases.length === 0) {
          // 添加一些示例数据
          this.addKnowledgeBase({
            kb_name: '产品文档库',
            kb_description: '包含所有产品相关的文档和规格说明',
            category: '产品管理',
          })
          
          this.addKnowledgeBase({
            kb_name: '技术文档库',
            kb_description: '技术架构、API文档和开发指南',
            category: '技术开发',
          })
          
          this.addKnowledgeBase({
            kb_name: '用户手册库',
            kb_description: '用户操作指南和FAQ文档',
            category: '用户支持',
          })
        }
      },
    },
    persist: true,
  })()
