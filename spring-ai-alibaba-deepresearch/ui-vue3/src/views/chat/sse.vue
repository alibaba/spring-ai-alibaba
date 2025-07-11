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
<script setup lang="ts">
import { TagsOutlined } from '@ant-design/icons-vue'
import { Button, Flex } from 'ant-design-vue'
import { Bubble, ThoughtChain, XStream } from 'ant-design-x-vue'
import { computed, h, ref } from 'vue'
import { XStreamBody } from '@/utils/stream'

function request() {
  return new ReadableStream({
    async start(controller) {
      const response = await fetch('/stream', {
        method: 'GET',
      })
      let idx = 0
      for await (const chunk of XStream({
        readableStream: response.body,
      })) {
        if (idx > 10) break
        let chunk1 = {
          event: 'message',
          data: JSON.stringify({
            id: idx,
            content: chunk.data,
          }),
        }
        controller.enqueue(new TextEncoder().encode(chunk1))
        idx++
      }
      controller.close()
    },
  })
}

defineOptions({ name: 'AXXStreamDefaultProtocolSetup' })

const contentChunks = ['He', 'llo', ', w', 'or', 'ld!']

function mockReadableStream() {
  const sseChunks: string[] = []

  for (let i = 0; i < contentChunks.length; i++) {
    const sseEventPart = `event: message\ndata: {"id":"${i}","content":"${contentChunks[i]}"}\n\n`
    sseChunks.push(sseEventPart)
  }

  return new ReadableStream({
    async start(controller) {
      for (const chunk of sseChunks) {
        await new Promise(resolve => setTimeout(resolve, 100))
        console.log(chunk)
        controller.enqueue(new TextEncoder().encode(chunk))
      }
      controller.close()
    },
  })
}

const lines = ref<Record<string, string>[]>([])
const content = computed(() => lines.value.map(line => JSON.parse(line.data).content).join(''))

async function readStream() {
  const response = await fetch('/stream', {
    method: 'GET',
  })
  // 🌟 Read the stream
  for await (const chunk of XStream({
    readableStream: response.body,
  })) {
    lines.value = [
      ...lines.value,
      {
        event: 'message',
        data: JSON.stringify({
          content: chunk.data,
        }),
      },
    ]
  }
}

const streamBody = new XStreamBody({
  url: '/stream',
  method: 'GET',
})
const contentInfo = computed(() => {
  return streamBody.content()
})
</script>
<template>
  <Flex :gap="8">
    <div>
      <!-- -------------- Emit -------------- -->
      <Button type="primary" :style="{ marginBottom: '16px' }" @click="streamBody.readStream()">
        Mock Default Protocol - SSE
      </Button>
      <!-- -------------- Content Concat -------------- -->
      <Bubble v-if="contentInfo" :content="contentInfo" />
    </div>
    <div>
      <ThoughtChain
        :items="
          lines.length
            ? [
                {
                  title: 'Mock Default Protocol - Log',
                  status: 'success',
                  icon: h(TagsOutlined),
                  content: h('pre', { style: { overflow: 'scroll' } }, [
                    lines.map(i => h('code', { key: i.data }, i.data)),
                  ]),
                },
              ]
            : []
        "
      />
    </div>
  </Flex>
</template>
<style scoped>
pre {
  width: 'auto';
  margin: 0;

  code {
    display: block;
    padding: 12px 16px;
    font-size: 14px;
  }
}
</style>
