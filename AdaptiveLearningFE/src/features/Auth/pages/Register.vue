<script setup>
import { computed, nextTick, onMounted, ref } from 'vue'
import { RouterLink, useRouter } from 'vue-router'
import {
  AlertCircle,
  ArrowRight,
  BarChart3,
  BookOpen,
  Brain,
  Check,
  Eye,
  EyeOff,
  GraduationCap,
  Loader2,
  LockKeyhole,
  Mail,
  ShieldCheck,
  Sparkles,
  UserRound,
} from 'lucide-vue-next'
import { userService } from '@/services/userService'
import { useUserStore } from '@/stores/user'
import { toast } from '@/components/ui/overlay'
import { Badge } from '@/components/ui/badge'
import { Input } from '@/components/ui/input'

const router = useRouter()
const userStore = useUserStore()

const fullName = ref('')
const email = ref('')
const password = ref('')
const confirmPassword = ref('')
const agreeTerms = ref(true)
const showPassword = ref(false)
const showConfirmPassword = ref(false)
const loading = ref(false)
const googleLoading = ref(false)
const error = ref('')

const googleButton = ref(null)
const googleClientId = ref('')
const googleStatus = ref('')
const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/

// Tính toán độ mạnh mật khẩu (1 = Yếu, 2 = Trung bình, 3 = Mạnh)
const passwordStrength = computed(() => {
  const val = password.value
  if (!val) return 0
  let score = 0
  if (val.length >= 8) score++
  if (/[A-Z]/.test(val) && /[a-z]/.test(val)) score++
  if (/\d/.test(val) || /[^A-Za-z0-9]/.test(val)) score++
  return Math.min(score, 3)
})

const passwordStrengthLabel = computed(() => {
  if (!password.value) return ''
  if (passwordStrength.value === 1) return 'Yếu (cần kết hợp chữ hoa, chữ thường hoặc số)'
  if (passwordStrength.value === 2) return 'Trung bình (thêm ký tự đặc biệt để an toàn hơn)'
  return 'Mạnh (mật khẩu an toàn)'
})

function errorMessage(exception) {
  const payload = exception?.response?.data
  const validationMessage =
    payload && typeof payload === 'object'
      ? Object.values(payload).find((value) => typeof value === 'string')
      : null
  const code = payload?.responseCode
  if (code === 'AUTH-ERR-405') return 'Email này đã được đăng ký. Hãy đăng nhập hoặc dùng email khác.'
  if (code === 'AUTH-ERR-424') return 'Bạn vừa yêu cầu mã. Hãy chờ một chút rồi thử lại.'
  if (code === 'AUTH-ERR-425') return 'Email chưa gửi được do hệ thống chưa cấu hình dịch vụ gửi thư.'
  const status = exception?.response?.status
  if (status === 400) return payload?.responseMessage || 'Thông tin đăng ký chưa hợp lệ.'
  if (status === 401) return 'Yêu cầu đăng ký không được xác thực. Vui lòng tải lại trang rồi thử lại.'
  if (status === 409) return 'Tài khoản cần được xác minh hoặc email đã được sử dụng.'
  if (status === 412 || status === 429) return 'Bạn vừa yêu cầu mã. Hãy chờ trước khi thử gửi lại.'
  if (status >= 500) return 'Dịch vụ đăng ký đang gặp sự cố. Vui lòng thử lại sau.'
  if (!exception?.response) {
    if (exception?.code === 'ECONNABORTED' || /timeout/i.test(exception?.message || '')) {
      return 'Máy chủ phản hồi quá thời gian chờ. Vui lòng thử lại.'
    }
    if (exception?.message && exception.message !== 'Network Error') return exception.message
    return 'Không thể kết nối máy chủ. Hãy kiểm tra kết nối rồi thử lại.'
  }
  return (
    payload?.responseMessage ||
    payload?.message ||
    validationMessage ||
    'Không thể tạo tài khoản. Vui lòng thử lại.'
  )
}

async function submit() {
  if (loading.value) return
  error.value = ''
  const trimmedName = fullName.value.trim()
  if (!trimmedName) {
    error.value = 'Vui lòng nhập họ và tên của bạn.'
    return
  }
  const normalizedEmail = email.value.trim().toLowerCase()
  if (!EMAIL_PATTERN.test(normalizedEmail)) {
    error.value = 'Vui lòng nhập địa chỉ email hợp lệ.'
    return
  }
  if (password.value.length < 8 || password.value.length > 72) {
    error.value = 'Mật khẩu cần có từ 8 đến 72 ký tự.'
    return
  }
  if (password.value !== confirmPassword.value) {
    error.value = 'Mật khẩu xác nhận chưa khớp.'
    return
  }
  if (!agreeTerms.value) {
    error.value = 'Vui lòng đồng ý với Điều khoản sử dụng.'
    return
  }

  loading.value = true
  try {
    const verification = await userService.signUp({
      email: normalizedEmail,
      fullName: trimmedName,
      password: password.value,
    })
    if (
      verification?.verified !== false ||
      verification?.email !== normalizedEmail ||
      !Number.isFinite(Date.parse(verification?.expiresAt)) ||
      !Number.isFinite(Date.parse(verification?.resendAvailableAt))
    ) {
      throw new Error('Máy chủ chưa xác nhận yêu cầu đăng ký. Vui lòng thử lại.')
    }
    toast.success('Mã xác minh đã được gửi', { description: `Vui lòng kiểm tra hộp thư ${normalizedEmail}` })
    await router.push({
      path: '/verify-email',
      query: {
        email: verification?.email || normalizedEmail,
        expiresAt: verification?.expiresAt,
        resendAvailableAt: verification?.resendAvailableAt,
      },
    })
  } catch (exception) {
    error.value = errorMessage(exception)
    toast.error('Đăng ký không thành công', { description: error.value })
  } finally {
    loading.value = false
  }
}

async function onGoogleSignIn(idToken) {
  googleLoading.value = true
  try {
    await userStore.loginWithGoogle(idToken)
    toast.success('Đăng nhập Google thành công', { description: 'Chào mừng bạn đến với hệ thống!' })
    const role = userStore.state.role
    await router.push(role === 'ROLE_ADMIN' ? '/admin' : '/home')
  } catch (err) {
    const errorMsg =
      err?.response?.data?.responseMessage || err?.message || 'Đăng ký/Đăng nhập qua Google thất bại.'
    toast.error('Đăng nhập thất bại', { description: errorMsg })
  } finally {
    googleLoading.value = false
  }
}

function renderGoogleButton() {
  if (!window.google?.accounts?.id || !googleButton.value) return
  window.google.accounts.id.initialize({
    client_id: googleClientId.value,
    callback: ({ credential }) => {
      if (credential) onGoogleSignIn(credential)
    },
    error_callback: (err) => {
      console.warn('Google GSI error:', err)
      if (err?.type === 'popup_closed_by_user') return
      googleStatus.value =
        'Chưa thể xác thực qua Google. Vui lòng kiểm tra Authorized JavaScript Origins trong Google Cloud Console.'
    },
  })
  const buttonWidth = Math.max(260, Math.min(googleButton.value.clientWidth || 360, 400))
  window.google.accounts.id.renderButton(googleButton.value, {
    type: 'standard',
    theme: 'outline',
    size: 'large',
    text: 'signup_with',
    shape: 'rectangular',
    width: buttonWidth,
    logo_alignment: 'left',
  })
}

onMounted(async () => {
  try {
    const config = await userService.googleConfig()
    googleClientId.value = String(config?.clientId || '').trim()
  } catch {
    googleClientId.value = import.meta.env.VITE_GOOGLE_CLIENT_ID?.trim() || ''
  }

  if (!googleClientId.value) {
    googleClientId.value = import.meta.env.VITE_GOOGLE_CLIENT_ID?.trim() || ''
  }

  if (!googleClientId.value) {
    googleStatus.value = 'Cần cấu hình Google OAuth Client ID để bật đăng ký qua Google.'
    return
  }

  await nextTick()

  if (window.google?.accounts?.id) {
    renderGoogleButton()
    return
  }

  const script = document.createElement('script')
  script.src = 'https://accounts.google.com/gsi/client'
  script.async = true
  script.defer = true
  script.onload = renderGoogleButton
  script.onerror = () => {
    googleStatus.value = 'Không tải được dịch vụ đăng ký Google.'
  }
  document.head.appendChild(script)
})
</script>

<template>
  <main class="register-page">
    <!-- CỘT TRÁI: GIỚI THIỆU HỌC TẬP THÍCH ỨNG AI (DESKTOP) -->
    <section class="intro-panel" aria-label="Giới thiệu Adaptive Learning">
      <div class="intro-inner">
        <!-- Brand Header -->
        <RouterLink class="brand-lockup" to="/login" aria-label="Về trang đăng nhập">
          <span class="brand-icon">
            <GraduationCap :size="24" :stroke-width="2.2" />
          </span>
          <span class="brand-text">Adaptive Learning</span>
        </RouterLink>

        <!-- Hero Content -->
        <div class="intro-content">
          <Badge variant="ai" class="intro-badge">
            <Sparkles :size="13" class="text-ai-accent" />
            NỀN TẢNG LUYỆN THI THÍCH ỨNG AI
          </Badge>

          <h1 class="intro-title">
            Khởi đầu hành trình<br class="hidden xl:inline" />
            học tập thông minh
          </h1>

          <p class="intro-desc">
            Đăng ký tài khoản để trải nghiệm gia sư AI cá nhân hóa, đo lường năng lực chuẩn xác và liên tục tối ưu lộ trình ôn luyện theo phương pháp khoa học.
          </p>
        </div>

        <!-- 3 Feature Highlight Cards -->
        <div class="features-list">
          <div class="feature-item">
            <div class="feature-icon bg-violet-50 text-ai-accent border border-violet-200/60">
              <Brain :size="20" />
            </div>
            <div class="feature-text">
              <h3>Lộ trình học tập cá nhân hóa</h3>
              <p>Mô hình Bayesian Knowledge Tracing tự động lấp đầy lỗ hổng kiến thức.</p>
            </div>
          </div>

          <div class="feature-item">
            <div class="feature-icon bg-blue-50 text-brand-primary border border-blue-200/60">
              <BookOpen :size="20" />
            </div>
            <div class="feature-text">
              <h3>Kho ngân hàng đề thi đa dạng</h3>
              <p>Hàng ngàn bài kiểm tra trắc nghiệm bám sát ma trận năng lực chuẩn.</p>
            </div>
          </div>

          <div class="feature-item">
            <div class="feature-icon bg-sky-50 text-brand-secondary border border-sky-200/60">
              <BarChart3 :size="20" />
            </div>
            <div class="feature-text">
              <h3>Theo dõi tiến độ thực tế</h3>
              <p>Thống kê xác suất thành thạo từng kỹ năng và phân tích điểm số chi tiết.</p>
            </div>
          </div>
        </div>

        <!-- Metric Proof Strip -->
        <div class="stats-strip">
          <div class="stat-unit">
            <strong>98.5%</strong>
            <span>Học viên tiến bộ</span>
          </div>
          <div class="stat-separator"></div>
          <div class="stat-unit">
            <strong>50.000+</strong>
            <span>Lượt làm bài thi</span>
          </div>
          <div class="stat-separator"></div>
          <div class="stat-unit">
            <strong>24/7</strong>
            <span>Gia sư AI đồng hành</span>
          </div>
        </div>
      </div>
    </section>

    <!-- CỘT PHẢI: FORM ĐĂNG KÝ (RESPONSIVE SINGLE-COLUMN TRÊN MOBILE) -->
    <section class="form-panel" aria-labelledby="register-heading">
      <div class="register-card-wrapper">
        <!-- Header di động -->
        <div class="mobile-brand-header">
          <span class="brand-icon">
            <GraduationCap :size="22" :stroke-width="2.2" />
          </span>
          <span class="brand-text">Adaptive Learning</span>
        </div>

        <!-- Form Heading -->
        <div class="register-header">
          <h2 id="register-heading" class="register-title">Tạo tài khoản học viên</h2>
          <p class="register-subtitle">Khởi đầu học tập thích ứng cùng gia sư AI thông minh</p>
        </div>

        <!-- Google OAuth Section -->
        <div class="google-login-wrap">
          <div v-if="googleClientId" ref="googleButton" class="google-button-container"></div>
          <button v-else class="google-fallback-button" type="button" disabled>
            <span class="google-mark">G</span>
            Đăng ký với Google
          </button>
          <p v-if="googleLoading" class="google-status is-loading" role="status">
            <Loader2 class="animate-spin" :size="14" /> Đang xử lý đăng ký với Google…
          </p>
          <p v-if="googleStatus" class="google-status" role="status">{{ googleStatus }}</p>
        </div>

        <!-- Divider -->
        <div class="form-divider">
          <span>hoặc đăng ký bằng email</span>
        </div>

        <!-- Form Đăng ký -->
        <form class="register-form" @submit.prevent="submit">
          <!-- Họ và tên -->
          <div class="field-item">
            <label class="field-label" for="register-fullname">Họ và tên</label>
            <div class="field-container">
              <UserRound class="field-icon" :size="18" />
              <Input
                id="register-fullname"
                v-model="fullName"
                type="text"
                maxlength="100"
                required
                autocomplete="name"
                placeholder="Ví dụ: Nguyễn Văn A"
                aria-label="Họ và tên"
                class="field-input"
              />
            </div>
          </div>

          <!-- Email -->
          <div class="field-item">
            <label class="field-label" for="register-email">Địa chỉ Email</label>
            <div class="field-container">
              <Mail class="field-icon" :size="18" />
              <Input
                id="register-email"
                v-model="email"
                type="email"
                maxlength="254"
                required
                autocomplete="email"
                placeholder="name@example.com"
                aria-label="Địa chỉ Email"
                class="field-input"
              />
            </div>
          </div>

          <!-- Mật khẩu -->
          <div class="field-item">
            <label class="field-label" for="register-password">Mật khẩu</label>
            <div class="field-container">
              <LockKeyhole class="field-icon" :size="18" />
              <Input
                id="register-password"
                v-model="password"
                :type="showPassword ? 'text' : 'password'"
                maxlength="72"
                required
                autocomplete="new-password"
                placeholder="Từ 8 đến 72 ký tự..."
                aria-label="Mật khẩu"
                class="field-input"
              />
              <button
                class="visibility-toggle-btn"
                type="button"
                :aria-label="showPassword ? 'Ẩn mật khẩu' : 'Hiện mật khẩu'"
                tabindex="-1"
                @click="showPassword = !showPassword"
              >
                <EyeOff v-if="showPassword" :size="18" />
                <Eye v-else :size="18" />
              </button>
            </div>

            <!-- Thanh đo độ mạnh mật khẩu -->
            <div v-if="password" class="password-strength-wrap">
              <div class="strength-meter">
                <span
                  class="strength-bar"
                  :class="{
                    'bar-weak': passwordStrength >= 1,
                    'bar-medium': passwordStrength >= 2,
                    'bar-strong': passwordStrength >= 3,
                  }"
                ></span>
                <span
                  class="strength-bar"
                  :class="{
                    'bar-medium': passwordStrength >= 2,
                    'bar-strong': passwordStrength >= 3,
                  }"
                ></span>
                <span
                  class="strength-bar"
                  :class="{
                    'bar-strong': passwordStrength >= 3,
                  }"
                ></span>
              </div>
              <span class="strength-text" :class="'level-' + passwordStrength">
                {{ passwordStrengthLabel }}
              </span>
            </div>
          </div>

          <!-- Xác nhận mật khẩu -->
          <div class="field-item">
            <label class="field-label" for="register-confirm-password">Xác nhận mật khẩu</label>
            <div class="field-container">
              <LockKeyhole class="field-icon" :size="18" />
              <Input
                id="register-confirm-password"
                v-model="confirmPassword"
                :type="showConfirmPassword ? 'text' : 'password'"
                maxlength="72"
                required
                autocomplete="new-password"
                placeholder="Nhập lại mật khẩu..."
                aria-label="Xác nhận mật khẩu"
                class="field-input"
              />
              <button
                class="visibility-toggle-btn"
                type="button"
                :aria-label="showConfirmPassword ? 'Ẩn mật khẩu xác nhận' : 'Hiện mật khẩu xác nhận'"
                tabindex="-1"
                @click="showConfirmPassword = !showConfirmPassword"
              >
                <EyeOff v-if="showConfirmPassword" :size="18" />
                <Eye v-else :size="18" />
              </button>
            </div>
          </div>

          <!-- Điều khoản dịch vụ -->
          <div class="terms-row">
            <label class="terms-label">
              <input v-model="agreeTerms" type="checkbox" class="sr-only" />
              <span class="checkbox-indicator" :class="{ 'is-checked': agreeTerms }">
                <Check v-if="agreeTerms" :size="12" :stroke-width="3" />
              </span>
              <span class="terms-caption">
                Tôi đồng ý với Điều khoản dịch vụ và Chính sách bảo mật
              </span>
            </label>
          </div>

          <!-- Thông báo lỗi (từ validation hoặc Backend API) -->
          <div v-if="error" class="register-error-banner" role="alert">
            <AlertCircle :size="18" class="error-banner-icon" />
            <p class="error-banner-text">{{ error }}</p>
          </div>

          <!-- Nút Đăng ký -->
          <button class="submit-action-btn" type="submit" :disabled="loading">
            <Loader2 v-if="loading" class="animate-spin" :size="18" />
            <span v-else>ĐĂNG KÝ TÀI KHOẢN</span>
            <ArrowRight v-if="!loading" :size="18" />
          </button>
        </form>

        <!-- Link Chuyển sang Đăng nhập -->
        <p class="login-prompt">
          Đã có tài khoản học viên?
          <RouterLink to="/login" class="login-link">Đăng nhập ngay</RouterLink>
        </p>

        <!-- Trust / Security Indicator -->
        <div class="trust-indicator">
          <ShieldCheck :size="15" class="text-brand-primary" />
          <span>Hệ thống bảo vệ dữ liệu học tập an toàn</span>
        </div>
      </div>
    </section>
  </main>
</template>

<style scoped>
.register-page {
  display: grid;
  grid-template-columns: minmax(0, 1.15fr) minmax(460px, 0.85fr);
  min-height: 100vh;
  width: 100%;
  background-color: var(--bg-app);
  color: var(--text-primary);
  font-family: var(--font-sans);
}

/* ==========================================================================
   CỘT TRÁI: INTRO PANEL (ĐỒNG BỘ VỚI LOGIN)
   ========================================================================== */
.intro-panel {
  position: relative;
  display: flex;
  flex-direction: column;
  justify-content: center;
  padding: 48px clamp(36px, 5vw, 72px);
  background: linear-gradient(145deg, #F0FDFA 0%, #F8FAFC 55%, #F5F3FF 100%);
  border-right: 1px solid var(--border-subtle);
  overflow: hidden;
}

.intro-panel::before {
  position: absolute;
  top: -15%;
  right: -10%;
  width: 480px;
  height: 480px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(124, 58, 237, 0.06) 0%, transparent 70%);
  content: '';
  pointer-events: none;
}

.intro-panel::after {
  position: absolute;
  bottom: -20%;
  left: -10%;
  width: 520px;
  height: 520px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(15, 118, 110, 0.07) 0%, transparent 70%);
  content: '';
  pointer-events: none;
}

.intro-inner {
  position: relative;
  z-index: 1;
  max-width: 540px;
}

/* Brand Lockup */
.brand-lockup {
  display: inline-flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 36px;
  text-decoration: none;
  transition: opacity 0.15s ease;
}

.brand-lockup:hover {
  opacity: 0.9;
}

.brand-icon {
  display: grid;
  place-items: center;
  width: 42px;
  height: 42px;
  border-radius: var(--radius-md);
  background: linear-gradient(135deg, var(--brand-primary) 0%, #1D4ED8 100%);
  color: #ffffff;
  box-shadow: 0 4px 12px rgba(37, 99, 235, 0.2);
}

.brand-text {
  font-size: 20px;
  font-weight: 750;
  letter-spacing: -0.4px;
  color: var(--text-primary);
}

/* Intro Content */
.intro-content {
  margin-bottom: 32px;
}

.intro-badge {
  margin-bottom: 16px;
}

.intro-title {
  margin: 0 0 16px;
  font-size: clamp(28px, 3.2vw, 38px);
  font-weight: 800;
  line-height: 1.2;
  letter-spacing: -0.8px;
  color: var(--text-primary);
}

.intro-desc {
  margin: 0;
  font-size: 15px;
  line-height: 1.6;
  color: var(--text-secondary);
}

/* Features List */
.features-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
  margin-bottom: 36px;
}

.feature-item {
  display: flex;
  align-items: flex-start;
  gap: 14px;
  padding: 12px 14px;
  border-radius: var(--radius-md);
  background-color: rgba(255, 255, 255, 0.65);
  border: 1px solid rgba(226, 232, 240, 0.8);
  backdrop-filter: blur(8px);
  transition: background-color 0.2s ease;
}

.feature-item:hover {
  background-color: rgba(255, 255, 255, 0.95);
}

.feature-icon {
  display: grid;
  place-items: center;
  width: 38px;
  height: 38px;
  border-radius: 8px;
  flex-shrink: 0;
}

.feature-text h3 {
  margin: 0 0 3px;
  font-size: 14px;
  font-weight: 650;
  color: var(--text-primary);
}

.feature-text p {
  margin: 0;
  font-size: 13px;
  line-height: 1.45;
  color: var(--text-secondary);
}

/* Stats Strip */
.stats-strip {
  display: flex;
  align-items: center;
  gap: 24px;
  padding-top: 24px;
  border-top: 1px solid var(--border-subtle);
}

.stat-unit {
  display: flex;
  flex-direction: column;
}

.stat-unit strong {
  font-size: 18px;
  font-weight: 800;
  color: var(--brand-primary);
  line-height: 1.2;
}

.stat-unit span {
  font-size: 12px;
  color: var(--text-muted);
}

.stat-separator {
  width: 1px;
  height: 28px;
  background-color: var(--border-subtle);
}

/* ==========================================================================
   CỘT PHẢI: FORM PANEL
   ========================================================================== */
.form-panel {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px clamp(20px, 4vw, 56px);
  background-color: #ffffff;
  overflow-y: auto;
}

.register-card-wrapper {
  width: 100%;
  max-width: 440px;
}

/* Mobile Brand Header */
.mobile-brand-header {
  display: none;
  align-items: center;
  gap: 10px;
  margin-bottom: 24px;
}

.register-header {
  margin-bottom: 22px;
}

.register-title {
  margin: 0 0 6px;
  font-size: 26px;
  font-weight: 750;
  letter-spacing: -0.3px;
  color: var(--text-primary);
}

.register-subtitle {
  margin: 0;
  font-size: 14px;
  line-height: 1.5;
  color: var(--text-secondary);
}

/* Google OAuth Section */
.google-login-wrap {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.google-button-container {
  display: flex;
  justify-content: center;
  min-height: 44px;
  width: 100%;
}

.google-fallback-button {
  display: flex;
  width: 100%;
  height: 44px;
  align-items: center;
  justify-content: center;
  gap: 10px;
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-md);
  background-color: #ffffff;
  color: var(--text-primary);
  font-size: 13.5px;
  font-weight: 500;
  cursor: not-allowed;
  opacity: 0.8;
}

.google-mark {
  font-size: 16px;
  font-weight: 700;
  color: #4285F4;
}

.google-status {
  margin: 4px 0 0;
  font-size: 12px;
  line-height: 1.4;
  color: var(--text-secondary);
  text-align: center;
}

.google-status.is-loading {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  color: var(--brand-primary);
}

/* Form Divider */
.form-divider {
  display: flex;
  align-items: center;
  gap: 12px;
  margin: 18px 0;
  color: var(--text-secondary);
  font-size: 12px;
}

.form-divider::before,
.form-divider::after {
  height: 1px;
  flex: 1;
  background-color: var(--border-subtle);
  content: '';
}

/* Register Form */
.register-form {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.field-item {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.field-label {
  font-size: 13px;
  font-weight: 600;
  color: var(--text-primary);
}

.field-container {
  position: relative;
  display: flex;
  align-items: center;
  height: 46px;
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-md);
  background-color: #ffffff;
  transition: border-color 0.15s ease, box-shadow 0.15s ease;
}

.field-container:focus-within {
  border-color: var(--brand-primary);
  box-shadow: var(--shadow-focus-ring);
}

.field-icon {
  margin-left: 14px;
  color: var(--text-secondary);
  flex-shrink: 0;
}

.field-input {
  height: 100% !important;
  border: none !important;
  box-shadow: none !important;
  padding: 0 12px !important;
  font-size: 14px !important;
  color: var(--text-primary) !important;
  background: transparent !important;
}

.field-input:focus-visible {
  outline: none !important;
  box-shadow: none !important;
}

.visibility-toggle-btn {
  display: grid;
  place-items: center;
  width: 40px;
  height: 40px;
  margin-right: 4px;
  border: none;
  background: transparent;
  color: var(--text-secondary);
  cursor: pointer;
  border-radius: var(--radius-sm);
  transition: color 0.15s ease;
}

.visibility-toggle-btn:hover {
  color: var(--text-primary);
}

/* Password Strength Meter */
.password-strength-wrap {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin-top: 4px;
}

.strength-meter {
  display: flex;
  gap: 4px;
  flex: 1;
}

.strength-bar {
  height: 4px;
  flex: 1;
  border-radius: 2px;
  background-color: #E2E8F0;
  transition: background-color 0.2s ease;
}

.strength-bar.bar-weak {
  background-color: #EF4444;
}

.strength-bar.bar-medium {
  background-color: #F59E0B;
}

.strength-bar.bar-strong {
  background-color: #10B981;
}

.strength-text {
  font-size: 11.5px;
  font-weight: 500;
}

.strength-text.level-1 {
  color: #DC2626;
}

.strength-text.level-2 {
  color: #D97706;
}

.strength-text.level-3 {
  color: #059669;
}

/* Terms Row */
.terms-row {
  display: flex;
  align-items: flex-start;
  margin-top: 2px;
}

.terms-label {
  display: inline-flex;
  align-items: flex-start;
  gap: 10px;
  cursor: pointer;
  user-select: none;
}

.checkbox-indicator {
  display: grid;
  place-items: center;
  width: 18px;
  height: 18px;
  margin-top: 2px;
  border: 1.5px solid #CBD5E1;
  border-radius: 4px;
  background-color: #ffffff;
  color: #ffffff;
  flex-shrink: 0;
  transition: all 0.15s ease;
}

.checkbox-indicator.is-checked {
  border-color: var(--brand-primary);
  background-color: var(--brand-primary);
}

.terms-caption {
  font-size: 13px;
  line-height: 1.45;
  color: var(--text-secondary);
}

/* Error Banner */
.register-error-banner {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding: 12px 14px;
  border-radius: var(--radius-md);
  background-color: #FEF2F2;
  border: 1px solid #FECACA;
}

.error-banner-icon {
  color: #DC2626;
  flex-shrink: 0;
  margin-top: 1px;
}

.error-banner-text {
  margin: 0;
  font-size: 13px;
  line-height: 1.45;
  color: #991B1B;
}

/* Submit Button */
.submit-action-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  width: 100%;
  height: 48px;
  margin-top: 4px;
  border: none;
  border-radius: var(--radius-md);
  background: linear-gradient(135deg, var(--brand-primary) 0%, #1D4ED8 100%);
  color: #ffffff;
  font-size: 14.5px;
  font-weight: 700;
  letter-spacing: 0.3px;
  cursor: pointer;
  box-shadow: 0 4px 14px rgba(37, 99, 235, 0.25);
  transition: transform 0.15s ease, box-shadow 0.15s ease, opacity 0.15s ease;
}

.submit-action-btn:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 6px 18px rgba(37, 99, 235, 0.35);
}

.submit-action-btn:active:not(:disabled) {
  transform: translateY(0);
}

.submit-action-btn:disabled {
  opacity: 0.65;
  cursor: not-allowed;
  box-shadow: none;
}

/* Login Prompt */
.login-prompt {
  margin: 22px 0 0;
  font-size: 13.5px;
  color: var(--text-secondary);
  text-align: center;
}

.login-link {
  color: var(--brand-primary);
  font-weight: 650;
  text-decoration: none;
  margin-left: 4px;
  transition: color 0.15s ease;
}

.login-link:hover {
  color: var(--brand-primary-hover);
  text-decoration: underline;
}

/* Trust Indicator */
.trust-indicator {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  margin-top: 24px;
  padding-top: 18px;
  border-top: 1px solid var(--border-subtle);
  font-size: 12px;
  color: var(--text-muted);
}

/* ==========================================================================
   RESPONSIVE (TABLET & MOBILE)
   ========================================================================== */
@media (max-width: 900px) {
  .register-page {
    grid-template-columns: 1fr;
  }

  .intro-panel {
    display: none;
  }

  .form-panel {
    padding: 32px 20px;
    align-items: flex-start;
  }

  .mobile-brand-header {
    display: flex;
  }

  .register-card-wrapper {
    max-width: 100%;
  }
}
</style>
