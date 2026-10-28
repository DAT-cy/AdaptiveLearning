<script setup>
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import {
  AlertCircle,
  ArrowRight,
  BarChart3,
  BookOpen,
  Brain,
  CheckCircle2,
  Clock,
  GraduationCap,
  Loader2,
  Mail,
  MailCheck,
  RotateCw,
  ShieldCheck,
  Sparkles,
} from 'lucide-vue-next'
import { userService } from '@/services/userService'
import { Badge } from '@/components/ui/badge'
import { EMAIL_RESEND_COOLDOWN_MS } from '../constants'

const route = useRoute()
const router = useRouter()
const email = ref(String(route.query.email || ''))
const code = ref('')
const otpDigits = ref(['', '', '', '', '', ''])
const inputRefs = ref([])

const expiresAt = ref(Number(new Date(route.query.expiresAt || 0)) || 0)
const resendAvailableAt = ref(Number(new Date(route.query.resendAvailableAt || 0)) || 0)
const now = ref(Date.now())
const loading = ref(false)
const resending = ref(false)
const verified = ref(false)
const error = ref('')
const notice = ref('')
let clock

const expiresIn = computed(() => Math.max(0, Math.ceil((expiresAt.value - now.value) / 1000)))
const resendIn = computed(() => Math.max(0, Math.ceil((resendAvailableAt.value - now.value) / 1000)))
const codeExpired = computed(() => expiresAt.value > 0 && expiresIn.value === 0)
const canResend = computed(() => resendIn.value === 0 && !resending.value)
const hasExpiry = computed(() => expiresAt.value > 0)

function formatCountdown(seconds) {
  const minutes = String(Math.floor(seconds / 60)).padStart(2, '0')
  const remainder = String(seconds % 60).padStart(2, '0')
  return `${minutes}:${remainder}`
}

function syncCode() {
  code.value = otpDigits.value.join('')
}

watch(code, (newVal) => {
  const chars = (newVal || '').split('')
  for (let i = 0; i < 6; i++) {
    otpDigits.value[i] = chars[i] || ''
  }
})

function onInput(index, event) {
  error.value = ''
  const val = (event?.target?.value || '').replace(/\D/g, '')

  // Hỗ trợ trường hợp trình duyệt tự điền cả chuỗi OTP vào ô đầu tiên
  if (val.length > 1) {
    const digits = val.slice(0, 6).split('')
    digits.forEach((d, i) => {
      if (index + i < 6) {
        otpDigits.value[index + i] = d
      }
    })
    syncCode()
    const nextIdx = Math.min(index + digits.length, 5)
    inputRefs.value[nextIdx]?.focus()
    return
  }

  otpDigits.value[index] = val
  syncCode()

  if (val && index < 5) {
    inputRefs.value[index + 1]?.focus()
  }
}

function onKeydown(index, event) {
  if (event.key === 'Backspace') {
    if (!otpDigits.value[index] && index > 0) {
      event.preventDefault()
      otpDigits.value[index - 1] = ''
      syncCode()
      inputRefs.value[index - 1]?.focus()
    } else {
      otpDigits.value[index] = ''
      syncCode()
    }
  } else if (event.key === 'ArrowLeft' && index > 0) {
    event.preventDefault()
    inputRefs.value[index - 1]?.focus()
  } else if (event.key === 'ArrowRight' && index < 5) {
    event.preventDefault()
    inputRefs.value[index + 1]?.focus()
  }
}

function onPaste(event) {
  event.preventDefault()
  error.value = ''
  const pasted = (event.clipboardData || window.clipboardData)?.getData('text') || ''
  const digits = pasted.replace(/\D/g, '').slice(0, 6)
  if (!digits) return

  for (let i = 0; i < 6; i++) {
    otpDigits.value[i] = digits[i] || ''
  }
  syncCode()
  const targetIndex = Math.min(digits.length, 5)
  inputRefs.value[targetIndex]?.focus()
}

function errorMessage(exception) {
  const payload = exception?.response?.data
  const validationMessage =
    payload && typeof payload === 'object'
      ? Object.values(payload).find((value) => typeof value === 'string')
      : null
  const responseCode = payload?.responseCode
  if (responseCode === 'AUTH-ERR-421') return 'Mã xác minh chưa chính xác. Vui lòng kiểm tra lại email.'
  if (responseCode === 'AUTH-ERR-422') return 'Mã xác minh đã hết hạn. Vui lòng bấm gửi lại mã mới.'
  if (responseCode === 'AUTH-ERR-424') return 'Đã đạt giới hạn thử hoặc gửi mã. Vui lòng chờ vài phút rồi thử lại.'
  if (responseCode === 'AUTH-ERR-425') return 'Dịch vụ email chưa được cấu hình. Vui lòng thử lại sau.'
  const status = exception?.response?.status
  if (status === 400) return payload?.responseMessage || 'Mã OTP hoặc thông tin xác minh không hợp lệ.'
  if (status === 401) return 'Yêu cầu không được xác thực. Vui lòng tải lại trang rồi thử lại.'
  if (status === 409) return 'Tài khoản chưa ở trạng thái có thể xác minh.'
  if (status === 412 || status === 429) return 'Bạn đã yêu cầu hoặc nhập sai quá nhiều lần. Vui lòng chờ rồi thử lại.'
  if (status >= 500) return 'Dịch vụ xác minh đang gặp sự cố. Vui lòng thử lại sau.'
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
    'Không thể xác minh email. Vui lòng thử lại.'
  )
}

async function submitCode() {
  if (code.value.length !== 6 || loading.value) return
  error.value = ''
  notice.value = ''
  loading.value = true
  try {
    const result = await userService.verifyEmail({ email: email.value, code: code.value })
    if (result?.verified !== true) {
      throw new Error('Máy chủ chưa xác nhận email đã được xác minh.')
    }
    verified.value = true
    code.value = ''
    otpDigits.value = ['', '', '', '', '', '']
  } catch (exception) {
    error.value = errorMessage(exception)
  } finally {
    loading.value = false
  }
}

async function resendCode() {
  if (!canResend.value || verified.value) return
  error.value = ''
  notice.value = ''
  resending.value = true
  try {
    const result = await userService.resendVerification({ email: email.value })
    if (
      result?.verified !== false ||
      !Number.isFinite(Date.parse(result?.expiresAt)) ||
      !Number.isFinite(Date.parse(result?.resendAvailableAt))
    ) {
      throw new Error('Máy chủ chưa xác nhận yêu cầu gửi lại mã.')
    }
    expiresAt.value = new Date(result.expiresAt).getTime()
    resendAvailableAt.value = new Date(result.resendAvailableAt).getTime()
    code.value = ''
    otpDigits.value = ['', '', '', '', '', '']
    inputRefs.value[0]?.focus()
    notice.value = 'Nếu email cần xác minh, mã mới sẽ được gửi tới hộp thư của bạn.'
  } catch (exception) {
    error.value = errorMessage(exception)
    if (exception?.response?.data?.responseCode === 'AUTH-ERR-424'
      || exception?.response?.status === 412
      || exception?.response?.status === 429) {
      resendAvailableAt.value = Date.now() + EMAIL_RESEND_COOLDOWN_MS
      now.value = Date.now()
    }
  } finally {
    resending.value = false
  }
}

onMounted(() => {
  if (!email.value) {
    router.replace('/register')
    return
  }
  clock = window.setInterval(() => {
    now.value = Date.now()
  }, 1000)
  inputRefs.value[0]?.focus()
})

onUnmounted(() => {
  if (clock) window.clearInterval(clock)
})
</script>

<template>
  <main class="verify-page">
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
            BẢO MẬT TÀI KHOẢN HỌC VIÊN
          </Badge>

          <h1 class="intro-title">
            Xác minh email<br class="hidden xl:inline" />
            kích hoạt tài khoản
          </h1>

          <p class="intro-desc">
            Chỉ một bước đơn giản để đảm bảo an toàn thông tin và kích hoạt đầy đủ lộ trình học tập được cá nhân hóa bởi trí tuệ nhân tạo.
          </p>
        </div>

        <!-- 3 Feature Highlight Cards -->
        <div class="features-list">
          <div class="feature-item">
            <div class="feature-icon bg-violet-50 text-ai-accent border border-violet-200/60">
              <Brain :size="20" />
            </div>
            <div class="feature-text">
              <h3>Kích hoạt lộ trình AI</h3>
              <p>Hệ thống tự động khởi tạo ma trận kỹ năng ban đầu cho tài khoản của bạn.</p>
            </div>
          </div>

          <div class="feature-item">
            <div class="feature-icon bg-blue-50 text-brand-primary border border-blue-200/60">
              <BookOpen :size="20" />
            </div>
            <div class="feature-text">
              <h3>Mở khóa kho đề thi</h3>
              <p>Truy cập toàn bộ câu hỏi trắc nghiệm, tự luận và các đề thi thử chuẩn hóa.</p>
            </div>
          </div>

          <div class="feature-item">
            <div class="feature-icon bg-sky-50 text-brand-secondary border border-sky-200/60">
              <BarChart3 :size="20" />
            </div>
            <div class="feature-text">
              <h3>Bảo mật và an toàn</h3>
              <p>Bảo vệ lịch sử làm bài, chứng nhận điểm số và thông tin cá nhân tuyệt đối.</p>
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

    <!-- CỘT PHẢI: FORM NHẬP MÃ OTP HOẶC TRẠNG THÁI THÀNH CÔNG -->
    <section class="form-panel" aria-labelledby="verify-heading">
      <div class="verify-card-wrapper">
        <!-- Header di động -->
        <div class="mobile-brand-header">
          <span class="brand-icon">
            <GraduationCap :size="22" :stroke-width="2.2" />
          </span>
          <span class="brand-text">Adaptive Learning</span>
        </div>

        <!-- TRẠNG THÁI CHƯA XÁC MINH -->
        <template v-if="!verified">
          <div class="verify-header">
            <div class="header-icon-wrap">
              <Mail :size="26" class="text-brand-primary" />
            </div>
            <h2 id="verify-heading" class="verify-title">Xác thực địa chỉ email</h2>
            <p class="verify-subtitle">
              Mã bảo mật gồm 6 chữ số đã được gửi tới hộp thư:
            </p>
            <div class="email-chip">
              <strong>{{ email }}</strong>
            </div>
          </div>

          <!-- Form OTP -->
          <form class="verify-form" @submit.prevent="submitCode">
            <!-- Khu vực 6 ô nhập OTP -->
            <div class="otp-section">
              <label class="otp-label" for="otp-digit-0">Nhập mã OTP gồm 6 chữ số</label>
              <div class="otp-boxes-grid" @paste="onPaste">
                <input
                  v-for="(_, index) in 6"
                  :id="`otp-digit-${index}`"
                  :key="index"
                  :ref="(el) => (inputRefs[index] = el)"
                  v-model="otpDigits[index]"
                  type="text"
                  inputmode="numeric"
                  pattern="[0-9]*"
                  maxlength="1"
                  :autocomplete="index === 0 ? 'one-time-code' : 'off'"
                  class="otp-box"
                  :class="{
                    'is-filled': otpDigits[index] !== '',
                    'is-error': !!error,
                    'is-expired': codeExpired,
                  }"
                  :disabled="loading"
                  :aria-label="`Ký tự OTP số ${index + 1}`"
                  @input="onInput(index, $event)"
                  @keydown="onKeydown(index, $event)"
                  @focus="$event.target.select()"
                />
              </div>
            </div>

            <!-- Đồng hồ đếm ngược / Trạng thái hết hạn -->
            <div
              class="timer-badge"
              :class="{
                'is-expired': codeExpired,
                'is-warning': expiresIn > 0 && expiresIn <= 60,
              }"
            >
              <Clock :size="15" />
              <span v-if="codeExpired">
                Mã xác minh đã hết hạn. Vui lòng bấm gửi lại mã mới bên dưới.
              </span>
              <span v-else-if="hasExpiry">
                Mã còn hiệu lực trong <strong>{{ formatCountdown(expiresIn) }}</strong>
              </span>
              <span v-else>
                Vui lòng kiểm tra hòm thư đến hoặc mục thư rác (spam).
              </span>
            </div>

            <!-- Banner Lỗi -->
            <div v-if="error" class="verify-error-banner" role="alert">
              <AlertCircle :size="18" class="error-banner-icon" />
              <p class="error-banner-text">{{ error }}</p>
            </div>

            <!-- Banner Thông báo (gửi lại mã) -->
            <div v-if="notice" class="verify-notice-banner" role="status">
              <CheckCircle2 :size="18" class="notice-banner-icon" />
              <p class="notice-banner-text">{{ notice }}</p>
            </div>

            <!-- Nút Kích hoạt -->
            <button
              class="submit-action-btn"
              type="submit"
              :disabled="loading || code.length !== 6"
            >
              <Loader2 v-if="loading" class="animate-spin" :size="18" />
              <span v-else>KÍCH HOẠT TÀI KHOẢN</span>
              <ArrowRight v-if="!loading" :size="18" />
            </button>
          </form>

          <!-- Tác vụ phụ: Gửi lại mã & Đổi email -->
          <div class="verify-footer-actions">
            <button
              type="button"
              class="resend-action-btn"
              :disabled="!canResend"
              @click="resendCode"
            >
              <Loader2 v-if="resending" class="animate-spin" :size="15" />
              <RotateCw v-else :size="15" />
              <span>
                {{
                  resending
                    ? 'Đang gửi mã mới…'
                    : resendIn
                    ? `Gửi lại mã (${formatCountdown(resendIn)})`
                    : 'Gửi lại mã OTP'
                }}
              </span>
            </button>

            <RouterLink to="/register" class="change-email-link">
              Đổi địa chỉ email khác
            </RouterLink>
          </div>
        </template>

        <!-- TRẠNG THÁI XÁC MINH THÀNH CÔNG -->
        <div v-else class="verified-state" role="status">
          <div class="success-icon-wrap">
            <MailCheck :size="40" class="text-brand-primary" />
          </div>

          <h2 class="verified-title">Xác minh email thành công!</h2>
          <p class="verified-subtitle">
            Tài khoản của bạn đã được kích hoạt hoàn tất. Lộ trình học tập thích ứng cùng gia sư AI đã sẵn sàng chào đón bạn.
          </p>

          <RouterLink to="/login" class="submit-action-btn success-action-link">
            <span>ĐĂNG NHẬP NGAY</span>
            <ArrowRight :size="18" />
          </RouterLink>
        </div>

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
.verify-page {
  display: grid;
  grid-template-columns: minmax(0, 1.15fr) minmax(460px, 0.85fr);
  min-height: 100vh;
  width: 100%;
  background-color: var(--bg-app);
  color: var(--text-primary);
  font-family: var(--font-sans);
}

/* ==========================================================================
   CỘT TRÁI: INTRO PANEL (ĐỒNG BỘ VỚI LOGIN & REGISTER)
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

.verify-card-wrapper {
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

/* Verify Header */
.verify-header {
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  margin-bottom: 24px;
}

.header-icon-wrap {
  display: grid;
  place-items: center;
  width: 52px;
  height: 52px;
  border-radius: 50%;
  background-color: #F0FDFA;
  border: 1px solid #CCFBF1;
  margin-bottom: 14px;
}

.verify-title {
  margin: 0 0 6px;
  font-size: 24px;
  font-weight: 750;
  letter-spacing: -0.3px;
  color: var(--text-primary);
}

.verify-subtitle {
  margin: 0 0 8px;
  font-size: 14px;
  line-height: 1.5;
  color: var(--text-secondary);
}

.email-chip {
  display: inline-block;
  padding: 4px 14px;
  border-radius: 9999px;
  background-color: #F1F5F9;
  border: 1px solid var(--border-subtle);
  color: var(--brand-primary);
  font-size: 13.5px;
  word-break: break-all;
}

/* Verify Form */
.verify-form {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.otp-section {
  display: flex;
  flex-direction: column;
  gap: 8px;
  align-items: center;
}

.otp-label {
  font-size: 13px;
  font-weight: 600;
  color: var(--text-secondary);
}

/* 6 Ô Nhập OTP */
.otp-boxes-grid {
  display: flex;
  gap: 10px;
  justify-content: center;
  width: 100%;
}

.otp-box {
  width: clamp(42px, 11vw, 54px);
  height: clamp(48px, 12vw, 60px);
  text-align: center;
  font-size: 22px;
  font-weight: 700;
  color: var(--text-primary);
  background-color: #ffffff;
  border: 1.5px solid var(--border-subtle);
  border-radius: var(--radius-md);
  transition: all 0.15s ease;
  caret-color: var(--brand-primary);
}

.otp-box:focus {
  outline: none;
  border-color: var(--brand-primary);
  box-shadow: var(--shadow-focus-ring);
  background-color: #ffffff;
}

.otp-box.is-filled {
  border-color: var(--brand-primary);
  background-color: #F0FDFA;
}

.otp-box.is-error {
  border-color: #EF4444;
  background-color: #FEF2F2;
  color: #DC2626;
}

.otp-box.is-expired {
  border-color: #E2E8F0;
  background-color: #F8FAFC;
  opacity: 0.7;
  cursor: not-allowed;
}

/* Timer Badge */
.timer-badge {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 8px 12px;
  border-radius: var(--radius-md);
  background-color: #F8FAFC;
  border: 1px solid var(--border-subtle);
  font-size: 13px;
  color: var(--text-secondary);
  text-align: center;
}

.timer-badge strong {
  color: var(--brand-primary);
  font-weight: 700;
}

.timer-badge.is-warning {
  background-color: #FFFBEB;
  border-color: #FDE68A;
  color: #B45309;
}

.timer-badge.is-warning strong {
  color: #B45309;
}

.timer-badge.is-expired {
  background-color: #FEF2F2;
  border-color: #FECACA;
  color: #DC2626;
  font-weight: 500;
}

/* Banners */
.verify-error-banner {
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

.verify-notice-banner {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding: 12px 14px;
  border-radius: var(--radius-md);
  background-color: #F0FDF4;
  border: 1px solid #BBF7D0;
}

.notice-banner-icon {
  color: #16A34A;
  flex-shrink: 0;
  margin-top: 1px;
}

.notice-banner-text {
  margin: 0;
  font-size: 13px;
  line-height: 1.45;
  color: #166534;
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
  text-decoration: none;
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

/* Footer Actions */
.verify-footer-actions {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  margin-top: 22px;
}

.resend-action-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  background: transparent;
  border: none;
  color: var(--brand-primary);
  font-size: 13.5px;
  font-weight: 600;
  cursor: pointer;
  transition: color 0.15s ease;
  padding: 4px 8px;
  border-radius: var(--radius-sm);
}

.resend-action-btn:hover:not(:disabled) {
  color: var(--brand-primary-hover);
  text-decoration: underline;
}

.resend-action-btn:disabled {
  color: var(--text-muted);
  cursor: not-allowed;
  text-decoration: none;
}

.change-email-link {
  font-size: 13px;
  color: var(--text-secondary);
  text-decoration: none;
  transition: color 0.15s ease;
}

.change-email-link:hover {
  color: var(--text-primary);
  text-decoration: underline;
}

/* Verified State */
.verified-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  padding: 16px 0;
}

.success-icon-wrap {
  display: grid;
  place-items: center;
  width: 76px;
  height: 76px;
  border-radius: 50%;
  background: linear-gradient(135deg, #F0FDF4 0%, #DCFCE7 100%);
  border: 2px solid #BBF7D0;
  margin-bottom: 20px;
  box-shadow: 0 8px 24px rgba(22, 163, 74, 0.15);
}

.verified-title {
  margin: 0 0 10px;
  font-size: 24px;
  font-weight: 750;
  letter-spacing: -0.3px;
  color: var(--text-primary);
}

.verified-subtitle {
  margin: 0 0 24px;
  font-size: 14.5px;
  line-height: 1.6;
  color: var(--text-secondary);
  max-width: 360px;
}

.success-action-link {
  margin-top: 8px;
}

/* Trust Indicator */
.trust-indicator {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  margin-top: 26px;
  padding-top: 18px;
  border-top: 1px solid var(--border-subtle);
  font-size: 12px;
  color: var(--text-muted);
}

/* ==========================================================================
   RESPONSIVE (TABLET & MOBILE)
   ========================================================================== */
@media (max-width: 900px) {
  .verify-page {
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

  .verify-card-wrapper {
    max-width: 100%;
  }

  .otp-boxes-grid {
    gap: 6px;
  }

  .otp-box {
    width: 44px;
    height: 52px;
    font-size: 20px;
  }
}
</style>
