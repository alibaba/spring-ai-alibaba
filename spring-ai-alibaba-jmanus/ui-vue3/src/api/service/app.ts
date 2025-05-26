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

export const searchApplications = (params: any): Promise<any> => {
  return request({
    url: '/application/search',
    method: 'get',
    params
  })
}

export const getApplicationDetail = (params: any): Promise<any> => {
  return request({
    url: '/application/detail',
    method: 'get',
    params
  })
}

export const getApplicationInstanceStatistics = (params: any): Promise<any> => {
  return request({
    url: '/application/instance/statistics',
    method: 'get',
    params
  })
}
export const getApplicationInstanceInfo = (params: any): Promise<any> => {
  return request({
    url: '/application/instance/info',
    method: 'get',
    params
  })
}

export const getApplicationServiceForm = (params: any): Promise<any> => {
  return request({
    url: '/application/service/form',
    method: 'get',
    params
  })
}

export const getApplicationMetricsDashboard = (params: any): Promise<any> => {
  return request({
    url: '/application/metric-dashboard',
    method: 'get',
    params
  })
}
export const getApplicationTraceDashboard = (params: any): Promise<any> => {
  return request({
    url: '/application/trace-dashboard',
    method: 'get',
    params
  })
}
export const listApplicationEvent = (params: any): Promise<any> => {
  return request({
    url: '/application/event',
    method: 'get',
    params
  })
}

/**
 * @description Get whether the execution log is turned on
 * @param appName application name
 */
export const getAppLogSwitch = (appName: string): Promise<any> => {
  return request({
    url: '/application/config/operatorLog',
    method: 'get',
    params: {
      appName
    }
  })
}

/**
 * @description Modify the execution log switch
 * @param appName application name
 * @param operatorLog Whether to turn on?
 */
export const updateAppLogSwitch = (appName: string, operatorLog: boolean): Promise<any> => {
  return request({
    url: '/application/config/operatorLog',
    method: 'put',
    params: {
      appName,
      operatorLog
    }
  })
}

/**
 * @description Obtain traffic weight.
 * @param appName application name
 */
export const getAppTrafficWeight = (appName: string): Promise<any> => {
  return request({
    url: '/application/config/flowWeight',
    method: 'get',
    params: {
      appName
    }
  })
}

/**
 * @description Modify traffic weight.
 * @param appName application name
 * @param  flowWeightSets traffic weight
 */
export const updateAppTrafficWeight = (
  appName: string,
  flowWeightSets: Array<any>
): Promise<any> => {
  return request({
    url: '/application/config/flowWeight',
    method: 'put',
    params: {
      appName
    },
    data: {
      flowWeightSets
    }
  })
}

/**
 * @description Obtain gray-scale isolation configuration
 * @param appName application name
 */
export const getAppGrayIsolation = (appName: string): Promise<any> => {
  return request({
    url: '/application/config/gray',
    method: 'get',
    params: {
      appName
    }
  })
}

/**
 * @description Modify gray-scale isolation configuration
 * @param appName application name
 * @param  graySets gray-scale isolation configuration
 */
export const updateAppGrayIsolation = (appName: string, graySets: Array<any>): Promise<any> => {
  return request({
    url: '/application/config/gray',
    method: 'put',
    params: {
      appName
    },
    data: {
      graySets
    }
  })
}
