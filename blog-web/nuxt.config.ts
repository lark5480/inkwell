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
    // Server-only: SSR 时 $fetch 直连后端
    apiBase: 'http://localhost:8080',
    public: {
      // Browser: 走相对路径，通过 devProxy 转发到后端（支持局域网访问）
      apiBase: '',
    },
  },

  nitro: {
    devProxy: {
      '/api/**': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
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
