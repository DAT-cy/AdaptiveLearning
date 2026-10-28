<script setup>
import { computed, inject, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import {
  AlertCircle,
  AlertTriangle,
  ArrowRight,
  Bell,
  Camera,
  Check,
  CheckCircle2,
  Clock,
  Crown,
  Eye,
  EyeOff,
  HelpCircle,
  Info,
  KeyRound,
  LayoutDashboard,
  Lock,
  LogOut,
  Mail,
  PanelLeftClose,
  Shield,
  ShieldAlert,
  ShieldCheck,
  Sliders,
  Sparkles,
  Trash2,
  User,
  Zap,
} from 'lucide-vue-next'
import { homeContextKey } from '../../composables/homeContext'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { toast } from '@/components/ui/overlay'

const router = useRouter()
const home = inject(homeContextKey)

// Tabs điều hướng
const activeTab = ref('profile') // 'profile' | 'plan' | 'preferences' | 'security'

// Quản lý ảnh đại diện (Avatar)
const avatarInputRef = ref(null)
const avatarUrl = ref(
  home.userStore?.state?.avatar || localStorage.getItem('adaptive-learning-avatar') || ''
)

function triggerAvatarUpload() {
  if (avatarInputRef.value) {
    avatarInputRef.value.click()
  }
}

function handleAvatarChange(e) {
  const file = e.target.files?.[0]
  if (!file) return

  if (!file.type.startsWith('image/')) {
    toast.error('Vui lòng chọn định dạng ảnh hợp lệ (PNG, JPG, WEBP).')
    return
  }

  if (file.size > 2 * 1024 * 1024) {
    toast.error('Kích thước ảnh không được vượt quá 2MB.')
    return
  }

  const reader = new FileReader()
  reader.onload = (event) => {
    const dataUrl = event.target?.result
    if (typeof dataUrl === 'string') {
      avatarUrl.value = dataUrl
      if (home.userStore?.state) {
        home.userStore.state.avatar = dataUrl
      }
      try {
        localStorage.setItem('adaptive-learning-avatar', dataUrl)
      } catch (err) {
        // quota exceeded fallback
      }
      toast.success('Đã cập nhật ảnh đại diện thành công!')
    }
  }
  reader.readAsDataURL(file)
}

function removeAvatar() {
  avatarUrl.value = ''
  if (home.userStore?.state) {
    home.userStore.state.avatar = ''
  }
  try {
    localStorage.removeItem('adaptive-learning-avatar')
  } catch (err) {}
  toast.info('Đã gỡ ảnh đại diện. Hệ thống hiển thị chữ cái đầu mặc định.')
}

// Form thông tin cá nhân
const profileForm = reactive({
  fullName: home.userStore?.state?.fullName || localStorage.getItem('adaptive-learning-fullname') || '',
})

const profileErrors = reactive({
  fullName: '',
})

const isSavingProfile = ref(false)
const profileSavedSuccess = ref(false)

function validateProfile() {
  profileErrors.fullName = ''
  const trimmed = profileForm.fullName.trim()
  if (!trimmed) {
    profileErrors.fullName = 'Họ và tên không được để trống.'
    return false
  }
  if (trimmed.length < 2) {
    profileErrors.fullName = 'Họ và tên tối thiểu 2 ký tự.'
    return false
  }
  if (trimmed.length > 60) {
    profileErrors.fullName = 'Họ và tên tối đa 60 ký tự.'
    return false
  }
  return true
}

function saveProfile() {
  if (!validateProfile()) return

  isSavingProfile.value = true
  profileSavedSuccess.value = false

  setTimeout(() => {
    const trimmed = profileForm.fullName.trim()
    if (home.userStore?.state) {
      home.userStore.state.fullName = trimmed
    }
    try {
      localStorage.setItem('adaptive-learning-fullname', trimmed)
    } catch (err) {}

    isSavingProfile.value = false
    profileSavedSuccess.value = true
    toast.success('Đã lưu thông tin cá nhân thành công!')

    setTimeout(() => {
      profileSavedSuccess.value = false
    }, 4000)
  }, 350)
}

// Form Đổi mật khẩu
const passwordForm = reactive({
  currentPassword: '',
  newPassword: '',
  confirmPassword: '',
})

const passwordErrors = reactive({
  currentPassword: '',
  newPassword: '',
  confirmPassword: '',
})

const showCurrentPassword = ref(false)
const showNewPassword = ref(false)
const showConfirmPassword = ref(false)
const isChangingPassword = ref(false)
const passwordNoticeMessage = ref('')

const passwordStrength = computed(() => {
  const p = passwordForm.newPassword
  if (!p) return 0
  let score = 0
  if (p.length >= 8) score += 1
  if (/[A-Z]/.test(p) && /[a-z]/.test(p)) score += 1
  if (/\d/.test(p)) score += 1
  if (/[^A-Za-z0-9]/.test(p)) score += 1
  return score
})

const passwordStrengthLabel = computed(() => {
  if (!passwordForm.newPassword) return ''
  if (passwordStrength.value <= 1) return 'Yếu (cần thêm độ dài và số)'
  if (passwordStrength.value === 2) return 'Trung bình (thêm ký tự viết hoa & ký tự đặc biệt)'
  if (passwordStrength.value === 3) return 'Khá mạnh'
  return 'Rất mạnh và an toàn'
})

function validatePasswordForm() {
  let valid = true
  passwordErrors.currentPassword = ''
  passwordErrors.newPassword = ''
  passwordErrors.confirmPassword = ''

  if (!passwordForm.currentPassword) {
    passwordErrors.currentPassword = 'Vui lòng nhập mật khẩu hiện tại.'
    valid = false
  }

  if (!passwordForm.newPassword) {
    passwordErrors.newPassword = 'Vui lòng nhập mật khẩu mới.'
    valid = false
  } else if (passwordForm.newPassword.length < 8) {
    passwordErrors.newPassword = 'Mật khẩu mới phải có tối thiểu 8 ký tự.'
    valid = false
  }

  if (!passwordForm.confirmPassword) {
    passwordErrors.confirmPassword = 'Vui lòng xác nhận lại mật khẩu mới.'
    valid = false
  } else if (passwordForm.newPassword !== passwordForm.confirmPassword) {
    passwordErrors.confirmPassword = 'Mật khẩu xác nhận không khớp.'
    valid = false
  }

  return valid
}

function handlePasswordSubmit() {
  passwordNoticeMessage.value = ''
  if (!validatePasswordForm()) return

  isChangingPassword.value = true

  // Giữ nguyên hành vi mô phỏng qua setTimeout và không hiển thị thông báo thành công gây hiểu nhầm
  setTimeout(() => {
    isChangingPassword.value = false
    passwordForm.currentPassword = ''
    passwordForm.newPassword = ''
    passwordForm.confirmPassword = ''

    passwordNoticeMessage.value =
      'Tính năng đổi mật khẩu chưa được kết nối với API backend. Mật khẩu của bạn chưa thay đổi trên máy chủ.'
    toast.info('Tính năng đổi mật khẩu chưa được kết nối với API backend. Mật khẩu của bạn chưa thay đổi.')
  }, 400)
}

// Tùy chọn hiển thị & hệ thống
const sidebarPreference = ref(localStorage.getItem('adaptive-sidebar-collapsed') === 'true')

function toggleSidebarPreference() {
  sidebarPreference.value = !sidebarPreference.value
  try {
    localStorage.setItem('adaptive-sidebar-collapsed', String(sidebarPreference.value))
  } catch (err) {}
  toast.info(sidebarPreference.value ? 'Đã bật chế độ thu gọn thanh điều hướng mặc định.' : 'Đã bật chế độ mở rộng thanh điều hướng mặc định.')
}

function handleSetStatus(status) {
  if (home.userStatus !== undefined) {
    home.userStatus = status
  }
  toast.info(`Trạng thái hoạt động: ${status === 'ready' ? 'Sẵn sàng học' : status === 'busy' ? 'Đang bận' : 'Tạm vắng'}`)
}

function handleToggleNotifications() {
  if (typeof home.toggleNotifications === 'function') {
    home.toggleNotifications()
  } else if (home.notificationsMuted !== undefined) {
    home.notificationsMuted = !home.notificationsMuted
    toast.info(home.notificationsMuted ? 'Đã tắt thông báo.' : 'Đã bật thông báo.')
  }
}

// Chuyển sang trang bảng giá
function navigateToPricing() {
  router.push('/pricing')
}
</script>

<template>
  <div class="settings-wrapper">
    <!-- TIÊU ĐỀ TRANG -->
    <header class="settings-header">
      <div class="header-content">
        <p class="settings-kicker">
          <User :size="13" class="inline-icon" />
          TÀI KHOẢN & HỒ SƠ
        </p>
        <h1 class="settings-title">Cài đặt tài khoản</h1>
        <p class="settings-subtitle">
          Quản lý thông tin định danh, gói học tập, tùy chọn hệ thống và bảo mật tài khoản.
        </p>
      </div>

      <div class="header-actions">
        <Button variant="outline" size="sm" class="logout-quick-btn" @click="home.logout">
          <LogOut :size="15" />
          <span>Đăng xuất</span>
        </Button>
      </div>
    </header>

    <!-- THẺ TỔNG QUAN HỒ SƠ NGƯỜI DÙNG (PROFILE HERO CARD) -->
    <section class="profile-hero-card" aria-label="Tổng quan tài khoản">
      <div class="profile-hero-left">
        <div class="avatar-container">
          <img
            v-if="avatarUrl"
            :src="avatarUrl"
            alt="Ảnh đại diện"
            class="hero-avatar-image"
          />
          <div v-else class="hero-avatar-initials">
            {{ home.initials }}
          </div>
          <span
            class="status-indicator-dot"
            :class="home.userStatus || 'ready'"
            :title="`Trạng thái: ${home.userStatus === 'busy' ? 'Đang bận' : home.userStatus === 'away' ? 'Tạm vắng' : 'Sẵn sàng học'}`"
          ></span>
        </div>

        <div class="hero-info">
          <div class="hero-title-row">
            <h2 class="hero-name">{{ home.userName }}</h2>
            <Badge :variant="home.userStore?.state?.role === 'ROLE_ADMIN' ? 'destructive' : 'default'">
              {{ home.userStore?.state?.role === 'ROLE_ADMIN' ? 'Quản trị viên' : 'Học viên' }}
            </Badge>
            <Badge v-if="home.isPremium" variant="ai" class="premium-badge">
              <Sparkles :size="12" class="mr-1" />
              Gói Premium
            </Badge>
            <Badge v-else variant="secondary" class="free-badge">
              Gói Miễn phí
            </Badge>
          </div>

          <p class="hero-username">
            <Mail :size="14" class="inline-icon" />
            <span>{{ home.userStore?.state?.username || 'Chưa cập nhật email' }}</span>
          </p>
        </div>
      </div>

      <div class="profile-hero-right">
        <div class="hero-stat-box">
          <span class="stat-label">Hạn ngạch tạo đề AI</span>
          <strong class="stat-value">
            {{ home.isPremium ? 'Không giới hạn' : `${home.entitlements?.examsUsed ?? 0} / ${home.entitlements?.examLimit ?? 5}` }}
          </strong>
        </div>
      </div>
    </section>

    <!-- THANH TABS ĐIỀU HƯỚNG NHÓM THÔNG TIN -->
    <nav class="settings-tabs-nav" aria-label="Nhóm cài đặt">
      <button
        type="button"
        class="tab-nav-btn"
        :class="{ active: activeTab === 'profile' }"
        @click="activeTab = 'profile'"
      >
        <User :size="16" />
        <span>Hồ sơ cá nhân</span>
      </button>

      <button
        type="button"
        class="tab-nav-btn"
        :class="{ active: activeTab === 'plan' }"
        @click="activeTab = 'plan'"
      >
        <Crown :size="16" />
        <span>Gói học & Hạn ngạch</span>
      </button>

      <button
        type="button"
        class="tab-nav-btn"
        :class="{ active: activeTab === 'preferences' }"
        @click="activeTab = 'preferences'"
      >
        <Sliders :size="16" />
        <span>Tùy chọn trải nghiệm</span>
      </button>

      <button
        type="button"
        class="tab-nav-btn"
        :class="{ active: activeTab === 'security' }"
        @click="activeTab = 'security'"
      >
        <KeyRound :size="16" />
        <span>Bảo mật & Mật khẩu</span>
      </button>
    </nav>

    <!-- NỘI DUNG TỪNG TAB -->
    <main class="settings-tab-content">
      <!-- ============================================== -->
      <!-- TAB 1: HỒ SƠ CÁ NHÂN -->
      <!-- ============================================== -->
      <div v-show="activeTab === 'profile'" class="tab-pane">
        <!-- Khu vực ảnh đại diện -->
        <section class="settings-card">
          <div class="card-header">
            <div>
              <h3 class="card-title">Ảnh đại diện</h3>
              <p class="card-description">
                Ảnh đại diện giúp giảng viên và học viên khác dễ dàng nhận diện bạn trên diễn đàn và bài kiểm tra.
              </p>
            </div>
          </div>

          <div class="avatar-edit-layout">
            <div class="avatar-preview-box">
              <img
                v-if="avatarUrl"
                :src="avatarUrl"
                alt="Ảnh xem trước"
                class="avatar-large-preview"
              />
              <div v-else class="avatar-large-initials">
                {{ home.initials }}
              </div>
            </div>

            <div class="avatar-controls">
              <div class="avatar-button-group">
                <input
                  ref="avatarInputRef"
                  type="file"
                  accept="image/png,image/jpeg,image/webp"
                  class="sr-only"
                  @change="handleAvatarChange"
                />
                <Button variant="default" size="sm" @click="triggerAvatarUpload">
                  <Camera :size="15" />
                  <span>Tải ảnh mới</span>
                </Button>
                <Button
                  v-if="avatarUrl"
                  variant="outline"
                  size="sm"
                  class="btn-danger-outline"
                  @click="removeAvatar"
                >
                  <Trash2 :size="15" />
                  <span>Xóa ảnh</span>
                </Button>
              </div>
              <p class="avatar-hint-text">
                Định dạng hỗ trợ: PNG, JPG hoặc WebP. Dung lượng tệp tối đa 2MB. Nếu không tải ảnh, hệ thống hiển thị chữ cái viết tắt của bạn.
              </p>
            </div>
          </div>
        </section>

        <!-- Form thông tin tài khoản -->
        <section class="settings-card">
          <div class="card-header">
            <div>
              <h3 class="card-title">Thông tin định danh</h3>
              <p class="card-description">
                Họ và tên hiển thị trong hệ thống, bảng xếp hạng năng lực và chứng chỉ hoàn thành khóa học.
              </p>
            </div>
          </div>

          <!-- Thông báo lưu thành công -->
          <div v-if="profileSavedSuccess" class="alert-banner alert-success" role="status">
            <CheckCircle2 :size="17" />
            <span>Đã lưu thông tin cá nhân thành công.</span>
          </div>

          <form class="settings-form" @submit.prevent="saveProfile">
            <div class="form-grid">
              <!-- Trường Họ và tên (Editable) -->
              <div class="field-group">
                <label for="profile-fullname" class="field-label">
                  Họ và tên <span class="required-mark">*</span>
                </label>
                <input
                  id="profile-fullname"
                  v-model="profileForm.fullName"
                  type="text"
                  class="field-input"
                  :class="{ 'field-input-error': profileErrors.fullName }"
                  placeholder="Nhập họ và tên của bạn"
                  maxlength="60"
                  autocomplete="name"
                  :aria-invalid="!!profileErrors.fullName"
                  :aria-describedby="profileErrors.fullName ? 'fullname-error' : 'fullname-hint'"
                />
                <p v-if="profileErrors.fullName" id="fullname-error" class="field-error" role="alert">
                  <AlertCircle :size="13" />
                  <span>{{ profileErrors.fullName }}</span>
                </p>
                <p v-else id="fullname-hint" class="field-hint">
                  Tên hiển thị chính thức trên hệ thống học tập Adaptive Learning.
                </p>
              </div>

              <!-- Trường Tên đăng nhập (Read-only) -->
              <div class="field-group">
                <label for="profile-username" class="field-label">
                  Tên đăng nhập / Email
                </label>
                <div class="input-with-icon readonly-wrapper">
                  <input
                    id="profile-username"
                    :value="home.userStore?.state?.username || '—'"
                    type="text"
                    class="field-input readonly-input"
                    readonly
                    disabled
                  />
                  <Lock :size="15" class="trailing-icon" />
                </div>
                <p class="field-hint">
                  Tên đăng nhập cố định liên kết với tài khoản, không thể tự chỉnh sửa.
                </p>
              </div>

              <!-- Trường Vai trò hệ thống (Read-only) -->
              <div class="field-group full-width">
                <label for="profile-role" class="field-label">
                  Vai trò tài khoản
                </label>
                <div class="input-with-icon readonly-wrapper">
                  <input
                    id="profile-role"
                    :value="home.userStore?.state?.role === 'ROLE_ADMIN' ? 'Quản trị viên hệ thống (Administrator)' : 'Học viên (Learner)'"
                    type="text"
                    class="field-input readonly-input"
                    readonly
                    disabled
                  />
                  <ShieldCheck :size="15" class="trailing-icon" />
                </div>
                <p class="field-hint">
                  Xác định quyền truy cập ngân hàng câu hỏi, bài thi và công cụ quản trị.
                </p>
              </div>
            </div>

            <div class="form-actions">
              <Button
                type="submit"
                variant="default"
                :loading="isSavingProfile"
                :disabled="isSavingProfile"
              >
                <Check :size="16" />
                <span>Lưu thay đổi</span>
              </Button>
            </div>
          </form>
        </section>
      </div>

      <!-- ============================================== -->
      <!-- TAB 2: GÓI HỌC & HẠN NGẠCH -->
      <!-- ============================================== -->
      <div v-show="activeTab === 'plan'" class="tab-pane">
        <section class="settings-card">
          <div class="card-header">
            <div>
              <h3 class="card-title">Gói dịch vụ hiện tại</h3>
              <p class="card-description">
                Chi tiết quyền lợi, hạn ngạch bài thi AI và trạng thái kích hoạt tài nguyên học tập.
              </p>
            </div>

            <Button
              v-if="!home.isPremium"
              variant="default"
              size="sm"
              class="btn-upgrade"
              @click="navigateToPricing"
            >
              <Crown :size="15" />
              <span>Nâng cấp Premium</span>
            </Button>
          </div>

          <!-- Banner tóm tắt gói -->
          <div class="plan-status-card" :class="{ 'is-premium-card': home.isPremium }">
            <div class="plan-status-content">
              <div class="plan-status-badge-wrap">
                <Badge :variant="home.isPremium ? 'ai' : 'default'" class="plan-tag">
                  {{ home.isPremium ? 'GÓI CHUYÊN NGHIỆP (PREMIUM)' : 'GÓI CƠ BẢN (FREE)' }}
                </Badge>
                <span class="plan-active-status">
                  <span class="status-pulse-dot"></span>
                  Đang hoạt động
                </span>
              </div>
              <h4 class="plan-card-title">
                {{ home.isPremium ? 'Toàn quyền sử dụng Trợ lý AI và Lộ trình thích ứng' : 'Tài khoản Miễn phí' }}
              </h4>
              <p class="plan-card-desc">
                {{
                  home.isPremium
                    ? 'Bạn đang được kích hoạt đầy đủ mọi tính năng AI Tutor 24/7, không giới hạn lượt thi thử thích ứng và phân tích chuyên sâu BKT.'
                    : 'Gói Miễn phí cung cấp trải nghiệm học tập cơ bản với hạn ngạch tạo bài thi thử thích ứng mỗi chu kỳ. Hãy nâng cấp để mở khóa Trợ lý AI Tutor.'
                }}
              </p>
            </div>

            <div class="plan-features-list">
              <div class="feature-item">
                <Check :size="16" class="feature-icon feature-ok" />
                <span>Bài kiểm tra chính thức</span>
              </div>
              <div class="feature-item">
                <Check :size="16" class="feature-icon feature-ok" />
                <span>Theo dõi tiến độ BKT</span>
              </div>
              <div class="feature-item">
                <Check
                  v-if="home.isPremium"
                  :size="16"
                  class="feature-icon feature-ok"
                />
                <AlertCircle v-else :size="16" class="feature-icon feature-locked" />
                <span :class="{ 'text-muted': !home.isPremium }">
                  Trợ lý AI Tutor giải thích bài thi
                </span>
              </div>
              <div class="feature-item">
                <Check
                  v-if="home.isPremium"
                  :size="16"
                  class="feature-icon feature-ok"
                />
                <AlertCircle v-else :size="16" class="feature-icon feature-locked" />
                <span :class="{ 'text-muted': !home.isPremium }">
                  Tạo đề luyện tập AI không giới hạn
                </span>
              </div>
            </div>
          </div>

          <!-- Chi tiết các hạn ngạch thực tế -->
          <div class="quota-grid">
            <div class="quota-card">
              <div class="quota-card-header">
                <div class="quota-icon-wrap quota-blue">
                  <Zap :size="18" />
                </div>
                <div>
                  <h5 class="quota-title">Lượt tạo đề thi thử AI</h5>
                  <p class="quota-subtitle">Hạn ngạch thực tế từ backend</p>
                </div>
              </div>
              <div class="quota-progress-section">
                <div class="quota-numbers">
                  <span class="quota-used">
                    {{ home.isPremium ? 'Không giới hạn' : `${home.entitlements?.examsUsed ?? 0} đã dùng` }}
                  </span>
                  <span v-if="!home.isPremium" class="quota-total">
                    / {{ home.entitlements?.examLimit ?? 5 }} lượt
                  </span>
                </div>
                <div v-if="!home.isPremium" class="progress-bar-track">
                  <div
                    class="progress-bar-fill"
                    :style="{
                      width: `${Math.min(100, Math.round(((home.entitlements?.examsUsed ?? 0) / (home.entitlements?.examLimit || 5)) * 100))}%`
                    }"
                  ></div>
                </div>
              </div>
            </div>

            <div class="quota-card">
              <div class="quota-card-header">
                <div class="quota-icon-wrap quota-purple">
                  <Sparkles :size="18" />
                </div>
                <div>
                  <h5 class="quota-title">Trợ lý AI Tutor</h5>
                  <p class="quota-subtitle">Hỏi đáp & giải thích lỗi sai</p>
                </div>
              </div>
              <div class="quota-status-row">
                <Badge :variant="home.isPremium ? 'ai' : 'outline'">
                  {{ home.isPremium ? 'Đã kích hoạt' : 'Yêu cầu gói Premium' }}
                </Badge>
                <p class="quota-detail-text">
                  {{ home.isPremium ? 'Hỏi đáp trực tiếp không giới hạn câu hỏi' : 'Mở khóa tính năng hỗ trợ học tập thông minh' }}
                </p>
              </div>
            </div>
          </div>
        </section>
      </div>

      <!-- ============================================== -->
      <!-- TAB 3: TÙY CHỌN TRẢI NGHIỆM -->
      <!-- ============================================== -->
      <div v-show="activeTab === 'preferences'" class="tab-pane">
        <section class="settings-card">
          <div class="card-header">
            <div>
              <h3 class="card-title">Tùy chọn hiển thị & Thông báo</h3>
              <p class="card-description">
                Điều chỉnh các chế độ hiển thị giao diện và nhận diện trạng thái học tập.
              </p>
            </div>
          </div>

          <div class="preference-list">
            <!-- Tùy chọn Thông báo -->
            <div class="preference-item">
              <div class="preference-info">
                <div class="pref-icon-circle">
                  <Bell :size="18" />
                </div>
                <div>
                  <h4 class="pref-label">Thông báo hệ thống</h4>
                  <p class="pref-desc">
                    Nhận thông báo khi có bài kiểm tra mới, lời nhắc kế hoạch học và kết quả thi.
                  </p>
                </div>
              </div>
              <div class="preference-action">
                <button
                  type="button"
                  class="toggle-switch"
                  :class="{ active: !home.notificationsMuted }"
                  role="switch"
                  :aria-checked="!home.notificationsMuted"
                  aria-label="Bật hoặc tắt thông báo"
                  @click="handleToggleNotifications"
                >
                  <span class="toggle-thumb"></span>
                </button>
              </div>
            </div>

            <!-- Tùy chọn Sidebar mặc định -->
            <div class="preference-item">
              <div class="preference-info">
                <div class="pref-icon-circle">
                  <PanelLeftClose :size="18" />
                </div>
                <div>
                  <h4 class="pref-label">Thanh điều hướng thu gọn</h4>
                  <p class="pref-desc">
                    Tự động thu gọn thanh điều hướng bên trái khi vào trang để mở rộng không gian học tập.
                  </p>
                </div>
              </div>
              <div class="preference-action">
                <button
                  type="button"
                  class="toggle-switch"
                  :class="{ active: sidebarPreference }"
                  role="switch"
                  :aria-checked="sidebarPreference"
                  aria-label="Thu gọn hoặc mở rộng thanh điều hướng"
                  @click="toggleSidebarPreference"
                >
                  <span class="toggle-thumb"></span>
                </button>
              </div>
            </div>

            <!-- Tùy chọn Trạng thái học tập -->
            <div class="preference-item column-item">
              <div class="preference-info">
                <div class="pref-icon-circle">
                  <Clock :size="18" />
                </div>
                <div>
                  <h4 class="pref-label">Trạng thái hoạt động</h4>
                  <p class="pref-desc">
                    Chọn trạng thái hiển thị của bạn trên thanh tiêu đề và hồ sơ học viên.
                  </p>
                </div>
              </div>

              <div class="status-selector-row">
                <button
                  type="button"
                  class="status-choice-btn"
                  :class="{ active: (home.userStatus || 'ready') === 'ready' }"
                  @click="handleSetStatus('ready')"
                >
                  <span class="status-dot dot-ready"></span>
                  <span>Sẵn sàng học</span>
                </button>

                <button
                  type="button"
                  class="status-choice-btn"
                  :class="{ active: home.userStatus === 'busy' }"
                  @click="handleSetStatus('busy')"
                >
                  <span class="status-dot dot-busy"></span>
                  <span>Đang bận</span>
                </button>

                <button
                  type="button"
                  class="status-choice-btn"
                  :class="{ active: home.userStatus === 'away' }"
                  @click="handleSetStatus('away')"
                >
                  <span class="status-dot dot-away"></span>
                  <span>Tạm vắng</span>
                </button>
              </div>
            </div>
          </div>
        </section>
      </div>

      <!-- ============================================== -->
      <!-- TAB 4: BẢO MẬT & MẬT KHẨU -->
      <!-- ============================================== -->
      <div v-show="activeTab === 'security'" class="tab-pane">
        <!-- Card Đổi mật khẩu -->
        <section class="settings-card">
          <div class="card-header">
            <div>
              <h3 class="card-title">Đổi mật khẩu tài khoản</h3>
              <p class="card-description">
                Để bảo vệ an toàn cho tài khoản, hãy sử dụng mật khẩu mạnh gồm chữ hoa, chữ thường và chữ số.
              </p>
            </div>
          </div>

          <!-- Thông báo minh bạch về hiện trạng hệ thống -->
          <div
            v-if="passwordNoticeMessage"
            class="alert-banner alert-warning"
            role="status"
          >
            <AlertTriangle :size="18" class="flex-shrink-0" />
            <div class="alert-content">
              <strong>Thông báo hệ thống:</strong>
              <p>{{ passwordNoticeMessage }}</p>
            </div>
          </div>

          <form class="settings-form" @submit.prevent="handlePasswordSubmit">
            <div class="form-grid">
              <!-- Mật khẩu hiện tại -->
              <div class="field-group full-width">
                <label for="current-password" class="field-label">
                  Mật khẩu hiện tại <span class="required-mark">*</span>
                </label>
                <div class="input-with-icon">
                  <input
                    id="current-password"
                    v-model="passwordForm.currentPassword"
                    :type="showCurrentPassword ? 'text' : 'password'"
                    class="field-input"
                    :class="{ 'field-input-error': passwordErrors.currentPassword }"
                    placeholder="Nhập mật khẩu đang dùng"
                    autocomplete="current-password"
                    :aria-invalid="!!passwordErrors.currentPassword"
                    :aria-describedby="passwordErrors.currentPassword ? 'current-pw-error' : undefined"
                  />
                  <button
                    type="button"
                    class="field-toggle-btn"
                    :aria-label="showCurrentPassword ? 'Ẩn mật khẩu' : 'Hiện mật khẩu'"
                    @click="showCurrentPassword = !showCurrentPassword"
                  >
                    <EyeOff v-if="showCurrentPassword" :size="16" />
                    <Eye v-else :size="16" />
                  </button>
                </div>
                <p v-if="passwordErrors.currentPassword" id="current-pw-error" class="field-error" role="alert">
                  <AlertCircle :size="13" />
                  <span>{{ passwordErrors.currentPassword }}</span>
                </p>
              </div>

              <!-- Mật khẩu mới -->
              <div class="field-group">
                <label for="new-password" class="field-label">
                  Mật khẩu mới <span class="required-mark">*</span>
                </label>
                <div class="input-with-icon">
                  <input
                    id="new-password"
                    v-model="passwordForm.newPassword"
                    :type="showNewPassword ? 'text' : 'password'"
                    class="field-input"
                    :class="{ 'field-input-error': passwordErrors.newPassword }"
                    placeholder="Tối thiểu 8 ký tự"
                    autocomplete="new-password"
                    :aria-invalid="!!passwordErrors.newPassword"
                    :aria-describedby="passwordErrors.newPassword ? 'new-pw-error' : 'new-pw-hint'"
                  />
                  <button
                    type="button"
                    class="field-toggle-btn"
                    :aria-label="showNewPassword ? 'Ẩn mật khẩu' : 'Hiện mật khẩu'"
                    @click="showNewPassword = !showNewPassword"
                  >
                    <EyeOff v-if="showNewPassword" :size="16" />
                    <Eye v-else :size="16" />
                  </button>
                </div>

                <!-- Thanh đo độ mạnh mật khẩu -->
                <div v-if="passwordForm.newPassword" class="password-meter-wrap">
                  <div class="meter-bars">
                    <span class="meter-bar" :class="{ filled: passwordStrength >= 1, weak: passwordStrength === 1, medium: passwordStrength === 2, strong: passwordStrength >= 3 }"></span>
                    <span class="meter-bar" :class="{ filled: passwordStrength >= 2, medium: passwordStrength === 2, strong: passwordStrength >= 3 }"></span>
                    <span class="meter-bar" :class="{ filled: passwordStrength >= 3, strong: passwordStrength >= 3 }"></span>
                    <span class="meter-bar" :class="{ filled: passwordStrength >= 4, strong: passwordStrength >= 4 }"></span>
                  </div>
                  <span class="meter-label">{{ passwordStrengthLabel }}</span>
                </div>

                <p v-if="passwordErrors.newPassword" id="new-pw-error" class="field-error" role="alert">
                  <AlertCircle :size="13" />
                  <span>{{ passwordErrors.newPassword }}</span>
                </p>
                <p v-else id="new-pw-hint" class="field-hint">
                  Nên kết hợp chữ cái viết hoa, viết thường, chữ số và ký tự đặc biệt.
                </p>
              </div>

              <!-- Xác nhận mật khẩu mới -->
              <div class="field-group">
                <label for="confirm-password" class="field-label">
                  Xác nhận mật khẩu mới <span class="required-mark">*</span>
                </label>
                <div class="input-with-icon">
                  <input
                    id="confirm-password"
                    v-model="passwordForm.confirmPassword"
                    :type="showConfirmPassword ? 'text' : 'password'"
                    class="field-input"
                    :class="{ 'field-input-error': passwordErrors.confirmPassword }"
                    placeholder="Nhập lại mật khẩu mới"
                    autocomplete="new-password"
                    :aria-invalid="!!passwordErrors.confirmPassword"
                    :aria-describedby="passwordErrors.confirmPassword ? 'confirm-pw-error' : undefined"
                  />
                  <button
                    type="button"
                    class="field-toggle-btn"
                    :aria-label="showConfirmPassword ? 'Ẩn mật khẩu' : 'Hiện mật khẩu'"
                    @click="showConfirmPassword = !showConfirmPassword"
                  >
                    <EyeOff v-if="showConfirmPassword" :size="16" />
                    <Eye v-else :size="16" />
                  </button>
                </div>
                <p v-if="passwordErrors.confirmPassword" id="confirm-pw-error" class="field-error" role="alert">
                  <AlertCircle :size="13" />
                  <span>{{ passwordErrors.confirmPassword }}</span>
                </p>
              </div>
            </div>

            <div class="form-actions">
              <Button
                type="submit"
                variant="default"
                :loading="isChangingPassword"
                :disabled="isChangingPassword"
              >
                <KeyRound :size="16" />
                <span>Cập nhật mật khẩu</span>
              </Button>
            </div>
          </form>
        </section>

        <!-- Card Phiên đăng nhập & Đăng xuất -->
        <section class="settings-card danger-zone-card">
          <div class="card-header">
            <div>
              <h3 class="card-title text-danger">Quản lý phiên đăng nhập</h3>
              <p class="card-description">
                Đăng xuất khỏi thiết bị này để bảo vệ dữ liệu học tập và bảo mật tài khoản cá nhân.
              </p>
            </div>
          </div>

          <div class="danger-zone-content">
            <div class="session-info">
              <ShieldCheck :size="20" class="session-icon" />
              <div>
                <strong class="session-title">Phiên làm việc bảo mật</strong>
                <p class="session-desc">
                  Tài khoản đang được bảo vệ bằng mã thông báo xác thực JWT.
                </p>
              </div>
            </div>

            <Button
              variant="destructive"
              size="default"
              class="btn-logout"
              @click="home.logout"
            >
              <LogOut :size="16" />
              <span>Đăng xuất tài khoản</span>
            </Button>
          </div>
        </section>
      </div>
    </main>
  </div>
</template>

<style scoped>
/* ==========================================================================
   SETTINGS VIEW - LIGHT AI EDUCATION THEME
   Tokens: Primary: #2563EB | Secondary: #64748B | Border: #E2E8F0 | Surface: #FFF
   ========================================================================== */

.settings-wrapper {
  max-width: 980px;
  margin: 0 auto;
  display: flex;
  flex-direction: column;
  gap: 24px;
}

/* Header */
.settings-header {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 16px;
  padding-bottom: 4px;
}

.settings-kicker {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  margin: 0 0 6px 0;
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.06em;
  color: #2563EB;
  text-transform: uppercase;
}

.inline-icon {
  display: inline-block;
  vertical-align: middle;
}

.settings-title {
  margin: 0;
  font-size: 26px;
  font-weight: 700;
  color: #0F172A;
  line-height: 1.25;
}

.settings-subtitle {
  margin: 6px 0 0 0;
  font-size: 13px;
  color: #64748B;
  line-height: 1.5;
}

.logout-quick-btn {
  color: #DC2626;
  border-color: #FCA5A5;
  background: #FEF2F2;
}

.logout-quick-btn:hover {
  background: #FEE2E2;
  color: #B91C1C;
}

/* Profile Hero Card */
.profile-hero-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  padding: 24px;
  background: #FFFFFF;
  border: 1px solid #E2E8F0;
  border-radius: 12px;
  box-shadow: 0 1px 3px rgba(15, 23, 42, 0.04);
}

.profile-hero-left {
  display: flex;
  align-items: center;
  gap: 20px;
}

.avatar-container {
  position: relative;
  width: 72px;
  height: 72px;
  flex-shrink: 0;
}

.hero-avatar-image {
  width: 100%;
  height: 100%;
  border-radius: 50%;
  object-fit: cover;
  border: 2px solid #E2E8F0;
}

.hero-avatar-initials {
  width: 100%;
  height: 100%;
  border-radius: 50%;
  display: grid;
  place-items: center;
  background: linear-gradient(135deg, #2563EB 0%, #1D4ED8 100%);
  color: #FFFFFF;
  font-size: 26px;
  font-weight: 700;
  box-shadow: 0 2px 8px rgba(37, 99, 235, 0.2);
}

.status-indicator-dot {
  position: absolute;
  bottom: 2px;
  right: 2px;
  width: 14px;
  height: 14px;
  border-radius: 50%;
  border: 2.5px solid #FFFFFF;
}

.status-indicator-dot.ready {
  background: #16A34A;
}

.status-indicator-dot.busy {
  background: #D97706;
}

.status-indicator-dot.away {
  background: #94A3B8;
}

.hero-info {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.hero-title-row {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
}

.hero-name {
  margin: 0;
  font-size: 19px;
  font-weight: 700;
  color: #0F172A;
}

.premium-badge {
  background: #F5F3FF;
  color: #7C3AED;
  border: 1px solid #DDD6FE;
}

.free-badge {
  background: #F1F5F9;
  color: #475569;
}

.hero-username {
  margin: 0;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: #64748B;
}

.profile-hero-right {
  display: flex;
  align-items: center;
}

.hero-stat-box {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  padding: 10px 16px;
  border-radius: 8px;
  background: #F8FAFC;
  border: 1px solid #E2E8F0;
}

.stat-label {
  font-size: 11px;
  font-weight: 600;
  color: #64748B;
  text-transform: uppercase;
  letter-spacing: 0.04em;
}

.stat-value {
  font-size: 17px;
  font-weight: 700;
  color: #0F172A;
  margin-top: 2px;
}

/* Tabs Navigation */
.settings-tabs-nav {
  display: flex;
  gap: 6px;
  border-bottom: 1px solid #E2E8F0;
  padding-bottom: 0;
  overflow-x: auto;
}

.tab-nav-btn {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 10px 16px;
  border: 0;
  border-bottom: 2px solid transparent;
  background: transparent;
  color: #64748B;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  white-space: nowrap;
  transition: all 0.15s ease;
}

.tab-nav-btn:hover {
  color: #0F172A;
}

.tab-nav-btn.active {
  color: #2563EB;
  border-bottom-color: #2563EB;
}

/* Tab Content */
.settings-tab-content {
  display: flex;
  flex-direction: column;
}

.tab-pane {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

/* Settings Card */
.settings-card {
  padding: 24px;
  background: #FFFFFF;
  border: 1px solid #E2E8F0;
  border-radius: 12px;
  box-shadow: 0 1px 3px rgba(15, 23, 42, 0.03);
}

.card-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 20px;
}

.card-title {
  margin: 0;
  font-size: 16px;
  font-weight: 700;
  color: #0F172A;
}

.card-title.text-danger {
  color: #DC2626;
}

.card-description {
  margin: 4px 0 0 0;
  font-size: 13px;
  color: #64748B;
  line-height: 1.5;
}

/* Avatar edit layout */
.avatar-edit-layout {
  display: flex;
  align-items: center;
  gap: 24px;
}

.avatar-preview-box {
  width: 80px;
  height: 80px;
  flex-shrink: 0;
}

.avatar-large-preview {
  width: 100%;
  height: 100%;
  border-radius: 50%;
  object-fit: cover;
  border: 2px solid #E2E8F0;
}

.avatar-large-initials {
  width: 100%;
  height: 100%;
  border-radius: 50%;
  display: grid;
  place-items: center;
  background: linear-gradient(135deg, #2563EB 0%, #1D4ED8 100%);
  color: #FFFFFF;
  font-size: 28px;
  font-weight: 700;
}

.avatar-controls {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.avatar-button-group {
  display: flex;
  align-items: center;
  gap: 10px;
}

.btn-danger-outline {
  color: #DC2626;
  border-color: #FCA5A5;
}

.btn-danger-outline:hover {
  background: #FEF2F2;
}

.avatar-hint-text {
  margin: 0;
  font-size: 12px;
  color: #64748B;
  line-height: 1.5;
  max-width: 480px;
}

/* Forms */
.settings-form {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 18px;
}

.field-group {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.field-group.full-width {
  grid-column: 1 / -1;
}

.field-label {
  font-size: 13px;
  font-weight: 600;
  color: #334155;
}

.required-mark {
  color: #DC2626;
}

.field-input {
  width: 100%;
  box-sizing: border-box;
  height: 40px;
  padding: 0 12px;
  font-size: 13px;
  color: #0F172A;
  background: #FFFFFF;
  border: 1px solid #CBD5E1;
  border-radius: 8px;
  transition: all 0.15s ease;
  outline: none;
}

.field-input:focus {
  border-color: #2563EB;
  box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.12);
}

.field-input-error {
  border-color: #DC2626;
}

.field-input-error:focus {
  border-color: #DC2626;
  box-shadow: 0 0 0 3px rgba(220, 38, 38, 0.12);
}

.readonly-wrapper {
  background: #F8FAFC;
  border-radius: 8px;
}

.readonly-input {
  background: #F8FAFC;
  border-color: #E2E8F0;
  color: #64748B;
  cursor: not-allowed;
}

.input-with-icon {
  position: relative;
  display: flex;
  align-items: center;
}

.trailing-icon {
  position: absolute;
  right: 12px;
  color: #94A3B8;
  pointer-events: none;
}

.field-toggle-btn {
  position: absolute;
  right: 8px;
  display: grid;
  place-items: center;
  width: 28px;
  height: 28px;
  border: 0;
  background: transparent;
  color: #64748B;
  border-radius: 4px;
  cursor: pointer;
}

.field-toggle-btn:hover {
  color: #0F172A;
  background: #F1F5F9;
}

.field-error {
  margin: 0;
  display: inline-flex;
  align-items: center;
  gap: 5px;
  font-size: 12px;
  color: #DC2626;
}

.field-hint {
  margin: 0;
  font-size: 12px;
  color: #64748B;
}

.form-actions {
  display: flex;
  justify-content: flex-end;
  padding-top: 6px;
}

/* Alert banners */
.alert-banner {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 12px 16px;
  border-radius: 8px;
  font-size: 13px;
  line-height: 1.5;
  margin-bottom: 16px;
}

.alert-success {
  background: #F0FDF4;
  border: 1px solid #BBF7D0;
  color: #166534;
}

.alert-warning {
  background: #FFFBEB;
  border: 1px solid #FDE68A;
  color: #92400E;
}

.alert-content p {
  margin: 2px 0 0 0;
}

/* Password meter */
.password-meter-wrap {
  display: flex;
  flex-direction: column;
  gap: 4px;
  margin-top: 4px;
}

.meter-bars {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 4px;
}

.meter-bar {
  height: 4px;
  background: #E2E8F0;
  border-radius: 2px;
  transition: background 0.2s ease;
}

.meter-bar.filled.weak {
  background: #EF4444;
}

.meter-bar.filled.medium {
  background: #F59E0B;
}

.meter-bar.filled.strong {
  background: #10B981;
}

.meter-label {
  font-size: 11px;
  color: #64748B;
}

/* Plan card & Quotas */
.btn-upgrade {
  background: linear-gradient(135deg, #7C3AED 0%, #6D28D9 100%);
  color: #FFFFFF;
}

.btn-upgrade:hover {
  background: linear-gradient(135deg, #6D28D9 0%, #5B21B6 100%);
}

.plan-status-card {
  padding: 20px;
  border-radius: 10px;
  background: #F8FAFC;
  border: 1px solid #E2E8F0;
  display: grid;
  grid-template-columns: 1.4fr 1fr;
  gap: 24px;
  align-items: center;
  margin-bottom: 20px;
}

.plan-status-card.is-premium-card {
  background: #FAF5FF;
  border-color: #E9D5FF;
}

.plan-status-badge-wrap {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 8px;
}

.plan-active-status {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  font-weight: 600;
  color: #16A34A;
}

.status-pulse-dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: #16A34A;
  box-shadow: 0 0 0 3px rgba(22, 163, 74, 0.2);
}

.plan-card-title {
  margin: 0;
  font-size: 16px;
  font-weight: 700;
  color: #0F172A;
}

.plan-card-desc {
  margin: 6px 0 0 0;
  font-size: 13px;
  color: #64748B;
  line-height: 1.5;
}

.plan-features-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
  background: #FFFFFF;
  padding: 16px;
  border-radius: 8px;
  border: 1px solid #E2E8F0;
}

.feature-item {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  color: #334155;
}

.feature-icon.feature-ok {
  color: #16A34A;
  flex-shrink: 0;
}

.feature-icon.feature-locked {
  color: #94A3B8;
  flex-shrink: 0;
}

.text-muted {
  color: #94A3B8;
}

.quota-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
}

.quota-card {
  padding: 16px;
  border: 1px solid #E2E8F0;
  border-radius: 8px;
  background: #F8FAFC;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.quota-card-header {
  display: flex;
  align-items: center;
  gap: 12px;
}

.quota-icon-wrap {
  width: 36px;
  height: 36px;
  border-radius: 8px;
  display: grid;
  place-items: center;
  flex-shrink: 0;
}

.quota-icon-wrap.quota-blue {
  background: #EFF6FF;
  color: #2563EB;
}

.quota-icon-wrap.quota-purple {
  background: #F5F3FF;
  color: #7C3AED;
}

.quota-title {
  margin: 0;
  font-size: 14px;
  font-weight: 700;
  color: #0F172A;
}

.quota-subtitle {
  margin: 2px 0 0 0;
  font-size: 12px;
  color: #64748B;
}

.quota-numbers {
  display: flex;
  align-items: baseline;
  gap: 4px;
  font-size: 14px;
  font-weight: 600;
  color: #0F172A;
  margin-bottom: 6px;
}

.quota-total {
  font-size: 12px;
  color: #64748B;
}

.progress-bar-track {
  height: 6px;
  background: #E2E8F0;
  border-radius: 3px;
  overflow: hidden;
}

.progress-bar-fill {
  height: 100%;
  background: #2563EB;
  border-radius: 3px;
  transition: width 0.3s ease;
}

.quota-status-row {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.quota-detail-text {
  margin: 0;
  font-size: 12px;
  color: #64748B;
}

/* Preferences */
.preference-list {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.preference-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 16px;
  background: #F8FAFC;
  border: 1px solid #E2E8F0;
  border-radius: 8px;
}

.preference-item.column-item {
  flex-direction: column;
  align-items: flex-start;
  gap: 14px;
}

.preference-info {
  display: flex;
  align-items: flex-start;
  gap: 14px;
}

.pref-icon-circle {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: #EFF6FF;
  color: #2563EB;
  display: grid;
  place-items: center;
  flex-shrink: 0;
}

.pref-label {
  margin: 0;
  font-size: 14px;
  font-weight: 700;
  color: #0F172A;
}

.pref-desc {
  margin: 3px 0 0 0;
  font-size: 12px;
  color: #64748B;
  line-height: 1.5;
}

.toggle-switch {
  position: relative;
  width: 44px;
  height: 24px;
  border: 0;
  border-radius: 999px;
  background: #CBD5E1;
  cursor: pointer;
  padding: 2px;
  transition: background 0.2s ease;
}

.toggle-switch.active {
  background: #2563EB;
}

.toggle-thumb {
  display: block;
  width: 20px;
  height: 20px;
  border-radius: 50%;
  background: #FFFFFF;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.15);
  transition: transform 0.2s ease;
}

.toggle-switch.active .toggle-thumb {
  transform: translateX(20px);
}

.status-selector-row {
  display: flex;
  gap: 10px;
  width: 100%;
}

.status-choice-btn {
  flex: 1;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 10px;
  border: 1px solid #E2E8F0;
  border-radius: 8px;
  background: #FFFFFF;
  color: #475569;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.15s ease;
}

.status-choice-btn:hover {
  border-color: #CBD5E1;
  background: #F8FAFC;
}

.status-choice-btn.active {
  border-color: #2563EB;
  background: #EFF6FF;
  color: #1D4ED8;
}

.status-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
}

.dot-ready {
  background: #16A34A;
}

.dot-busy {
  background: #D97706;
}

.dot-away {
  background: #94A3B8;
}

/* Danger Zone / Session */
.danger-zone-card {
  border-color: #FEE2E2;
}

.danger-zone-content {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 16px;
  border-radius: 8px;
  background: #FEF2F2;
  border: 1px solid #FECACA;
}

.session-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.session-icon {
  color: #16A34A;
  flex-shrink: 0;
}

.session-title {
  display: block;
  font-size: 13px;
  color: #0F172A;
}

.session-desc {
  margin: 2px 0 0 0;
  font-size: 12px;
  color: #64748B;
}

.btn-logout {
  background: #DC2626;
  color: #FFFFFF;
}

.btn-logout:hover {
  background: #B91C1C;
}

.sr-only {
  position: absolute;
  width: 1px;
  height: 1px;
  padding: 0;
  margin: -1px;
  overflow: hidden;
  clip: rect(0, 0, 0, 0);
  white-space: nowrap;
  border: 0;
}

/* Responsive */
@media (max-width: 768px) {
  .profile-hero-card {
    flex-direction: column;
    align-items: flex-start;
  }

  .profile-hero-right {
    width: 100%;
  }

  .hero-stat-box {
    width: 100%;
    align-items: flex-start;
  }

  .form-grid {
    grid-template-columns: 1fr;
  }

  .plan-status-card {
    grid-template-columns: 1fr;
  }

  .quota-grid {
    grid-template-columns: 1fr;
  }

  .avatar-edit-layout {
    flex-direction: column;
    align-items: flex-start;
  }

  .status-selector-row {
    flex-direction: column;
  }

  .danger-zone-content {
    flex-direction: column;
    align-items: flex-start;
  }

  .btn-logout {
    width: 100%;
    justify-content: center;
  }
}
</style>
