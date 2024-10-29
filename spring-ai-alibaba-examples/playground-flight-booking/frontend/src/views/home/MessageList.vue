<script setup lang="ts">
import Message from "@/views/home/Message.vue";
import { reactive } from "vue";
import type { MessageItem } from "@/types/message";

const messageList = defineProps<{ list: MessageItem[] }>();
</script>

<template>
  <div style="">
    <template v-for="msg in messageList.list">
      <Message :role="msg.role" :content="msg.content"></Message>
    </template>
  </div>
</template>

<style scoped lang="less"></style>
