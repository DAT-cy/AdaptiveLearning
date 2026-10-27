<script setup>
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import { GraduationCap, ArrowRight, MailCheck } from 'lucide-vue-next'
import { userService } from '@/services/userService'

const route = useRoute()
const router = useRouter()
const email = ref(String(route.query.email || ''))
const code = ref('')
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

function errorMessage(exception) {
  const responseCode = exception?.response?.data?.responseCode
  if (responseCode === 'AUTH-ERR-421') return 'Mã chưa chính xác. Hãy kiểm tra email và thử lại.'
  if (responseCode === 'AUTH-ERR-422') return 'Mã đã hết hạn. Hãy gửi mã mới.'
  if (responseCode === 'AUTH-ERR-424') return 'Đã đạt giới hạn thử hoặc gửi mã. Vui lòng chờ rồi thử lại.'
  if (responseCode === 'AUTH-ERR-425') return 'Dịch vụ email chưa được cấu hình. Vui lòng thử lại sau.'
  return exception?.response?.data?.responseMessage || 'Không thể xác minh email. Vui lòng thử lại.'
}

async function submitCode() {
  error.value = ''
  notice.value = ''
  loading.value = true
  try {
    await userService.verifyEmail({ email: email.value, code: code.value })
    verified.value = true
  } catch (exception) {
    error.value = errorMessage(exception)
  } finally {
    loading.value = false
  }
}

async function resendCode() {
  if (!canResend.value) return
  error.value = ''
  notice.value = ''
  resending.value = true
  try {
    const result = await userService.resendVerification({ email: email.value })
    expiresAt.value = new Date(result.expiresAt).getTime()
    resendAvailableAt.value = new Date(result.resendAvailableAt).getTime()
    code.value = ''
    notice.value = 'Nếu địa chỉ này đang chờ xác minh, mã mới đã được gửi.'
  } catch (exception) {
    error.value = errorMessage(exception)
  } finally {
    resending.value = false
  }
}

onMounted(() => {
  if (!email.value) {
    router.replace('/register')
    return
  }
  clock = window.setInterval(() => { now.value = Date.now() }, 1000)
})
onUnmounted(() => window.clearInterval(clock))
</script>

<template>
  <main class="auth-flow-page">
    <section class="auth-flow-panel verify-panel" aria-labelledby="verify-title">
      <RouterLink to="/login" class="auth-flow-brand"><GraduationCap :size="22" />Adaptive Learning</RouterLink>
      <template v-if="!verified">
        <p class="auth-flow-kicker">XÁC MINH TÀI KHOẢN</p>
        <h1 id="verify-title">Nhập mã xác minh</h1>
        <p class="auth-flow-intro">Mã 6 chữ số đã được gửi đến <strong>{{ email }}</strong>.</p>
        <form class="auth-flow-form" @submit.prevent="submitCode">
          <label for="verification-code">Mã xác minh</label>
          <input id="verification-code" v-model="code" class="verification-code-input" type="text" inputmode="numeric" autocomplete="one-time-code" pattern="[0-9]{6}" maxlength="6" minlength="6" required aria-describedby="code-timer" />
          <p id="code-timer" class="code-timer" :class="{ expired: codeExpired }">{{ codeExpired ? 'Mã đã hết hạn.' : hasExpiry ? `Mã còn hiệu lực ${formatCountdown(expiresIn)}` : 'Nếu chưa nhận được mã, hãy yêu cầu gửi lại.' }}</p>
          <p v-if="error" class="auth-flow-error" role="alert">{{ error }}</p>
          <p v-if="notice" class="auth-flow-notice" role="status">{{ notice }}</p>
          <button class="auth-flow-submit" type="submit" :disabled="loading || code.length !== 6 || codeExpired">
            {{ loading ? 'Đang xác minh…' : 'Xác minh email' }}<ArrowRight :size="17" />
          </button>
        </form>
        <div class="verify-actions">
          <button type="button" class="resend-code" :disabled="!canResend" @click="resendCode">
            {{ resending ? 'Đang gửi…' : resendIn ? `Gửi lại mã sau ${formatCountdown(resendIn)}` : 'Gửi lại mã' }}
          </button>
          <RouterLink to="/register">Đổi email</RouterLink>
        </div>
      </template>
      <div v-else class="verified-state" role="status">
        <MailCheck :size="30" />
        <p class="auth-flow-kicker">ĐÃ XÁC MINH</p>
        <h1 id="verify-title">Email đã xác minh</h1>
        <p class="auth-flow-intro">Tài khoản của bạn đã được kích hoạt. Bạn có thể đăng nhập ngay.</p>
        <RouterLink class="auth-flow-submit verified-link" to="/login">Đăng nhập<ArrowRight :size="17" /></RouterLink>
      </div>
    </section>
  </main>
</template>

<style scoped>
.auth-flow-page { display: grid; min-height: 100vh; place-items: center; padding: 24px; background: var(--bg-page); color: var(--text); font-family: var(--font-sans); }
.auth-flow-panel { width: min(100%, 480px); padding: 32px; border: 1px solid var(--ds-border); border-radius: var(--radius-2xl); background: var(--surface); box-shadow: var(--shadow-card); }
.auth-flow-brand { display: inline-flex; align-items: center; gap: 9px; color: var(--text-strong); font-size: 15px; font-weight: 700; text-decoration: none; }
.auth-flow-brand svg,.verified-state > svg { color: var(--ds-primary); }
.auth-flow-kicker { margin: 30px 0 7px; color: var(--text-overline); font-size: 11px; font-weight: 700; }
.auth-flow-panel h1 { margin: 0; color: var(--text); font-size: 26px; line-height: 34px; font-weight: 700; }
.auth-flow-intro { margin: 7px 0 23px; color: var(--text-muted); font-size: 13px; line-height: 1.6; overflow-wrap: anywhere; }
.auth-flow-intro strong { color: var(--text-strong); font-weight: 600; }
.auth-flow-form { display: grid; gap: 10px; color: var(--text-strong); font-size: 13px; font-weight: 600; }
.verification-code-input { width: 100%; min-height: 56px; padding: 0 14px; border: 1px solid var(--border-control); border-radius: var(--radius-md); color: var(--text); background: var(--surface); font: inherit; font-size: 23px; font-weight: 600; letter-spacing: 6px; text-align: center; font-variant-numeric: tabular-nums; }
.verification-code-input:focus-visible { border-color: var(--ds-primary); outline: 2px solid var(--ds-primary); outline-offset: 2px; }
.code-timer { margin: 0 0 4px; color: var(--text-muted); font-size: 12px; font-weight: 400; }
.code-timer.expired,.auth-flow-error { color: var(--danger); }
.auth-flow-error,.auth-flow-notice { margin: 0; font-size: 13px; line-height: 1.5; }
.auth-flow-notice { color: var(--primary-ink); font-weight: 400; }
.auth-flow-submit { display: flex; min-height: 48px; align-items: center; justify-content: center; gap: 10px; margin-top: 5px; border: 0; border-radius: var(--radius-md); color: white; background: var(--ds-primary); font: inherit; font-size: 14px; font-weight: 600; text-decoration: none; cursor: pointer; }
.auth-flow-submit:hover:not(:disabled) { background: var(--primary-hover); }
.auth-flow-submit:disabled { opacity: .55; cursor: not-allowed; }
.verify-actions { display: flex; align-items: center; justify-content: space-between; gap: 12px; margin-top: 20px; }
.verify-actions button,.verify-actions a { min-height: 44px; border: 0; color: var(--primary-ink); background: transparent; font: inherit; font-size: 12px; font-weight: 600; text-decoration: none; cursor: pointer; }
.verify-actions button:disabled { color: var(--text-muted); cursor: not-allowed; }
.verify-actions a:hover { text-decoration: underline; }
.verified-state { display: grid; justify-items: start; margin-top: 34px; }
.verified-state .auth-flow-kicker { margin-top: 18px; }
.verified-link { width: 100%; margin-top: 8px; }
@media (max-width: 480px) { .auth-flow-page { padding: 16px; }.auth-flow-panel { padding: 24px 20px; }.auth-flow-kicker { margin-top: 24px; } }
</style>
