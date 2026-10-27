<script setup>
import { computed, onMounted, ref } from 'vue'
import { RouterLink, useRouter } from 'vue-router'
import { ArrowLeft, ArrowRight, Check, Crown, LoaderCircle, LockKeyhole } from 'lucide-vue-next'
import { paymentService } from '@/services/paymentService'

const router = useRouter()
const entitlements = ref(null)
const loading = ref(true)
const checkoutProvider = ref('')
const error = ref('')
const freeFeatures = ['Nghe, Đọc và Viết', 'Tối đa 5 lượt tạo đề luyện tập', 'Xem lại đề và kết quả đã lưu']
const premiumFeatures = ['Đầy đủ 4 kỹ năng: Nghe, Đọc, Viết, Nói', 'Trợ lý AI học tập', 'Không giới hạn tạo đề và lượt đánh giá', 'Mở khóa vĩnh viễn sau thanh toán một lần']
const availableMethods = computed(() => entitlements.value?.paymentMethods || [])

function money(value, currency) {
  if (value == null || !currency) return 'Chưa cấu hình giá'
  return new Intl.NumberFormat('vi-VN', { style: 'currency', currency }).format(Number(value))
}

function messageFor(exception) {
  const code = exception?.response?.data?.responseCode
  if (code === 'PAYMENT-ERR-503') return 'Thanh toán chưa được cấu hình. Vui lòng thử lại sau.'
  if (code === 'PAYMENT-ERR-504') return 'Cổng thanh toán chưa phản hồi. Không phát sinh giao dịch, hãy thử lại.'
  if (code === 'PAYMENT-ERR-409') return 'Tài khoản đã có gói Premium.'
  return exception?.response?.data?.responseMessage || 'Không thể khởi tạo thanh toán.'
}

async function load() {
  loading.value = true
  error.value = ''
  try {
    entitlements.value = await paymentService.entitlements()
  } catch (exception) {
    error.value = exception?.response?.data?.responseMessage || 'Không tải được thông tin gói học.'
  } finally {
    loading.value = false
  }
}

async function checkout(provider) {
  if (checkoutProvider.value) return
  checkoutProvider.value = provider
  error.value = ''
  try {
    const result = await paymentService.checkout(provider)
    window.location.assign(result.redirectUrl)
  } catch (exception) {
    error.value = messageFor(exception)
    checkoutProvider.value = ''
  }
}

onMounted(load)
</script>

<template>
  <main class="pricing-page">
    <header class="pricing-header">
      <RouterLink to="/home" class="pricing-back"><ArrowLeft :size="17" />Về học tập</RouterLink>
      <span class="pricing-brand"><Crown :size="19" />Adaptive Learning</span>
    </header>
    <section class="pricing-content" aria-labelledby="pricing-title">
      <p class="pricing-kicker">GÓI HỌC TẬP</p>
      <div class="pricing-title-row"><div><h1 id="pricing-title">Chọn cách bạn muốn học</h1><p>Mở rộng kỹ năng và luyện tập không giới hạn.</p></div><span v-if="entitlements" class="current-plan">Đang dùng: {{ entitlements.plan }}</span></div>
      <div v-if="loading" class="pricing-loading" role="status"><LoaderCircle class="spin" :size="19" />Đang tải gói học…</div>
      <div v-else class="pricing-layout">
        <section class="pricing-plan free-plan" aria-labelledby="free-title">
          <div class="plan-heading"><div><p>KHỞI ĐẦU</p><h2 id="free-title">Free</h2></div><span class="plan-state">{{ entitlements?.plan === 'FREE' ? 'Gói hiện tại' : 'Miễn phí' }}</span></div>
          <p class="plan-description">Nền tảng thiết yếu để bắt đầu ôn luyện.</p>
          <p class="plan-price">0 <span>đ</span></p>
          <div class="plan-quota" v-if="entitlements && entitlements.plan === 'FREE'">
            <div><span>Lượt tạo đề còn lại</span><strong>{{ entitlements.examsRemaining }} / {{ entitlements.examLimit }}</strong></div>
            <div class="quota-track" role="progressbar" :aria-valuenow="entitlements.examsUsed" :aria-valuemax="entitlements.examLimit" aria-label="Lượt tạo đề đã sử dụng"><i :style="{ width: `${Math.min(100, entitlements.examsUsed / Math.max(1, entitlements.examLimit) * 100)}%` }"></i></div>
          </div>
          <ul class="plan-features"><li v-for="feature in freeFeatures" :key="feature"><Check :size="16" />{{ feature }}</li><li class="locked-feature"><LockKeyhole :size="15" />Nói và Trợ lý AI</li></ul>
          <button v-if="entitlements?.plan === 'FREE'" class="plan-current-button" type="button" disabled>Gói hiện tại</button>
          <RouterLink v-else class="plan-secondary-button" to="/home">Quay lại học</RouterLink>
        </section>

        <section class="pricing-plan premium-plan" aria-labelledby="premium-title">
          <div class="plan-heading"><div><p>NÂNG CAO</p><h2 id="premium-title">Premium</h2></div><span class="plan-state premium-state"><Crown :size="13" />Mở khóa vĩnh viễn</span></div>
          <p class="plan-description">Mở mọi kỹ năng và luyện tập không giới hạn.</p>
          <p class="plan-price premium-price">{{ money(entitlements?.premiumPrice, entitlements?.premiumCurrency) }} <span>· thanh toán một lần</span></p>
          <ul class="plan-features premium-features"><li v-for="feature in premiumFeatures" :key="feature"><Check :size="16" />{{ feature }}</li></ul>
          <div v-if="error" class="pricing-error" role="alert">{{ error }}</div>
          <div v-if="entitlements?.plan === 'PREMIUM'" class="premium-active">Tài khoản của bạn đã có Premium.</div>
          <div v-else-if="!entitlements?.premiumAvailable" class="premium-unavailable">Thanh toán đang tạm thời chưa khả dụng.</div>
          <div v-else class="payment-actions">
            <button v-if="availableMethods.includes('STRIPE')" class="payment-button stripe-button" type="button" :disabled="!!checkoutProvider" @click="checkout('stripe')"><LoaderCircle v-if="checkoutProvider === 'stripe'" class="spin" :size="17" /><span v-else class="stripe-mark">S</span>Thanh toán với Stripe<ArrowRight v-if="checkoutProvider !== 'stripe'" :size="16" /></button>
            <button v-if="availableMethods.includes('PAYPAL')" class="payment-button paypal-button" type="button" :disabled="!!checkoutProvider" @click="checkout('paypal')"><LoaderCircle v-if="checkoutProvider === 'paypal'" class="spin" :size="17" /><span v-else class="paypal-mark">P</span>Thanh toán với PayPal<ArrowRight v-if="checkoutProvider !== 'paypal'" :size="16" /></button>
          </div>
          <p class="payment-note">Thanh toán được xử lý an toàn bởi cổng đã chọn. Không lưu thông tin thẻ trong ứng dụng.</p>
        </section>
      </div>
      <p class="pricing-footnote">Premium là khoản thanh toán một lần. Gói được mở sau khi hệ thống xác nhận thanh toán.</p>
    </section>
  </main>
</template>

<style scoped>
.pricing-page { min-height: 100vh; color: var(--text); background: var(--bg-page); font-family: var(--font-sans); }
.pricing-header { display: flex; min-height: 64px; align-items: center; justify-content: space-between; gap: 18px; padding: 0 clamp(18px, 4vw, 48px); border-bottom: 1px solid var(--ds-border); background: var(--surface); }
.pricing-back,.pricing-brand { display: inline-flex; align-items: center; gap: 8px; color: var(--text-secondary); font-size: 13px; font-weight: 600; text-decoration: none; }
.pricing-back:hover { color: var(--primary-ink); }.pricing-brand { color: var(--text-strong); font-size: 15px; }.pricing-brand svg { color: var(--primary); }
.pricing-content { width: min(100% - 40px, 1000px); margin: 0 auto; padding: 42px 0 48px; }
.pricing-kicker { margin: 0 0 7px; color: var(--text-overline); font-size: 11px; font-weight: 700; }
.pricing-title-row { display: flex; align-items: flex-end; justify-content: space-between; gap: 18px; margin-bottom: 24px; }
.pricing-title-row h1 { margin: 0; color: var(--text); font-size: 28px; line-height: 36px; font-weight: 700; }
.pricing-title-row p { margin: 6px 0 0; color: var(--text-muted); font-size: 14px; }
.current-plan { padding: 7px 10px; border: 1px solid var(--primary-border); border-radius: var(--radius-full); color: var(--primary-ink); background: var(--primary-soft); font-size: 11px; font-weight: 600; }
.pricing-layout { display: grid; grid-template-columns: minmax(0, .82fr) minmax(0, 1.18fr); align-items: stretch; gap: 16px; }
.pricing-plan { display: flex; min-width: 0; flex-direction: column; padding: 24px; border: 1px solid var(--ds-border); border-radius: var(--radius-xl); background: var(--surface); }
.premium-plan { border-color: var(--primary-border); }
.plan-heading { display: flex; align-items: flex-start; justify-content: space-between; gap: 12px; }
.plan-heading p { margin: 0 0 3px; color: var(--text-overline); font-size: 10px; font-weight: 700; }
.plan-heading h2 { margin: 0; color: var(--text-strong); font-size: 20px; line-height: 28px; font-weight: 700; }
.plan-state { display: inline-flex; align-items: center; gap: 5px; padding: 6px 9px; border-radius: var(--radius-full); color: var(--text-secondary); background: var(--surface-muted); font-size: 10px; font-weight: 600; white-space: nowrap; }
.premium-state { color: var(--primary-ink); background: var(--primary-soft); }
.plan-description { min-height: 38px; margin: 12px 0 14px; color: var(--text-muted); font-size: 12px; line-height: 1.6; }
.plan-price { margin: 0; color: var(--text); font-size: 30px; line-height: 38px; font-weight: 700; font-variant-numeric: tabular-nums; }
.plan-price span { color: var(--text-muted); font-size: 12px; font-weight: 400; }
.premium-price { font-size: 27px; }.premium-price span { font-size: 11px; }
.plan-quota { display: grid; gap: 8px; margin: 14px 0 4px; padding: 12px 0; border-top: 1px solid var(--border-subtle); border-bottom: 1px solid var(--border-subtle); }
.plan-quota > div:first-child { display: flex; justify-content: space-between; gap: 12px; color: var(--text-muted); font-size: 11px; }.plan-quota strong { color: var(--text-strong); font-variant-numeric: tabular-nums; }
.quota-track { height: 5px; overflow: hidden; background: var(--surface-muted); }.quota-track i { display: block; height: 100%; background: var(--primary); }
.plan-features { display: grid; gap: 12px; margin: 20px 0 24px; padding: 0; list-style: none; }
.plan-features li { display: flex; align-items: flex-start; gap: 9px; color: var(--text-secondary); font-size: 12px; line-height: 1.55; }
.plan-features svg { flex: 0 0 auto; color: var(--primary-ink); }
.plan-features .locked-feature,.plan-features .locked-feature svg { color: var(--text-muted); }
.plan-current-button,.plan-secondary-button { display: flex; min-height: 44px; align-items: center; justify-content: center; margin-top: auto; border: 1px solid var(--border-control); border-radius: var(--radius-md); color: var(--text-secondary); background: var(--surface-muted); font: inherit; font-size: 12px; font-weight: 600; text-decoration: none; }
.plan-current-button:disabled { cursor: default; }
.payment-actions { display: grid; gap: 9px; margin-top: auto; }
.payment-button { display: flex; min-height: 48px; align-items: center; gap: 10px; padding: 0 13px; border: 1px solid var(--ds-primary); border-radius: var(--radius-md); color: white; background: var(--ds-primary); font: inherit; font-size: 13px; font-weight: 600; cursor: pointer; }
.payment-button > svg:last-child { margin-left: auto; }.payment-button:hover:not(:disabled) { background: var(--primary-hover); }.payment-button:disabled { opacity: .65; cursor: wait; }
.stripe-button { border-color: #3e4ad6; background: #3e4ad6; }.stripe-button:hover:not(:disabled) { background: #313cb7; }
.paypal-button { border-color: #173e72; background: #173e72; }.paypal-button:hover:not(:disabled) { background: #102e56; }
.stripe-mark,.paypal-mark { display: grid; width: 24px; height: 24px; flex: 0 0 auto; place-items: center; border-radius: 5px; color: #fff; background: rgba(255,255,255,.15); font-size: 14px; font-weight: 700; }
.paypal-mark { font-style: italic; }
.payment-note { margin: 12px 0 0; color: var(--text-muted); font-size: 10px; line-height: 1.5; }
.pricing-error { margin: 0 0 12px; color: var(--danger); font-size: 12px; line-height: 1.5; }
.premium-active,.premium-unavailable { margin-top: auto; padding: 12px; border-top: 1px solid var(--border-subtle); color: var(--text-muted); font-size: 12px; }
.premium-active { color: var(--primary-ink); }
.pricing-footnote { margin: 16px 0 0; color: var(--text-muted); font-size: 11px; line-height: 1.5; }
.pricing-loading { display: flex; min-height: 220px; align-items: center; justify-content: center; gap: 10px; color: var(--text-muted); font-size: 13px; }
.spin { animation: pricing-spin .9s linear infinite; }
@keyframes pricing-spin { to { transform: rotate(360deg); } }
@media (max-width: 680px) { .pricing-content { width: min(100% - 32px, 480px); padding: 30px 0 36px; }.pricing-title-row { align-items: flex-start; flex-direction: column; }.pricing-title-row h1 { font-size: 24px; line-height: 32px; }.pricing-layout { grid-template-columns: minmax(0,1fr); }.pricing-plan { padding: 20px; }.plan-description { min-height: 0; }.free-plan { order: 2; } }
@media (prefers-reduced-motion: reduce) { .spin { animation-duration: 2s; } }
</style>
