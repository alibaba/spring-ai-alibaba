import I18N from '../../../spark-i18n/src/index';
import enLangMap from './locales/en-us.json';
import jaLangMap from './locales/ja-jp.json';
import cnLangMap from './locales/zh-cn.json';

const multiLangMap = {
  zh: cnLangMap,
  en: enLangMap,
  ja: jaLangMap,
};

export default new I18N({ multiLangMap });
