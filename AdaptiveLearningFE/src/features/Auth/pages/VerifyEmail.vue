<script setup>
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import {
  ArrowRight,
  BarChart3,
  BookOpen,
  Brain,
  Check,
  GraduationCap,
  LockKeyhole,
  MailCheck,
  Target,
} from 'lucide-vue-next'
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
  clock = window.setInterval(() => {
    now.value = Date.now()
  }, 1000)
})
onUnmounted(() => window.clearInterval(clock))
</script>

<template>
  <main class="login-page">
    <!-- CỘT GIỚI THIỆU ĐỒNG BỘ -->
    <section class="intro-panel" aria-label="Giới thiệu Adaptive Learning">
      <RouterLink class="brand-lockup" to="/login" aria-label="Adaptive Learning">
        <span class="brand-icon"><GraduationCap :size="31" :stroke-width="2.1" /></span>
        <span>Adaptive Learning</span>
      </RouterLink>

      <div class="intro-copy">
        <p class="intro-kicker">BẢO MẬT TÀI KHOẢN</p>
        <h1>Xác minh email<br class="desktop-break"> để kích hoạt tài khoản</h1>
        <p class="intro-description">
          Chỉ một bước đơn giản để đảm bảo an toàn thông tin và kích hoạt đầy đủ lộ trình học tập được cá nhân hóa bởi AI.
        </p>
      </div>

      <div class="benefit-list">
        <div class="benefit"><span class="benefit-icon target-icon"><Target :size="21" /></span><span>Cá nhân hóa<br>lộ trình học</span></div>
        <div class="benefit"><span class="benefit-icon progress-icon"><BarChart3 :size="21" /></span><span>Theo dõi tiến độ<br>thực tế</span></div>
        <div class="benefit"><span class="benefit-icon ai-icon"><Brain :size="21" /></span><span>Ứng dụng AI<br>thông minh</span></div>
      </div>

      <div class="study-visual" aria-label="Minh họa lộ trình học tập">
        <div class="visual-orbit orbit-one"></div><div class="visual-orbit orbit-two"></div>
        <div class="visual-note"><span class="note-spark">✦</span><span>Bảo mật tài khoản<br><strong>An toàn & Tin cậy</strong></span></div>
        <div class="visual-path"><span><BookOpen :size="19" /></span><i></i><b><Check :size="13" :stroke-width="3" /></b><i></i><strong><BarChart3 :size="22" /></strong></div>
        <div class="laptop-scene">
          <div class="laptop-screen">
            <div class="screen-top"><span class="mini-logo"><GraduationCap :size="13" /> AdaptiveLearn</span><span class="screen-avatar">A</span></div>
            <div class="screen-welcome">Xác thực tài khoản <span>Bước 2/2</span></div>
            <div class="screen-progress"><span><i></i></span><small>Kích hoạt <b>95%</b></small></div>
            <div class="screen-courses">
              <div><span class="course-check"><Check :size="8" :stroke-width="3" /></span><span><b>Tạo thông tin</b><small>Hoàn thành</small></span><em>100%</em></div>
              <div><span class="course-dot"></span><span><b>Xác minh hộp thư</b><small>Đang chờ</small></span><em>Mã OTP</em></div>
            </div>
          </div>
          <div class="laptop-base"></div>
        </div>
        <div class="visual-book"></div>
        <div class="visual-plant"><i></i><i></i><i></i><b></b></div>
      </div>
    </section>

    <!-- CỘT FORM XÁC THỰC -->
    <section class="form-panel" aria-labelledby="verify-title">
      <div class="login-card">
        <span class="card-brand-icon"><GraduationCap :size="38" :stroke-width="2" /></span>
        <template v-if="!verified">
          <h2 id="verify-title">Xác thực email</h2>
          <p class="card-subtitle">
            Mã gồm 6 chữ số đã được gửi tới<br>
            <strong class="email-highlight">{{ email }}</strong>
          </p>

          <form class="login-form" @submit.prevent="submitCode">
            <div class="field-item">
              <label class="field-label" for="verification-code">Nhập mã OTP (6 số)</label>
              <input
                id="verification-code"
                v-model="code"
                class="otp-input"
                type="text"
                inputmode="numeric"
                autocomplete="one-time-code"
                pattern="[0-9]{6}"
                maxlength="6"
                minlength="6"
                placeholder="• • • • • •"
                required
                aria-describedby="code-timer"
              />
            </div>

            <p id="code-timer" class="code-timer" :class="{ expired: codeExpired }">
              {{ codeExpired ? 'Mã xác minh đã hết hạn. Vui lòng bấm gửi lại.' : hasExpiry ? `Mã còn hiệu lực trong ${formatCountdown(expiresIn)}` : 'Vui lòng kiểm tra hòm thư đến hoặc thư rác.' }}
            </p>

            <p v-if="error" class="form-error-banner" role="alert">{{ error }}</p>
            <p v-if="notice" class="form-notice-banner" role="status">{{ notice }}</p>

            <button class="submit-button" type="submit" :disabled="loading || code.length !== 6 || codeExpired">
              {{ loading ? 'Đang xác minh…' : 'Kích hoạt tài khoản' }}
              <ArrowRight :size="19" />
            </button>
          </form>

          <div class="verify-footer-actions">
            <button type="button" class="resend-btn" :disabled="!canResend" @click="resendCode">
              {{ resending ? 'Đang gửi mã…' : resendIn ? `Gửi lại mã (${formatCountdown(resendIn)})` : 'Gửi lại mã OTP' }}
            </button>
            <RouterLink to="/register" class="change-email-link">Đổi địa chỉ email khác</RouterLink>
          </div>
        </template>

        <div v-else class="verified-state">
          <div class="success-icon-wrap"><MailCheck :size="36" /></div>
          <h2>Xác minh thành công!</h2>
          <p class="card-subtitle">Email của bạn đã được xác thực thành công. Bạn có thể đăng nhập ngay bây giờ.</p>
          <RouterLink to="/login" class="submit-button">
            Đăng nhập ngay
            <ArrowRight :size="19" />
          </RouterLink>
        </div>
      </div>

      <p class="secure-note"><LockKeyhole :size="13" /> Kết nối được bảo vệ an toàn với SSL/TLS</p>
    </section>
  </main>
</template>

<style scoped>
.login-page {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(470px, 0.98fr);
  min-height: 100vh;
  overflow: hidden;
  color: #101d49;
  background: #f4f8fe;
  font-family: var(--font-sans);
}

.intro-panel {
  position: relative;
  display: flex;
  min-height: 100vh;
  flex-direction: column;
  padding: 42px max(46px, calc((100vw - 1360px) / 2));
  padding-right: 40px;
  overflow: hidden;
  background:
    radial-gradient(ellipse at 5% 88%, rgba(102, 170, 255, 0.22), transparent 36%),
    radial-gradient(ellipse at 73% 70%, rgba(189, 215, 255, 0.4), transparent 37%),
    linear-gradient(137deg, #edf6ff 2%, #f7faff 58%, #e9f2ff 100%);
}

.intro-panel::before {
  position: absolute;
  right: -18%;
  bottom: -37%;
  width: 88%;
  height: 74%;
  border-radius: 50%;
  background: linear-gradient(145deg, rgba(255, 255, 255, 0.05), rgba(165, 199, 255, 0.38));
  content: '';
  transform: rotate(-22deg);
}

.intro-panel::after {
  position: absolute;
  right: 0;
  bottom: 0;
  left: 0;
  height: 20%;
  background: linear-gradient(0deg, rgba(194, 220, 255, 0.24), transparent);
  content: '';
  pointer-events: none;
}

.brand-lockup {
  z-index: 1;
  display: inline-flex;
  width: fit-content;
  align-items: center;
  gap: 10px;
  color: #101d49;
  text-decoration: none;
  font-size: 22px;
  font-weight: 750;
}

.brand-icon,
.card-brand-icon {
  display: grid;
  place-items: center;
  color: #2864f5;
}

.brand-icon {
  width: 48px;
  height: 44px;
  filter: drop-shadow(0 5px 8px rgba(42, 100, 245, 0.15));
}

.intro-copy {
  z-index: 1;
  width: min(100%, 560px);
  margin-top: 60px;
}

.intro-kicker {
  margin: 0 0 13px;
  color: #3267cc;
  font-size: 11px;
  font-weight: 750;
  letter-spacing: 1.1px;
}

.intro-copy h1 {
  margin: 0;
  color: #101d49;
  font-size: clamp(31px, 3vw, 43px);
  line-height: 1.27;
  font-weight: 750;
}

.intro-description {
  max-width: 520px;
  margin: 17px 0 0;
  color: #5b7199;
  font-size: 16px;
  line-height: 1.7;
}

.benefit-list {
  z-index: 1;
  display: flex;
  align-items: center;
  margin-top: 31px;
}

.benefit {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 0 18px;
  color: #506b96;
  font-size: 12px;
  line-height: 1.5;
}

.benefit:first-child {
  padding-left: 0;
}

.benefit + .benefit {
  border-left: 1px solid #d3e0f2;
}

.benefit-icon {
  display: grid;
  width: 40px;
  height: 40px;
  flex: 0 0 auto;
  place-items: center;
  border-radius: 50%;
}

.target-icon {
  color: #286ef1;
  background: #dceaff;
}

.progress-icon {
  color: #19ae8a;
  background: #def7f0;
}

.ai-icon {
  color: #8357ed;
  background: #eee6ff;
}

.study-visual {
  position: relative;
  z-index: 1;
  width: min(100%, 640px);
  min-height: 350px;
  flex: 1;
  margin: 26px auto -42px;
}

.visual-orbit {
  position: absolute;
  border-radius: 50%;
  border: 1px solid rgba(160, 191, 241, 0.24);
}

.orbit-one {
  right: 1%;
  bottom: 7%;
  width: 59%;
  aspect-ratio: 1;
}

.orbit-two {
  right: 8%;
  bottom: 14%;
  width: 44%;
  aspect-ratio: 1;
  border-style: dashed;
}

.laptop-scene {
  position: absolute;
  z-index: 2;
  right: 14%;
  bottom: 3%;
  width: 56%;
}

.laptop-screen {
  position: relative;
  min-height: 172px;
  padding: 13px 14px;
  border: 7px solid #15274f;
  border-bottom-width: 10px;
  border-radius: 9px 9px 3px 3px;
  background: #fff;
  box-shadow: 0 15px 30px rgba(37, 72, 142, 0.18);
  transform: perspective(700px) rotateY(-5deg) rotateX(2deg);
}

.laptop-base {
  width: 111%;
  height: 12px;
  margin-left: -5.5%;
  border-radius: 2px 2px 14px 14px;
  background: linear-gradient(#627cae, #304b7f);
  box-shadow: 0 9px 15px rgba(35, 73, 142, 0.16);
}

.screen-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-bottom: 9px;
  border-bottom: 1px solid #edf1f7;
  color: #64789e;
  font-size: 7px;
}

.mini-logo {
  display: flex;
  align-items: center;
  gap: 3px;
  color: #2864f5;
  font-weight: 700;
}

.screen-avatar {
  display: grid;
  width: 16px;
  height: 16px;
  place-items: center;
  border-radius: 50%;
  color: white;
  background: #31b698;
  font-size: 7px;
}

.screen-welcome {
  display: flex;
  justify-content: space-between;
  margin: 11px 0 7px;
  color: #172858;
  font-size: 10px;
  font-weight: 700;
}

.screen-welcome span {
  color: #8092af;
  font-size: 7px;
  font-weight: 500;
}

.screen-progress {
  padding: 7px;
  border-radius: 5px;
  background: #f3f7ff;
}

.screen-progress > span {
  display: block;
  height: 5px;
  margin-bottom: 6px;
  overflow: hidden;
  border-radius: 4px;
  background: #dbe6f7;
}

.screen-progress i {
  display: block;
  width: 68%;
  height: 100%;
  border-radius: 4px;
  background: #31b698;
}

.screen-progress small {
  display: flex;
  justify-content: space-between;
  color: #8090aa;
  font-size: 7px;
}

.screen-progress b {
  color: #148e6e;
}

.screen-courses {
  display: grid;
  gap: 5px;
  margin-top: 7px;
}

.screen-courses > div {
  display: flex;
  align-items: center;
  gap: 6px;
}

.screen-courses > div > span:nth-child(2) {
  display: grid;
  gap: 2px;
  flex: 1;
}

.screen-courses b {
  color: #3f5274;
  font-size: 7px;
}

.screen-courses small {
  color: #91a0b8;
  font-size: 6px;
}

.screen-courses em {
  color: #7183a1;
  font-size: 7px;
  font-style: normal;
}

.course-check {
  display: grid;
  width: 12px;
  height: 12px;
  place-items: center;
  border-radius: 50%;
  color: white;
  background: #31b698;
}

.course-check svg {
  width: 8px;
  height: 8px;
}

.course-dot {
  width: 12px;
  height: 12px;
  border: 2px solid #4580f5;
  border-radius: 50%;
}

.visual-note {
  position: absolute;
  z-index: 4;
  top: 25%;
  right: 4%;
  display: flex;
  gap: 8px;
  padding: 12px 15px;
  border: 1px solid rgba(228, 236, 249, 0.9);
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.9);
  color: #61769d;
  font-size: 10px;
  line-height: 1.5;
  box-shadow: 0 12px 28px rgba(58, 95, 163, 0.1);
}

.visual-note strong {
  color: #334e82;
  font-weight: 650;
}

.note-spark {
  color: #3674f5;
  font-size: 18px;
}

.visual-path {
  position: absolute;
  z-index: 3;
  top: 39%;
  left: 47%;
  display: flex;
  align-items: center;
}

.visual-path > span,
.visual-path > b,
.visual-path > strong {
  display: grid;
  width: 38px;
  height: 38px;
  place-items: center;
  border-radius: 50%;
  color: white;
  background: #3778f5;
  box-shadow: 0 8px 18px rgba(55, 120, 245, 0.2);
}

.visual-path svg {
  width: 19px;
  height: 19px;
}

.visual-path > i {
  width: 27px;
  border-top: 2px dashed #93b7fb;
}

.visual-path > b {
  width: 24px;
  height: 24px;
  background: #2dbb98;
}

.visual-path > strong {
  width: 42px;
  height: 42px;
  background: #7962e9;
}

.visual-book {
  position: absolute;
  z-index: 3;
  right: 2%;
  bottom: 3%;
  width: 18%;
  height: 20px;
  border: 3px solid #4378d9;
  border-radius: 3px;
  background: #fff;
  box-shadow: 0 -8px 0 -2px #8dc8fb, 0 -14px 0 -5px #4384de, 0 5px 10px rgba(30, 70, 140, 0.16);
}

.visual-plant {
  position: absolute;
  left: 5%;
  bottom: 3%;
  width: 48px;
  height: 92px;
}

.visual-plant b {
  position: absolute;
  bottom: 0;
  left: 12px;
  width: 26px;
  height: 21px;
  border-radius: 3px 3px 8px 8px;
  background: #4d83d9;
}

.visual-plant i {
  position: absolute;
  bottom: 21px;
  left: 24px;
  width: 15px;
  height: 35px;
  border-radius: 100% 0 100% 0;
  background: #51b69a;
  transform: rotate(-36deg);
  transform-origin: bottom;
}

.visual-plant i:nth-child(2) {
  left: 22px;
  height: 43px;
  transform: rotate(35deg) scaleX(-1);
}

.visual-plant i:nth-child(3) {
  left: 22px;
  height: 30px;
  transform: rotate(-4deg);
}

/* FORM PANEL & CARD */
.form-panel {
  display: flex;
  align-items: center;
  justify-content: center;
  flex-direction: column;
  padding: 30px clamp(24px, 5vw, 68px);
  background: linear-gradient(145deg, #f8fbff, #eff6ff);
  overflow-y: auto;
}

.login-card {
  width: min(100%, 500px);
  padding: 34px 42px 30px;
  border: 1px solid rgba(255, 255, 255, 0.9);
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.95);
  box-shadow: 0 18px 55px rgba(47, 89, 156, 0.1);
  text-align: center;
}

.card-brand-icon {
  margin: 0 auto 10px;
  filter: drop-shadow(0 6px 10px rgba(42, 100, 245, 0.12));
}

.login-card h2 {
  margin: 0;
  color: #101d49;
  font-size: 25px;
  letter-spacing: 0;
  font-weight: 750;
}

.card-subtitle {
  margin: 8px 0 24px;
  color: #6980a4;
  font-size: 13px;
  line-height: 1.5;
}

.email-highlight {
  color: #1a4cd2;
  font-weight: 700;
  word-break: break-all;
}

.login-form {
  display: grid;
  gap: 14px;
  text-align: left;
}

.field-item {
  display: grid;
  gap: 5px;
}

.field-label {
  color: #435b80;
  font-size: 12px;
  font-weight: 600;
}

.otp-input {
  width: 100%;
  height: 54px;
  text-align: center;
  font-size: 24px;
  font-weight: 700;
  letter-spacing: 10px;
  border: 1.5px solid #d0dbe9;
  border-radius: 8px;
  color: #172858;
  background: #fff;
  transition: border-color 0.15s, box-shadow 0.15s;
}

.otp-input:focus {
  border-color: #2864f5;
  outline: 0;
  box-shadow: 0 0 0 3px rgba(40, 100, 245, 0.12);
}

.code-timer {
  margin: 2px 0 0;
  font-size: 12px;
  color: #697d9e;
  text-align: center;
}

.code-timer.expired {
  color: #e11d48;
  font-weight: 600;
}

.form-error-banner {
  margin: 0;
  padding: 8px 12px;
  border-radius: 6px;
  background: #fff0f0;
  border: 1px solid #fecaca;
  color: #dc2626;
  font-size: 12px;
  line-height: 1.5;
  text-align: center;
}

.form-notice-banner {
  margin: 0;
  padding: 8px 12px;
  border-radius: 6px;
  background: #eff6ff;
  border: 1px solid #bfdbfe;
  color: #1d4ed8;
  font-size: 12px;
  line-height: 1.5;
  text-align: center;
}

.submit-button {
  display: flex;
  height: 50px;
  align-items: center;
  justify-content: center;
  gap: 12px;
  margin-top: 4px;
  border: 0;
  border-radius: 8px;
  background: #2d68f3;
  box-shadow: 0 7px 15px rgba(45, 104, 243, 0.18);
  color: white;
  font: inherit;
  font-size: 14px;
  font-weight: 650;
  cursor: pointer;
  text-decoration: none;
  transition: background 0.15s ease, transform 0.15s ease;
}

.submit-button:hover:not(:disabled) {
  background: #2059df;
}

.submit-button:disabled {
  opacity: 0.7;
  cursor: not-allowed;
}

.verify-footer-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 20px;
  padding-top: 16px;
  border-top: 1px solid #eef2f7;
  font-size: 12.5px;
}

.resend-btn {
  border: 0;
  background: transparent;
  color: #2864f5;
  font-weight: 600;
  cursor: pointer;
  padding: 0;
}

.resend-btn:disabled {
  color: #94a3b8;
  cursor: not-allowed;
}

.change-email-link {
  color: #64748b;
  text-decoration: none;
}

.change-email-link:hover {
  text-decoration: underline;
}

.verified-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  padding: 10px 0;
}

.success-icon-wrap {
  display: grid;
  place-items: center;
  width: 68px;
  height: 68px;
  border-radius: 50%;
  background: #ecfdf5;
  color: #10b981;
  margin-bottom: 16px;
}

.secure-note {
  display: flex;
  align-items: center;
  gap: 6px;
  margin: 15px 0 0;
  color: #8a9bb4;
  font-size: 11px;
}

@media (max-width: 980px) {
  .login-page {
    grid-template-columns: minmax(0, 0.9fr) minmax(420px, 1.1fr);
  }
  .intro-panel {
    padding-left: 32px;
    padding-right: 24px;
  }
  .intro-copy {
    margin-top: 50px;
  }
  .intro-copy h1 {
    font-size: 33px;
  }
  .benefit {
    padding: 0 10px;
    gap: 7px;
    font-size: 11px;
  }
  .benefit-icon {
    width: 34px;
    height: 34px;
  }
  .form-panel {
    padding: 24px 20px;
  }
  .login-card {
    padding: 28px 24px;
  }
}

@media (max-width: 720px) {
  .login-page {
    display: block;
    width: 100%;
    min-width: 0;
    overflow: hidden;
    background: #f2f7ff;
  }
  .intro-panel {
    width: 100%;
    min-width: 0;
    min-height: 0;
    box-sizing: border-box;
    padding: 20px 23px 28px;
  }
  .intro-copy {
    width: 100%;
    min-width: 0;
    margin-top: 30px;
  }
  .study-visual {
    display: none;
  }
  .form-panel {
    width: 100%;
    min-width: 0;
    min-height: 0;
    box-sizing: border-box;
    padding: 24px 16px 28px;
    background: #f2f7ff;
  }
  .login-card {
    width: 100%;
    max-width: 450px;
    min-width: 0;
    box-sizing: border-box;
    padding: 24px 20px;
    border-radius: 12px;
  }
}
</style>
