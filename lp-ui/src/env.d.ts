/**
 * @Author: Yeharn
 * @Date:2026-11-04
 * @Version: 1.0.0
 * @Description: Vite 与 Vue 单文件组件的 TypeScript 类型声明
 */

/// <reference types="vite/client" />

declare module '*.vue' {
  import type { DefineComponent } from 'vue'
  const component: DefineComponent<{}, {}, any>
  export default component
}

interface ImportMetaEnv {
  readonly VITE_APP_TITLE: string
  readonly VITE_APP_BASE_API: string
}

interface ImportMeta {
  readonly env: ImportMetaEnv
}
