import { createApp } from "vue";
import Antd from "ant-design-vue";

import App from "./App.vue";
import "ant-design-vue/dist/reset.css";
import "highlight.js/styles/monokai.css";

import Vue3ColorPicker from "vue3-colorpicker";
import "vue3-colorpicker/style.css";
import "nprogress/nprogress.css";
import Markdown from "vue3-markdown-it";
import { PRIMARY_COLOR } from "@/base/constants";

const app = createApp(App);

app.use(Antd).use(Vue3ColorPicker).use(Markdown).mount("#app");
