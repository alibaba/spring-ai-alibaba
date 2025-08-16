import dayjs from 'dayjs';

// Interface for translation variables
interface I18nVariables {
  [key: string]: any;
}

// Interface for multi-language mapping
interface IMultiLangMap {
  [key: string]: { [key: string]: string };
}

// Supported language types
type TLanguage = 'en' | 'zh' | 'ja' | string;

/**
 * Internationalization (i18n) utility class
 * Handles language management and string translations
 */
export default class I18N {
  multiLangMap: IMultiLangMap;
  language: TLanguage;

  /**
   * Initialize I18N instance with multi-language map
   * @param config Configuration object containing multiLangMap
   */
  constructor({ multiLangMap }: { multiLangMap: IMultiLangMap }) {
    this.multiLangMap = multiLangMap;
    this.language =
      localStorage.getItem('spark-flow-data-prefers-language') || 'en';
    dayjs.locale(this.language);
  }

  /**
   * Convert value to string with fallback
   * @param value Input value to convert
   * @param defaultValue Fallback value if conversion fails
   * @returns String representation of the value
   */
  toString = (value: any, defaultValue: string) => {
    if (value === undefined || value === null) {
      return defaultValue;
    }

    if (typeof value === 'string') {
      return value;
    }

    if (value?.toString && typeof value.toString === 'function') {
      return value.toString();
    }

    return value || defaultValue;
  };

  /**
   * Set current application language
   * @param language Language code to set ('en' or 'zh')
   */
  setCurrentLanguage(language: TLanguage) {
    localStorage.setItem('spark-flow-data-prefers-language', language);
    this.language = language;
    dayjs.locale(language);
  }

  /**
   * Get current application language
   * @returns Current language code
   */
  getCurrentLanguage() {
    return this.language;
  }

  /**
   * Format translation string with variables
   * @param idObj Translation ID or object containing ID and default message
   * @param variables Variables to interpolate into the translation
   * @returns Formatted translation string
   */
  format = (
    idObj: string | { id: string; dm: string },
    variables: I18nVariables,
  ) => {
    const { multiLangMap, language } = this;
    const langMap = multiLangMap[language] || {};
    let template = '';

    if (typeof idObj === 'string') {
      template = langMap[idObj as keyof typeof langMap] || idObj;
    } else {
      const { id, dm } = idObj;
      template = langMap[id as keyof typeof langMap] || dm || id;
    }

    return template.replace(/\{(\w+)\}/g, (_match: string, key: string) =>
      this.toString(variables[key], _match),
    );
  };

  /**
   * Get translation for given ID
   * @param id Translation ID or object containing ID and default message
   * @param variables Optional variables to interpolate
   * @returns Translated string
   */
  get(id: string | { id: string; dm: string }, variable?: I18nVariables) {
    return this.format(id, variable || {});
  }
}
