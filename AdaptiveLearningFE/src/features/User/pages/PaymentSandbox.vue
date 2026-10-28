<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  AlertCircle,
  ArrowLeft,
  CheckCircle2,
  Clock,
  CreditCard,
  Crown,
  HelpCircle,
  LoaderCircle,
  QrCode,
  ShieldCheck,
  Sparkles,
  X,
  XCircle,
} from 'lucide-vue-next'
import { paymentService } from '@/services/paymentService'

const route = useRoute()
const router = useRouter()

const paymentId = computed(() => Number(route.query.paymentId))
const payment = ref(null)
const loading = ref(true)
const processing = ref(false)
const error = ref('')
const selectedSimMethod = ref('vietqr')

const formattedAmount = computed(() => {
  if (!payment.value) return '199.000 ₫'
  try {
    return new Intl.NumberFormat('vi-VN', {
      style: 'currency',
      currency: payment.value.currency || 'VND',
    }).format(Number(payment.value.amount))
  } catch {
    return `${payment.value.amount} ${payment.value.currency}`
  }
})

const formattedDate = computed(() => {
  if (!payment.value?.createdAt) return 'Vừa xong'
  try {
    return new Intl.DateTimeFormat('vi-VN', {
      dateStyle: 'medium',
      timeStyle: 'short',
    }).format(new Date(payment.value.createdAt))
  } catch {
    return payment.value.createdAt
  }
})

async function loadPayment() {
  if (!paymentId.value) {
    error.value = 'Không tìm thấy mã giao dịch thanh toán.'
    loading.value = false
    return
  }
  loading.value = true
  error.value = ''
  try {
    payment.value = await paymentService.status(paymentId.value)
    if (payment.value?.status === 'SUCCEEDED') {
      router.replace(`/payment/success?paymentId=${paymentId.value}`)
    }
  } catch (err) {
    error.value = err?.response?.data?.responseMessage || 'Không thể tải thông tin giao dịch.'
  } finally {
    loading.value = false
  }
}

async function handleComplete(success) {
  if (processing.value) return
  processing.value = true
  error.value = ''
  try {
    await paymentService.completeSandbox(paymentId.value, success)
    if (success) {
      router.push(`/payment/success?paymentId=${paymentId.value}`)
    } else {
      router.push(`/payment/cancel?paymentId=${paymentId.value}`)
    }
  } catch (err) {
    error.value = err?.response?.data?.responseMessage || 'Không thể hoàn tất giao dịch mô phỏng.'
    processing.value = false
  }
}

onMounted(loadPayment)
</script>

<template>
  <main class="sandbox-page">
    <header class="sandbox-header">
      <div class="header-inner">
        <div class="brand-row">
          <div class="brand-badge">
            <Sparkles :size="16" class="text-amber-400" />
            <span>SANDBOX ENVIRONMENT</span>
          </div>
          <span class="header-tag">Môi trường thử nghiệm an toàn</span>
        </div>
        <p class="header-desc">Cổng giả lập thanh toán phục vụ kiểm thử tính năng nâng cấp gói Adaptive Learning</p>
      </div>
    </header>

    <div class="sandbox-container">
      <!-- TRẠNG THÁI LOADING -->
      <div v-if="loading" class="sandbox-card status-card">
        <LoaderCircle class="spinner-icon text-primary" :size="36" />
        <p class="mt-3 text-secondary font-medium">Đang tải thông tin giao dịch...</p>
      </div>

      <!-- TRẠNG THÁI LỖI -->
      <div v-else-if="error" class="sandbox-card status-card">
        <AlertCircle class="text-error" :size="40" />
        <h2 class="error-title">Không thể tiếp tục giao dịch</h2>
        <p class="error-desc">{{ error }}</p>
        <button type="button" class="btn-back" @click="router.push('/pricing')">
          <ArrowLeft :size="16" />
          Quay lại trang bảng giá
        </button>
      </div>

      <!-- NỘI DUNG CHÍNH -->
      <div v-else class="sandbox-grid">
        <!-- CỘT TRÁI: THÔNG TIN ĐƠN HÀNG -->
        <section class="order-summary-card">
          <div class="summary-header">
            <span class="summary-label">Đơn hàng cần thanh toán</span>
            <span class="payment-id-tag">#{{ payment?.paymentId }}</span>
          </div>

          <div class="plan-hero">
            <div class="plan-icon-wrap">
              <Crown :size="24" class="text-white" />
            </div>
            <div>
              <h3 class="plan-name">Adaptive Learning Premium</h3>
              <p class="plan-sub">Thanh toán 1 lần · Mở khóa vĩnh viễn</p>
            </div>
          </div>

          <div class="detail-rows">
            <div class="detail-row">
              <span class="row-label">Số tiền:</span>
              <strong class="row-value amount-highlight">{{ formattedAmount }}</strong>
            </div>
            <div class="detail-row">
              <span class="row-label">Thời gian tạo:</span>
              <span class="row-value">{{ formattedDate }}</span>
            </div>
            <div class="detail-row">
              <span class="row-label">Trạng thái:</span>
              <span class="status-badge pending">Chờ thanh toán</span>
            </div>
            <div class="detail-row">
              <span class="row-label">Quyền lợi mở khóa:</span>
              <ul class="perks-list">
                <li><CheckCircle2 :size="14" class="text-success" /> Kỹ năng Nói (Speaking AI)</li>
                <li><CheckCircle2 :size="14" class="text-success" /> Trợ lý AI Tutor 24/7</li>
                <li><CheckCircle2 :size="14" class="text-success" /> Tạo đề thi thích ứng không giới hạn</li>
              </ul>
            </div>
          </div>

          <div class="sandbox-notice-box">
            <HelpCircle :size="16" class="text-amber-600 flex-shrink-0" />
            <p>
              Đây là giao diện mô phỏng quy trình thanh toán. Bạn có thể kiểm tra cả luồng
              <strong>thành công</strong> lẫn <strong>hủy bỏ</strong> mà không bị trừ tiền thật.
            </p>
          </div>
        </section>

        <!-- CỘT PHẢI: CHỌN PHƯƠNG THỨC MÔ PHỎNG VÀ XÁC NHẬN -->
        <section class="payment-action-card">
          <h2 class="action-card-title">Chọn hình thức mô phỏng</h2>

          <div class="methods-selection">
            <label
              class="method-option"
              :class="{ active: selectedSimMethod === 'vietqr' }"
            >
              <input
                type="radio"
                v-model="selectedSimMethod"
                value="vietqr"
                class="sr-only"
              />
              <div class="method-icon-wrap qr">
                <QrCode :size="20" />
              </div>
              <div class="method-info">
                <strong>Quét mã VietQR (Chuyển khoản tức thì)</strong>
                <span>Mô phỏng quét mã ngân hàng 24/7</span>
              </div>
            </label>

            <label
              class="method-option"
              :class="{ active: selectedSimMethod === 'card' }"
            >
              <input
                type="radio"
                v-model="selectedSimMethod"
                value="card"
                class="sr-only"
              />
              <div class="method-icon-wrap card">
                <CreditCard :size="20" />
              </div>
              <div class="method-info">
                <strong>Thẻ ATM / Thẻ quốc tế (Sandbox Card)</strong>
                <span>Mô phỏng xác thực OTP / 3D-Secure</span>
              </div>
            </label>
          </div>

          <!-- MÔ PHỎNG THEO PHƯƠNG THỨC -->
          <div v-if="selectedSimMethod === 'vietqr'" class="method-preview-box">
            <div class="qr-mock-placeholder">
              <QrCode :size="120" class="text-slate-800" />
              <p class="qr-caption">Mã QR thử nghiệm hợp lệ</p>
            </div>
            <div class="qr-mock-info">
              <p>Ngân hàng mô phỏng: <strong>Adaptive Bank (Sandbox)</strong></p>
              <p>Chủ tài khoản: <strong>CONG TY ADAPTIVE LEARNING</strong></p>
              <p>Nội dung CK: <strong>PAY {{ payment?.paymentId }}</strong></p>
            </div>
          </div>

          <div v-else class="method-preview-box">
            <div class="card-mock-form">
              <div class="mock-field">
                <span class="mock-label">Số thẻ thử nghiệm:</span>
                <span class="mock-val">4000 0012 3456 7890</span>
              </div>
              <div class="mock-row">
                <div class="mock-field">
                  <span class="mock-label">Hết hạn:</span>
                  <span class="mock-val">12/30</span>
                </div>
                <div class="mock-field">
                  <span class="mock-label">CVV:</span>
                  <span class="mock-val">123</span>
                </div>
              </div>
            </div>
          </div>

          <!-- NÚT HÀNH ĐỘNG -->
          <div class="action-buttons-group">
            <button
              type="button"
              class="btn-confirm-success"
              :disabled="processing"
              @click="handleComplete(true)"
            >
              <LoaderCircle v-if="processing" class="spinner-icon" :size="18" />
              <template v-else>
                <CheckCircle2 :size="18" />
                <span>Xác nhận thanh toán thành công</span>
              </template>
            </button>

            <button
              type="button"
              class="btn-simulate-cancel"
              :disabled="processing"
              @click="handleComplete(false)"
            >
              <X :size="16" />
              <span>Mô phỏng hủy / thất bại</span>
            </button>
          </div>

          <div class="security-footer">
            <ShieldCheck :size="16" class="text-success" />
            <span>Kết quả thử nghiệm sẽ đồng bộ trực tiếp vào cơ sở dữ liệu và kích hoạt gói ngay.</span>
          </div>
        </section>
      </div>
    </div>
  </main>
</template>

<style scoped>
.sandbox-page {
  min-height: 100vh;
  background-color: #F8FAFC;
  color: #0F172A;
  font-family: inherit;
  display: flex;
  flex-direction: column;
}

.sandbox-header {
  background: #0F172A;
  color: #FFFFFF;
  padding: 24px 20px;
  border-bottom: 1px solid #1E293B;
}

.header-inner {
  max-width: 960px;
  margin: 0 auto;
}

.brand-row {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.brand-badge {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  background: rgba(255, 255, 255, 0.1);
  padding: 4px 10px;
  border-radius: 9999px;
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.05em;
  color: #F8FAFC;
}

.header-tag {
  font-size: 13px;
  color: #94A3B8;
}

.header-desc {
  margin: 8px 0 0 0;
  font-size: 14px;
  color: #CBD5E1;
}

.sandbox-container {
  max-width: 960px;
  width: 100%;
  margin: 32px auto;
  padding: 0 20px;
  flex: 1;
}

.sandbox-card {
  background: #FFFFFF;
  border: 1px solid #E2E8F0;
  border-radius: 12px;
  padding: 32px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
}

.status-card {
  text-align: center;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 20px;
}

.error-title {
  font-size: 18px;
  font-weight: 700;
  color: #0F172A;
  margin: 16px 0 8px;
}

.error-desc {
  font-size: 14px;
  color: #64748B;
  margin-bottom: 24px;
}

.btn-back {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 10px 18px;
  background: #EFF6FF;
  color: #2563EB;
  border: 1px solid #DBEAFE;
  border-radius: 8px;
  font-weight: 600;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.15s ease;
}

.btn-back:hover {
  background: #DBEAFE;
}

.sandbox-grid {
  display: grid;
  grid-template-columns: 1fr 1.3fr;
  gap: 24px;
  align-items: start;
}

@media (max-width: 768px) {
  .sandbox-grid {
    grid-template-columns: 1fr;
  }
}

/* CỘT TRÁI: ORDER SUMMARY */
.order-summary-card {
  background: #FFFFFF;
  border: 1px solid #E2E8F0;
  border-radius: 12px;
  padding: 24px;
}

.summary-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.summary-label {
  font-size: 13px;
  font-weight: 600;
  color: #64748B;
  text-transform: uppercase;
  letter-spacing: 0.04em;
}

.payment-id-tag {
  background: #F1F5F9;
  color: #334155;
  font-family: monospace;
  font-weight: 700;
  font-size: 12px;
  padding: 3px 8px;
  border-radius: 6px;
}

.plan-hero {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 16px;
  background: #F5F3FF;
  border: 1px solid #DDD6FE;
  border-radius: 10px;
  margin-bottom: 20px;
}

.plan-icon-wrap {
  width: 44px;
  height: 44px;
  background: #7C3AED;
  border-radius: 10px;
  display: grid;
  place-items: center;
  flex-shrink: 0;
}

.plan-name {
  margin: 0;
  font-size: 16px;
  font-weight: 700;
  color: #4C1D95;
}

.plan-sub {
  margin: 2px 0 0 0;
  font-size: 12px;
  color: #6D28D9;
}

.detail-rows {
  display: flex;
  flex-direction: column;
  gap: 14px;
  padding: 16px 0;
  border-top: 1px solid #F1F5F9;
  border-bottom: 1px solid #F1F5F9;
}

.detail-row {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.row-label {
  font-size: 13px;
  color: #64748B;
}

.row-value {
  font-size: 14px;
  color: #0F172A;
}

.amount-highlight {
  font-size: 22px;
  font-weight: 800;
  color: #2563EB;
}

.status-badge {
  display: inline-flex;
  align-items: center;
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 600;
  width: fit-content;
}

.status-badge.pending {
  background: #FEF3C7;
  color: #B45309;
}

.perks-list {
  list-style: none;
  padding: 0;
  margin: 6px 0 0 0;
  display: flex;
  flex-direction: column;
  gap: 6px;
  font-size: 13px;
  color: #334155;
}

.perks-list li {
  display: flex;
  align-items: center;
  gap: 8px;
}

.sandbox-notice-box {
  display: flex;
  gap: 10px;
  padding: 12px;
  background: #FFFBEB;
  border: 1px solid #FDE68A;
  border-radius: 8px;
  margin-top: 20px;
  font-size: 12px;
  line-height: 1.5;
  color: #92400E;
}

/* CỘT PHẢI: ACTION CARD */
.payment-action-card {
  background: #FFFFFF;
  border: 1px solid #E2E8F0;
  border-radius: 12px;
  padding: 24px;
}

.action-card-title {
  margin: 0 0 16px 0;
  font-size: 16px;
  font-weight: 700;
  color: #0F172A;
}

.methods-selection {
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin-bottom: 20px;
}

.method-option {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px;
  border: 2px solid #E2E8F0;
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.15s ease;
}

.method-option:hover {
  border-color: #CBD5E1;
}

.method-option.active {
  border-color: #2563EB;
  background: #EFF6FF;
}

.method-icon-wrap {
  width: 40px;
  height: 40px;
  border-radius: 8px;
  display: grid;
  place-items: center;
  flex-shrink: 0;
}

.method-icon-wrap.qr {
  background: #F1F5F9;
  color: #0F172A;
}

.method-icon-wrap.card {
  background: #EDE9FE;
  color: #6D28D9;
}

.method-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.method-info strong {
  font-size: 14px;
  color: #0F172A;
}

.method-info span {
  font-size: 12px;
  color: #64748B;
}

.method-preview-box {
  background: #F8FAFC;
  border: 1px solid #E2E8F0;
  border-radius: 10px;
  padding: 16px;
  margin-bottom: 24px;
  display: flex;
  gap: 20px;
  align-items: center;
}

.qr-mock-placeholder {
  background: #FFFFFF;
  padding: 12px;
  border: 1px solid #E2E8F0;
  border-radius: 8px;
  text-align: center;
}

.qr-caption {
  font-size: 11px;
  color: #64748B;
  margin: 6px 0 0 0;
}

.qr-mock-info {
  display: flex;
  flex-direction: column;
  gap: 8px;
  font-size: 13px;
  color: #334155;
}

.card-mock-form {
  width: 100%;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.mock-field {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.mock-label {
  font-size: 12px;
  color: #64748B;
}

.mock-val {
  font-family: monospace;
  font-size: 14px;
  font-weight: 700;
  color: #0F172A;
  background: #FFFFFF;
  padding: 6px 10px;
  border: 1px solid #E2E8F0;
  border-radius: 6px;
}

.mock-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}

.action-buttons-group {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.btn-confirm-success {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  height: 48px;
  background: #2563EB;
  color: #FFFFFF;
  border: 1px solid #1D4ED8;
  border-radius: 8px;
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.15s ease;
}

.btn-confirm-success:hover:not(:disabled) {
  background: #1D4ED8;
}

.btn-confirm-success:disabled {
  opacity: 0.7;
  cursor: wait;
}

.btn-simulate-cancel {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  height: 40px;
  background: #FFFFFF;
  color: #64748B;
  border: 1px solid #CBD5E1;
  border-radius: 8px;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.15s ease;
}

.btn-simulate-cancel:hover:not(:disabled) {
  background: #F1F5F9;
  color: #DC2626;
  border-color: #FCA5A5;
}

.security-footer {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 16px;
  font-size: 12px;
  color: #64748B;
}

.spinner-icon {
  animation: spin 1s linear infinite;
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}
</style>
