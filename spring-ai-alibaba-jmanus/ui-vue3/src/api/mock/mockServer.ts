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
import devTool from '../../utils/DevToolUtil'
Mock.mock(devTool.mockUrl('/mock/metadata'), 'get', {
  code: 200,
  msg: 'success',
  data: {
    registry: 'nacos://47.101.215.139:8848?username=nacos&password=nacos',
    metadata: 'nacos://47.101.215.139:8848?username=nacos&password=nacos',
    config: 'nacos://47.101.215.139:8848?username=nacos&password=nacos',
    prometheus: 'http://prometheus.observability.svc.cluster.local:9090/',
    grafana: 'http://47.251.100.138:3000/d/a0b114ca-edf7-4dfe-ac2c-34a4fc545fed/application',
    tracing: 'http://47.251.100.138:3000/d/e968a89b-f03d-42e3-8ad3-930ae815cb0f/application'
  }
})

Mock.mock(devTool.mockUrl('/mock/overview'), 'get', {
  code: 200,
  msg: 'success',
  data: {
    appCount: 0,
    serviceCount: 0,
    insCount: 0,
    protocols: {},
    releases: {},
    discoveries: {}
  }
})
