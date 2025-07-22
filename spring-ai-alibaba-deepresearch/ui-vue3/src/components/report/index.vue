<template>
  <Flex class="aux" v-if="visible" style="width: 60%" vertical>
    <a-card style="height: 100%">
      <template #title>
        <Flex justify="space-between">
          研究细节
          <Button type="text" @click="handleClose">
            <CloseOutlined />
          </Button>
        </Flex>
      </template>
      细节
    </a-card>
  </Flex>
</template>

<script setup lang="ts">
import { Flex, Button } from 'ant-design-vue'
import { CloseOutlined } from '@ant-design/icons-vue'
import { parseJsonText } from '@/utils/jsonParser';
import { useMessageStore } from '@/store/MessageStore'

const messageStore = useMessageStore()

interface Props {
  visible?: boolean
  convId: string
}

interface Emits {
  (e: 'close'): void
}

withDefaults(defineProps<Props>(), {
  visible: false
})

// 从messageStore 拿出消息，然后进行解析并且渲染

const emit = defineEmits<Emits>()

const handleClose = () => {
  emit('close')
}
</script>

<style lang="less" scoped>
.aux {
  padding-top: 20px;
  height: 100%;
  padding-bottom: 38px;
}
</style>