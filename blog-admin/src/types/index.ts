// ========== Article ==========
export interface Article {
  id: number
  title: string
  slug: string
  content?: string
  summary?: string
  coverImage?: string
  status: 'DRAFT' | 'PUBLISHED' | 'HIDDEN'
  viewCount: number
  likeCount: number
  commentCount: number
  isTop: boolean
  isFeatured: boolean
  categoryName?: string
  categoryId?: number
  tags: TagDTO[]
  authorName?: string
  userId?: number
  publishedAt?: string
  createTime?: string
  updateTime?: string
}

export interface ArticleCreateRequest {
  title: string
  slug?: string
  content: string
  summary?: string
  coverImage?: string
  categoryId?: number | null
  tags?: number[]
  status?: string
  isFeatured?: boolean
}

export interface ArticleUpdateRequest {
  title?: string
  slug?: string
  content?: string
  summary?: string
  coverImage?: string
  categoryId?: number | null
  tags?: number[]
  status?: string
  isFeatured?: boolean
}

// ========== Category ==========
export interface Category {
  id: number
  name: string
  slug: string
  description?: string
  sort: number
  articleCount?: number
}

// ========== Tag ==========
export interface TagDTO {
  id: number
  name: string
  slug: string
  articleCount?: number
}

// ========== Comment ==========
export interface Comment {
  id: number
  articleId: number
  articleTitle?: string
  content: string
  authorName?: string
  authorEmail?: string
  userId?: number
  userNickname?: string
  status: 'PENDING' | 'APPROVED' | 'SPAM'
  ip?: string
  createdAt?: string
}

// ========== Link ==========
export interface Link {
  id: number
  name: string
  url: string
  avatar?: string
  description?: string
  sort: number
  status: number
}

// ========== User ==========
export interface User {
  id: number
  username: string
  nickname?: string
  email?: string
  avatar?: string
  role: string
  status: number
  createTime?: string
}

// ========== Settings ==========
export interface Setting {
  settingKey: string
  settingValue: string
}

export interface SettingsMap {
  [key: string]: string
}

// ========== Stats ==========
export interface StatsOverview {
  articleCount: number
  commentCount: number
  viewCount: number
  categoryCount: number
  tagCount: number
  linkCount: number
}

// ========== Auth ==========
export interface UserInfo {
  id: number
  username: string
  nickname?: string
  avatar?: string
  role: string
}

export interface LoginResponse {
  token: string
  user: UserInfo
}

export interface LoginRequest {
  username: string
  password: string
}
