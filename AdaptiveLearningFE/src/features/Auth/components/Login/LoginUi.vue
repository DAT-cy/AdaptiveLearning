<script setup>
import { nextTick, onMounted, ref } from 'vue'
import { useForm } from 'vee-validate'
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
  ShieldCheck,
  Sparkles,
  UserRound,
} from 'lucide-vue-next'
import { FormControl, FormField, FormItem, FormMessage } from '@/components/ui/form'
import { Input } from '@/components/ui/input'
import { Badge } from '@/components/ui/badge'
import { userService } from '@/services/userService'

const props = defineProps({
  schema: { type: null, required: true },
  errorMessage: { type: String, default: '' },
  loading: { type: Boolean, default: false },
  googleLoading: { type: Boolean, default: false },
})
const emit = defineEmits(['submit', 'google-sign-in'])
const showPassword = ref(false)
const googleButton = ref(null)
const googleClientId = ref('')
const googleStatus = ref('')
const remember = ref(localStorage.getItem('adaptive-learning-remember') === 'true')
const { handleSubmit } = useForm({
  validationSchema: props.schema,
  initialValues: { username: localStorage.getItem('adaptive-learning-username') || '', password: '' },
})

const onSubmit = handleSubmit((values) => {
  localStorage.setItem('adaptive-learning-remember', String(remember.value))
  if (remember.value) localStorage.setItem('adaptive-learning-username', values.username)
  else localStorage.removeItem('adaptive-learning-username')
  emit('submit', values)
})

function renderGoogleButton() {
  if (!window.google?.accounts?.id || !googleButton.value) return
  window.google.accounts.id.initialize({
    client_id: googleClientId.value,
    callback: ({ credential }) => {
      if (credential) emit('google-sign-in', credential)
    },
    error_callback: (err) => {
      console.warn('Google GSI error:', err)
      if (err?.type === 'popup_closed_by_user') return
      googleStatus.value = 'Chưa thể xác thực qua Google. Vui lòng kiểm tra Authorized JavaScript Origins trong Google Cloud Console.'
    },
  })
  const buttonWidth = Math.max(260, Math.min(googleButton.value.clientWidth || 360, 400))
  window.google.accounts.id.renderButton(googleButton.value, {
    type: 'standard',
    theme: 'outline',
    size: 'large',
    text: 'continue_with',
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
    googleStatus.value = 'Cần cấu hình Google OAuth Client ID để bật đăng nhập.'
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
    googleStatus.value = 'Không tải được dịch vụ đăng nhập Google.'
  }
  document.head.appendChild(script)
})
</script>

<template>
  <main class="login-page">
    <!-- CỘT TRÁI: KHÔNG GIAN GIỚI THIỆU HỌC TẬP THÍCH ỨNG AI (DESKTOP) -->
    <section class="intro-panel" aria-label="Giới thiệu Adaptive Learning">
      <div class="intro-inner">
        <!-- Brand Header -->
        <a class="brand-lockup" href="/login" aria-label="Adaptive Learning">
          <span class="brand-icon">
            <GraduationCap :size="24" :stroke-width="2.2" />
          </span>
          <span class="brand-text">Adaptive Learning</span>
        </a>

        <!-- Hero Content -->
        <div class="intro-content">
          <Badge variant="ai" class="intro-badge">
            <Sparkles :size="13" class="text-ai-accent" />
            NỀN TẢNG LUYỆN THI THÍCH ỨNG AI
          </Badge>

          <h1 class="intro-title">
            Học tập thông minh,<br class="hidden xl:inline">
            bứt phá mục tiêu cá nhân
          </h1>

          <p class="intro-desc">
            Ứng dụng mô hình AI và Bayesian Knowledge Tracing để phân tích năng lực, tự động điều chỉnh lộ trình và lấp đầy lỗ hổng kiến thức cho người học.
          </p>
        </div>

        <!-- 3 Feature Highlight Cards -->
        <div class="features-list">
          <div class="feature-item">
            <div class="feature-icon bg-violet-50 text-ai-accent border border-violet-200/60">
              <Brain :size="20" />
            </div>
            <div class="feature-text">
              <h3>Lộ trình thích ứng thông minh</h3>
              <p>Tự động cập nhật bài luyện tập theo tỷ lệ thành thạo kỹ năng.</p>
            </div>
          </div>

          <div class="feature-item">
            <div class="feature-icon bg-blue-50 text-brand-primary border border-blue-200/60">
              <BookOpen :size="20" />
            </div>
            <div class="feature-text">
              <h3>Kho đề thi chuẩn hóa</h3>
              <p>Hàng ngàn câu hỏi thi trắc nghiệm và tự luận phân loại rõ ràng.</p>
            </div>
          </div>

          <div class="feature-item">
            <div class="feature-icon bg-sky-50 text-brand-secondary border border-sky-200/60">
              <BarChart3 :size="20" />
            </div>
            <div class="feature-text">
              <h3>Đo lường tiến độ thực tế</h3>
              <p>Thống kê điểm số và xác suất đạt mục tiêu theo thời gian thực.</p>
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

    <!-- CỘT PHẢI: FORM ĐĂNG NHẬP (SINGLE-COLUMN TRÊN MOBILE) -->
    <section class="form-panel" aria-labelledby="login-heading">
      <div class="login-card-wrapper">
        <!-- Header di động -->
        <div class="mobile-brand-header">
          <span class="brand-icon">
            <GraduationCap :size="22" :stroke-width="2.2" />
          </span>
          <span class="brand-text">Adaptive Learning</span>
        </div>

        <!-- Form Heading -->
        <div class="login-header">
          <h2 id="login-heading" class="login-title">Đăng nhập tài khoản</h2>
          <p class="login-subtitle">Tiếp tục hành trình học tập cá nhân hóa của bạn</p>
        </div>

        <!-- Google OAuth Section -->
        <div class="google-login-wrap">
          <div v-if="googleClientId" ref="googleButton" class="google-button-container"></div>
          <button v-else class="google-fallback-button" type="button" disabled>
            <span class="google-mark">G</span>
            Đăng nhập với Google
          </button>
          <p v-if="googleLoading" class="google-status is-loading" role="status">
            <Loader2 class="animate-spin" :size="14" /> Đang xác thực với Google…
          </p>
          <p v-if="googleStatus" class="google-status" role="status">{{ googleStatus }}</p>
        </div>

        <!-- Divider -->
        <div class="form-divider">
          <span>hoặc với email / username</span>
        </div>

        <!-- Form Đăng Nhập -->
        <form class="login-form" @submit="onSubmit">
          <!-- Tên đăng nhập -->
          <FormField v-slot="{ componentField }" name="username">
            <FormItem class="field-item">
              <label class="field-label" for="login-username">Tên đăng nhập hoặc Email</label>
              <FormControl>
                <div class="field-container">
                  <UserRound class="field-icon" :size="18" />
                  <Input
                    id="login-username"
                    type="text"
                    maxlength="254"
                    autocomplete="username"
                    placeholder="Nhập tên đăng nhập hoặc email..."
                    aria-label="Tên đăng nhập hoặc email"
                    class="field-input"
                    v-bind="componentField"
                  />
                </div>
              </FormControl>
              <FormMessage class="field-error-msg" />
            </FormItem>
          </FormField>

          <!-- Mật khẩu -->
          <FormField v-slot="{ componentField }" name="password">
            <FormItem class="field-item">
              <label class="field-label" for="login-password">Mật khẩu</label>
              <FormControl>
                <div class="field-container">
                  <LockKeyhole class="field-icon" :size="18" />
                  <Input
                    id="login-password"
                    :type="showPassword ? 'text' : 'password'"
                    maxlength="72"
                    autocomplete="current-password"
                    placeholder="Nhập mật khẩu..."
                    aria-label="Mật khẩu"
                    class="field-input"
                    v-bind="componentField"
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
              </FormControl>
              <FormMessage class="field-error-msg" />
            </FormItem>
          </FormField>

          <!-- Checkbox Ghi nhớ đăng nhập -->
          <div class="remember-row">
            <label class="remember-label">
              <input v-model="remember" type="checkbox" class="sr-only">
              <span class="checkbox-indicator" :class="{ 'is-checked': remember }">
                <Check v-if="remember" :size="12" :stroke-width="3" />
              </span>
              <span class="remember-caption">Ghi nhớ đăng nhập</span>
            </label>
          </div>

          <!-- Thông báo lỗi đăng nhập (từ Backend API) -->
          <div v-if="errorMessage" class="login-error-banner" role="alert">
            <AlertCircle :size="18" class="error-banner-icon" />
            <p class="error-banner-text">{{ errorMessage }}</p>
          </div>

          <!-- Nút Đăng nhập -->
          <button
            class="submit-action-btn"
            type="submit"
            :disabled="loading"
          >
            <Loader2 v-if="loading" class="animate-spin" :size="18" />
            <span v-else>ĐĂNG NHẬP</span>
            <ArrowRight v-if="!loading" :size="18" />
          </button>
        </form>

        <!-- Link Chuyển sang Đăng ký -->
        <p class="register-prompt">
          Chưa có tài khoản học viên?
          <RouterLink to="/register" class="register-link">Đăng ký ngay</RouterLink>
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
.login-page {
  display: grid;
  grid-template-columns: minmax(0, 1.15fr) minmax(460px, 0.85fr);
  min-height: 100vh;
  width: 100%;
  background-color: var(--bg-app);
  color: var(--text-primary);
  font-family: var(--font-sans);
}

/* ==========================================================================
   CỘT TRÁI: INTRO PANEL
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
  max-width: 580px;
  display: flex;
  flex-direction: column;
  gap: 36px;
}

/* Brand Lockup */
.brand-lockup {
  display: inline-flex;
  align-items: center;
  gap: 12px;
  text-decoration: none;
  color: var(--text-primary);
}

.brand-icon {
  display: grid;
  place-items: center;
  width: 44px;
  height: 44px;
  border-radius: var(--radius-md);
  background-color: var(--brand-primary);
  color: #ffffff;
  box-shadow: 0 4px 14px rgba(15, 118, 110, 0.28);
}

.brand-text {
  font-size: 20px;
  font-weight: 700;
  letter-spacing: -0.3px;
  color: var(--text-primary);
}

/* Intro Content */
.intro-content {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 14px;
}

.intro-badge {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 4px 10px;
  font-size: 11px;
  letter-spacing: 0.5px;
}

.intro-title {
  margin: 0;
  font-size: clamp(28px, 3.2vw, 40px);
  font-weight: 800;
  line-height: 1.25;
  color: var(--text-primary);
  letter-spacing: -0.5px;
}

.intro-desc {
  margin: 0;
  font-size: 15px;
  line-height: 1.65;
  color: var(--text-secondary);
}

/* Features List */
.features-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.feature-item {
  display: flex;
  align-items: flex-start;
  gap: 16px;
  padding: 16px;
  border-radius: var(--radius-lg);
  background-color: rgba(255, 255, 255, 0.85);
  border: 1px solid var(--border-subtle);
  box-shadow: var(--shadow-card);
  backdrop-filter: blur(8px);
  transition: transform 0.15s ease, box-shadow 0.15s ease;
}

.feature-item:hover {
  transform: translateY(-2px);
  box-shadow: var(--shadow-card-hover);
}

.feature-icon {
  display: grid;
  place-items: center;
  width: 42px;
  height: 42px;
  flex-shrink: 0;
  border-radius: var(--radius-md);
}

.feature-text h3 {
  margin: 0 0 4px;
  font-size: 14px;
  font-weight: 600;
  color: var(--text-primary);
}

.feature-text p {
  margin: 0;
  font-size: 13px;
  line-height: 1.5;
  color: var(--text-secondary);
}

/* Stats Strip */
.stats-strip {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 24px;
  border-radius: var(--radius-lg);
  background-color: rgba(255, 255, 255, 0.65);
  border: 1px solid var(--border-subtle);
}

.stat-unit {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.stat-unit strong {
  font-size: 18px;
  font-weight: 700;
  color: var(--brand-primary);
}

.stat-unit span {
  font-size: 11px;
  color: var(--text-secondary);
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
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px clamp(24px, 5vw, 64px);
  background-color: var(--surface-card);
}

.login-card-wrapper {
  width: 100%;
  max-width: 440px;
  display: flex;
  flex-direction: column;
}

.mobile-brand-header {
  display: none;
  align-items: center;
  gap: 10px;
  margin-bottom: 24px;
}

.login-header {
  margin-bottom: 24px;
}

.login-title {
  margin: 0 0 6px;
  font-size: 26px;
  font-weight: 750;
  letter-spacing: -0.3px;
  color: var(--text-primary);
}

.login-subtitle {
  margin: 0;
  font-size: 14px;
  line-height: 1.5;
  color: var(--text-secondary);
}

/* Google Login Section */
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
  margin: 20px 0;
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

/* Login Form */
.login-form {
  display: flex;
  flex-direction: column;
  gap: 18px;
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

.field-error-msg {
  color: var(--status-danger) !important;
  font-size: 12px !important;
  margin-top: 2px !important;
}

/* Remember Option */
.remember-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.remember-label {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  user-select: none;
}

.checkbox-indicator {
  display: grid;
  place-items: center;
  width: 18px;
  height: 18px;
  border: 1.5px solid #CBD5E1;
  border-radius: 4px;
  background-color: #ffffff;
  color: #ffffff;
  transition: all 0.15s ease;
}

.checkbox-indicator.is-checked {
  border-color: var(--brand-primary);
  background-color: var(--brand-primary);
}

.remember-caption {
  font-size: 13px;
  color: var(--text-secondary);
}

/* Error Banner */
.login-error-banner {
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

/* Submit Action Button */
.submit-action-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  height: 48px;
  margin-top: 4px;
  border: none;
  border-radius: var(--radius-md);
  background-color: var(--brand-primary);
  color: #ffffff;
  font-size: 14px;
  font-weight: 650;
  letter-spacing: 0.3px;
  cursor: pointer;
  box-shadow: var(--shadow-primary-btn);
  transition: background-color 0.15s ease, transform 0.15s ease;
}

.submit-action-btn:hover {
  background-color: var(--brand-primary-hover);
  transform: translateY(-1px);
}

.submit-action-btn:active {
  transform: translateY(0);
}

.submit-action-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
  transform: none;
}

/* Register Prompt */
.register-prompt {
  margin: 24px 0 0;
  font-size: 13px;
  color: var(--text-secondary);
  text-align: center;
}

.register-link {
  color: var(--brand-primary);
  font-weight: 600;
  text-decoration: none;
  margin-left: 4px;
}

.register-link:hover {
  text-decoration: underline;
}

/* Security Trust Indicator */
.trust-indicator {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  margin-top: 20px;
  font-size: 11.5px;
  color: var(--text-secondary);
}

/* ==========================================================================
   RESPONSIVE DESIGN (MOBILE & TABLET)
   ========================================================================== */
@media (max-width: 900px) {
  .login-page {
    display: flex;
    flex-direction: column;
    min-height: 100vh;
  }

  .intro-panel {
    display: none; /* Ẩn cột minh họa phức tạp trên mobile để học viên tập trung ngay vào form đăng nhập */
  }

  .form-panel {
    flex: 1;
    padding: 32px 20px;
    background-color: var(--bg-app);
  }

  .login-card-wrapper {
    max-width: 420px;
    padding: 32px 24px;
    border-radius: var(--radius-xl);
    background-color: var(--surface-card);
    border: 1px solid var(--border-subtle);
    box-shadow: var(--shadow-card);
  }

  .mobile-brand-header {
    display: inline-flex;
  }

  .login-title {
    font-size: 22px;
  }
}

@media (max-width: 440px) {
  .form-panel {
    padding: 16px;
  }

  .login-card-wrapper {
    padding: 24px 18px;
    border-radius: var(--radius-lg);
  }

  .field-container {
    height: 44px;
  }

  .submit-action-btn {
    height: 44px;
  }
}
</style>

