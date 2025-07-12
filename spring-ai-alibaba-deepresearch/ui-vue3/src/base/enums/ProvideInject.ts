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

const KEY_PREFIX = '__PROVIDE_INJECT_KEY_'

export const PROVIDE_INJECT_KEY = {
  LOCALE: KEY_PREFIX + 'LOCALE',
  CONVERSATIONS: KEY_PREFIX + 'CONVERSATIONS',
  GRAFANA: KEY_PREFIX + 'GRAFANA',
  SEARCH_DOMAIN: KEY_PREFIX + 'SEARCH_DOMAIN',
  COLLAPSED: KEY_PREFIX + 'COLLAPSED',
  TAB_LAYOUT_STATE: KEY_PREFIX + 'TAB_LAYOUT_STATE',
}
