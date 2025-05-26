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

Mock.mock('/mock/service/distribution', 'get', {
  code: 200,
  message: 'success',
  data: {
    total: 8,
    curPage: 1,
    pageSize: 1,
    data: [
      {
        applicationName: 'shop-order',
        instanceNum: 15,
        instanceName: 'shop-order0',
        rpcPort: '172.168.45.89:20888',
        timeout: '1000ms',
        retryNum: '2',
        label: 'region=beijing'
      },
      {
        applicationName: 'shop-order',
        instanceNum: 15,
        instanceName: 'shop-order1',
        rpcPort: '172.168.45.24:20888',
        timeout: '500ms',
        retryNum: '1',
        label: 'region=wuhan'
      },
      {
        applicationName: 'shop-user',
        instanceNum: 12,
        instanceName: 'shop-order2',
        rpcPort: '172.161.23.89:20888',
        timeout: '200ms',
        retryNum: '1',
        label: 'region=shanghai'
      },
      {
        applicationName: 'shop-order',
        instanceNum: 15,
        instanceName: 'shop-order3',
        rpcPort: '172.168.45.89:12423',
        timeout: '2000ms',
        retryNum: '2',
        label: 'region=hangzhou'
      },
      {
        applicationName: 'shop-order',
        instanceNum: 15,
        instanceName: 'shop-order4',
        rpcPort: '172.168.45.89:20888',
        timeout: '100ms',
        retryNum: '0',
        label: 'region=wuxi'
      },
      {
        applicationName: 'shop-user',
        instanceNum: 12,
        instanceName: 'shop-order5',
        rpcPort: '172.168.45.89:20888',
        timeout: '1000ms',
        retryNum: '2',
        label: 'region=beijing'
      },
      {
        applicationName: 'shop-order',
        instanceNum: 15,
        instanceName: 'shop-order6',
        rpcPort: '172.168.45.89:20888',
        timeout: '1000ms',
        retryNum: '2',
        label: 'region=ningbo'
      },
      {
        applicationName: 'shop-user',
        instanceNum: 12,
        instanceName: 'shop-order7',
        rpcPort: '172.168.45.89:20888',
        timeout: '1000ms',
        retryNum: '2',
        label: 'region=shenzhen'
      },
      {
        applicationName: 'shop-user',
        instanceNum: 12,
        instanceName: 'shop-order8',
        rpcPort: '172.168.45.89:20888',
        timeout: '1000ms',
        retryNum: '2',
        label: 'region=guangzhou'
      },
      {
        applicationName: 'shop-order',
        instanceNum: 15,
        instanceName: 'shop-order9',
        rpcPort: '172.168.45.89:20888',
        timeout: '1000ms',
        retryNum: '2',
        label: 'region=nanjing'
      },
      {
        applicationName: 'shop-user',
        instanceNum: 12,
        instanceName: 'shop-order10',
        rpcPort: '172.168.45.89:20888',
        timeout: '1000ms',
        retryNum: '2',
        label: 'region=beijing'
      }
    ]
  }
})
