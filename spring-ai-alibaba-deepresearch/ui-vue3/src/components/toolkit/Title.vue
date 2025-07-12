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
  <div style="line-height: 1.5em; margin: 20px 0">
    <span
      class="__container_Gap"
      :style="{ background: token.colorPrimary }"
      style="width: 4px; margin-right: 10px; height: auto; border-radius: 2px"
      >&nbsp;</span
    >
    <span style="font-size: 1.1em; font-weight: bold">
      <slot></slot>
    </span>
  </div>
</template>

<script setup lang="ts">
import { theme } from 'ant-design-vue'

const props = defineProps({})
const { useToken } = theme
const { token } = useToken()
</script>
<style lang="less" scoped>
.__container_Gap {
  display: inline-block;
}
</style>
