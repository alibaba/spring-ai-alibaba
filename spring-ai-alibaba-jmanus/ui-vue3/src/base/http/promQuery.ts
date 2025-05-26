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

import { queryPromSql } from '@/api/service/metricInfo'

export async function queryMetrics(promSql: string) {
  try {
    let prom_result = (
      await queryPromSql({
        query: promSql
      })
    )?.data?.result[0]
    return prom_result?.value && prom_result.value.length > 0 ? Number(prom_result.value[1]) : 'NA'
  } catch (e) {
    console.error('fetch from prom error: ', e)
  }
  return 'NA'
}
