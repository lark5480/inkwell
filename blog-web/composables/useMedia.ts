/**
 * 将后端返回的媒体资源相对路径（如 /uploads/xxx.jpg）解析为完整 URL。
 * 已是绝对 URL（http/https 开头）或 data URI 则原样返回。
 */
export const useMedia = () => {
  // 浏览器端动态取主机名拼接后端地址，SSR 时用相对路径
  const base = import.meta.server ? '' : `http://${location.hostname}:8080`

  function resolve(url?: string | null): string {
    if (!url) return ''
    if (/^(https?:|data:|blob:)/.test(url)) return url
    // 相对路径拼后端 base
    return base + url
  }

  return { resolve }
}
