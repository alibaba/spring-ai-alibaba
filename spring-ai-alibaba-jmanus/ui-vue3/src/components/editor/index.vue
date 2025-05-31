<!--
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
-->
<template>
  <div
    :id="editorId"
    :style="{
      width: typeof width === 'number' ? width + 'px' : width,
      height: typeof height === 'number' ? height + 'px' : height,
    }"
  ></div>
</template>

<script lang="ts" setup>
import { onMounted, watch } from 'vue'
import useMonaco from './MonacoEditor'

const emit = defineEmits(['update:modelValue', 'blur', 'change'])

const props = defineProps({
  modelValue: {
    type: String,
    default: '',
  },
  width: {
    type: [String, Number],
    default: '100%',
  },
  height: {
    type: [String, Number],
    default: '100%',
  },
  theme: {
    type: String,
    default: 'vs-light',
  },
  language: {
    type: String,
    default: 'json',
  },
  editorId: {
    type: String,
    default: 'editor',
  },
  editorOptions: {
    type: Object,
    default: () => ({}),
  },
  readonly: {
    type: Boolean,
    default: false,
  },
})

const { createEditor, updateVal, getEditor, onFormatDoc } = useMonaco(props.language)

const updateMonacoVal = (_val?: string) => {
  const val = _val || props.modelValue
  updateVal(val)
}
watch(
  () => props.modelValue,
  val => {
    if (val !== getEditor()?.getValue()) {
      updateMonacoVal(val)
    }
  }
)

onMounted(() => {
  const editor = createEditor(document.querySelector(`#${props.editorId}`), {
    theme: props.theme,
    readOnly: props.readonly,
    ...props.editorOptions,
  })
  updateMonacoVal()
  if (editor) {
    editor.onDidChangeModelContent(() => {
      emit('update:modelValue', editor.getValue())
      emit('change', editor.getValue())
    })
    editor.onDidBlurEditorText(() => {
      console.log(222)
      emit('blur')
    })
  }
})

defineExpose({
  onFormatDoc,
})
</script>
