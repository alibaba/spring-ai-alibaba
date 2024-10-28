<script setup lang="ts">
import enUS from "ant-design-vue/es/locale/en_US";
import zhCN from "ant-design-vue/es/locale/zh_CN";
import { provide, reactive, watch } from "vue";
import dayjs from "dayjs";
import { PROVIDE_INJECT_KEY } from "@/base/enums/ProvideInject";
import { PRIMARY_COLOR } from "@/base/constants";
import { i18n, localeConfig } from "@/base/i18n";
import devTool from "@/utils/DevToolUtil";
import home from "@/views/home/index.vue";

dayjs.locale("en");

const i18nConfig = reactive(localeConfig);
watch(i18nConfig, (val) => {
  dayjs.locale(val.locale);
});

provide(PROVIDE_INJECT_KEY.LOCALE, i18nConfig);

/**
 * this function is showing some tips about our Q&A
 * TODO
 */
function globalQuestion() {
  devTool.todo("show Q&A tips");
}

const localeGlobal = reactive(i18n.global.locale);
</script>

<template>
  <a-config-provider
    :locale="localeGlobal === 'en' ? enUS : zhCN"
    :theme="{
      token: {
        colorPrimary: PRIMARY_COLOR,
      },
    }"
  >
    <home></home>
  </a-config-provider>
</template>

<style lang="less"></style>
