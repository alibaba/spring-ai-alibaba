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

import Mock from 'mockjs'
import devTool from '@/utils/DevToolUtil'

Mock.mock(devTool.mockUrl('/mock/instance/search'), 'get', () => {
  let total = Mock.mock('@integer(8, 1000)')
  let list = []
  for (let i = 0; i < total; i++) {
    list.push({
      ip: '121.90.211.162',
      name: 'shop-user',
      deployState: Mock.Random.pick(['Running', 'Pending', 'Terminating', 'Crashing']),
      deployCluster: 'tx-shanghai-1',
      registerState: 'Registed',
      registerClusters: ['ali-hangzhou-1', 'ali-hangzhou-2'],
      cpu: '1.2c',
      memory: '2349MB',
      startTime_k8s: '2023-06-09 03:47:10',
      registerTime: '2023-06-09 03:48:20',
      labels: {
        region: 'beijing',
        version: 'v1'
      }
    })
  }
  return {
    code: 200,
    msg: 'success',
    data: Mock.mock({
      pageInfo: {
        Total: total,
        NextOffset: '0'
      },
      list: list
    })
  }
})

Mock.mock(devTool.mockUrl('/mock/instance/detail'), 'get', () => {
  return {
    code: 200,
    msg: 'success',
    data: {
      deployState: 'Running',
      registerStates: 'Unregisted',
      ip: '45.7.37.227',
      rpcPort: '20880',
      appName: 'shop-user',
      workloadName: 'shop-user-prod(deployment)',
      labels: {
        app: 'shop-user',
        version: 'v1',
        region: 'beijing'
      },
      createTime: '2023/12/19 22:09:34',
      readyTime: '2023/12/19  22:12:34',
      registerTime: '2023/12/19   22:16:56',
      registerClusters: ['sz-ali-zk-f8otyo4r', 'hz-ali-zk-oqgiq9gq'],
      deployCluster: 'tx-shanghai-1',
      node: 'hz-ali-30.33.0.1',
      image: 'apache/org.apahce.dubbo.samples.shop-user:v1',
      probes: {
        startupProbe: {
          type: 'http',
          open: true
        },
        readinessProbe: {
          type: 'http',
          open: true
        },
        livenessProbe: {
          type: 'http',
          open: true
        }
      }
    }
  }
})

Mock.mock('/mock/instance/metrics', 'get', () => {
  return {
    code: 200,
    message: 'success',
    data: 'http://8.147.104.101:3000/d/dcf5defe-d198-4704-9edf-6520838880e9/instance?orgId=1&refresh=1m&from=1710644821536&to=1710731221536&theme=light'
  }
})
