<script setup>
import { nextTick, onMounted, ref } from 'vue'
import { useForm } from 'vee-validate'
import { ArrowRight, BarChart3, BookOpen, Brain, Check, Eye, EyeOff, GraduationCap, LockKeyhole, Target, UserRound } from 'lucide-vue-next'
import { FormControl, FormField, FormItem, FormMessage } from '@/components/ui/form'
import { Input } from '@/components/ui/input'
import { userService } from '@/services/userService'

const props = defineProps({ schema: { type: null, required: true } })
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
  })
  window.google.accounts.id.renderButton(googleButton.value, {
    type: 'standard',
    theme: 'outline',
    size: 'large',
    text: 'continue_with',
    shape: 'rectangular',
    width: Math.min(googleButton.value.clientWidth, 400),
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
    <section class="intro-panel" aria-label="Giới thiệu Adaptive Learning">
      <a class="brand-lockup" href="/login" aria-label="Adaptive Learning">
        <span class="brand-icon"><GraduationCap :size="31" :stroke-width="2.1" /></span>
        <span>Adaptive Learning</span>
      </a>

      <div class="intro-copy">
        <p class="intro-kicker">HỌC TẬP THEO CÁCH CỦA BẠN</p>
        <h1>Học thông minh hơn<br class="desktop-break"> với lộ trình cá nhân hóa</h1>
        <p class="intro-description">Adaptive Learning sử dụng AI để phân tích năng lực, điểm mạnh và điểm cần cải thiện, từ đó xây dựng lộ trình học phù hợp để bạn tiến bộ mỗi ngày.</p>
      </div>

      <div class="benefit-list">
        <div class="benefit"><span class="benefit-icon target-icon"><Target :size="21" /></span><span>Cá nhân hóa<br>lộ trình học</span></div>
        <div class="benefit"><span class="benefit-icon progress-icon"><BarChart3 :size="21" /></span><span>Theo dõi tiến độ<br>thực tế</span></div>
        <div class="benefit"><span class="benefit-icon ai-icon"><Brain :size="21" /></span><span>Ứng dụng AI<br>thông minh</span></div>
      </div>

      <div class="study-visual" aria-label="Minh họa lộ trình học tập">
        <div class="visual-orbit orbit-one"></div><div class="visual-orbit orbit-two"></div>
        <div class="visual-note"><span class="note-spark">✦</span><span>Học đúng lộ trình<br><strong>Hiệu quả hơn</strong></span></div>
        <div class="visual-path"><span><BookOpen :size="19" /></span><i></i><b><Check :size="13" :stroke-width="3" /></b><i></i><strong><BarChart3 :size="22" /></strong></div>
        <div class="laptop-scene">
          <div class="laptop-screen">
            <div class="screen-top"><span class="mini-logo"><GraduationCap :size="13" /> AdaptiveLearn</span><span class="screen-avatar">A</span></div>
            <div class="screen-welcome">Lộ trình của bạn <span>Tuần 4</span></div>
            <div class="screen-progress"><span><i></i></span><small>Tiến độ học tập <b>68%</b></small></div>
            <div class="screen-courses"><div><span class="course-check"><Check :size="8" :stroke-width="3" /></span><span><b>Ngữ pháp cơ bản</b><small>Hoàn thành</small></span><em>100%</em></div><div><span class="course-dot"></span><span><b>Từ vựng học thuật</b><small>Đang học</small></span><em>64%</em></div></div>
          </div>
          <div class="laptop-base"></div>
        </div>
        <div class="visual-book"></div>
        <div class="visual-plant"><i></i><i></i><i></i><b></b></div>
      </div>
    </section>

    <section class="form-panel" aria-labelledby="login-title">
      <div class="login-card">
        <span class="card-brand-icon"><GraduationCap :size="38" :stroke-width="2" /></span>
        <h2 id="login-title">Adaptive Learning</h2>
        <p class="card-subtitle">Đăng nhập để tiếp tục hành trình học tập của bạn</p>

        <form class="login-form" @submit="onSubmit">
          <FormField v-slot="{ componentField }" name="username">
            <FormItem>
              <FormControl>
                <div class="field-wrap"><UserRound class="field-icon" :size="19" /><Input type="text" autocomplete="username" placeholder="Tên đăng nhập hoặc email" aria-label="Tên đăng nhập hoặc email" v-bind="componentField" /></div>
              </FormControl>
              <FormMessage />
            </FormItem>
          </FormField>

          <FormField v-slot="{ componentField }" name="password">
            <FormItem>
              <FormControl>
                <div class="field-wrap"><LockKeyhole class="field-icon" :size="18" /><Input :type="showPassword ? 'text' : 'password'" autocomplete="current-password" placeholder="Mật khẩu" aria-label="Mật khẩu" v-bind="componentField" /><button class="visibility-toggle" type="button" :aria-label="showPassword ? 'Ẩn mật khẩu' : 'Hiện mật khẩu'" @click="showPassword = !showPassword"><EyeOff v-if="showPassword" :size="18" /><Eye v-else :size="18" /></button></div>
              </FormControl>
              <FormMessage />
            </FormItem>
          </FormField>

          <div class="form-options"><label class="remember-option"><input v-model="remember" type="checkbox"><span class="custom-check"></span><span>Ghi nhớ đăng nhập</span></label></div>
          <button class="submit-button" type="submit">Đăng nhập <ArrowRight :size="19" /></button>
        </form>

        <div class="login-divider"><span>hoặc</span></div>
        <div v-if="googleClientId" ref="googleButton" class="google-button-container"></div>
        <button v-else class="google-fallback-button" type="button" disabled>
          <span class="google-mark">G</span>
          Đăng nhập với Google
        </button>
        <p v-if="googleStatus" class="google-status" role="status">{{ googleStatus }}</p>

        <p class="signup-prompt">Chưa có tài khoản? <RouterLink to="/register">Đăng ký</RouterLink></p>
      </div>
      <p class="secure-note"><LockKeyhole :size="13" /> Kết nối được bảo vệ an toàn</p>
    </section>
  </main>
</template>

<style scoped>
.login-page { display: grid; grid-template-columns: minmax(0, 1fr) minmax(470px, .98fr); min-height: 100vh; overflow: hidden; color: #101d49; background: #f4f8fe; font-family: Inter, ui-sans-serif, system-ui, -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif; }
.intro-panel { position: relative; display: flex; min-height: 100vh; flex-direction: column; padding: 42px max(46px, calc((100vw - 1360px) / 2)); padding-right: 40px; overflow: hidden; background: radial-gradient(ellipse at 5% 88%, rgba(102, 170, 255, .22), transparent 36%), radial-gradient(ellipse at 73% 70%, rgba(189, 215, 255, .4), transparent 37%), linear-gradient(137deg, #edf6ff 2%, #f7faff 58%, #e9f2ff 100%); }
.intro-panel::before { position: absolute; right: -18%; bottom: -37%; width: 88%; height: 74%; border-radius: 50%; background: linear-gradient(145deg, rgba(255,255,255,.05), rgba(165,199,255,.38)); content: ''; transform: rotate(-22deg); }
.intro-panel::after { position: absolute; right: 0; bottom: 0; left: 0; height: 20%; background: linear-gradient(0deg, rgba(194,220,255,.24), transparent); content: ''; pointer-events: none; }
.brand-lockup { z-index: 1; display: inline-flex; width: fit-content; align-items: center; gap: 10px; color: #101d49; text-decoration: none; font-size: 22px; font-weight: 750; }
.brand-icon, .card-brand-icon { display: grid; place-items: center; color: #2864f5; }.brand-icon { width: 48px; height: 44px; filter: drop-shadow(0 5px 8px rgba(42,100,245,.15)); }
.intro-copy { z-index: 1; width: min(100%, 560px); margin-top: 76px; }.intro-kicker { margin: 0 0 13px; color: #3267cc; font-size: 11px; font-weight: 750; letter-spacing: 1.1px; }.intro-copy h1 { margin: 0; color: #101d49; font-size: clamp(31px, 3vw, 43px); line-height: 1.27; font-weight: 750; letter-spacing: 0; }.intro-description { max-width: 520px; margin: 17px 0 0; color: #5b7199; font-size: 16px; line-height: 1.7; }
.benefit-list { z-index: 1; display: flex; align-items: center; margin-top: 31px; }.benefit { display: flex; align-items: center; gap: 10px; padding: 0 18px; color: #506b96; font-size: 12px; line-height: 1.5; }.benefit:first-child { padding-left: 0; }.benefit + .benefit { border-left: 1px solid #d3e0f2; }.benefit-icon { display: grid; width: 40px; height: 40px; flex: 0 0 auto; place-items: center; border-radius: 50%; }.target-icon { color: #286ef1; background: #dceaff; }.progress-icon { color: #19ae8a; background: #def7f0; }.ai-icon { color: #8357ed; background: #eee6ff; }
.study-visual { position: relative; z-index: 1; width: min(100%, 640px); min-height: 350px; flex: 1; margin: 26px auto -42px; }.visual-orbit { position: absolute; border-radius: 50%; border: 1px solid rgba(160,191,241,.24); }.orbit-one { right: 1%; bottom: 7%; width: 59%; aspect-ratio: 1; }.orbit-two { right: 8%; bottom: 14%; width: 44%; aspect-ratio: 1; border-style: dashed; }
.laptop-scene { position: absolute; z-index: 2; right: 14%; bottom: 3%; width: 56%; }.laptop-screen { position: relative; min-height: 172px; padding: 13px 14px; border: 7px solid #15274f; border-bottom-width: 10px; border-radius: 9px 9px 3px 3px; background: #fff; box-shadow: 0 15px 30px rgba(37,72,142,.18); transform: perspective(700px) rotateY(-5deg) rotateX(2deg); }.laptop-base { width: 111%; height: 12px; margin-left: -5.5%; border-radius: 2px 2px 14px 14px; background: linear-gradient(#627cae,#304b7f); box-shadow: 0 9px 15px rgba(35,73,142,.16); }.screen-top { display: flex; align-items: center; justify-content: space-between; padding-bottom: 9px; border-bottom: 1px solid #edf1f7; color: #64789e; font-size: 7px; }.mini-logo { display: flex; align-items: center; gap: 3px; color: #2864f5; font-weight: 700; }.screen-avatar { display: grid; width: 16px; height: 16px; place-items: center; border-radius: 50%; color: white; background: #31b698; font-size: 7px; }.screen-welcome { display: flex; justify-content: space-between; margin: 11px 0 7px; color: #172858; font-size: 10px; font-weight: 700; }.screen-welcome span { color: #8092af; font-size: 7px; font-weight: 500; }.screen-progress { padding: 7px; border-radius: 5px; background: #f3f7ff; }.screen-progress > span { display: block; height: 5px; margin-bottom: 6px; overflow: hidden; border-radius: 4px; background: #dbe6f7; }.screen-progress i { display: block; width: 68%; height: 100%; border-radius: 4px; background: #31b698; }.screen-progress small { display: flex; justify-content: space-between; color: #8090aa; font-size: 7px; }.screen-progress b { color: #148e6e; }.screen-courses { display: grid; gap: 5px; margin-top: 7px; }.screen-courses > div { display: flex; align-items: center; gap: 6px; }.screen-courses > div > span:nth-child(2) { display: grid; gap: 2px; flex: 1; }.screen-courses b { color: #3f5274; font-size: 7px; }.screen-courses small { color: #91a0b8; font-size: 6px; }.screen-courses em { color: #7183a1; font-size: 7px; font-style: normal; }.course-check { display: grid; width: 12px; height: 12px; place-items: center; border-radius: 50%; color: white; background: #31b698; }.course-check svg { width: 8px; height: 8px; }.course-dot { width: 12px; height: 12px; border: 2px solid #4580f5; border-radius: 50%; }
.visual-note { position: absolute; z-index: 4; top: 25%; right: 4%; display: flex; gap: 8px; padding: 12px 15px; border: 1px solid rgba(228,236,249,.9); border-radius: 12px; background: rgba(255,255,255,.9); color: #61769d; font-size: 10px; line-height: 1.5; box-shadow: 0 12px 28px rgba(58,95,163,.1); }.visual-note strong { color: #334e82; font-weight: 650; }.note-spark { color: #3674f5; font-size: 18px; }.visual-path { position: absolute; z-index: 3; top: 39%; left: 47%; display: flex; align-items: center; }.visual-path > span,.visual-path > b,.visual-path > strong { display: grid; width: 38px; height: 38px; place-items: center; border-radius: 50%; color: white; background: #3778f5; box-shadow: 0 8px 18px rgba(55,120,245,.2); }.visual-path svg { width: 19px; height: 19px; }.visual-path > i { width: 27px; border-top: 2px dashed #93b7fb; }.visual-path > b { width: 24px; height: 24px; background: #2dbb98; }.visual-path > b svg { width: 13px; height: 13px; }.visual-path > strong { width: 42px; height: 42px; background: #7962e9; }.visual-book { position: absolute; z-index: 3; right: 2%; bottom: 3%; width: 18%; height: 20px; border: 3px solid #4378d9; border-radius: 3px; background: #fff; box-shadow: 0 -8px 0 -2px #8dc8fb, 0 -14px 0 -5px #4384de, 0 5px 10px rgba(30,70,140,.16); }.visual-plant { position: absolute; left: 5%; bottom: 3%; width: 48px; height: 92px; }.visual-plant b { position: absolute; bottom: 0; left: 12px; width: 26px; height: 21px; border-radius: 3px 3px 8px 8px; background: #4d83d9; }.visual-plant i { position: absolute; bottom: 21px; left: 24px; width: 15px; height: 35px; border-radius: 100% 0 100% 0; background: #51b69a; transform: rotate(-36deg); transform-origin: bottom; }.visual-plant i:nth-child(2) { left: 22px; height: 43px; transform: rotate(35deg) scaleX(-1); }.visual-plant i:nth-child(3) { left: 22px; height: 30px; transform: rotate(-4deg); }
.form-panel { display: flex; align-items: center; justify-content: center; flex-direction: column; padding: 40px clamp(28px, 6vw, 82px); background: linear-gradient(145deg, #f8fbff, #eff6ff); }.login-card { width: min(100%, 500px); padding: 39px 42px 34px; border: 1px solid rgba(255,255,255,.9); border-radius: 14px; background: rgba(255,255,255,.9); box-shadow: 0 18px 55px rgba(47,89,156,.1); text-align: center; }.card-brand-icon { margin: 0 auto 10px; filter: drop-shadow(0 6px 10px rgba(42,100,245,.12)); }.login-card h2 { margin: 0; color: #101d49; font-size: 26px; letter-spacing: 0; }.card-subtitle { margin: 10px 0 30px; color: #6980a4; font-size: 13px; line-height: 1.5; }.login-form { display: grid; gap: 16px; text-align: left; }.field-wrap { position: relative; display: flex; height: 54px; align-items: center; border: 1px solid #d7e0ec; border-radius: 8px; background: #fff; transition: border-color .15s ease, box-shadow .15s ease; }.field-wrap:focus-within { border-color: #4780f5; box-shadow: 0 0 0 3px rgba(71,128,245,.1); }.field-icon { flex: 0 0 auto; margin-left: 16px; color: #8095b5; }.field-wrap :deep(input) { height: 100%; padding: 0 13px; border: 0; outline: 0; box-shadow: none; color: #172858; font: inherit; font-size: 14px; }.field-wrap :deep(input::placeholder) { color: #93a3bc; }.field-wrap :deep(input:focus-visible) { box-shadow: none; }.visibility-toggle { display: grid; width: 44px; height: 44px; flex: 0 0 auto; place-items: center; border: 0; background: transparent; color: #8193b0; cursor: pointer; }.form-options { display: flex; align-items: center; justify-content: space-between; margin-top: -1px; }.remember-option { display: inline-flex; align-items: center; gap: 9px; color: #516b94; font-size: 12px; cursor: pointer; }.remember-option input { position: absolute; width: 1px; height: 1px; opacity: 0; }.custom-check { display: grid; width: 19px; height: 19px; place-items: center; border: 1px solid #cad6e7; border-radius: 5px; background: #fff; }.remember-option input:checked + .custom-check { border-color: #2869f4; background: #2869f4; }.remember-option input:checked + .custom-check::after { width: 5px; height: 9px; border: solid #fff; border-width: 0 2px 2px 0; content: ''; transform: rotate(45deg) translate(-1px,-1px); }.remember-option input:focus-visible + .custom-check { outline: 3px solid rgba(40,105,244,.2); }.submit-button { display: flex; height: 54px; align-items: center; justify-content: center; gap: 13px; margin-top: 6px; border: 0; border-radius: 8px; background: #2d68f3; box-shadow: 0 7px 15px rgba(45,104,243,.18); color: white; font: inherit; font-size: 14px; font-weight: 650; cursor: pointer; transition: background .15s ease, transform .15s ease; }.submit-button:hover { background: #2059df; }.submit-button:active { transform: translateY(1px); }.signup-prompt { margin: 26px 0 0; color: #7184a3; font-size: 12px; }.signup-prompt span { margin-left: 4px; color: #2869f4; font-weight: 600; }.secure-note { display: flex; align-items: center; gap: 6px; margin: 17px 0 0; color: #8a9bb4; font-size: 11px; }
@media (max-width: 980px) { .login-page { grid-template-columns: minmax(0, .9fr) minmax(420px, 1.1fr); }.intro-panel { padding-left: 32px; padding-right: 24px; }.intro-copy { margin-top: 62px; }.intro-copy h1 { font-size: 33px; }.benefit { padding: 0 10px; gap: 7px; font-size: 11px; }.benefit-icon { width: 34px; height: 34px; }.form-panel { padding: 32px 26px; }.login-card { padding: 34px 30px; } }
@media (max-width: 720px) { :global(html), :global(body), :global(#app) { width: 100%; min-width: 0; overflow-x: hidden; }.login-page { display: block; width: 100%; min-width: 0; overflow: hidden; background: #f2f7ff; }.intro-panel { width: 100%; min-width: 0; min-height: 0; box-sizing: border-box; padding: 20px 23px 28px; }.brand-lockup { font-size: 19px; }.brand-icon { width: 40px; height: 38px; }.intro-copy { width: 100%; min-width: 0; margin-top: 35px; }.intro-kicker { margin-bottom: 9px; font-size: 10px; }.intro-copy h1 { font-size: 29px; overflow-wrap: anywhere; }.desktop-break { display: none; }.intro-description { max-width: 55ch; margin-top: 11px; font-size: 14px; line-height: 1.6; }.benefit-list { width: 100%; min-width: 0; margin-top: 20px; }.benefit { min-width: 0; flex: 1; padding: 0 8px; font-size: 10px; }.benefit:first-child { padding-left: 0; }.benefit-icon { width: 30px; height: 30px; }.study-visual { display: none; }.form-panel { width: 100%; min-width: 0; min-height: 0; box-sizing: border-box; padding: 27px 16px 32px; background: #f2f7ff; }.login-card { width: 100%; max-width: 450px; min-width: 0; box-sizing: border-box; padding: 28px 23px 25px; border-radius: 12px; }.card-brand-icon { margin-bottom: 6px; }.login-card h2 { font-size: 23px; }.card-subtitle { margin: 8px 0 23px; font-size: 12px; }.field-wrap { height: 51px; }.submit-button { height: 51px; }.secure-note { margin-top: 13px; } }
@media (max-width: 360px) { .intro-panel { padding-right: 16px; padding-left: 16px; }.intro-copy h1 { font-size: 26px; }.benefit { gap: 5px; padding-right: 6px; padding-left: 6px; font-size: 9px; }.benefit-icon { width: 27px; height: 27px; }.login-card { padding-right: 17px; padding-left: 17px; } }
.login-divider { display: flex; align-items: center; gap: 12px; margin: 20px 0 14px; color: #8a9bb4; font-size: 11px; }.login-divider::before,.login-divider::after { height: 1px; flex: 1; background: #e4ebf5; content: ''; }.google-button-container { display: flex; min-height: 42px; justify-content: center; }.google-fallback-button { display: flex; width: 100%; height: 44px; align-items: center; justify-content: center; gap: 11px; border: 1px solid #d7e0ec; border-radius: 6px; background: #fff; color: #52627b; font: inherit; font-size: 13px; font-weight: 550; }.google-fallback-button:disabled { cursor: not-allowed; opacity: .82; }.google-mark { font-size: 17px; font-weight: 750; color: #4285f4; }.google-status { margin: 8px 0 0; color: #7b8ca6; font-size: 11px; line-height: 1.5; }
.signup-prompt a { margin-left: 4px; color: #2869f4; font-weight: 600; text-decoration: none; }
.signup-prompt a:hover { text-decoration: underline; }
</style>
