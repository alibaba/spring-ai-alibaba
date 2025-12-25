import { IValueType } from '@/types/work-flow';

export const defaultValueMap: Record<IValueType, string> = {
  String: '',
  Number: '0',
  Boolean: 'false',
  Object: JSON.stringify({ key: 'value' }, null, 2),
  File: '',
  'Array<Object>': JSON.stringify([{ key: 'value' }], null, 2),
  'Array<String>': JSON.stringify([''], null, 2),
  'Array<Number>': JSON.stringify([0], null, 2),
  'Array<Boolean>': JSON.stringify([false], null, 2),
  'Array<File>': JSON.stringify([], null, 2),
};
