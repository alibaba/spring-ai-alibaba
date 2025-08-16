<template>
  <div class="monaco-editor-container">
    <div ref="editorContainer" class="editor-wrapper"></div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, watch, nextTick } from 'vue'
import * as monaco from 'monaco-editor'

interface Props {
  modelValue: string
  placeholder?: string
  readonly?: boolean
  language?: string
}

interface Emits {
  (e: 'update:modelValue', value: string): void
  (e: 'change', value: string): void
}

const props = withDefaults(defineProps<Props>(), {
  placeholder: '',
  readonly: false,
  language: 'json'
})

const emit = defineEmits<Emits>()

const editorContainer = ref<HTMLElement>()
let editor: monaco.editor.IStandaloneCodeEditor | null = null

// Create editor instance
const createEditor = () => {
  if (!editorContainer.value) return

  // Configure Monaco editor
  editor = monaco.editor.create(editorContainer.value, {
    value: props.modelValue,
    language: props.language,
    theme: 'vs-dark',
    automaticLayout: true,
    minimap: { enabled: false },
    scrollBeyondLastLine: false,
    fontSize: 13,
    fontFamily: "'Monaco', 'Menlo', 'Ubuntu Mono', monospace",
    lineNumbers: 'on',
    roundedSelection: false,
    scrollbar: {
      vertical: 'visible',
      horizontal: 'visible',
      verticalScrollbarSize: 8,
      horizontalScrollbarSize: 8
    },
    folding: true,
    wordWrap: 'on',
    renderWhitespace: 'selection',
    tabSize: 2,
    insertSpaces: true,
    detectIndentation: false,
    trimAutoWhitespace: true,
    largeFileOptimizations: false,
    readOnly: props.readonly
  })

  // Listen for content changes
  editor.onDidChangeModelContent(() => {
    const value = editor?.getValue() || ''
    emit('update:modelValue', value)
    emit('change', value)
  })

  // Custom theme
  monaco.editor.defineTheme('custom-dark', {
    base: 'vs-dark',
    inherit: true,
    rules: [
      { token: 'string', foreground: '#34d399' },
      { token: 'number', foreground: '#60a5fa' },
      { token: 'keyword', foreground: '#a78bfa' },
      { token: 'comment', foreground: '#6b7280' },
      { token: 'operator', foreground: '#fbbf24' },
      { token: 'delimiter', foreground: '#fbbf24' }
    ],
    colors: {
      'editor.background': 'rgba(255, 255, 255, 0.05)',
      'editor.foreground': '#f9fafb',
      'editor.lineHighlightBackground': 'rgba(255, 255, 255, 0.08)',
      'editor.selectionBackground': '#3b82f6',
      'editor.inactiveSelectionBackground': '#4b5563',
      'editorCursor.foreground': '#f9fafb',
      'editorWhitespace.foreground': '#6b7280',
      'editorIndentGuide.background': 'rgba(255, 255, 255, 0.1)',
      'editorIndentGuide.activeBackground': '#6b7280',
      'editorLineNumber.foreground': '#9ca3af',
      'editorLineNumber.activeForeground': '#f9fafb',
      'editorGutter.background': 'rgba(255, 255, 255, 0.05)'
    }
  })

  editor.updateOptions({ theme: 'custom-dark' })
}

// Update editor content
const updateContent = (content: string) => {
  if (editor) {
    const currentContent = editor.getValue()
    if (currentContent !== content) {
      editor.setValue(content)
    }
  }
}

// Watch for modelValue changes
watch(() => props.modelValue, (newValue) => {
  if (editor) {
    updateContent(newValue)
  }
})

// Watch for readonly changes
watch(() => props.readonly, (readonly) => {
  if (editor) {
    editor.updateOptions({ readOnly: readonly })
  }
})

onMounted(() => {
  nextTick(() => {
    createEditor()
  })
})

onUnmounted(() => {
  if (editor) {
    editor.dispose()
  }
})
</script>

<style scoped>
.monaco-editor-container {
  position: relative;
  border: none;
  border-radius: 0;
  overflow: hidden;
  background: rgba(255, 255, 255, 0.05);
}

.editor-wrapper {
  height: 100%;
  min-height: 300px;
}

:deep(.monaco-editor) {
  border-radius: 0;
}

:deep(.monaco-editor .margin) {
  background-color: rgba(255, 255, 255, 0.05) !important;
}

:deep(.monaco-editor .line-numbers) {
  color: rgba(156, 163, 175, 0.8) !important;
}

:deep(.monaco-editor .current-line) {
  background-color: rgba(55, 65, 81, 0.5) !important;
}

:deep(.monaco-editor .selection) {
  background-color: rgba(59, 130, 246, 0.3) !important;
}

:deep(.monaco-editor .cursor) {
  border-left-color: rgba(249, 250, 251, 0.9) !important;
}

:deep(.monaco-editor .scrollbar .slider) {
  background-color: rgba(156, 163, 175, 0.3) !important;
}

:deep(.monaco-editor .scrollbar .slider:hover) {
  background-color: rgba(156, 163, 175, 0.5) !important;
}

:deep(.monaco-editor .scrollbar .slider.active) {
  background-color: rgba(156, 163, 175, 0.7) !important;
}
</style>
