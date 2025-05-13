import type zhUI from './zh-cn/ui';
import type { languages } from './languages';

export type UIDictionaryKeys = keyof typeof zhUI;
export type UIDict = Partial<typeof zhUI>;
export type UILanguageKeys = keyof typeof languages;

/** Helper to type check a dictionary of UI string translations. */
export const UIDictionary = (dict: Partial<typeof zhUI>) => dict;
