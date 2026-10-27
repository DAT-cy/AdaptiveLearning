<script setup>
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { RouterLink, useRoute } from 'vue-router'
import { ArrowRight, CheckCircle2, CircleAlert, Clock3, LoaderCircle, X } from 'lucide-vue-next'
import { paymentService } from '@/services/paymentService'

const route = useRoute()
const payment = ref(null)
const entitlements = ref(null)
const loading = ref(true)
const timedOut = ref(false)
const error = ref('')
const isCancel = computed(() => route.path.endsWith('/cancel'))
const isSucceeded = computed(() => payment.value?.status === 'SUCCEEDED')
const isFailed = computed(() => ['FAILED', 'REFUNDED'].includes(payment.value?.status))
let pollTimer
let pollStartedAt = 0

function amountLabel() {
  if (!payment.value) return ''
  try {
    return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: payment.value.currency }).format(Number(payment.value.amount))
  } catch {
    return `${payment.value.amount} ${payment.value.currency}`
  }
}

async function pollPayment() {
  const paymentId = Number(route.query.paymentId)
  if (!paymentId) {
    error.value = 'Không tìm thấy mã thanh toán.'
    loading.value = false
    return
  }
  try {
    payment.value = await paymentService.status(paymentId)
  } catch (exception) {
    error.value = exception?.response?.data?.responseMessage || 'Không tải được trạng thái giao dịch.'
    loading.value = false
    return
  }
  if (payment.value.status !== 'PENDING') {
    loading.value = false
    clearTimeout(pollTimer)
    if (payment.value.status === 'SUCCEEDED') {
      try { entitlements.value = await paymentService.entitlements() } catch { entitlements.value = null }
    }
    return
  }
  if (Date.now() - pollStartedAt >= 60000) {
    timedOut.value = true
    loading.value = false
    return
  }
  pollTimer = window.setTimeout(pollPayment, 2500)
}

onMounted(() => {
  if (isCancel.value) {
    loading.value = false
    return
  }
  pollStartedAt = Date.now()
  pollPayment()
})
onUnmounted(() => window.clearTimeout(pollTimer))
</script>

<template>
  <main class="payment-status-page">
    <section class="payment-status-panel" aria-live="polite">
      <template v-if="isCancel">
        <span class="payment-state-icon neutral"><X :size="23" /></span>
        <p class="payment-kicker">THANH TOÁN</p>
        <h1>Giao dịch đã hủy</h1>
        <p class="payment-description">Bạn chưa bị tính phí. Gói hiện tại vẫn được giữ nguyên.</p>
      </template>
      <template v-else-if="error">
        <span class="payment-state-icon neutral"><CircleAlert :size="23" /></span>
        <p class="payment-kicker">TRẠNG THÁI GIAO DỊCH</p>
        <h1>Chưa tải được thanh toán</h1>
        <p class="payment-description">{{ error }}</p>
      </template>
      <template v-else-if="isSucceeded">
        <span class="payment-state-icon success"><CheckCircle2 :size="24" /></span>
        <p class="payment-kicker">THANH TOÁN HOÀN TẤT</p>
        <h1>Premium đã được mở khóa</h1>
        <p class="payment-description">{{ amountLabel() }} · {{ payment?.provider }}</p>
        <p class="payment-supporting">{{ entitlements?.plan === 'PREMIUM' ? 'Đã mở khóa Speaking, Trợ lý AI và lượt luyện tập không giới hạn.' : 'Giao dịch đã xác nhận. Quyền học đang được đồng bộ.' }}</p>
      </template>
      <template v-else-if="isFailed">
        <span class="payment-state-icon neutral"><CircleAlert :size="23" /></span>
        <p class="payment-kicker">THANH TOÁN CHƯA THÀNH CÔNG</p>
        <h1>Gói Premium chưa được mở</h1>
        <p class="payment-description">Giao dịch chưa được xác nhận. Bạn có thể thử thanh toán lại.</p>
      </template>
      <template v-else-if="loading">
        <span class="payment-state-icon pending"><LoaderCircle class="payment-spinner" :size="22" /></span>
        <p class="payment-kicker">ĐANG XÁC NHẬN</p>
        <h1>Đang cập nhật giao dịch</h1>
        <p class="payment-description">Cổng thanh toán đã trả về. Đang chờ xác nhận chính thức.</p>
      </template>
      <template v-else>
        <span class="payment-state-icon pending"><Clock3 :size="23" /></span>
        <p class="payment-kicker">ĐANG XỬ LÝ</p>
        <h1>Thanh toán đang được xác nhận</h1>
        <p class="payment-description">Giao dịch chưa hoàn tất đồng bộ. Premium sẽ tự cập nhật khi nhận được xác nhận.</p>
      </template>
      <div class="payment-actions">
        <RouterLink v-if="isSucceeded || isCancel" class="payment-primary-link" to="/home">Về trang học tập<ArrowRight :size="16" /></RouterLink>
        <RouterLink v-else-if="isFailed || timedOut || error" class="payment-primary-link" to="/pricing">Quay lại gói học<ArrowRight :size="16" /></RouterLink>
        <RouterLink v-else class="payment-secondary-link" to="/home">Tiếp tục học trong khi chờ</RouterLink>
      </div>
      <RouterLink class="payment-history-link" to="/pricing">Xem lựa chọn gói học</RouterLink>
    </section>
  </main>
</template>

<style scoped>
.payment-status-page { display: grid; min-height: 100vh; place-items: center; padding: 24px; color: var(--text); background: var(--bg-page); font-family: var(--font-sans); }
.payment-status-panel { width: min(100%, 480px); padding: 34px; border: 1px solid var(--ds-border); border-radius: var(--radius-2xl); background: var(--surface); text-align: center; }
.payment-state-icon { display: grid; width: 48px; height: 48px; margin: 0 auto 20px; place-items: center; border-radius: var(--radius-lg); }
.payment-state-icon.success { color: var(--primary-ink); background: var(--primary-soft); }
.payment-state-icon.pending { color: var(--text-secondary); background: var(--surface-muted); }
.payment-state-icon.neutral { color: var(--text-secondary); background: var(--surface-muted); }
.payment-spinner { animation: payment-spin 1s linear infinite; }
.payment-kicker { margin: 0 0 7px; color: var(--text-overline); font-size: 11px; font-weight: 700; }
.payment-status-panel h1 { margin: 0; color: var(--text); font-size: 24px; line-height: 32px; font-weight: 700; }
.payment-description { margin: 9px 0 0; color: var(--text-muted); font-size: 13px; line-height: 1.6; }
.payment-supporting { margin: 13px 0 0; color: var(--text-secondary); font-size: 12px; line-height: 1.6; }
.payment-actions { display: grid; gap: 9px; margin-top: 24px; }
.payment-primary-link,.payment-secondary-link { display: flex; min-height: 46px; align-items: center; justify-content: center; gap: 9px; border: 1px solid var(--ds-primary); border-radius: var(--radius-md); color: white; background: var(--ds-primary); font-size: 13px; font-weight: 600; text-decoration: none; }
.payment-primary-link:hover { background: var(--primary-hover); }.payment-secondary-link { border-color: var(--border-control); color: var(--text-strong); background: var(--surface); }
.payment-history-link { display: inline-block; margin-top: 17px; color: var(--text-muted); font-size: 12px; text-decoration: none; }.payment-history-link:hover { color: var(--primary-ink); text-decoration: underline; }
@keyframes payment-spin { to { transform: rotate(360deg); } }
@media (max-width: 480px) { .payment-status-page { padding: 16px; }.payment-status-panel { padding: 26px 20px; } }
</style>
