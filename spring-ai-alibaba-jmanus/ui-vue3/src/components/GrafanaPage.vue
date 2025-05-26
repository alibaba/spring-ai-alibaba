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
  <div class="__container_tabDemo3">
    <!--    <div class="option">-->
    <!--      <a-button class="btn" @click="refresh"> refresh </a-button>-->
    <!--      <a-button class="btn" @click="newPageForGrafana"> grafana </a-button>-->
    <!--    </div>-->
    <a-spin class="spin" :spinning="!grafana.showIframe">
      <div class="__container_iframe_container">
        <iframe
          v-if="grafana.showIframe"
          :onload="onIframeLoad"
          id="grafanaIframe"
          style="padding-top: 60px"
          :src="grafana.url"
          frameborder="0"
        ></iframe>
      </div>
    </a-spin>
  </div>
</template>

<script setup lang="ts">
import { inject, onMounted, ref } from 'vue'
import { PROVIDE_INJECT_KEY } from '@/base/enums/ProvideInject'
import { useRoute } from 'vue-router'

const grafana: any = inject(PROVIDE_INJECT_KEY.GRAFANA)

const grafanaUrl = ref('')
const route = useRoute()
onMounted(async () => {
  let res = await grafana.api({})
  grafana.url = `${window.location.origin}/grafana/d/${
    res.data?.baseURL.split('/d/')[1].split('?')[0]
  }?var-${grafana.type}=${grafana.name}&kiosk=tv`
  grafana.showIframe = true
})

function tryDo(handle: any) {
  try {
    handle()
  } catch (e) {
    console.log(e)
  }
}

function onIframeLoad() {
  console.log('The iframe has been loaded.')
  setTimeout(() => {
    try {
      let iframeDocument = document.querySelector('#grafanaIframe').contentDocument
      tryDo(() => {
        iframeDocument.querySelector('header').remove()
      })
      tryDo(() => {
        iframeDocument.querySelector(`[data-testid*='controls']`).remove()
      })
      setTimeout(() => {
        tryDo(() => {
          iframeDocument.querySelector(`[data-testid*='navigation mega-menu']`).remove()
        })
        tryDo(() => {
          for (let querySelectorAllElement of iframeDocument.querySelectorAll(
            `[data-testid*='Panel menu']`
          )) {
            querySelectorAllElement.remove()
          }
        })
      }, 2000)
    } catch (e) {}
    grafana.showIframe = true
  }, 1000)
}

function newPageForGrafana() {
  window.open(grafana.url, '_blank')
}
</script>
<style lang="less" scoped>
.__container_iframe_container {
  z-index: 1;
  position: relative;
  width: calc(100vw - 332px);
  height: calc(100vh - 200px);
  clip-path: inset(0px 0px);

  #grafanaIframe {
    z-index: 0;
    top: -112px;
    position: absolute;
    width: calc(100vw - 332px);
    height: calc(100vh - 200px);
  }
}
</style>
