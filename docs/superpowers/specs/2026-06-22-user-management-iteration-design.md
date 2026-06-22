# 用户管理迭代设计：头像上传 + 全站展示 + 个人编辑

## 概述

本次迭代聚焦用户管理三个方面：

1. **头像上传到 MinIO**：前端裁剪后上传，复用现有文件存储服务
2. **全站头像展示**：评论区、文章列表、私信等所有涉及用户的界面统一显示头像（参考 B 站风格）
3. **C 端个人编辑页**：用户可自行编辑昵称、上传头像、修改个人签名

## 变更清单总览

| 变更 | 文件 | 性质 |
|------|------|------|
| 后端新增头像上传端点 | `UserProfileController.java` | 新增 |
| CommentResponse 加 userAvatar | `CommentResponse.java` + `CommentServiceImpl.java` | 修改 |
| 通用头像组件 | `components/UserAvatar.vue` | 新增 |
| 评论区头像展示 | `components/CommentItem.vue` | 修改 |
| 文章卡片作者信息 | `components/ArticleCard.vue` | 修改 |
| 私信会话列表头像 | `pages/messages/index.vue` | 修改 |
| 聊天窗口头像 | `pages/messages/[userId].vue` | 修改 |
| 搜索结果用户头像 | `pages/search.vue`（用户卡片头像） | 修改 |
| C 端个人设置页 | `pages/settings.vue` | 新增 |

---

## 1. 后端设计

### 1.1 头像上传端点

```java
// UserProfileController.java

@PostMapping("/api/web/user/avatar")
public Result<String> uploadAvatar(@RequestParam("file") MultipartFile file) {
    User user = userRepository.findById(getUserId())
            .orElseThrow(() -> new BusinessException(404, "User not found"));
    String avatarUrl = fileStorageService.uploadImage(file);
    user.setAvatar(avatarUrl);
    userRepository.save(user);
    return Result.success(avatarUrl);
}
```

- 复用 `FileStorageService.uploadImage()` → 通过 MinIO 存储配置自动选择 MinIO 或本地存储
- 前端裁剪后上传原始图片（裁剪由前端负责，后端只收最终文件）
- 文件校验（类型、大小）由 `MinioFileStorageService` 的 `validateFile()` 处理
- 返回 MinIO 公开访问 URL，前端更新 localStorage 中的 `userInfo.avatar`

### 1.2 CommentResponse 扩充

```java
// CommentResponse 新增字段
public record CommentResponse(
        // ...
        String userAvatar,   // 新增
        // ...
) {}

// CommentServiceImpl.toCommentResponse()
String userAvatar = comment.getUser() != null ? comment.getUser().getAvatar() : null;
```

### 1.3 其他 DTO 状态

| DTO | 已有头像字段？ |
|-----|--------------|
| ArticleDetailResponse | ✅ `authorAvatar` |
| ArticleWebResponse | ✅ `authorAvatar` |
| MessageResponse | ✅ `fromUserAvatar` |
| ConversationResponse | ✅ `userAvatar` |
| NotificationResponse | ✅ `fromUserAvatar` |
| FollowerResponse | ✅ `avatar` |
| FollowingResponse | ✅ `avatar` |
| UserProfileResponse | ✅ `avatar` |
| UserPublicResponse | ✅ `avatar` |

无需修改。前端只需在展示时使用这些字段。

### 1.4 默认头像

不设默认头像 URL。前端 `UserAvatar` 组件无头像时自动显示昵称首字母 + 渐变色背景。

---

## 2. 前端通用组件

### 2.1 UserAvatar.vue

**新建** `components/UserAvatar.vue`，统一处理所有头像展示。

Props：

| Prop | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| user | `{ id, nickname, avatar }` | required | 用户信息 |
| size | `number` | 40 | 头像尺寸（px），正方形 |
| link | `boolean` | true | 是否可点击跳转到用户主页 |

行为：

- `user.avatar` 有值 → 显示 `<img>` 圆形裁剪
- `user.avatar` 无值 → 显示昵称首字母 + 渐变色背景（渐变颜色根据 user.id 生成，保证同一用户颜色一致）
- `link=true` → 包裹 `<NuxtLink :to="\`/user/${user.id}\`">`
- 鼠标悬停轻微上浮

尺寸映射：

| 场景 | 尺寸 |
|------|------|
| 文章卡片作者 | 24px |
| 评论列表 | 36px |
| 私信会话 | 40px |
| 聊天气泡 | 28px |
| 个人页大头像 | 80px |

---

## 3. 各页面改造

### 3.1 评论区（CommentItem.vue）

当前：使用纯色圆显示首字母（`.comment-avatar`）

改造：
- 删除 `.comment-avatar` div
- 在根评论左侧插入 `<UserAvatar :user="{ id: comment.userId, avatar: comment.userAvatar, nickname: comment.userNickname }" :size="36" />`
- 回复（子评论）保持现有内联布局，不带头像（减少视觉噪音，参考 B 站做法）
- 后端同时返回 `CommentResponse.userAvatar`

### 3.2 文章卡片（ArticleCard.vue）

当前：显示封面、分类标签、日期、标题、摘要、统计

改造：
- 在 `.article-card-meta` 区域加一行作者信息：`<UserAvatar :size="24" />` + 作者昵称 + 发布时间
- 作者昵称可点击跳转到 `/user/{authorId}`
- 利用 `ArticleWebResponse` 已有的 `authorId`、`authorName`、`authorAvatar` 字段

### 3.3 私信会话列表（messages/index.vue）

当前：`.conv-avatar` 纯色圆显示首字母

改造：
- 将 `.conv-avatar` 替换为 `<UserAvatar :size="40" />`
- 会话列表中的头像默认可点击跳转到用户主页

### 3.4 聊天窗口（messages/[userId].vue）

当前：纯文字气泡，无头像

改造：
- 对方消息：左侧加 `<UserAvatar :size="28" />`，气泡左对齐
- 自己消息：右侧加 `<UserAvatar :size="28" />`，气泡右对齐
- 头像只显示首字母或图片，不可点击（聊天场景）

### 3.5 通知列表（NotificationList.vue）

当前已使用 `notif.fromUserAvatar` 显示头像图片，无头像时显示 emoji 图标（💬/❤️/👤）。保持现状，不强制改为 `UserAvatar` 组件（通知场景的 emoji 图标比首字母更有语义）。

### 3.6 搜索用户列表（pages/search.vue）

当前用户卡片已有独立头像 `<img v-if="u.avatar">` + 首字母占位。改为使用 `<UserAvatar>` 组件统一风格。

搜索页的文章列表（Articles tab）使用 `ArticleCard` 组件，后者已在 §3.2 中改造为显示作者信息。

---

## 4. C 端个人设置页

### 4.1 新建 pages/settings.vue

路径：`/settings`（需登录，`middleware: 'auth'`）

布局：

```
┌──────────────────────────────────┐
│  个人设置                          │
│                                   │
│  ┌──────┐                         │
│  │ 头像  │  [上传头像]              │
│  │      │  点击更换 · 支持 JPG/PNG │
│  └──────┘                         │
│                                   │
│  昵称                              │
│  [________________]                │
│                                   │
│  个人签名                           │
│  [_____________________________]  │
│  [_____________________________]  │
│                                   │
│  [保存修改]                         │
└──────────────────────────────────┘
```

### 4.2 头像裁剪流程

1. 点击"上传头像" → 触发 `<input type="file" accept="image/*">`
2. 选择图片 → 本地预览 → 调用裁剪 UI（使用 `cropperjs` 或原生 canvas 绘制裁剪框）
3. 用户调整裁剪框（1:1 正方形约束）
4. 确认裁剪 → 将选中区域导出为 `Blob` → `FormData` → `POST /api/web/user/avatar`
5. 上传成功 → 更新本地 `userInfo.avatar` → 页面头像实时更新

### 4.3 信息保存

- 昵称 + 签名使用现有 `PUT /api/web/user/profile` 接口
- 头像使用独立 `POST /api/web/user/avatar` 接口
- 保存成功 → 更新 localStorage 中的 user info → 反馈成功提示

---

## 5. 数据流

### 5.1 头像上传流程

```
用户选择图片 → 前端裁剪(1:1) → Blob → FormData → POST /avatar → MinIO 存储
    → 返回 URL → 更新 DB → 更新前端 userInfo.avatar → 全站头像刷新
```

### 5.2 头像展示流程

```
后端 API 返回 userAvatar URL
    → 前端 UserAvatar 组件接收
    → avatar 存在？显示 <img> : 显示首字母
    → 点击跳转到 /user/{id}
```

---

## 6. 缓存与更新策略

- 用户上传头像后，localStorage 中的 `userInfo` 更新 `avatar` 字段
- 已缓存的文章列表、评论列表等可能仍显示旧头像 → 刷新页面后更新
- 评论列表目前没有服务端缓存，每次请求都是最新的
- 文章列表页有 Redis 缓存（`@Cacheable(value = CacheNames.ARTICLE_LIST)`），头像更新后需等缓存过期或手动清除

---

## 7. 测试要点

| 场景 | 验证 |
|------|------|
| 上传有效图片 | 返回 200 + MinIO URL |
| 上传空文件 | 返回 400 |
| 上传超大文件 | 返回 400 |
| 上传非图片文件 | 返回 400 |
| 未登录上传 | 返回 401 |
| CommentResponse 含 userAvatar | 字段不为 null |
| 无头像用户 | UserAvatar 显示首字母 |
| 评论区头像点击 | 跳转到对应用户页 |
| 文章卡片作者行 | 显示头像 + 昵称 |
| 个人设置页保存 | 刷新后数据持久化 |

---

## 8. 非功能性

- 头像文件路径格式：`yyyy/MM/{timestamp}_{uuid8}.{ext}`（复用现有 `MinioFileStorageService.generateObjectName()` 生成策略）
- 限制：仅支持 JPG/PNG/GIF/WebP，最大 10MB（复用现有配置）
- 前端裁剪仅生成 JPG/PNG 格式（根据原图格式决定），质量 0.9
