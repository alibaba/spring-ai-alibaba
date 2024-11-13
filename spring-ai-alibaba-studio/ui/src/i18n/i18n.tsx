import i18n from 'i18next';
import enUsTrans from './locales/en-us.json';
import zhCnTrans from './locales/zh-cn.json';
import { initReactI18next } from 'react-i18next';

export default i18n.use(initReactI18next).init({
  resources: {
    en: {
      translation: enUsTrans,
    },
    zh: {
      translation: zhCnTrans,
    },
  },
  fallbackLng: 'zh',
  debug: false,
  interpolation: {
    escapeValue: false,
  },
});
