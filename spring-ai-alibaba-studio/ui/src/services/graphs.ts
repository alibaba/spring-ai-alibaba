///
/// Copyright 2024-2025 the original author or authors.
///
/// Licensed under the Apache License, Version 2.0 (the "License");
/// you may not use this file except in compliance with the License.
/// You may obtain a copy of the License at
///
///      https://www.apache.org/licenses/LICENSE-2.0
///
/// Unless required by applicable law or agreed to in writing, software
/// distributed under the License is distributed on an "AS IS" BASIS,
/// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
/// See the License for the specific language governing permissions and
/// limitations under the License.
///

/**
 * Copyright 2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import { request } from 'ice';

export interface GraphData {
  id: string;
  name: string;
  // Add other relevant fields as needed
}

export interface GraphRunActionParam {
  graphName: string;
  input: Record<string, any>;
  // Add other relevant fields as needed
}

export interface GraphRunResult {
  id: string;
  status: string;
  result: any;
  // Add other relevant fields as needed
}

export default {
  // 获取Graphs列表
  async getGraphs(): Promise<GraphData[]> {
    return await request({
      url: '/studio/api/graphs',
      method: 'get',
    });
  },

  // 根据graph name获取Graph
  async getGraphByName(name: string): Promise<GraphData> {
    return await request({
      url: `/studio/api/graphs/${name}`,
      method: 'get',
    });
  },

  async postGraph(data: GraphRunActionParam): Promise<GraphRunResult> {
    return await request({
      url: '/studio/api/graphs',
      method: 'post',
      data,
    });
  },
};
