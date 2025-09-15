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

<script setup lang="ts">
import { reactive } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { UserOutlined, LockOutlined, ArrowRightOutlined } from '@ant-design/icons-vue'
import { i18n } from '@/base/i18n'
import { useAuthStore } from '@/store/AuthStore'
import { useRouterStore } from '@/store/RouterStore'
const userinfo = reactive({
  username: '',
  password: '',
})

const authStore = useAuthStore()
const routerStore = useRouterStore()
routerStore.clear()

const router = useRouter()
const route = useRoute()
const redirect: any = route.query.redirect || '/'
function loginHandle() {
  let formData = new FormData()
  formData.append('user', userinfo.username)
  formData.append('password', userinfo.password)
  authStore.token = userinfo.username
  router.replace(redirect)
}
</script>

<template>
  <div class="login-container">
    <div class="login-background">
      <div class="gradient-overlay"></div>
      <div class="floating-shapes">
        <div class="shape shape-1"></div>
        <div class="shape shape-2"></div>
        <div class="shape shape-3"></div>
      </div>
    </div>
    
    <div class="login-content">
      <div class="login-card">
        <div class="brand-section">
          <div class="brand-logo">
            <div class="logo-icon">
              <svg viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                <path d="M12 2L2 7L12 12L22 7L12 2Z" stroke="currentColor" stroke-width="2" stroke-linejoin="round"/>
                <path d="M2 17L12 22L22 17" stroke="currentColor" stroke-width="2" stroke-linejoin="round"/>
                <path d="M2 12L12 17L22 12" stroke="currentColor" stroke-width="2" stroke-linejoin="round"/>
              </svg>
            </div>
          </div>
          <h1 class="brand-title">
            <span class="gradient-text">Deep</span>Research
          </h1>
          <p class="brand-subtitle">AI-Powered Research Platform</p>
        </div>
        
        <div class="form-section">
          <h2 class="form-title">{{ $t('loginDomain.title') }}</h2>
          <a-form layout="vertical" :model="userinfo" ref="login-form-ref" class="login-form">
            <a-form-item
              :label="$t('loginDomain.username')"
              name="username"
              :rules="[{ required: true, message: 'Please enter your username' }]"
            >
              <a-input 
                v-model:value="userinfo.username" 
                size="large"
                placeholder="Enter your username"
                class="custom-input"
              >
                <template #prefix>
                  <UserOutlined class="input-icon" />
                </template>
              </a-input>
            </a-form-item>
            
            <a-form-item
              :label="$t('loginDomain.password')"
              name="password"
              :rules="[{ required: true, message: 'Please enter your password' }]"
            >
              <a-input-password 
                v-model:value="userinfo.password" 
                size="large"
                placeholder="Enter your password"
                class="custom-input"
              >
                <template #prefix>
                  <LockOutlined class="input-icon" />
                </template>
              </a-input-password>
            </a-form-item>
            
            <a-form-item>
              <a-button 
                @click="loginHandle" 
                size="large" 
                type="primary" 
                class="login-button"
                block
              >
                <span>{{ $t('loginDomain.login') }}</span>
                <ArrowRightOutlined class="login-arrow" />
              </a-button>
            </a-form-item>
          </a-form>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped lang="less">
.login-container {
  position: relative;
  width: 100vw;
  height: 100vh;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
}

.login-background {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  
  .gradient-overlay {
    position: absolute;
    top: 0;
    left: 0;
    width: 100%;
    height: 100%;
    background: linear-gradient(45deg, 
      rgba(35, 86, 246, 0.9) 0%, 
      rgba(103, 75, 222, 0.8) 50%, 
      rgba(246, 122, 196, 0.7) 100%);
  }
}

.floating-shapes {
  position: absolute;
  width: 100%;
  height: 100%;
  overflow: hidden;
  
  .shape {
    position: absolute;
    border-radius: 50%;
    background: rgba(255, 255, 255, 0.1);
    animation: float 6s ease-in-out infinite;
    
    &.shape-1 {
      width: 80px;
      height: 80px;
      top: 20%;
      left: 10%;
      animation-delay: 0s;
    }
    
    &.shape-2 {
      width: 120px;
      height: 120px;
      top: 60%;
      right: 15%;
      animation-delay: 2s;
    }
    
    &.shape-3 {
      width: 60px;
      height: 60px;
      bottom: 20%;
      left: 20%;
      animation-delay: 4s;
    }
  }
}

@keyframes float {
  0%, 100% {
    transform: translateY(0px) rotate(0deg);
  }
  50% {
    transform: translateY(-20px) rotate(180deg);
  }
}

.login-content {
  position: relative;
  z-index: 10;
  width: 100%;
  max-width: 1000px;
  margin: 0 auto;
  padding: 20px;
}

.login-card {
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(20px);
  border-radius: 24px;
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.1);
  display: grid;
  grid-template-columns: 1fr 1fr;
  min-height: 600px;
  overflow: hidden;
  
  @media (max-width: 768px) {
    grid-template-columns: 1fr;
    max-width: 400px;
  }
}

.brand-section {
  background: linear-gradient(135deg, #2356F6 0%, #6b4dee 100%);
  padding: 60px 40px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  text-align: center;
  color: white;
  position: relative;
  
  &::before {
    content: '';
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    background: url('data:image/svg+xml,<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100"><defs><pattern id="grain" width="100" height="100" patternUnits="userSpaceOnUse"><circle cx="50" cy="50" r="1" fill="%23ffffff" opacity="0.1"/></pattern></defs><rect width="100" height="100" fill="url(%23grain)"/></svg>') repeat;
    opacity: 0.3;
  }
}

.brand-logo {
  margin-bottom: 30px;
  position: relative;
  z-index: 1;
  
  .logo-icon {
    width: 80px;
    height: 80px;
    background: rgba(255, 255, 255, 0.2);
    border-radius: 20px;
    display: flex;
    align-items: center;
    justify-content: center;
    margin: 0 auto;
    
    svg {
      width: 40px;
      height: 40px;
      color: white;
    }
  }
}

.brand-title {
  font-size: 48px;
  font-weight: 700;
  margin: 0 0 16px 0;
  position: relative;
  z-index: 1;
  
  .gradient-text {
    background: linear-gradient(45deg, #f67ac4, #ffffff);
    -webkit-background-clip: text;
    -webkit-text-fill-color: transparent;
    background-clip: text;
  }
}

.brand-subtitle {
  font-size: 18px;
  opacity: 0.9;
  margin: 0;
  position: relative;
  z-index: 1;
}

.form-section {
  padding: 60px 40px;
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.form-title {
  font-size: 32px;
  font-weight: 600;
  color: #1a1a1a;
  margin: 0 0 40px 0;
  text-align: center;
}

.login-form {
  .ant-form-item {
    margin-bottom: 24px;
    
    .ant-form-item-label {
      padding-bottom: 8px;
      
      label {
        font-weight: 500;
        color: #4a5568;
        font-size: 14px;
      }
    }
  }
}

:deep(.custom-input) {
  border-radius: 12px;
  border: 2px solid #e2e8f0;
  transition: all 0.3s ease;
  
  &:hover {
    border-color: #2356F6;
  }
  
  &:focus,
  &.ant-input-focused {
    border-color: #2356F6;
    box-shadow: 0 0 0 3px rgba(35, 86, 246, 0.1);
  }
  
  .ant-input {
    border: none;
    box-shadow: none;
    padding-left: 12px;
    
    &:focus {
      box-shadow: none;
    }
  }
  
  .input-icon {
    color: #a0aec0;
    margin-right: 8px;
  }
}

.login-button {
  height: 56px;
  border-radius: 12px;
  background: linear-gradient(135deg, #2356F6 0%, #6b4dee 100%);
  border: none;
  font-size: 16px;
  font-weight: 600;
  margin-top: 16px;
  position: relative;
  overflow: hidden;
  transition: all 0.3s ease;
  
  &:hover {
    transform: translateY(-2px);
    box-shadow: 0 10px 25px rgba(35, 86, 246, 0.3);
  }
  
  &:active {
    transform: translateY(0);
  }
  
  .login-arrow {
    margin-left: 8px;
    transition: transform 0.3s ease;
  }
  
  &:hover .login-arrow {
    transform: translateX(4px);
  }
}

// 响应式设计
@media (max-width: 768px) {
  .login-card {
    margin: 20px;
    min-height: auto;
  }
  
  .brand-section {
    padding: 40px 20px;
  }
  
  .brand-title {
    font-size: 36px;
  }
  
  .form-section {
    padding: 40px 20px;
  }
  
  .form-title {
    font-size: 24px;
    margin-bottom: 30px;
  }
}
</style>
