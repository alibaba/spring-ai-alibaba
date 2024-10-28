import * as monaco from "monaco-editor";

export default function useMonaco(language = "json") {
  let monacoEditor: monaco.editor.IStandaloneCodeEditor | null = null;
  let initReadOnly = false;

  const updateVal = async (val: string) => {
    monacoEditor?.setValue(val);
    setTimeout(async () => {
      initReadOnly && monacoEditor?.updateOptions({ readOnly: false });
      await monacoEditor?.getAction("editor.action.formatDocument")?.run();
      initReadOnly && monacoEditor?.updateOptions({ readOnly: true });
    }, 100);
  };

  const createEditor = (
    el: HTMLElement | null,
    editorOption: monaco.editor.IStandaloneEditorConstructionOptions = {},
  ) => {
    if (monacoEditor) {
      return;
    }
    initReadOnly = !!editorOption.readOnly;
    monacoEditor =
      el &&
      monaco.editor.create(el, {
        language,
        minimap: { enabled: false },
        theme: "vs-light",
        multiCursorModifier: "ctrlCmd",
        tabSize: 2,
        automaticLayout: true, // 自适应宽高
        ...editorOption,
      });
    return monacoEditor;
  };
  const onFormatDoc = () => {
    monacoEditor?.getAction("editor.action.formatDocument")?.run();
  };
  return {
    updateVal,
    getEditor: () => monacoEditor,
    createEditor,
    onFormatDoc,
  };
}
