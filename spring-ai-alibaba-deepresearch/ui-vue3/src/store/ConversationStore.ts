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
import {MessageOutlined} from '@ant-design/icons-vue'
import {h} from "vue";
import _ from 'lodash'
export const useConversationStore = defineStore("conversationStore", {
    state(): { current: number, conversations: any } {
        return {
            current: 0,
            conversations: [
                {
                    key: '1',
                    label: 'Conversation - 1',
                    messages: null
                }
            ]
        }
    },
    getters: {
        curConv: state=>state.conversations[state.current],
        curConvKey: state=>state.conversations[state.current].key,
        items: state=>state.conversations.map((x: any)=>{
            x.icon = h(MessageOutlined)
            return x
        })
    },
    actions: {
        newOne(){
            const newVar = {
                key: _.uniqueId('ds_conv'),
                label: 'Unnamed conversation',
                messages: null
            };
            this.conversations = [...this.conversations, newVar]
            this.current ++
            console.log(this,newVar )
        }
    },
    persist: true
})
