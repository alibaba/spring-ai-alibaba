<!--
  ~ Licensed to the Apache Software Foundation (ASF) under one or more
  ~ contributor license agreements.  See the NOTICE file distributed with
  ~ this work for additional information regarding copyright ownership.
  ~ The ASF licenses this file to You under the Apache License, Version 2.0
  ~ (the "License"); you may not use this file except in compliance with
  ~ the License.  You may obtain a copy of the License at
  ~
  ~     http://www.apache.org/licenses/LICENSE-2.0
  ~
  ~ Unless required by applicable law or agreed to in writing, software
  ~ distributed under the License is distributed on an "AS IS" BASIS,
  ~ WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  ~ See the License for the specific language governing permissions and
  ~ limitations under the License.
-->
<template>
  <div class="__container_app_event">
    <a-timeline mode="left">
      <a-timeline-item v-for="(item, i) in events.list">
        <div class="box">
          <div class="label" :class="{ yellow: i === 0 }">
            <div class="type"></div>
            <div class="body">
              <b class="title">{{ item.type }}</b>
              <p>{{ item.desc }}</p>
            </div>
          </div>
          <span class="time">
            {{ item.time }}
          </span>
        </div>
        <template v-if="i === 0" #dot>
          <clock-circle-outlined style="font-size: 16px; color: red" />
        </template>

        <!--        <a-card>-->
        <!--          <a-row>-->
        <!--            <a-col :span="4">-->
        <!--&lt;!&ndash;             <div class="box">&ndash;&gt;-->
        <!--&lt;!&ndash;               <div class="type"></div>&ndash;&gt;-->
        <!--&lt;!&ndash;               <div class="body"></div>&ndash;&gt;-->
        <!--&lt;!&ndash;             </div>&ndash;&gt;-->
        <!--            </a-col>-->
        <!--            <a-col :span="10">{{item.desc}}</a-col>-->
        <!--          </a-row>-->
        <!--        </a-card>-->
      </a-timeline-item>
    </a-timeline>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive } from 'vue'
import { listApplicationEvent } from '@/api/service/app'
import { ClockCircleOutlined } from '@ant-design/icons-vue'
import { PRIMARY_COLOR } from '@/base/constants'

let __ = PRIMARY_COLOR
let events: any = reactive({ list: [] })
onMounted(async () => {
  let eventsRes = await listApplicationEvent({})
  events.list = eventsRes.data.list
  console.log(events)
})
</script>
<style lang="less" scoped>
.__container_app_event {
  :deep(.ant-timeline-item-label) {
    width: 200px;
  }

  background: #fafafa;
  border-radius: 10px;
  padding: 80px 300px 20px;

  .box {
    position: relative;
    height: 100px;
    margin-bottom: 20px;
    //top:-38px;

    .label {
      position: absolute;
      height: 100px;
      top: -40px;

      &.yellow {
        .type {
          border-right-color: #f8d347;
        }
        .body {
          background: #f8d347;
        }
      }
      &.red {
        .type {
          border-right-color: #eb4325;
        }
        .body {
          background: #eb4325;
        }
      }
      &.blue {
        .type {
          border-right-color: #3d89f6;
        }
        .body {
          background: #3d89f6;
        }
      }
      &.green {
        .type {
          border-right-color: #9cac35;
        }
        .body {
          background: #9cac35;
        }
      }
      .type {
        position: absolute;
        width: 50px;
        height: 50px;
        border-style: solid;
        border-color: transparent;
        border-width: 50px 26px 50px 0px;
        border-right-color: v-bind('PRIMARY_COLOR');
        display: inline;
        border-radius: 4px;
      }
      .body {
        position: absolute;
        left: 49px;
        width: 50vw;
        border-radius: 3px 5px 5px 3px;
        height: 100%;
        display: inline;
        color: white;
        padding-left: 20px;
        background: v-bind('PRIMARY_COLOR');
        box-shadow: 8px 5px 10px #9f9c9c;

        .title {
          font-size: 30px;
          line-height: 40px;
        }
      }
    }
    .time {
      position: absolute;
      left: -200px;
      //top: 38px;
    }
  }
}
</style>
