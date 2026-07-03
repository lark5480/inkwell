/**
 * 图片 URL 解析工具。
 * 将相对路径（/ 开头）补全为后端绝对 URL，支持局域网 IP 访问。
 */
export function resolveImageUrl(url: string | undefined | null): string | null {
  if (!url) return null
  if (url.startsWith('/')) {
    const base = import.meta.client
      ? `http://${location.hostname}:8080`
      : 'http://localhost:8080'
    return base + url
  }
  return url
}
