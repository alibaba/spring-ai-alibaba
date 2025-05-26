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

Mock.mock('/mock/service/detail', 'get', {
  code: 200,
  message: 'success',
  data: {
    total: 8,
    curPage: 1,
    pageSize: 1,
    data: {
      serviceName: 'org.apache.dubbo.samples.UserService',
      versionGroup: ['version=v1', 'version=2.0,group=group1'],
      protocol: 'triple',
      delay: '3000ms',
      timeOut: '3000ms',
      retry: 3,
      requestTotal: 1384,
      avgRT: '96ms',
      avgQPS: 12,
      obsolete: false
    }
  }
})
