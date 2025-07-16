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
  env: {
    node: true,
    es2022: true,
    browser: true,
  },
  'extends': [
    'eslint:recommended',
    'plugin:vue/essential',
    '@vue/eslint-config-typescript/recommended',
    '@vue/eslint-config-prettier/skip-formatting'
  ],
  plugins: ['unused-imports'],
  parser: 'vue-eslint-parser',
  parserOptions: {
    ecmaVersion: 'latest',
    parser: '@typescript-eslint/parser',
    sourceType: 'module',
    extraFileExtensions: ['.vue'],
  },
  overrides: [
    {
      files: ['*.ts', '*.tsx', '*.vue'],
      parserOptions: {
        project: './tsconfig.app.json',
      },
      rules: {
        // TypeScript specific rules that require project - keep as warnings for better DX
        '@typescript-eslint/prefer-nullish-coalescing': 'warn',
        '@typescript-eslint/prefer-optional-chain': 'warn',
        '@typescript-eslint/no-unnecessary-condition': 'warn'
      }
    },
    {
      files: ['*.js', '*.cjs', '*.mjs'],
      env: {
        node: true,
      },
      rules: {
        // Disable TypeScript-specific rules for JS files
        '@typescript-eslint/no-var-requires': 'off',
      }
    },
    {
      files: ['*.config.ts', '*.config.js', 'cypress/**/*', 'vite.config.ts', 'vitest.config.ts'],
      env: {
        node: true,
      },
      parserOptions: {
        // Don't use project for config files
        project: null,
      },
      rules: {
        // Disable TypeScript-specific rules for config files
        '@typescript-eslint/prefer-nullish-coalescing': 'off',
        '@typescript-eslint/prefer-optional-chain': 'off',
        '@typescript-eslint/no-unnecessary-condition': 'off'
      }
    }
  ],
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
    // Enhanced rules for better error detection during refactoring - keep important ones as errors
    'vue/no-undef-properties': 'error',
    'vue/no-unused-properties': 'warn', // back to warn - can be noisy
    'vue/no-unused-refs': 'warn', // back to warn
    'vue/require-prop-types': 'error',
    'vue/require-default-prop': 'warn', // this can be annoying, make it warn
    'vue/no-unused-emit-declarations': 'warn', // back to warn
    'vue/no-use-v-if-with-v-for': 'warn', // back to warn
    // Basic rules that don't require project
    'prefer-const': 'warn', // back to warn - not critical
    '@typescript-eslint/no-inferrable-types': 'off',
    // Additional rules for better code quality - set to warn for less noise
    'no-prototype-builtins': 'warn',
  }
};
