/**
 * 将后端返回的媒体资源相对路径（如 /uploads/xxx.jpg）解析为完整 URL。
 * 已是绝对 URL（http/https 开头）或 data URI 则原样返回。
 */
export const useMedia = () => {
  // SSR 和浏览器端都用动态主机名，避免 hydration 不匹配
  const base = import.meta.server
    ? 'http://localhost:8080'
    : `http://${location.hostname}:8080`

  function resolve(url?: string | null): string {
    if (!url) return ''
    if (/^(https?:|data:|blob:)/.test(url)) return url
    // 相对路径拼后端 base
    return base + url
  }

  return { resolve }
}
