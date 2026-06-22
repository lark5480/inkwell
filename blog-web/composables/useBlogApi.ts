// API Response types matching backend DTOs
export interface ApiResult<T = unknown> {
  code: number
  message: string
  data: T
}

export interface PageDTO<T = unknown> {
  records: T[]
  total: number
  page: number
  pageSize: number
}

// Domain types matching backend DTOs
export interface ArticleWebResponse {
  id: number
  title: string
  slug: string
  summary: string
  coverImage: string
  categoryName: string | null
  tags: TagDTO[]
  viewCount: number
  likeCount: number
  commentCount: number
  publishedAt: string
  isTop: boolean
  isFeatured: boolean
  authorId: number | null
  authorName: string | null
  authorAvatar: string | null
  status: string
}

export interface ArticleDetailResponse {
  id: number
  title: string
  slug: string
  content: string
  summary: string
  coverImage: string
  categoryName: string | null
  tags: TagDTO[]
  viewCount: number
  likeCount: number
  commentCount: number
  publishedAt: string
  isTop: boolean
  createTime: string
  updateTime: string
  authorId: number | null
  authorName: string | null
  authorAvatar: string | null
  isLiked: boolean
  status: string
}

export interface CategoryDTO {
  id: number
  name: string
  slug: string
  description: string
  sort: number
  articleCount: number
}

export interface TagDTO {
  id: number
  name: string
  slug: string
  articleCount: number
}

export interface CommentResponse {
  id: number
  articleId: number
  content: string
  authorName: string
  authorEmail: string
  userId: number | null
  userNickname: string | null
  userAvatar: string | null
  createdAt: string
  replies: CommentResponse[]
  likeCount: number
  dislikeCount: number
  likedByCurrentUser: boolean
  dislikedByCurrentUser: boolean
}

export interface VoteResult {
  likeCount: number
  dislikeCount: number
  likedByCurrentUser: boolean
  dislikedByCurrentUser: boolean
}

export interface LinkDTO {
  id: number
  name: string
  url: string
  avatar: string | null
  description: string
  sort: number
}

export interface ArchiveItem {
  yearMonth: string
  articles: ArchiveArticle[]
}

export interface ArchiveArticle {
  id: number
  title: string
  slug: string
  publishedAt: string
}

export interface SiteInfoResponse {
  siteTitle: string
  siteDescription: string
  articleCount: number
  categoryCount: number
  tagCount: number
  aboutContent: string
}

export interface PrevNextDTO {
  prev: PrevNextItem | null
  next: PrevNextItem | null
}

export interface PrevNextItem {
  id: number
  title: string
  slug: string
}

export interface CommentCreateRequest {
  articleId: number
  content: string
  authorName?: string
  authorEmail?: string
  parentId?: number | null
}

export interface ArticleCreateRequest {
  title: string
  slug?: string
  content: string
  summary?: string
  coverImage?: string
  categoryId?: number | null
  tags?: number[]
  status: 'DRAFT' | 'PUBLISHED'
}

export interface ArticleUpdateRequest {
  title?: string
  slug?: string
  content?: string
  summary?: string
  coverImage?: string
  categoryId?: number | null
  tags?: number[]
  status?: 'DRAFT' | 'PUBLISHED'
}

export interface UserProfileResponse {
  id: number
  username: string
  nickname: string
  avatar: string | null
  bio: string | null
  email: string | null
  createTime: string
}

export interface UserPublicResponse {
  id: number
  nickname: string
  avatar: string | null
  bio: string | null
  createTime: string
  articleCount: number
  followerCount: number
  followingCount: number
}

export interface UserSearchResponse {
  id: number
  nickname: string
  avatar: string | null
  bio: string | null
  followerCount: number
}

export interface UserProfileUpdateRequest {
  nickname?: string
  avatar?: string
  bio?: string
}

export interface HistoryItemResponse {
  id: number
  articleId: number
  articleTitle: string
  articleSlug: string
  coverImage: string | null
  viewedAt: string
}

export interface LikeToggleResponse {
  liked: boolean
  likeCount: number
}

export interface FollowToggleResponse {
  followed: boolean
  followerCount: number
  followingCount: number
}

export interface FollowerResponse {
  userId: number
  nickname: string
  avatar: string | null
  bio: string | null
  followedAt: string
}

export interface FollowingResponse {
  userId: number
  nickname: string
  avatar: string | null
  bio: string | null
  followedAt: string
}

export interface NotificationResponse {
  id: number
  type: string
  fromUserId: number | null
  fromUserName: string | null
  fromUserAvatar: string | null
  articleId: number | null
  articleTitle: string | null
  articleSlug: string | null
  content: string | null
  isRead: boolean
  createTime: string
}

export interface UnreadCountResponse {
  count: number
  comment: number
  like: number
  follow: number
}

export interface MessageResponse {
  id: number
  fromUserId: number
  fromUserName: string | null
  fromUserAvatar: string | null
  toUserId: number
  content: string
  isRead: boolean
  createTime: string
}

export interface BlockedUserResponse {
  userId: number
  nickname: string
  avatar: string | null
  bio: string | null
}

export interface ConversationResponse {
  userId: number
  userName: string | null
  userAvatar: string | null
  lastMessage: string
  lastMessageTime: string | null
  unreadCount: number
}

// Helper: get auth header if token exists
function getAuthHeaders(): Record<string, string> {
  const headers: Record<string, string> = {}
  if (import.meta.client) {
    const token = localStorage.getItem('blog_token')
    if (token) headers.Authorization = `Bearer ${token}`
  }
  return headers
}

// Composable
export const useBlogApi = () => {
  const config = useRuntimeConfig()
  // SSR 直连后端；浏览器端动态取当前主机名（支持局域网访问）
  const apiBase = import.meta.server
    ? config.apiBase + '/api'
    : `http://${location.hostname}:8080/api`

  function handleResponse<T>(result: ApiResult<T>): T {
    if (result.code !== 200) {
      throw new Error(result.message || 'Request failed')
    }
    return result.data
  }

  // Articles
  function getArticles(params?: { page?: number; pageSize?: number; categoryId?: number; tagSlug?: string }) {
    return $fetch<ApiResult<PageDTO<ArticleWebResponse>>>(`${apiBase}/web/articles`, { params })
      .then(handleResponse)
  }

  function getFeaturedArticles() {
    return $fetch<ApiResult<ArticleWebResponse[]>>(`${apiBase}/web/articles/featured`)
      .then(handleResponse)
  }

  function getArticle(slug: string) {
    return $fetch<ApiResult<ArticleDetailResponse>>(`${apiBase}/web/articles/${slug}`, { headers: getAuthHeaders() })
      .then(handleResponse)
  }

  function getPrevNext(slug: string) {
    return $fetch<ApiResult<PrevNextDTO>>(`${apiBase}/web/articles/${slug}/prev-next`)
      .then(handleResponse)
  }

  function likeArticle(slug: string) {
    return $fetch<ApiResult<LikeToggleResponse>>(`${apiBase}/web/articles/${slug}/like`, {
      method: 'POST',
      headers: getAuthHeaders(),
    }).then(handleResponse)
  }

  function getArticlesByCategory(slug: string, params?: { page?: number; pageSize?: number }) {
    // First resolve category slug to ID, then fetch articles
    return getCategories().then(categories => {
      const cat = categories.find(c => c.slug === slug)
      if (!cat) throw new Error(`Category "${slug}" not found`)
      return getArticles({ ...params, categoryId: cat.id })
    })
  }

  function getArticlesByTag(slug: string, params?: { page?: number; pageSize?: number }) {
    return getArticles({ ...params, tagSlug: slug })
  }

  // Categories
  function getCategories() {
    return $fetch<ApiResult<CategoryDTO[]>>(`${apiBase}/web/categories`)
      .then(handleResponse)
  }

  // Tags
  function getTags() {
    return $fetch<ApiResult<TagDTO[]>>(`${apiBase}/web/tags`)
      .then(handleResponse)
  }

  // Comments
  function getComments(articleId: number) {
    return $fetch<ApiResult<CommentResponse[]>>(`${apiBase}/web/comments`, {
      params: { articleId },
      headers: getAuthHeaders(),
    }).then(handleResponse)
  }

  function createComment(data: CommentCreateRequest, token?: string) {
    const headers: Record<string, string> = {}
    if (token) {
      headers.Authorization = `Bearer ${token}`
    }
    return $fetch<ApiResult<number>>(`${apiBase}/web/comments`, {
      method: 'POST',
      body: data,
      headers,
    }).then(handleResponse)
  }

  function voteComment(commentId: number, voteType: string | null) {
    return $fetch<ApiResult<VoteResult>>(`${apiBase}/web/comments/${commentId}/vote`, {
      method: 'POST',
      body: { voteType },
      headers: getAuthHeaders(),
    }).then(handleResponse)
  }

  function reportComment(commentId: number, reason: string) {
    return $fetch<ApiResult<void>>(`${apiBase}/web/comments/${commentId}/report`, {
      method: 'POST',
      body: { reason },
      headers: getAuthHeaders(),
    }).then(handleResponse)
  }

  function deleteComment(commentId: number) {
    return $fetch<ApiResult<void>>(`${apiBase}/web/comments/${commentId}`, {
      method: 'DELETE',
      headers: getAuthHeaders(),
    }).then(handleResponse)
  }

  function blockUser(userId: number) {
    return $fetch<ApiResult<void>>(`${apiBase}/web/users/${userId}/block`, {
      method: 'POST',
      headers: getAuthHeaders(),
    }).then(handleResponse)
  }

  function unblockUser(userId: number) {
    return $fetch<ApiResult<void>>(`${apiBase}/web/users/${userId}/block`, {
      method: 'DELETE',
      headers: getAuthHeaders(),
    }).then(handleResponse)
  }

  function getBlockedUsers() {
    return $fetch<ApiResult<BlockedUserResponse[]>>(`${apiBase}/web/user/blocks`, {
      headers: getAuthHeaders(),
    }).then(handleResponse)
  }

  // Links
  function getLinks() {
    return $fetch<ApiResult<LinkDTO[]>>(`${apiBase}/web/links`)
      .then(handleResponse)
  }

  // Search
  function searchArticles(params: { q: string; page?: number; pageSize?: number }) {
    return $fetch<ApiResult<PageDTO<ArticleWebResponse>>>(`${apiBase}/web/search`, { params })
      .then(handleResponse)
  }

  function searchUsers(q: string) {
    return $fetch<ApiResult<UserSearchResponse[]>>(`${apiBase}/web/users/search`, { params: { q } })
      .then(handleResponse)
  }

  // Site info
  function getSiteInfo() {
    return $fetch<ApiResult<SiteInfoResponse>>(`${apiBase}/web/site-info`)
      .then(handleResponse)
  }

  // Archives
  function getArchives() {
    return $fetch<ApiResult<ArchiveItem[]>>(`${apiBase}/web/archives`)
      .then(handleResponse)
  }

  // ========== Authenticated User API ==========
  function getAuthHeaders(): Record<string, string> {
    const { token } = useAuth()
    if (token.value) {
      return { Authorization: `Bearer ${token.value}` }
    }
    return {}
  }

  // User articles
  function createArticle(data: ArticleCreateRequest) {
    return $fetch<ApiResult<number>>(`${apiBase}/web/user/articles`, {
      method: 'POST',
      body: data,
      headers: getAuthHeaders(),
    }).then(handleResponse)
  }

  function updateArticle(id: number, data: ArticleUpdateRequest) {
    return $fetch<ApiResult<void>>(`${apiBase}/web/user/articles/${id}`, {
      method: 'PUT',
      body: data,
      headers: getAuthHeaders(),
    }).then(handleResponse)
  }

  function deleteArticle(id: number) {
    return $fetch<ApiResult<void>>(`${apiBase}/web/user/articles/${id}`, {
      method: 'DELETE',
      headers: getAuthHeaders(),
    }).then(handleResponse)
  }

  function getMyArticle(id: number) {
    return $fetch<ApiResult<ArticleDetailResponse>>(`${apiBase}/web/user/articles/${id}`, {
      headers: getAuthHeaders(),
    }).then(handleResponse)
  }

  function getMyArticles(params?: { page?: number; pageSize?: number; status?: string; keyword?: string }) {
    return $fetch<ApiResult<PageDTO<ArticleWebResponse>>>(`${apiBase}/web/user/articles`, {
      params,
      headers: getAuthHeaders(),
    }).then(handleResponse)
  }

  async function uploadArticleImage(file: File) {
    const formData = new FormData()
    formData.append('file', file)
    const token = import.meta.client ? localStorage.getItem('blog_token') : null
    const headers: Record<string, string> = {}
    if (token) headers.Authorization = `Bearer ${token}`
    const res = await fetch(`${apiBase}/web/user/articles/upload-image`, {
      method: 'POST',
      headers,
      body: formData,
    })
    const result: ApiResult<{ url: string }> = await res.json()
    return handleResponse(result)
  }

  // User profile
  function getMyProfile() {
    return $fetch<ApiResult<UserProfileResponse>>(`${apiBase}/web/user/profile`, {
      headers: getAuthHeaders(),
    }).then(handleResponse)
  }

  function updateMyProfile(data: UserProfileUpdateRequest) {
    return $fetch<ApiResult<UserProfileResponse>>(`${apiBase}/web/user/profile`, {
      method: 'PUT',
      body: data,
      headers: getAuthHeaders(),
    }).then(handleResponse)
  }

  function uploadAvatar(file: File) {
    const formData = new FormData()
    formData.append('file', file)
    return $fetch<ApiResult<string>>(`${apiBase}/web/user/avatar`, {
      method: 'POST',
      body: formData,
      headers: getAuthHeaders(),
    }).then(handleResponse)
  }

  function changePassword(oldPassword: string, newPassword: string) {
    return $fetch<ApiResult<void>>(`${apiBase}/web/user/password`, {
      method: 'PUT',
      body: { oldPassword, newPassword },
      headers: getAuthHeaders(),
    }).then(handleResponse)
  }

  // User history and likes
  function getMyHistory(params?: { page?: number; pageSize?: number; keyword?: string }) {
    return $fetch<ApiResult<PageDTO<HistoryItemResponse>>>(`${apiBase}/web/user/history`, {
      params,
      headers: getAuthHeaders(),
    }).then(handleResponse)
  }

  function deleteMyHistory(id: number) {
    return $fetch<ApiResult<void>>(`${apiBase}/web/user/history/${id}`, {
      method: 'DELETE',
      headers: getAuthHeaders(),
    }).then(handleResponse)
  }

  function getMyLikes(params?: { page?: number; pageSize?: number }) {
    return $fetch<ApiResult<PageDTO<HistoryItemResponse>>>(`${apiBase}/web/user/likes`, {
      params,
      headers: getAuthHeaders(),
    }).then(handleResponse)
  }

  // Public user profiles
  function getUserProfile(userId: number) {
    return $fetch<ApiResult<UserPublicResponse>>(`${apiBase}/web/users/${userId}`)
      .then(handleResponse)
  }

  function getUserArticles(userId: number, params?: { page?: number; pageSize?: number }) {
    return $fetch<ApiResult<PageDTO<ArticleWebResponse>>>(`${apiBase}/web/users/${userId}/articles`, { params })
      .then(handleResponse)
  }

  // Follow
  function toggleFollow(userId: number) {
    return $fetch<ApiResult<FollowToggleResponse>>(`${apiBase}/web/users/${userId}/follow`, {
      method: 'POST',
      headers: getAuthHeaders(),
    }).then(handleResponse)
  }

  function getFollowers(userId: number, params?: { page?: number; pageSize?: number }) {
    return $fetch<ApiResult<PageDTO<FollowerResponse>>>(`${apiBase}/web/users/${userId}/followers`, {
      params,
      headers: getAuthHeaders(),
    }).then(handleResponse)
  }

  function getFollowing(userId: number, params?: { page?: number; pageSize?: number }) {
    return $fetch<ApiResult<PageDTO<FollowingResponse>>>(`${apiBase}/web/users/${userId}/following`, {
      params,
      headers: getAuthHeaders(),
    }).then(handleResponse)
  }

  function getFollowStatus(userId: number) {
    return $fetch<ApiResult<{ following: boolean }>>(`${apiBase}/web/users/${userId}/follow-status`, {
      headers: getAuthHeaders(),
    }).then(handleResponse)
  }

  // Notifications
  function getNotifications(params?: { page?: number; pageSize?: number; type?: string }) {
    return $fetch<ApiResult<PageDTO<NotificationResponse>>>(`${apiBase}/web/user/notifications`, {
      params,
      headers: getAuthHeaders(),
    }).then(handleResponse)
  }

  function getUnreadCount() {
    return $fetch<ApiResult<UnreadCountResponse>>(`${apiBase}/web/user/notifications/unread-count`, {
      headers: getAuthHeaders(),
    }).then(handleResponse)
  }

  function markAllRead() {
    return $fetch<ApiResult<void>>(`${apiBase}/web/user/notifications/read-all`, {
      method: 'PUT',
      headers: getAuthHeaders(),
    }).then(handleResponse)
  }

  function markNotificationRead(id: number) {
    return $fetch<ApiResult<void>>(`${apiBase}/web/user/notifications/${id}/read`, {
      method: 'PUT',
      headers: getAuthHeaders(),
    }).then(handleResponse)
  }

  function markNotificationTypeAsRead(type: string) {
    return $fetch<ApiResult<void>>(`${apiBase}/web/user/notifications/mark-type-read`, {
      method: 'PUT',
      params: { type },
      headers: getAuthHeaders(),
    }).then(handleResponse)
  }

  // Messages
  function getConversations() {
    return $fetch<ApiResult<ConversationResponse[]>>(`${apiBase}/web/messages/conversations`, {
      headers: getAuthHeaders(),
    }).then(handleResponse)
  }

  function getMessages(userId: number, params?: { page?: number; pageSize?: number }) {
    return $fetch<ApiResult<PageDTO<MessageResponse>>>(`${apiBase}/web/messages`, {
      params: { ...params, userId },
      headers: getAuthHeaders(),
    }).then(handleResponse)
  }

  function sendMessage(toUserId: number, content: string) {
    return $fetch<ApiResult<MessageResponse>>(`${apiBase}/web/messages`, {
      method: 'POST',
      body: { toUserId, content },
      headers: getAuthHeaders(),
    }).then(handleResponse)
  }

  function markMessageRead(messageId: number) {
    return $fetch<ApiResult<void>>(`${apiBase}/web/messages/${messageId}/read`, {
      method: 'PUT',
      headers: getAuthHeaders(),
    }).then(handleResponse)
  }

  function markAllMessagesRead(userId: number) {
    return $fetch<ApiResult<void>>(`${apiBase}/web/messages/read-all`, {
      method: 'PUT',
      params: { userId },
      headers: getAuthHeaders(),
    }).then(handleResponse)
  }

  function getMessageUnreadCount() {
    return $fetch<ApiResult<number>>(`${apiBase}/web/messages/unread-count`, {
      headers: getAuthHeaders(),
    }).then(handleResponse)
  }

  return {
    getArticles,
    getFeaturedArticles,
    getArticle,
    getPrevNext,
    likeArticle,
    getArticlesByCategory,
    getArticlesByTag,
    getCategories,
    getTags,
    getComments,
    createComment,
    voteComment,
    reportComment,
    deleteComment,
    blockUser,
    unblockUser,
    getBlockedUsers,
    getLinks,
    searchArticles,
    searchUsers,
    getSiteInfo,
    getArchives,
    // User article management
    createArticle,
    updateArticle,
    deleteArticle,
    getMyArticle,
    getMyArticles,
    // User profile
    getMyProfile,
    updateMyProfile,
    getMyHistory,
    deleteMyHistory,
    getMyLikes,
    getUserProfile,
    getUserArticles,
    uploadArticleImage,
    uploadAvatar,
    changePassword,
    // Follow
    toggleFollow,
    getFollowers,
    getFollowing,
    getFollowStatus,
    // Notifications
    getNotifications,
    getUnreadCount,
    markAllRead,
    markNotificationRead,
    markNotificationTypeAsRead,
    // Messages
    getConversations,
    getMessages,
    sendMessage,
    markMessageRead,
    markAllMessagesRead,
    getMessageUnreadCount,
  }
}
