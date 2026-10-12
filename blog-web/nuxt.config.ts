// https://nuxt.com/docs/api/configuration/nuxt-config
export default defineNuxtConfig({
  ssr: true,
  devtools: true,

  modules: [
    '@unocss/nuxt',
    '@pinia/nuxt',
  ],

  css: [
    '~/assets/styles/main.scss',
  ],

  runtimeConfig: {
    // Server-only: SSR 时 $fetch 直连后端。
    // 生产环境用 NUXT_API_BASE=http://blog-server:8080 覆盖（容器内服务名）
    apiBase: 'http://localhost:8080',
    public: {
      // 站点对外地址，仅用于 RSS 等需要绝对 URL 的场景
      // 生产环境用 NUXT_PUBLIC_SITE_URL=https://blog.example.com 覆盖
      siteUrl: 'http://localhost:3000',
    },
  },

  devServer: {
    host: '0.0.0.0',
  },

  devProxy: {
    '/api/**': {
      target: 'http://localhost:8080',
      changeOrigin: true,
    },
  },

  app: {
    head: {
      charset: 'utf-8',
      viewport: 'width=device-width, initial-scale=1',
      titleTemplate: '%s | Inkwell',
      htmlAttrs: {
        lang: 'en',
      },
      script: [
        {
          innerHTML: `(function(){try{var t=localStorage.getItem('theme');var d=t==='dark'||(t!=='light'&&matchMedia('(prefers-color-scheme:dark)').matches);if(d)document.documentElement.classList.add('dark')}catch(e){}})()`,
          type: 'text/javascript',
        },
      ],
    },
  },

  compatibilityDate: '2025-03-01',
})
