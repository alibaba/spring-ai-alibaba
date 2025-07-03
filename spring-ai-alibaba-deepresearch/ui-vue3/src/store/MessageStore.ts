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


import {defineStore} from "pinia";
import {type MessageInfo, type SimpleType} from "ant-design-x-vue";
import {type Ref} from 'vue'
import { reactive } from "vue";
type MsgType<Message> = {
    current: {
        // 会话 id
        convId: string|number|null,
        info: MessageInfo<Message | any>,
        // 是否候选, 是: 不显示在界面上
        candidate: boolean,
        // 是否勾选了 deepresearch
        deepResearch: boolean,
        // 是否展示研究细节
        deepResearchDetail: boolean,
        // 记录ai内容的类型
        aiType: 'normal' | 'startDS' | 'onDS' | 'endDS',
    }
    // 记录历史
    history: string[],
}
export const useMessageStore = <Message extends SimpleType>() => defineStore("messageStore", {
    state(): MsgType<Message> {
        return reactive({
            convId: null,
            current: {
                info: {
                    status: 'local',
                    message: null,
                    id: "0",
                },
                candidate: true,
                deepResearch: true,
                aiType: 'normal',
            },
            history: []
        })
    },
    getters: {},
    actions: {
        // 归档消息
        archiveLocal() {
            this.history = [
                ...this.history,
                JSON.stringify(this.current)
            ]
        },
        archiveMsg() {
            this.history = [
                ...this.history,
                JSON.stringify(this.current)
            ]
        },
        trace(handle: any) {
            for (let historyElement of this.history) {
                handle(JSON.parse(historyElement))
            }
        },
        startDeepResearch(){

        },
        nextAIType() {
            console.log("nextAIType", this.current.aiType)
            if(!this.current.aiType || this.current.aiType === 'normal') {
                this.current.aiType = 'startDS'
            }else if(this.current.aiType === 'startDS') {
                this.current.aiType = 'onDS'
            }else if(this.current.aiType === 'onDS') {
                this.current.aiType = 'endDS'
            }else {
                this.current.aiType = 'normal'
            }
            console.log("nextAIType", this.current.aiType)
        }
    },
    persist: true
})()
