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
  <div class="plan-execution">
    <h1>{{ $t('menu.planExecution') }}</h1>
    <p class="description">{{ $t('menu.planExecution') }}</p>
  </div>
</template>

<script setup lang="ts">
import { PRIMARY_COLOR } from '@/base/constants'
import { onMounted, reactive } from 'vue'
import { getClusterInfo } from '@/api/service/clusterInfo'
import { getMetricsMetadata } from '@/api/service/serverInfo'
import { useRoute } from 'vue-router'
import { Chart } from '@antv/g2'

const routeName = <string>useRoute().name
let clusterInfo = reactive({
  info: <{ [key: string]: any }>{},
  report: <{ [key: string]: { value: any; icon: string } }>{}
})

let metricsMetadata = reactive({
  info: <{ [key: string]: string }>{}
})

onMounted(() => {
  setTimeout(async () => {
    let clusterData = (await getClusterInfo({})).data
    metricsMetadata.info = <{ [key: string]: string }>(await getMetricsMetadata({})).data
    clusterInfo.info = <{ [key: string]: any }>clusterData
    clusterInfo.report = {
      application: {
        icon: 'cil:applications-settings',
        value: clusterInfo.info.appCount
      },
      services: {
        icon: 'carbon:microservices-1',
        value: clusterInfo.info.serviceCount
      },
      instances: {
        icon: 'ri:instance-line',
        value: clusterInfo.info.insCount
      }
    }

    // releasesChart
    const releasesData: any = []
    const totalReleases = Object.values(clusterInfo.info.releases).reduce(
      (acc: number, count: number) => acc + count,
      0
    )

    if (typeof clusterInfo.info.releases === 'object') {
      Object.keys(clusterInfo.info.releases).forEach((key) => {
        const count = clusterInfo.info.releases[key]
        releasesData.push({
          item: key,
          count: count,
          percent: count / totalReleases
        })
      })
    }

    const releasesChart = new Chart({
      container: 'releases_container',
      width: 200,
      height: 200,
      autoFit: false
    })

    releasesChart.coordinate({ type: 'theta', outerRadius: 0.8, innerRadius: 0.5 })

    releasesChart
      .interval()
      .data(releasesData)
      .transform({ type: 'stackY' })
      .encode('y', 'percent')
      .encode('color', 'item')
      .legend('color', { position: 'bottom', layout: { justifyContent: 'center' } })
      .label({
        position: 'outside',
        text: (data) => `${data.item}: ${(data.percent * 100).toFixed(2)}%`
      })
      .tooltip((data) => ({
        name: data.item,
        value: `${(data.percent * 100).toFixed(2)}%`
      }))

    releasesChart
      .text()
      .style('text', '版本分布')
      // Relative position
      .style('x', '50%')
      .style('y', '50%')
      .style('fontSize', 10)
      .style('fill', '#8c8c8c')
      .style('textAlign', 'center')
      .style('textBaseline', 'middle') // 垂直对齐

    await releasesChart.render()

    // protocolsChart
    const protocolsData: any = []
    const totalProtocols = Object.values(clusterInfo.info.protocols).reduce(
      (acc: number, count: number) => acc + count,
      0
    )

    if (typeof clusterInfo.info.protocols === 'object') {
      Object.keys(clusterInfo.info.protocols).forEach((key) => {
        const count = clusterInfo.info.protocols[key]
        protocolsData.push({
          item: key,
          count: count,
          percent: count / totalProtocols
        })
      })
    }

    const protocolsChart = new Chart({
      container: 'protocols_container',
      width: 200,
      height: 200,
      autoFit: false
    })

    protocolsChart.coordinate({ type: 'theta', outerRadius: 0.8, innerRadius: 0.5 })

    protocolsChart
      .interval()
      .data(protocolsData)
      .transform({ type: 'stackY' })
      .encode('y', 'percent')
      .encode('color', 'item')
      .legend('color', { position: 'bottom', layout: { justifyContent: 'center' } })
      .label({
        position: 'outside',
        text: (data) => `${data.item}: ${(data.percent * 100).toFixed(2)}%`
      })
      .tooltip((data) => ({
        name: data.item,
        value: `${(data.percent * 100).toFixed(2)}%`
      }))

    protocolsChart
      .text()
      .style('text', '协议分布')
      // Relative position
      .style('x', '50%')
      .style('y', '50%')
      .style('fontSize', 10)
      .style('fill', '#8c8c8c')
      .style('textAlign', 'center')
      .style('textBaseline', 'middle') // 垂直对齐

    await protocolsChart.render()

    // discoveriesChart
    const discoveriesData: any = []
    const totalDiscoveries = Object.values(clusterInfo.info.discoveries).reduce(
      (acc: number, count: number) => acc + count,
      0
    )

    if (typeof clusterInfo.info.discoveries === 'object') {
      Object.keys(clusterInfo.info.discoveries).forEach((key) => {
        const count = clusterInfo.info.discoveries[key]
        discoveriesData.push({
          item: key,
          count: count,
          percent: count / totalDiscoveries
        })
      })
    }

    const discoveriesChart = new Chart({
      container: 'discoveries_container',
      width: 200,
      height: 200,
      autoFit: false
    })

    discoveriesChart.coordinate({ type: 'theta', outerRadius: 0.8, innerRadius: 0.5 })

    discoveriesChart
      .interval()
      .data(discoveriesData)
      .transform({ type: 'stackY' })
      .encode('y', 'percent')
      .encode('color', 'item')
      .legend('color', { position: 'bottom', layout: { justifyContent: 'center' } })
      .label({
        position: 'outside',
        text: (data) => `${data.item}: ${(data.percent * 100).toFixed(2)}%`
      })
      .tooltip((data) => ({
        name: data.item,
        value: `${(data.percent * 100).toFixed(2)}%`
      }))

    discoveriesChart
      .text()
      .style('text', '服务发现类型分布')
      // Relative position
      .style('x', '50%')
      .style('y', '50%')
      .style('fontSize', 10)
      .style('fill', '#8c8c8c')
      .style('textAlign', 'center')
      .style('textBaseline', 'middle') // 垂直对齐

    await discoveriesChart.render()
  })
})
</script>
<style lang="less" scoped>
.__container_home_index {
  max-height: calc(100vh - 60px);
  overflow: auto;
  .statistic {
    width: 16vw;
  }

  .statistic-card {
    border: 1px solid v-bind("(PRIMARY_COLOR) + '22'");
  }

  .statistic-icon {
    color: v-bind(PRIMARY_COLOR);
    margin-bottom: -3px;
  }

  .statistic-icon-big {
    width: 40px;
    height: 40px;
    background: v-bind('PRIMARY_COLOR');
    line-height: 44px;
    vertical-align: middle;
    text-align: center;
    border-radius: 5px;
    font-size: 56px;
    color: white;
  }
  .card {
    margin-top: 10px;
  }
}
</style>
