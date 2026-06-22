<template>
  <div class="container settings-page">
    <h1 class="page-title">{{ t('settings.title') }}</h1>

    <div v-if="loading" class="loading-state">{{ t('common.loading') }}</div>
    <div v-else-if="error" class="error-state">{{ error }}</div>

    <template v-else>
      <!-- Tabs -->
      <div class="settings-tabs">
        <button :class="['tab-btn', { active: activeTab === 'profile' }]" @click="activeTab = 'profile'">{{ t('settings.tabProfile') }}</button>
        <button :class="['tab-btn', { active: activeTab === 'security' }]" @click="activeTab = 'security'">{{ t('settings.tabSecurity') }}</button>
      </div>

      <!-- Profile Tab -->
      <div v-if="activeTab === 'profile'" class="tab-content">
        <!-- Avatar -->
        <section class="settings-section">
          <label class="section-label">{{ t('settings.avatar') }}</label>
          <div class="avatar-section">
            <UserAvatar :user="currentUserInfo" :size="80" />
            <div class="avatar-actions">
              <button class="btn btn-outline btn-sm" @click="triggerUpload">
                {{ t('settings.changeAvatar') }}
              </button>
              <p class="avatar-hint">{{ t('settings.avatarHint') }}</p>
            </div>
          </div>

          <input ref="fileInputRef" type="file" accept="image/jpeg,image/png,image/webp" class="hidden-input" @change="onFileSelected" />

          <!-- Crop Modal -->
          <Teleport to="body">
            <div v-if="cropModalOpen" class="crop-overlay" @click.self="cropModalOpen = false">
              <div class="crop-modal">
                <h3>{{ t('settings.cropTitle') }}</h3>
                <div class="crop-preview-wrapper">
                  <img ref="cropImageRef" :src="cropImageSrc" class="crop-image" alt="" @load="initCrop" />
                  <div v-if="cropBox" class="crop-box" :style="cropBoxStyle" @mousedown.prevent="startDrag">
                    <div class="crop-box-handle tl"></div>
                    <div class="crop-box-handle tr"></div>
                    <div class="crop-box-handle bl"></div>
                    <div class="crop-box-handle br"></div>
                  </div>
                </div>
                <div class="crop-actions">
                  <button class="btn btn-primary btn-sm" @click="confirmCrop">{{ t('settings.cropConfirm') }}</button>
                  <button class="btn btn-outline btn-sm" @click="cropModalOpen = false">{{ t('common.cancel') }}</button>
                </div>
                <div v-if="avatarUploading" class="upload-progress">{{ t('editor.uploading') }}</div>
              </div>
            </div>
          </Teleport>
        </section>

        <!-- Nickname -->
        <section class="settings-section">
          <label class="section-label" for="nickname">{{ t('settings.nickname') }}</label>
          <input id="nickname" v-model="form.nickname" type="text" class="form-input" :placeholder="t('settings.nicknamePlaceholder')" maxlength="50" />
        </section>

        <!-- Bio -->
        <section class="settings-section">
          <label class="section-label" for="bio">{{ t('settings.bio') }}</label>
          <textarea id="bio" v-model="form.bio" class="form-input form-textarea" :placeholder="t('settings.bioPlaceholder')" rows="4" maxlength="1000"></textarea>
        </section>

        <!-- Save Profile -->
        <div class="settings-actions">
          <button class="btn btn-primary" @click="handleSave" :disabled="saving">
            {{ saving ? t('settings.saving') : t('settings.save') }}
          </button>
          <span v-if="saveMessage" class="save-message" :class="{ error: saveError }">{{ saveMessage }}</span>
        </div>
      </div>

      <!-- Security Tab -->
      <div v-if="activeTab === 'security'" class="tab-content">
        <section class="settings-section">
          <label class="section-label">{{ t('settings.changePassword') }}</label>
          <input v-model="passwordForm.oldPassword" type="password" class="form-input" :placeholder="t('settings.oldPassword')" style="margin-bottom:8px" />
          <input v-model="passwordForm.newPassword" type="password" class="form-input" :placeholder="t('settings.newPassword')" />
          <div class="settings-actions" style="margin-top:12px;border:none;padding:0">
            <button class="btn btn-primary" @click="handlePasswordChange" :disabled="passwordSaving">
              {{ passwordSaving ? t('common.loading') : t('settings.changePasswordBtn') }}
            </button>
            <span v-if="passwordMessage" class="save-message" :class="{ error: passwordError }">{{ passwordMessage }}</span>
          </div>
        </section>
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
definePageMeta({ middleware: ['auth'] })

const { t } = useI18n()
const { getMyProfile, updateMyProfile, uploadAvatar, changePassword } = useBlogApi()
const { user, isLoggedIn } = useAuth()

const activeTab = ref<'profile' | 'security'>('profile')

// Profile data
const loading = ref(true)
const error = ref('')
const profile = ref<UserProfileResponse | null>(null)

// Form
const form = reactive({ nickname: '', bio: '' })
const saving = ref(false)
const saveMessage = ref('')
const saveError = ref(false)

// Avatar upload
const fileInputRef = ref<HTMLInputElement | null>(null)
const cropModalOpen = ref(false)
const cropImageRef = ref<HTMLImageElement | null>(null)
const cropImageSrc = ref('')
const avatarUploading = ref(false)

// Crop state
const cropBox = ref<{ x: number; y: number; size: number } | null>(null)
const dragging = ref(false)
const dragStart = ref({ x: 0, y: 0, boxX: 0, boxY: 0 })
const imageNatural = ref({ w: 0, h: 0 })
const imageDisplay = ref({ w: 0, h: 0 })

// Password change
const passwordForm = reactive({ oldPassword: '', newPassword: '' })
const passwordSaving = ref(false)
const passwordMessage = ref('')
const passwordError = ref(false)

// Current user info for avatar display
const currentUserInfo = computed(() => ({
  id: user.value?.id,
  nickname: form.nickname || user.value?.nickname,
  avatar: profile.value?.avatar || user.value?.avatar,
}))

// Crop box computed style
const cropBoxStyle = computed(() => {
  if (!cropBox.value) return {}
  return {
    left: cropBox.value.x + 'px',
    top: cropBox.value.y + 'px',
    width: cropBox.value.size + 'px',
    height: cropBox.value.size + 'px',
  }
})

onMounted(async () => {
  try {
    const p = await getMyProfile()
    profile.value = p
    form.nickname = p.nickname || ''
    form.bio = p.bio || ''
  } catch {
    error.value = t('settings.saveFailed')
  } finally {
    loading.value = false
  }
})

onMounted(() => {
  document.addEventListener('mousemove', onMouseMove)
  document.addEventListener('mouseup', stopDrag)
})

onBeforeUnmount(() => {
  document.removeEventListener('mousemove', onMouseMove)
  document.removeEventListener('mouseup', stopDrag)
})

function triggerUpload() {
  fileInputRef.value?.click()
}

function onFileSelected(e: Event) {
  const input = e.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file) return

  if (!['image/jpeg', 'image/png', 'image/webp'].includes(file.type)) {
    saveMessage.value = '仅支持 JPG/PNG/WebP 格式'
    saveError.value = true
    return
  }
  if (file.size > 10 * 1024 * 1024) {
    saveMessage.value = '文件大小不能超过 10MB'
    saveError.value = true
    return
  }

  const reader = new FileReader()
  reader.onload = () => {
    cropImageSrc.value = reader.result as string
    cropModalOpen.value = true
  }
  reader.readAsDataURL(file)
  input.value = ''
}

function initCrop() {
  const img = cropImageRef.value
  if (!img) return

  imageNatural.value = { w: img.naturalWidth, h: img.naturalHeight }
  imageDisplay.value = { w: img.clientWidth, h: img.clientHeight }

  const displaySize = Math.min(img.clientWidth, img.clientHeight)
  const pad = 20
  const maxSize = displaySize - pad * 2
  const size = Math.min(maxSize, Math.floor(displaySize * 0.8))
  const x = Math.floor((img.clientWidth - size) / 2)
  const y = Math.floor((img.clientHeight - size) / 2)
  cropBox.value = { x, y, size }
}

function startDrag(e: MouseEvent) {
  if (!cropBox.value) return
  dragging.value = true
  dragStart.value = {
    x: e.clientX,
    y: e.clientY,
    boxX: cropBox.value.x,
    boxY: cropBox.value.y,
  }
}

function onMouseMove(e: MouseEvent) {
  if (!dragging.value || !cropBox.value) return
  const dx = e.clientX - dragStart.value.x
  const dy = e.clientY - dragStart.value.y
  const imgEl = cropImageRef.value
  if (!imgEl) return

  cropBox.value.x = Math.max(0, Math.min(dragStart.value.boxX + dx, imgEl.clientWidth - cropBox.value.size))
  cropBox.value.y = Math.max(0, Math.min(dragStart.value.boxY + dy, imgEl.clientHeight - cropBox.value.size))
}

function stopDrag() {
  dragging.value = false
}

async function confirmCrop() {
  if (!cropBox.value || !cropImageRef.value) return

  const img = cropImageRef.value
  const box = cropBox.value

  const scaleX = img.naturalWidth / img.clientWidth
  const scaleY = img.naturalHeight / img.clientHeight
  const sx = box.x * scaleX
  const sy = box.y * scaleY
  const sw = box.size * scaleX
  const sh = box.size * scaleY

  const canvas = document.createElement('canvas')
  canvas.width = 200
  canvas.height = 200
  const ctx = canvas.getContext('2d')
  if (!ctx) return

  ctx.drawImage(img, sx, sy, sw, sh, 0, 0, 200, 200)

  const blob = await new Promise<Blob | null>((resolve) => {
    canvas.toBlob((b) => resolve(b), 'image/jpeg', 0.9)
  })
  if (!blob) return

  avatarUploading.value = true
  try {
    const url = await uploadAvatar(new File([blob], 'avatar.jpg', { type: 'image/jpeg' }))
    if (profile.value) profile.value.avatar = url
    if (user.value) user.value.avatar = url
    cropModalOpen.value = false
    saveMessage.value = t('settings.avatarUploaded')
    saveError.value = false
    setTimeout(() => { saveMessage.value = '' }, 3000)
  } catch {
    saveMessage.value = t('settings.avatarFailed')
    saveError.value = true
  } finally {
    avatarUploading.value = false
  }
}

async function handleSave() {
  saving.value = true
  saveMessage.value = ''
  saveError.value = false
  try {
    const result = await updateMyProfile({
      nickname: form.nickname || undefined,
      bio: form.bio || undefined,
    })
    profile.value = result
    if (user.value) {
      user.value.nickname = result.nickname
    }
    saveMessage.value = t('settings.saved')
    setTimeout(() => { saveMessage.value = '' }, 3000)
  } catch {
    saveMessage.value = t('settings.saveFailed')
    saveError.value = true
  } finally {
    saving.value = false
  }
}

async function handlePasswordChange() {
  if (!passwordForm.oldPassword || !passwordForm.newPassword) {
    passwordMessage.value = '请填写旧密码和新密码'
    passwordError.value = true
    return
  }
  if (passwordForm.newPassword.length < 6) {
    passwordMessage.value = '新密码至少 6 位'
    passwordError.value = true
    return
  }
  passwordSaving.value = true
  passwordMessage.value = ''
  try {
    await changePassword(passwordForm.oldPassword, passwordForm.newPassword)
    passwordForm.oldPassword = ''
    passwordForm.newPassword = ''
    passwordMessage.value = t('settings.passwordChanged')
    passwordError.value = false
    setTimeout(() => { passwordMessage.value = '' }, 3000)
  } catch (e: any) {
    passwordMessage.value = e?.message || t('settings.saveFailed')
    passwordError.value = true
  } finally {
    passwordSaving.value = false
  }
}
</script>

<script lang="ts">
import UserAvatar from '~/components/UserAvatar.vue'
import type { UserProfileResponse } from '~/composables/useBlogApi'
</script>

<style scoped>
.settings-page {
  max-width: 640px;
  margin: 0 auto;
  padding: var(--space-8) var(--space-6);
}

.page-title {
  font-family: var(--font-heading);
  font-size: var(--font-size-3xl);
  font-weight: 800;
  color: var(--text);
  margin-bottom: var(--space-6);
}

/* Tabs */
.settings-tabs {
  display: flex;
  gap: var(--space-1);
  margin-bottom: var(--space-6);
  border-bottom: 2px solid var(--border-color);
}

.tab-btn {
  padding: var(--space-3) var(--space-5);
  border: none;
  background: none;
  color: var(--text-secondary);
  font-family: var(--font-heading);
  font-size: var(--font-size-base);
  font-weight: 600;
  cursor: pointer;
  border-bottom: 2px solid transparent;
  margin-bottom: -2px;
  transition: all var(--transition-fast);
}

.tab-btn:hover {
  color: var(--primary);
}

.tab-btn.active {
  color: var(--primary);
  border-bottom-color: var(--primary);
}

.tab-content {
  min-height: 200px;
}

.settings-section {
  margin-bottom: var(--space-8);
}

.section-label {
  display: block;
  font-family: var(--font-heading);
  font-size: var(--font-size-sm);
  font-weight: 700;
  color: var(--text);
  margin-bottom: var(--space-3);
}

/* Avatar section */
.avatar-section {
  display: flex;
  align-items: center;
  gap: var(--space-5);
}

.avatar-actions {
  display: flex;
  flex-direction: column;
  gap: var(--space-1);
}

.avatar-hint {
  font-size: var(--font-size-xs);
  color: var(--text-muted);
  margin: 0;
}

.hidden-input {
  display: none;
}

/* Form inputs */
.form-input {
  width: 100%;
  padding: 0.625rem 0.875rem;
  border: 3px solid var(--border);
  border-radius: var(--radius-lg);
  background: var(--bg-card);
  color: var(--text);
  font-size: var(--font-size-base);
  outline: none;
  transition: border-color var(--transition-fast);
  box-sizing: border-box;
  font-family: var(--font-body);
}

.form-input:focus {
  border-color: var(--primary);
  box-shadow: 0 0 0 3px rgba(99, 102, 241, 0.1);
}

.form-input::placeholder {
  color: var(--text-muted);
}

.form-textarea {
  resize: vertical;
  min-height: 80px;
  line-height: 1.6;
}

/* Crop modal */
.crop-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.6);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.crop-modal {
  background: var(--bg-card);
  border-radius: var(--clay-border-radius);
  padding: var(--space-6);
  max-width: 500px;
  width: 90%;
  box-shadow: var(--clay-shadow-hover);
}

.crop-modal h3 {
  font-family: var(--font-heading);
  font-size: var(--font-size-lg);
  font-weight: 700;
  margin: 0 0 var(--space-4);
}

.crop-preview-wrapper {
  position: relative;
  width: 100%;
  max-height: 400px;
  overflow: hidden;
  border-radius: var(--radius-lg);
  background: var(--bg-tertiary);
  margin-bottom: var(--space-4);
  cursor: crosshair;
}

.crop-image {
  display: block;
  width: 100%;
  height: auto;
}

.crop-box {
  position: absolute;
  border: 2px solid white;
  box-shadow: 0 0 0 9999px rgba(0, 0, 0, 0.5);
  cursor: move;
}

.crop-box-handle {
  position: absolute;
  width: 10px;
  height: 10px;
  border: 2px solid white;
  background: var(--primary);
  border-radius: 2px;
}

.crop-box-handle.tl { top: -5px; left: -5px; }
.crop-box-handle.tr { top: -5px; right: -5px; }
.crop-box-handle.bl { bottom: -5px; left: -5px; }
.crop-box-handle.br { bottom: -5px; right: -5px; }

.crop-actions {
  display: flex;
  gap: var(--space-3);
  justify-content: flex-end;
}

.upload-progress {
  text-align: center;
  margin-top: var(--space-3);
  font-size: var(--font-size-sm);
  color: var(--text-muted);
}

/* Save actions */
.settings-actions {
  display: flex;
  align-items: center;
  gap: var(--space-4);
  padding-top: var(--space-6);
  border-top: 2px solid var(--border-light);
}

.save-message {
  font-size: var(--font-size-sm);
  color: var(--success);
}

.save-message.error {
  color: var(--error);
}

/* States */
.loading-state,
.error-state {
  text-align: center;
  padding: var(--space-16) 0;
  color: var(--text-secondary);
}

.error-state {
  color: var(--error);
}

@media (max-width: 768px) {
  .settings-page {
    padding: var(--space-4);
  }

  .page-title {
    font-size: var(--font-size-2xl);
  }
}
</style>
