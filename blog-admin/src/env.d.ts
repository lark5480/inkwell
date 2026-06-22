/// <reference types="vite/client" />

declare module '*.vue' {
  import type { DefineComponent } from 'vue'
  const component: DefineComponent<{}, {}, any>
  export default component
}

interface Result<T = any> {
  code: number
  message: string
  data: T
}

interface PageDTO<T = any> {
  records: T[]
  total: number
  page: number
  pageSize: number
}
