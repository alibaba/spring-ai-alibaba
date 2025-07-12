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
  <div class="background">
    <a-card class="login">
      <a-row class="title">
        <div>{{ $t('loginDomain.title') }}</div>
      </a-row>
      <a-row>
        <a-form layout="vertical" :model="userinfo" ref="login-form-ref">
          <a-form-item
            class="item"
            :label="$t('loginDomain.username')"
            name="username"
            :rules="[{ required: true }]"
          >
            <a-input type="" v-model:value="userinfo.username"></a-input>
          </a-form-item>
          <a-form-item
            class="item"
            :label="$t('loginDomain.password')"
            name="password"
            :rules="[{ required: true }]"
          >
            <a-input type="password" v-model:value="userinfo.password"></a-input>
          </a-form-item>
          <a-form-item class="item" label="">
            <a-button @click="loginHandle" size="large" type="primary" class="login-btn"
              >{{ $t('loginDomain.login') }}
            </a-button>
          </a-form-item>
        </a-form>
      </a-row>
    </a-card>
  </div>
</template>

<style scoped lang="less">
.background {
  background: url('assets/login.jpg') no-repeat center center fixed;
  background-size: cover;
  //background-color: #f4f4f4; height: 100vh;
  width: 100vw;
  height: 100vh;
  display: flex;
  justify-content: center;
  align-items: center;

  .login {
    background-color: #fff;
    padding: 20px;
    border-radius: 12px;
    box-shadow: 0 0 10px rgba(0, 0, 0, 0.1);
    display: flex;
    justify-content: center;
    width: 22vw;
    //height: 30vh;

    .title {
      width: 100%;
      display: flex;
      justify-content: center;
      font-size: 20px;
      font-weight: 500;
      margin-bottom: 20px;
    }

    .login-btn {
      width: 100%;
    }
  }
}
</style>
