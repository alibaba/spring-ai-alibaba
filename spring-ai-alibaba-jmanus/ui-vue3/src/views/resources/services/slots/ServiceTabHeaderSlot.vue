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
  <!--      example like blow-->
  <div class="__container_ServiceTabHeaderSlot">
    <span class="header-desc">{{ $t('serviceDomain.name') }}: {{ route.params?.pathId }}</span>
    <!-- <a-select
      v-model:value="versionGroupSelect.versionGroupValue"
      :bordered="false"
      style="width: 80%"
    >
      <a-select-option v-for="(item, index) in versionGroupSelect.versionGroupArr" :value="item" :key="index">
        {{ item }}
      </a-select-option>
    </a-select> -->
  </div>
</template>

<script setup lang="ts">
import { inject } from 'vue'
import { useRoute } from 'vue-router'
import { PROVIDE_INJECT_KEY } from '@/base/enums/ProvideInject'
const route = useRoute()
</script>
<style lang="less" scoped>
.__container_ServiceTabHeaderSlot {
  .header-desc {
    line-height: 30px;
    vertical-align: center;
  }
}
</style>
