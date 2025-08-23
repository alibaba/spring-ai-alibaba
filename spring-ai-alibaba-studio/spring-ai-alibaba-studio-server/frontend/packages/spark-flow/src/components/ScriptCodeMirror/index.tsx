import {
  INodeDataInputParamItem,
  INodeDataOutputParamItem,
} from '@/types/work-flow';
import {
  autocompletion,
  CompletionContext,
  CompletionResult,
} from '@codemirror/autocomplete';
import { javascript } from '@codemirror/lang-javascript';
import { python } from '@codemirror/lang-python';
import { Extension } from '@codemirror/state';
import { EditorView } from '@codemirror/view';
import { vscodeDark, vscodeLight } from '@uiw/codemirror-theme-vscode';
import ReactCodeMirror from '@uiw/react-codemirror';
import React, { memo, useMemo } from 'react';
import './index.less';

export interface IScriptCodeMirrorProps {
  value: string;
  onChange: (value: string) => void;
  inputParams: INodeDataInputParamItem[];
  outputParams: INodeDataOutputParamItem[];
  language: 'javascript' | 'python';
  theme?: 'light' | 'dark';
  disabled?: boolean;
}

const generateTypeDefinitions = (
  inputParams: INodeDataInputParamItem[],
  language: 'javascript' | 'python',
) => {
  if (language === 'javascript') {
    // generate input parameter type
    const inputTypeDef = inputParams
      .map((param) => `${param.key}: ${param.type}`)
      .join(',\n  ');

    return `type InputParams = {
  ${inputTypeDef}
};

function process(params: InputParams): void {
  const input = params;
  
  // Write your code here
  return output;
}`;
  } else {
    const inputTypeDef = inputParams
      .map((param) => `${param.key}: ${param.type}`)
      .join('\n  ');
    return `from typing import TypedDict, Dict, Any

class InputParams(TypedDict):
  ${inputTypeDef}

def process(params: InputParams):
    input = params
    
    # Write your code here
    return output`;
  }
};

const createCompletionSource = (inputParams: INodeDataInputParamItem[]) => {
  return (context: CompletionContext): CompletionResult | null => {
    const word = context.matchBefore(/\w*/);
    if (!word) return null;

    const completions = [
      ...inputParams.map((param) => ({
        label: `params.${param.key}`,
        type: 'variable',
        info: `Params parameter: ${param.key} (${param.type})`,
      })),
    ];

    return {
      from: word.from,
      options: completions,
    };
  };
};

// create an extension to hide type definitions
const createHiddenTypeDefinitions = (
  typeDefinitions: string,
  onChange: (value: string) => void,
): Extension => {
  return EditorView.updateListener.of((update) => {
    if (update.docChanged) {
      // get the current document content
      const content = update.state.doc.toString();
      // remove the type definition part, only keep the user code
      const userCode =
        content.split('// Write your code here').pop()?.trim() || '';
      onChange(userCode);
    }
  });
};

export default memo(function ScriptCodeMirror(props: IScriptCodeMirrorProps) {
  const { theme = 'dark', inputParams, outputParams, language } = props;

  const typeDefinitions = useMemo(
    () => generateTypeDefinitions(inputParams, language),
    [inputParams, language],
  );

  const extensions = useMemo(() => {
    const baseExtensions = [
      autocompletion({
        override: [createCompletionSource(inputParams)],
      }),
      createHiddenTypeDefinitions(typeDefinitions, props.onChange),
    ];

    switch (language) {
      case 'javascript':
        return [...baseExtensions, javascript({ typescript: true })];
      case 'python':
        return [...baseExtensions, python()];
    }
  }, [language, inputParams, outputParams, typeDefinitions, props.onChange]);

  return (
    <ReactCodeMirror
      className="script-code-mirror"
      value={props.value}
      onChange={props.onChange}
      lang={language}
      extensions={extensions}
      readOnly={props.disabled}
      theme={theme === 'dark' ? vscodeDark : vscodeLight}
    />
  );
});
