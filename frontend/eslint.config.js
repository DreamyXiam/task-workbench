import js from '@eslint/js'
import tseslint from 'typescript-eslint'
import vue from 'eslint-plugin-vue'
export default tseslint.config(
 { ignores:['dist/**','node_modules/**','coverage/**'] },
 js.configs.recommended,
 {files:['tests/**/*.mjs'],languageOptions:{globals:{process:'readonly',console:'readonly',URL:'readonly',setTimeout:'readonly',window:'readonly',document:'readonly'}},rules:{'no-unused-vars':['error',{argsIgnorePattern:'^_'}]}},
 ...tseslint.configs.recommended,
 ...vue.configs['flat/recommended'],
 {files:['**/*.vue'],languageOptions:{parserOptions:{parser:tseslint.parser,extraFileExtensions:['.vue']}},rules:{'vue/multi-word-component-names':'off','vue/html-self-closing':'off','vue/html-closing-bracket-newline':'off','vue/max-attributes-per-line':'off','vue/html-indent':'off','vue/singleline-html-element-content-newline':'off','vue/html-closing-bracket-spacing':'off','vue/first-attribute-linebreak':'off','vue/html-end-tags':'error','vue/require-default-prop':'off'}},
 {files:['**/*.{ts,vue}'],languageOptions:{globals:{window:'readonly',document:'readonly',navigator:'readonly',console:'readonly',crypto:'readonly',sessionStorage:'readonly',FormData:'readonly',URL:'readonly',URLSearchParams:'readonly',setTimeout:'readonly',setInterval:'readonly',clearTimeout:'readonly',clearInterval:'readonly',File:'readonly',Blob:'readonly',HTMLInputElement:'readonly',Event:'readonly'}},rules:{'@typescript-eslint/no-explicit-any':'off','@typescript-eslint/no-unused-vars':['error',{argsIgnorePattern:'^_',varsIgnorePattern:'^_'}],'no-undef':'off'}}
)
