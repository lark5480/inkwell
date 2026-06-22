import type { ArticleWebResponse, SiteInfoResponse } from '../../composables/useBlogApi'

export default defineEventHandler(async (event) => {
  const siteUrl = 'http://localhost:3000'
  const apiBase = 'http://localhost:8080/api'

  async function fetchJSON<T>(url: string): Promise<T> {
    const response = await fetch(url)
    if (!response.ok) throw new Error(`HTTP ${response.status}`)
    const json = await response.json()
    if (json.code !== 200) {
      throw new Error(json.message || 'API error')
    }
    return json.data as T
  }

  let siteName = 'Personal Blog'
  let siteDescription = 'A personal blog'

  try {
    const siteInfo = await fetchJSON<SiteInfoResponse>(`${apiBase}/web/site-info`)
    if (siteInfo?.siteTitle) siteName = siteInfo.siteTitle
    if (siteInfo?.siteDescription) siteDescription = siteInfo.siteDescription
  } catch {
    // Use defaults
  }

  let articles: ArticleWebResponse[] = []

  try {
    const page = await fetchJSON<{ records: ArticleWebResponse[]; total: number; page: number; pageSize: number }>(
      `${apiBase}/web/articles?page=1&pageSize=20`
    )
    articles = page.records || []
  } catch {
    // No articles
  }

  const feed = `<?xml version="1.0" encoding="UTF-8"?>
<rss version="2.0" xmlns:atom="http://www.w3.org/2005/Atom" xmlns:content="http://purl.org/rss/1.0/modules/content/">
  <channel>
    <title>${escapeXml(siteName)}</title>
    <link>${siteUrl}</link>
    <description>${escapeXml(siteDescription)}</description>
    <language>en</language>
    <lastBuildDate>${new Date().toUTCString()}</lastBuildDate>
    <atom:link href="${siteUrl}/rss.xml" rel="self" type="application/rss+xml"/>
    ${articles
      .map(
        (article) => `
    <item>
      <title>${escapeXml(article.title)}</title>
      <link>${siteUrl}/article/${escapeXml(article.slug)}</link>
      <guid isPermaLink="true">${siteUrl}/article/${escapeXml(article.slug)}</guid>
      <description>${escapeXml(article.summary || '')}</description>
      <pubDate>${new Date(article.publishedAt).toUTCString()}</pubDate>
      ${article.categoryName ? `<category>${escapeXml(article.categoryName)}</category>` : ''}
    </item>`
      )
      .join('')}
  </channel>
</rss>`

  setHeader(event, 'Content-Type', 'application/rss+xml; charset=utf-8')
  return feed
})

function escapeXml(str: string): string {
  return str
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&apos;')
}
