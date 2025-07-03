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

/* eslint-env node */
require('@rushstack/eslint-patch/modern-module-resolution');

module.exports = {
  root: true,
  'extends': [
    'plugin:vue/vue3-essential',
    'eslint:recommended',
    '@vue/eslint-config-typescript',
    '@vue/eslint-config-prettier/skip-formatting'
  ],
  plugins: ['unused-imports'],
  parserOptions: {
    ecmaVersion: 'latest',
    parser: '@typescript-eslint/parser',
    sourceType: 'module',
    project: './tsconfig.app.json'
  },
  rules: {
    'vue/multi-word-component-names': 'off',
    '@typescript-eslint/no-explicit-any': 'off',
    '@typescript-eslint/no-unused-vars': 'off', // Use unused-imports instead
    'unused-imports/no-unused-imports': 'error',
    'unused-imports/no-unused-vars': [
      'warn',
      { 'vars': 'all', 'varsIgnorePattern': '^_', 'args': 'after-used', 'argsIgnorePattern': '^_' }
    ],
    'no-console': process.env.NODE_ENV === 'production' ? 'warn' : 'off',
    'no-debugger': process.env.NODE_ENV === 'production' ? 'warn' : 'off',
    // Enhanced rules for better error detection during refactoring
    '@typescript-eslint/no-undef': 'error',
    'vue/no-undef-properties': 'error',
    'vue/no-unused-properties': 'warn',
    'vue/no-unused-refs': 'warn',
    'vue/require-prop-types': 'error',
    'vue/require-default-prop': 'error',
    'vue/no-unused-emit-declarations': 'error',
    // Stricter TypeScript rules for refactoring safety
    '@typescript-eslint/prefer-const': 'error',
    '@typescript-eslint/no-inferrable-types': 'off',
    '@typescript-eslint/strict-boolean-expressions': 'off',
    '@typescript-eslint/prefer-nullish-coalescing': 'warn',
    '@typescript-eslint/prefer-optional-chain': 'warn',
    '@typescript-eslint/no-unnecessary-condition': 'warn'
  }
};
