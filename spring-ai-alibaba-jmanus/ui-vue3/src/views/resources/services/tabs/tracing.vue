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
  <div class="__container_app_monitor">
    <GrafanaPage></GrafanaPage>
  </div>
</template>

<script setup lang="ts">
import GrafanaPage from '@/components/GrafanaPage.vue'
import { provide, reactive, ref } from 'vue'
import { PROVIDE_INJECT_KEY } from '@/base/enums/ProvideInject'
import { useRoute } from 'vue-router'
import { getServiceTracingDashboard } from '@/api/service/service'

const route = useRoute()
provide(
  PROVIDE_INJECT_KEY.GRAFANA,
  reactive({
    api: getServiceTracingDashboard,
    showIframe: false,
    name: route.params?.pathId + ':22222',
    type: 'service'
  })
)
</script>
<style lang="less" scoped></style>
