import React, { memo } from 'react';

export const typeAbbr = {
  'Array<Object>': 'Arr',
  'Array<File>': 'Arr',
  'Array<String>': 'Arr',
  'Array<Number>': 'Arr',
  'Array<Boolean>': 'Arr',
  Object: 'Obj',
  File: 'File',
  String: 'Str',
  Number: 'Num',
  Boolean: 'Bool',
};

export default memo(function VarTypePrefix(props: { prefix?: string }) {
  if (!props.prefix) {
    return null;
  }

  return (
    <span className="spark-flow-var-type">
      {typeAbbr[props.prefix as keyof typeof typeAbbr]}
    </span>
  );
});
