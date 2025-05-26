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

import request from '@/base/http/request'

export const searchInstances = (params: any): Promise<any> => {
  return request({
    url: '/instance/search',
    method: 'get',
    params
  })
}

export const getInstanceDetail = (params: any): Promise<any> => {
  return request({
    url: '/instance/detail',
    method: 'get',
    params
  })
}

export const getInstanceMetricsDashboard = (params: any): Promise<any> => {
  return request({
    url: '/instance/metric-dashboard',
    method: 'get',
    params
  })
}
export const getInstanceTracingDashboard = (params: any): Promise<any> => {
  return request({
    url: '/instance/trace-dashboard',
    method: 'get',
    params
  })
}

/**
 * @description Obtain whether the execution log is enabled.
 * @param instanceIP
 * @param appName
 */
export const getInstanceLogSwitchAPI = (instanceIP: string, appName: string): Promise<any> => {
  return request({
    url: '/instance/config/operatorLog',
    method: 'get',
    params: {
      instanceIP,
      appName
    }
  })
}

/**
 * @description Modify the execution log switch.
 * @param instanceIP
 * @param appName
 * @param operatorLog
 */
export const updateInstanceLogSwitchAPI = (
  instanceIP: string,
  appName: string,
  operatorLog: boolean
): Promise<any> => {
  return request({
    url: '/instance/config/operatorLog',
    method: 'put',
    params: {
      instanceIP,
      appName,
      operatorLog
    }
  })
}

/**
 * @description get the traffic switch.
 * @param instanceIP
 * @param appName
 */
export const getInstanceTrafficSwitchAPI = (instanceIP: string, appName: string): Promise<any> => {
  return request({
    url: '/instance/config/trafficDisable',
    method: 'get',
    params: {
      instanceIP,
      appName
    }
  })
}

/**
 * @description Modify the traffic switch.
 * @param instanceIP
 * @param appName
 * @param trafficDisable
 */
export const updateInstanceTrafficSwitchAPI = (
  instanceIP: string,
  appName: string,
  trafficDisable: boolean
): Promise<any> => {
  return request({
    url: '/instance/config/trafficDisable',
    method: 'put',
    params: {
      instanceIP,
      appName,
      trafficDisable
    }
  })
}
