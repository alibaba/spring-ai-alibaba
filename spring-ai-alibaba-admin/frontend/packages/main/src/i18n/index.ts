import I18N from '../../../spark-i18n/src/index';
import enLangMap from './locales/en-us.json';
import jaLangMap from './locales/ja-jp.json';
import cnLangMap from './locales/zh-cn.json';
import {
  en as evaluationEn,
  ja as evaluationJa,
  zh as evaluationZh,
} from './legacy-locales/evaluation';
import {
  en as experimentTracingEn,
  ja as experimentTracingJa,
  zh as experimentTracingZh,
} from './legacy-locales/experiment-tracing';
import {
  en as playgroundSharedEn,
  ja as playgroundSharedJa,
  zh as playgroundSharedZh,
} from './legacy-locales/playground-shared';
import {
  en as promptsEn,
  ja as promptsJa,
  zh as promptsZh,
} from './legacy-locales/prompts';

const multiLangMap = {
  zh: {
    ...cnLangMap,
    ...evaluationZh,
    ...experimentTracingZh,
    ...playgroundSharedZh,
    ...promptsZh,
  },
  en: {
    ...enLangMap,
    ...evaluationEn,
    ...experimentTracingEn,
    ...playgroundSharedEn,
    ...promptsEn,
  },
  ja: {
    ...jaLangMap,
    ...evaluationJa,
    ...experimentTracingJa,
    ...playgroundSharedJa,
    ...promptsJa,
  },
};

export default new I18N({ multiLangMap });
