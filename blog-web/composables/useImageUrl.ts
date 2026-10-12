/**
 * 图片 URL 解析工具。
 * 后端返回的 / 开头相对路径保持同源相对路径（由 nginx 转发到后端），
 * SSR 与浏览器端结果一致，也支持后续挂 HTTPS 域名。
 */
export function resolveImageUrl(url: string | undefined | null): string | null {
  if (!url) return null
  if (url.startsWith('/')) {
    return url
  }
  return url
}
