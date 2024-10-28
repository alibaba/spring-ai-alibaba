import { createI18n } from "vue-i18n";
import { LOCAL_STORAGE_LOCALE } from "@/base/constants";
import { messages } from "@/base/i18n/messages";
import { reactive } from "vue";

export const localeConfig = reactive({
  // todo use system's locale
  locale: localStorage.getItem(LOCAL_STORAGE_LOCALE) || "cn",
  opts: [
    {
      value: "en",
      title: "en",
    },
    {
      value: "cn",
      title: "中文",
    },
  ],
});

export const i18n: any = createI18n({
  locale: localeConfig.locale,
  legacy: false,
  globalInjection: true,
  messages,
});

export const changeLanguage = (l: any) => {
  localStorage.setItem(LOCAL_STORAGE_LOCALE, l);
  i18n.global.locale.value = l;
};
