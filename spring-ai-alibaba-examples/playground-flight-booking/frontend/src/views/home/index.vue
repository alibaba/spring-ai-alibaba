<template>
  <div class="__container_home_index">
    <a-row class="row" :gutter="[10, 10]">
      <a-col :span="8">
        <a-card class="card chat">
          <template #title>
            <label style="font-size: 25px">Funnair support</label>
          </template>
          <div class="flex-grow">
            <a-card class="chat-body">
              <MessageList :list="messageInfo.list"></MessageList>
              <div
                id="chat-body-id"
                style="height: 5px; margin-top: 20px"
              ></div>
            </a-card>
          </div>
          <a-row class="footer" :gutter="10">
            <a-col :span="20">
              <a-input
                @keydown.enter="forHelp"
                v-model:value="question"
                placeholder="Message"
              ></a-input>
            </a-col>
            <a-col :span="4">
              <a-button @click="forHelp" :disabled="lock" type="primary"
                >Send</a-button
              >
            </a-col>
          </a-row>
        </a-card>
      </a-col>
      <a-col :span="16">
        <a-card class="card">
          <template #title>
            <label style="font-size: 25px">机票预定信息</label>
          </template>
          <a-table
            :data-source="bookingInfo.dataSource"
            :columns="bookingInfo.columns"
            :pagination="false"
          >
            <template #bodyCell="{ record, index, column, text }">
              <template v-if="column.dataIndex === 'bookingStatus'">
                <template v-if="text === 'CONFIRMED'">
                  <Icon
                    style="color: #52c41a; font-size: 20px; margin-bottom: -4px"
                    icon="material-symbols:check-box-sharp"
                  />
                </template>
                <template v-else>
                  <Icon
                    style="color: #be0b4a; font-size: 20px; margin-bottom: -4px"
                    icon="material-symbols:cancel-presentation-sharp"
                  />
                </template>
              </template>
            </template>
          </a-table>
        </a-card>
      </a-col>
    </a-row>
  </div>
</template>

<script setup lang="ts">
import { PRIMARY_COLOR } from "@/base/constants";
import { nextTick, onMounted, reactive, ref } from "vue";
import { getBookings } from "@/api/service/booking";
import { Icon } from "@iconify/vue";
import Message from "@/views/home/Message.vue";
import MessageList from "@/views/home/MessageList.vue";
import type { MessageItem } from "@/types/message";
import { chat } from "@/api/service/assistant";
import { getUUID } from "ant-design-vue/lib/vc-dialog/util";
import { v4 as uuidv4 } from "uuid";
import { message } from "ant-design-vue";

const messageInfo: { cur: MessageItem | null; list: MessageItem[] } = reactive({
  cur: null,
  list: [
    {
      role: "assistant",
      content: "欢迎来到 Funnair! 请问有什么可以帮您的?",
    },
  ],
});
const bookingInfo = reactive({
  dataSource: [],
  columns: [
    {
      title: "#",
      dataIndex: "bookingNumber",
      key: "bookingNumber",
    },
    {
      title: "Name",
      dataIndex: "name",
      key: "name",
    },
    {
      title: "Date",
      dataIndex: "date",
      key: "date",
    },
    {
      title: "From",
      dataIndex: "from",
      key: "from",
    },
    {
      title: "To",
      dataIndex: "to",
      key: "to",
    },
    {
      title: "Status",
      dataIndex: "bookingStatus",
      key: "bookingStatus",
    },
    {
      title: "Booking Class",
      dataIndex: "bookingClass",
      key: "bookingClass",
    },
  ],
});
const question = ref("");
let scrollItem: any = null;

function scrollBottom() {
  scrollItem?.scrollIntoView({ behavior: "smooth", block: "end" });
}

function addMessage(role: "user" | "assistant", content: string) {
  let cur = {
    role,
    content,
  };
  messageInfo.cur = cur;
  messageInfo.list.push(cur);
  nextTick(() => {
    scrollBottom();
  });
}

const lock = ref(false);
function appendMessage(content: string) {
  if (messageInfo.cur) {
    messageInfo.cur.content += content;
  }
  scrollBottom();
}

const chatId = uuidv4();

function forHelp() {
  if (lock.value) {
    message.warn("助手正在生成, 请耐心等候");
    return;
  }
  let userMessage = question.value;
  addMessage("user", userMessage);
  question.value = "";
  const eventSource = new EventSource(
    `/api/assistant/chat?chatId=${chatId}&userMessage=${userMessage}`,
    {},
  );
  eventSource.onopen = function (event) {
    addMessage("assistant", "");
  };
  eventSource.onmessage = function (event) {
    lock.value = true;
    appendMessage(event.data);
  };
  eventSource.onerror = function () {
    eventSource.close();
    bookings();
    lock.value = false;
  };
}

function bookings() {
  getBookings({}).then((res) => {
    bookingInfo.dataSource = res;
  });
}

let __null = PRIMARY_COLOR;
onMounted(() => {
  scrollItem = document.getElementById("chat-body-id");
  bookings();
});
</script>
<style lang="less" scoped>
.__container_home_index {
  height: 100vh;
  max-height: 100vh;
  overflow: auto;
  padding-top: 2px;

  .row {
    height: 100%;
  }

  .card {
    height: 100%;
  }

  :deep(.ant-card-body) {
    height: calc(100vh - 180px);
    display: flex;
    flex-direction: column;
    padding: 5px;
    border-radius: 0;

    .chat-body {
      border: none;
      height: calc(100% - 80px);
      overflow: auto;
      background: #f4f5f7;
    }
  }

  .flex-grow {
    flex-grow: 1; /* 让其他元素占据剩余空间 */
  }

  .footer {
    width: 100%;
  }
}
</style>
