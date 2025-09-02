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
export interface GraphId {
  session_id: string
  thread_id: string
}
/**
 * 普通类型节点定义
 */
export interface NormalNode {
  /** 节点名称 */
  nodeName: string
  /** 节点内容 */
  content: string | any ,
  graphId: GraphId
  displayTitle: string,
  siteInformation: SiteInformation[]
}

export interface SiteInformation {
  icon: string
  weight: string
  title: string
  url: string
  content: string
}

export interface LlmStreamNode {
  visible: boolean,
  step_title: string,
  finishReason: string
  graphId: GraphId
}
