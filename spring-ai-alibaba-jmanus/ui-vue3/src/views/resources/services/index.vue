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
  <div class="__container_services">
    <Icon
      icon="material-symbols:arrow-back-ios-new-rounded"
      v-if="route.name !== 'search'"
      @click="backSearch"
      class="icon-back"
    />
    <LayoutTab></LayoutTab>
  </div>
</template>

<script setup lang="ts">
import LayoutTab from '@/layout/tab/layout_tab.vue'
import { Icon } from '@iconify/vue'
import { useRoute, useRouter } from 'vue-router'

const route = useRoute()
const router = useRouter()

const backSearch = () => {
  router.push({ name: 'search' })
}
</script>
<style lang="less" scoped>
.__container_services {
  .icon-back:hover {
    cursor: pointer;
  }
}
</style>
