/**
 * 将后端返回的媒体资源相对路径（如 /uploads/xxx.jpg、/api/web/files/...）解析为可访问 URL。
 * 已是绝对 URL（http/https 开头）或 data URI 则原样返回。
 *
 * 部署要点：这里刻意返回同源相对路径，由 nginx 把 /uploads/ 与 /api/ 转发到后端。
 * 这样前后端不需要暴露 :8080，SSR 与浏览器端产出的 URL 也完全一致（无 hydration 差异），
 * 后续挂 HTTPS 也不会出现混合内容拦截。
 */
export const useMedia = () => {
  function resolve(url?: string | null): string {
    if (!url) return ''
    if (/^(https?:|data:|blob:)/.test(url)) return url
    return url.startsWith('/') ? url : `/${url}`
  }

  return { resolve }
}
