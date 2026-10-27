<script setup>
import { ref } from 'vue'
import { RouterLink, useRouter } from 'vue-router'
import { GraduationCap, ArrowRight, Mail, LockKeyhole } from 'lucide-vue-next'
import { userService } from '@/services/userService'

const router = useRouter()
const email = ref('')
const password = ref('')
const confirmPassword = ref('')
const loading = ref(false)
const error = ref('')

function errorMessage(exception) {
  const code = exception?.response?.data?.responseCode
  if (code === 'AUTH-ERR-405') return 'Email này đã được đăng ký. Hãy đăng nhập hoặc dùng email khác.'
  if (code === 'AUTH-ERR-424') return 'Bạn vừa yêu cầu mã. Hãy chờ một chút rồi thử lại.'
  if (code === 'AUTH-ERR-425') return 'Email chưa gửi được do hệ thống chưa cấu hình dịch vụ gửi thư.'
  return exception?.response?.data?.responseMessage || 'Không thể tạo tài khoản. Vui lòng thử lại.'
}

async function submit() {
  error.value = ''
  const normalizedEmail = email.value.trim().toLowerCase()
  if (password.value.length < 8 || password.value.length > 72) {
    error.value = 'Mật khẩu cần có từ 8 đến 72 ký tự.'
    return
  }
  if (password.value !== confirmPassword.value) {
    error.value = 'Mật khẩu xác nhận chưa khớp.'
    return
  }

  loading.value = true
  try {
    const verification = await userService.signUp({ email: normalizedEmail, password: password.value })
    await router.push({
      path: '/verify-email',
      query: {
        email: verification.email || normalizedEmail,
        expiresAt: verification.expiresAt,
        resendAvailableAt: verification.resendAvailableAt,
      },
    })
  } catch (exception) {
    error.value = errorMessage(exception)
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <main class="auth-flow-page">
    <section class="auth-flow-panel" aria-labelledby="register-title">
      <RouterLink to="/login" class="auth-flow-brand"><GraduationCap :size="22" />Adaptive Learning</RouterLink>
      <p class="auth-flow-kicker">TÀI KHOẢN NGƯỜI HỌC</p>
      <h1 id="register-title">Tạo tài khoản</h1>
      <p class="auth-flow-intro">Nhập email để nhận mã xác minh và hoàn tất đăng ký.</p>
      <form class="auth-flow-form" @submit.prevent="submit">
        <label>Email
          <span class="auth-flow-input"><Mail :size="17" /><input v-model.trim="email" type="email" autocomplete="email" maxlength="254" required placeholder="ban@email.com" /></span>
        </label>
        <label>Mật khẩu
          <span class="auth-flow-input"><LockKeyhole :size="17" /><input v-model="password" type="password" autocomplete="new-password" minlength="8" maxlength="72" required placeholder="Tối thiểu 8 ký tự" /></span>
        </label>
        <label>Xác nhận mật khẩu
          <span class="auth-flow-input"><LockKeyhole :size="17" /><input v-model="confirmPassword" type="password" autocomplete="new-password" minlength="8" maxlength="72" required placeholder="Nhập lại mật khẩu" /></span>
        </label>
        <p v-if="error" class="auth-flow-error" role="alert">{{ error }}</p>
        <button class="auth-flow-submit" type="submit" :disabled="loading">
          {{ loading ? 'Đang gửi mã…' : 'Tạo tài khoản' }}<ArrowRight :size="17" />
        </button>
      </form>
      <p class="auth-flow-footnote">Đã có tài khoản? <RouterLink to="/login">Đăng nhập</RouterLink></p>
    </section>
  </main>
</template>

<style scoped>
.auth-flow-page { display: grid; min-height: 100vh; place-items: center; padding: 24px; background: var(--bg-page); color: var(--text); font-family: var(--font-sans); }
.auth-flow-panel { width: min(100%, 480px); padding: 32px; border: 1px solid var(--ds-border); border-radius: var(--radius-2xl); background: var(--surface); box-shadow: var(--shadow-card); }
.auth-flow-brand { display: inline-flex; align-items: center; gap: 9px; color: var(--text-strong); font-size: 15px; font-weight: 700; text-decoration: none; }
.auth-flow-brand svg { color: var(--ds-primary); }
.auth-flow-kicker { margin: 30px 0 7px; color: var(--text-overline); font-size: 11px; font-weight: 700; }
.auth-flow-panel h1 { margin: 0; color: var(--text); font-size: 26px; line-height: 34px; font-weight: 700; }
.auth-flow-intro { margin: 7px 0 23px; color: var(--text-muted); font-size: 13px; line-height: 1.6; }
.auth-flow-form { display: grid; gap: 16px; }
.auth-flow-form label { display: grid; gap: 6px; color: var(--text-strong); font-size: 13px; font-weight: 600; }
.auth-flow-input { display: flex; min-height: 48px; align-items: center; gap: 10px; padding: 0 12px; border: 1px solid var(--border-control); border-radius: var(--radius-md); color: var(--text-muted); background: var(--surface); }
.auth-flow-input:focus-within { border-color: var(--ds-primary); outline: 2px solid var(--primary-soft); }
.auth-flow-input input { width: 100%; min-width: 0; min-height: 44px; border: 0; outline: 0; color: var(--text); background: transparent; font: inherit; font-size: 14px; }
.auth-flow-error { margin: 0; color: var(--danger); font-size: 13px; line-height: 1.5; }
.auth-flow-submit { display: flex; min-height: 48px; align-items: center; justify-content: center; gap: 10px; border: 0; border-radius: var(--radius-md); color: white; background: var(--ds-primary); font: inherit; font-size: 14px; font-weight: 600; cursor: pointer; }
.auth-flow-submit:hover:not(:disabled) { background: var(--primary-hover); }
.auth-flow-submit:disabled { opacity: .65; cursor: wait; }
.auth-flow-footnote { margin: 22px 0 0; color: var(--text-muted); font-size: 13px; text-align: center; }
.auth-flow-footnote a { color: var(--primary-ink); font-weight: 600; text-decoration: none; }
.auth-flow-footnote a:hover { text-decoration: underline; }
@media (max-width: 480px) { .auth-flow-page { padding: 16px; }.auth-flow-panel { padding: 24px 20px; }.auth-flow-kicker { margin-top: 24px; } }
</style>
