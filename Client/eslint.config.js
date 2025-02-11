import js from '@eslint/js';
import importPlugin from 'eslint-plugin-import';
import reactHooks from 'eslint-plugin-react-hooks';
import reactRefresh from 'eslint-plugin-react-refresh';
import simpleImportSort from 'eslint-plugin-simple-import-sort';
import unusedImports from 'eslint-plugin-unused-imports';
import globals from 'globals';
import tseslint from 'typescript-eslint';
import react from 'eslint-plugin-react';

export default tseslint.config(
    { ignores: ['dist'] },
    {
        extends: [
            js.configs.recommended,
            ...tseslint.configs.recommended,
            importPlugin.flatConfigs.recommended,
            importPlugin.flatConfigs.typescript,
        ],
        files: ['**/*.{ts,tsx}'],
        languageOptions: {
            ecmaVersion: 2020,
            globals: globals.browser,
        },
        plugins: {
            'react-hooks': reactHooks,
            'react-refresh': reactRefresh,
            'simple-import-sort': simpleImportSort,
            'unused-imports': unusedImports,
            react,
        },
        rules: {
            ...reactHooks.configs.recommended.rules,
            'react-refresh/only-export-components': [
                'warn',
                { allowConstantExport: true },
            ],
            'simple-import-sort/imports': [
                'error',
                {
                    groups: [['^\\u0000', '^@?\\w', '^[^.]', '^\\.']],
                },
            ],
            'simple-import-sort/exports': 'error',
            'import/first': 'error',
            'import/newline-after-import': 'error',
            'import/no-duplicates': 'error',
            'import/no-unresolved': 'off',
            'no-unused-vars': 'off',
            'unused-imports/no-unused-imports': 'error',
            '@typescript-eslint/no-explicit-any': 'off',
            'unused-imports/no-unused-vars': [
                'warn',
                {
                    vars: 'all',
                    varsIgnorePattern: '^_',
                    args: 'after-used',
                    argsIgnorePattern: '^_',
                },
            ],
            'react/self-closing-comp': [
                'error',
                {
                    component: true,
                    html: true,
                },
            ],
            'react/react-in-jsx-scope': 'off',
        },
    }
);
