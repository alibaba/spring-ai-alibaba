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

Mock.mock('/mock/application/metrics', 'get', () => {
  return {
    code: 200,
    message: 'success',
    data: 'http://8.147.104.101:3000/d/a0b114ca-edf7-4dfe-ac2c-34a4fc545fed/application?orgId=1&refresh=1m&from=1711855893859&to=1711877493859&theme=light'
  }
})

Mock.mock(devTool.mockUrl('/mock/application/search'), 'get', () => {
  const total = Mock.mock('@integer(3, 20)')
  const list = []
  for (let i = 0; i < total; i++) {
    list.push({
      appName: Mock.Random.pick([
        'QuickStartApplication',
        'shop-comment',
        'shop-detail',
        'shop-order',
        'shop-user'
      ]),
      deployClusters: [Mock.Random.pick(['default', 'prod', 'test'])],
      instanceCount: Mock.mock('@integer(1, 5)'),
      registryClusters: [`${Mock.mock('@ip')}:8848`]
    })
  }

  return {
    code: 200,
    msg: 'success',
    data: {
      list: list,
      pageInfo: {
        Total: total,
        NextOffset: ''
      }
    }
  }
})

Mock.mock('/mock/application/instance/statistics', 'get', () => {
  return {
    code: 1000,
    message: 'success',
    data: {
      instanceTotal: 43,
      versionTotal: 4,
      cpuTotal: '56c',
      memoryTotal: '108.2GB'
    }
  }
})

Mock.mock(devTool.mockUrl('/mock/application/instance/info'), 'get', () => {
  let total = Mock.mock('@integer(8, 100)')
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
      startTime: '2023-06-09 03:47:10',
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
        Total: list.length,
        NextOffset: 0
      },
      list: list
    })
  }
})

Mock.mock(devTool.mockUrl('/mock/application/detail'), 'get', () => {
  return {
    code: 200,
    msg: 'success',
    data: {
      appName: Mock.mock('@word(10,20)'),
      appTypes: Mock.mock({
        'array|2-5': ['@word(5,10)']
      }).array,
      deployClusters: Mock.mock({
        'array|3-6': ['@word(8,15)']
      }).array,
      dubboPorts: Mock.mock({
        'array|1-3': ['@integer(10000,65535)']
      }).array,
      dubboVersions: Mock.mock({
        'array|2-4': ['@word(3,8)']
      }).array,
      images: Mock.mock({
        'array|2-5': ['@word(10,20)']
      }).array,
      registerClusters: Mock.mock({
        'array|2-4': ['@word(8,15)']
      }).array,
      registerModes: Mock.mock({
        'array|1-3': ['@word(5,10)']
      }).array,
      rpcProtocols: Mock.mock({
        'array|2-4': ['@word(3,8)']
      }).array,
      serialProtocols: Mock.mock({
        'array|2-4': ['@word(4,8)']
      }).array,
      workloads: Mock.mock({
        'array|3-6': ['@word(6,12)']
      }).array
    }
  }
})

Mock.mock('/mock/application/event', 'get', () => {
  let list = Mock.mock({
    'list|10': [
      {
        desc: `Scaled down replica set shop-detail-v1-5847b7cdfd to @integer(3,10) from @integer(3,10)`,
        time: '@DATETIME("yyyy-MM-dd HH:mm:ss")',
        type: 'deployment-controller'
      }
    ]
  })
  return {
    code: 200,
    message: 'success',
    data: {
      ...list
    }
  }
})

Mock.mock(devTool.mockUrl('/mock/application/service/form'), 'get', () => {
  return {
    code: 200,
    message: 'success',
    data: {
      list: [],
      pageInfo: {
        Total: 0,
        NextOffset: ''
      }
    }
  }
})
