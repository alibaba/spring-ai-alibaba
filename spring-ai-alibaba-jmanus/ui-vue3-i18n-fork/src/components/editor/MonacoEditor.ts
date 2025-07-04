/*
 * Copyright 2024-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import * as monaco from 'monaco-editor'

export default function useMonaco(language = 'json') {
  let monacoEditor: monaco.editor.IStandaloneCodeEditor | null = null
  let initReadOnly = false

  const updateVal = async (val: string) => {
    monacoEditor?.setValue(val)
    setTimeout(async () => {
      initReadOnly && monacoEditor?.updateOptions({ readOnly: false })
      await monacoEditor?.getAction('editor.action.formatDocument')?.run()
      initReadOnly && monacoEditor?.updateOptions({ readOnly: true })
    }, 100)
  }

  const createEditor = (
    el: HTMLElement | null,
    editorOption: monaco.editor.IStandaloneEditorConstructionOptions = {}
  ) => {
    if (monacoEditor) {
      return
    }
    initReadOnly = !!editorOption.readOnly

    monacoEditor =
      el &&
      monaco.editor.create(el, {
        language,
        minimap: { enabled: true },
        theme: 'vs-dark',
        multiCursorModifier: 'ctrlCmd',
        tabSize: 2,
        automaticLayout: true, // 自适应宽高
        ...editorOption,
      })
    return monacoEditor
  }
  const onFormatDoc = () => {
    monacoEditor?.getAction('editor.action.formatDocument')?.run()
  }
  return {
    updateVal,
    getEditor: () => monacoEditor,
    createEditor,
    onFormatDoc,
  }
}
