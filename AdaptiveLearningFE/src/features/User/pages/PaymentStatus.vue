<script setup>
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import {
  AlertCircle,
  AlertTriangle,
  ArrowLeft,
  ArrowRight,
  Check,
  CheckCircle2,
  CircleAlert,
  Clock3,
  CreditCard,
  Crown,
  Headphones,
  Info,
  LoaderCircle,
  RefreshCw,
  ShieldCheck,
  Sparkles,
  X,
  XCircle,
} from 'lucide-vue-next'
import { paymentService } from '@/services/paymentService'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'

const route = useRoute()
const router = useRouter()

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
    return new Intl.NumberFormat('vi-VN', {
      style: 'currency',
      currency: payment.value.currency,
    }).format(Number(payment.value.amount))
  } catch {
    return `${payment.value.amount} ${payment.value.currency}`
  }
}

function formatDate(dateStr) {
  if (!dateStr) return ''
  try {
    return new Intl.DateTimeFormat('vi-VN', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit',
    }).format(new Date(dateStr))
  } catch {
    return dateStr
  }
}

async function pollPayment() {
  const paymentId = Number(route.query.paymentId)
  if (!paymentId) {
    error.value = 'Không tìm thấy mã giao dịch thanh toán trong liên kết.'
    loading.value = false
    return
  }

  try {
    payment.value = await paymentService.status(paymentId)
  } catch (exception) {
    error.value =
      exception?.response?.data?.responseMessage || 'Không tải được trạng thái giao dịch từ máy chủ.'
    loading.value = false
    return
  }

  // Nếu giao dịch đã kết thúc (không còn PENDING)
  if (payment.value.status !== 'PENDING') {
    loading.value = false
    window.clearTimeout(pollTimer)

    // Khi thanh toán thành công, đồng bộ lại quyền lợi tài khoản
    if (payment.value.status === 'SUCCEEDED') {
      try {
        entitlements.value = await paymentService.entitlements()
      } catch {
        entitlements.value = null
      }
    }
    return
  }

  // Quá thời gian chờ 60 giây (Timeout)
  if (Date.now() - pollStartedAt >= 60000) {
    timedOut.value = true
    loading.value = false
    return
  }

  // Chu kỳ polling 2.5 giây (2500ms)
  pollTimer = window.setTimeout(pollPayment, 2500)
}

function retryPolling() {
  timedOut.value = false
  error.value = ''
  loading.value = true
  pollStartedAt = Date.now()
  pollPayment()
}

onMounted(() => {
  if (isCancel.value) {
    loading.value = false
    return
  }
  pollStartedAt = Date.now()
  pollPayment()
})

onUnmounted(() => {
  if (pollTimer) {
    window.clearTimeout(pollTimer)
  }
})
</script>

<template>
  <main class="payment-status-page">
    <!-- TOPBAR THƯƠNG HIỆU NHẸ -->
    <header class="payment-navbar">
      <div class="navbar-inner">
        <RouterLink to="/home" class="navbar-brand">
          <span class="brand-badge-icon">
            <Crown :size="15" />
          </span>
          <span class="brand-text">Adaptive Learning</span>
        </RouterLink>

        <span class="navbar-tag">Cổng thanh toán bảo mật</span>
      </div>
    </header>

    <div class="payment-status-wrapper">
      <section class="payment-status-card" aria-live="polite">
        <!-- ============================================== -->
        <!-- TRƯỜNG HỢP 1: NGƯỜI DÙNG HỦY GIAO DỊCH (/payment/cancel) -->
        <!-- ============================================== -->
        <template v-if="isCancel">
          <div class="status-icon-circle neutral-circle">
            <X :size="30" />
          </div>

          <p class="status-kicker neutral-kicker">GIAO DỊCH ĐÃ HỦY</p>
          <h1 class="status-title">Bạn đã hủy thanh toán</h1>
          <p class="status-description">
            Thao tác thanh toán đã dừng theo yêu cầu của bạn trên cổng đối tác. Thẻ của bạn chưa bị trừ tiền và gói học hiện tại được giữ nguyên.
          </p>

          <div class="info-callout neutral-callout">
            <Info :size="17" class="callout-icon" />
            <p>
              Bạn có thể quay lại trang Bảng giá bất cứ lúc nào để chọn lại phương thức thanh toán phù hợp.
            </p>
          </div>

          <div class="status-actions">
            <RouterLink to="/pricing" class="btn-primary-action">
              <span>Quay lại Bảng giá</span>
              <ArrowRight :size="16" />
            </RouterLink>
            <RouterLink to="/home" class="btn-secondary-action">
              <span>Về trang học tập</span>
            </RouterLink>
          </div>
        </template>

        <!-- ============================================== -->
        <!-- TRƯỜNG HỢP 2: LỖI KẾT NỐI HOẶC KHÔNG CÓ PAYMENT ID -->
        <!-- ============================================== -->
        <template v-else-if="error">
          <div class="status-icon-circle danger-circle">
            <AlertCircle :size="30" />
          </div>

          <p class="status-kicker danger-kicker">LỖI XÁC THỰC GIAO DỊCH</p>
          <h1 class="status-title">Chưa tải được thông tin thanh toán</h1>
          <p class="status-description">{{ error }}</p>

          <div class="info-callout danger-callout">
            <Info :size="17" class="callout-icon" />
            <p>
              Nếu bạn vừa thực hiện thanh toán, hãy kiểm tra lại email xác nhận từ Stripe hoặc PayPal hoặc liên hệ đội ngũ hỗ trợ để được kiểm tra tức thì.
            </p>
          </div>

          <div class="status-actions">
            <RouterLink to="/pricing" class="btn-primary-action">
              <span>Quay lại Bảng giá</span>
              <ArrowRight :size="16" />
            </RouterLink>
            <RouterLink to="/home" class="btn-secondary-action">
              <span>Về trang học tập</span>
            </RouterLink>
          </div>
        </template>

        <!-- ============================================== -->
        <!-- TRƯỜNG HỢP 3: THANH TOÁN THÀNH CÔNG (SUCCEEDED) -->
        <!-- ============================================== -->
        <template v-else-if="isSucceeded">
          <div class="status-icon-circle success-circle">
            <CheckCircle2 :size="32" />
          </div>

          <p class="status-kicker success-kicker">THANH TOÁN THÀNH CÔNG</p>
          <h1 class="status-title">Gói Premium đã được mở khóa!</h1>
          <p class="status-description">
            Chúc mừng bạn! Giao dịch đã được hệ thống xác nhận thành công và toàn bộ đặc quyền học tập đã sẵn sàng.
          </p>

          <!-- BIÊN NHẬN GIAO DỊCH THỰC TẾ -->
          <div class="receipt-card" v-if="payment">
            <div class="receipt-row">
              <span class="receipt-label">Mã giao dịch:</span>
              <strong class="receipt-value">#{{ payment.paymentId }}</strong>
            </div>
            <div class="receipt-row">
              <span class="receipt-label">Số tiền đã thanh toán:</span>
              <strong class="receipt-value highlight-amount">{{ amountLabel() }}</strong>
            </div>
            <div class="receipt-row">
              <span class="receipt-label">Cổng thanh toán:</span>
              <span class="receipt-value">{{ payment.provider }}</span>
            </div>
            <div class="receipt-row" v-if="payment.createdAt">
              <span class="receipt-label">Thời gian:</span>
              <span class="receipt-value">{{ formatDate(payment.createdAt) }}</span>
            </div>
            <div class="receipt-row">
              <span class="receipt-label">Gói kích hoạt:</span>
              <span class="receipt-value text-ai font-semibold">Premium · Vĩnh viễn</span>
            </div>
          </div>

          <!-- DANH SÁCH ĐẶC QUYỀN ĐÃ MỞ -->
          <div class="unlocked-features-box">
            <span class="unlocked-heading">Đặc quyền Premium của bạn:</span>
            <ul class="unlocked-list">
              <li>
                <Check :size="16" class="check-icon" />
                <span>Mở khóa toàn diện kỹ năng Nói (Speaking) & AI chấm điểm</span>
              </li>
              <li>
                <Check :size="16" class="check-icon" />
                <span>Trợ lý AI Tutor 24/7 giải thích đáp án & phân tích lỗi sai</span>
              </li>
              <li>
                <Check :size="16" class="check-icon" />
                <span>Không giới hạn số lượt tạo đề luyện tập thích ứng</span>
              </li>
            </ul>
          </div>

          <div class="status-actions">
            <RouterLink to="/home" class="btn-primary-action btn-success-action">
              <span>Bắt đầu học ngay với Premium</span>
              <ArrowRight :size="16" />
            </RouterLink>
            <RouterLink to="/pricing" class="btn-secondary-action">
              <span>Xem chi tiết gói học</span>
            </RouterLink>
          </div>
        </template>

        <!-- ============================================== -->
        <!-- TRƯỜNG HỢP 4: GIAO DỊCH THẤT BẠI (FAILED HOẶC REFUNDED) -->
        <!-- ============================================== -->
        <template v-else-if="isFailed">
          <div class="status-icon-circle danger-circle">
            <XCircle :size="30" />
          </div>

          <p class="status-kicker danger-kicker">THANH TOÁN CHƯA THÀNH CÔNG</p>
          <h1 class="status-title">Gói Premium chưa được mở khóa</h1>
          <p class="status-description">
            {{
              payment?.status === 'REFUNDED'
                ? 'Giao dịch này đã được hoàn tiền. Gói Premium đã được thu hồi tương ứng.'
                : 'Cổng thanh toán phản hồi giao dịch bị từ chối hoặc thẻ không đủ điều kiện thanh toán quốc tế.'
            }}
          </p>

          <div class="info-callout danger-callout">
            <Info :size="17" class="callout-icon" />
            <p>
              Vui lòng kiểm tra lại số dư thẻ, bật tính năng thanh toán trực tuyến hoặc thử lại bằng phương thức thanh toán khác trên trang Bảng giá.
            </p>
          </div>

          <div class="status-actions">
            <RouterLink to="/pricing" class="btn-primary-action">
              <span>Thử thanh toán lại</span>
              <ArrowRight :size="16" />
            </RouterLink>
            <RouterLink to="/home" class="btn-secondary-action">
              <span>Về trang học tập</span>
            </RouterLink>
          </div>
        </template>

        <!-- ============================================== -->
        <!-- TRƯỜNG HỢP 5: QUÁ THỜI GIAN CHỜ 60 GIÂY (TIMEOUT) -->
        <!-- ============================================== -->
        <template v-else-if="timedOut">
          <div class="status-icon-circle warning-circle">
            <Clock3 :size="30" />
          </div>

          <p class="status-kicker warning-kicker">GIAO DỊCH ĐANG CHỜ XÁC NHẬN</p>
          <h1 class="status-title">Hệ thống đang chờ phản hồi từ ngân hàng</h1>
          <p class="status-description">
            Đã quá thời gian kiểm tra tự động (60 giây), tuy nhiên giao dịch của bạn vẫn đang được ngân hàng hoặc cổng thanh toán xử lý.
          </p>

          <div class="info-callout warning-callout">
            <ShieldCheck :size="17" class="callout-icon" />
            <p>
              <strong>Yên tâm:</strong> Nếu tài khoản ngân hàng của bạn đã bị trừ tiền, gói Premium sẽ được tự động kích hoạt sau khi cổng thanh toán hoàn tất đối soát. Bạn không cần thanh toán lại.
            </p>
          </div>

          <div class="status-actions">
            <button type="button" class="btn-primary-action" @click="retryPolling">
              <RefreshCw :size="16" />
              <span>Kiểm tra lại trạng thái</span>
            </button>
            <RouterLink to="/home" class="btn-secondary-action">
              <span>Tiếp tục học trong khi chờ</span>
            </RouterLink>
          </div>
        </template>

        <!-- ============================================== -->
        <!-- TRƯỜNG HỢP 6: ĐANG KIỂM TRA POLLING (LOADING / PENDING) -->
        <!-- ============================================== -->
        <template v-else>
          <div class="status-icon-circle pending-circle">
            <LoaderCircle class="spinner-icon" :size="32" />
          </div>

          <p class="status-kicker pending-kicker">ĐANG XÁC NHẬN GIAO DỊCH</p>
          <h1 class="status-title">Đang đồng bộ trạng thái thanh toán...</h1>
          <p class="status-description">
            Cổng thanh toán đã phản hồi. Hệ thống đang kiểm tra chữ ký điện tử và cập nhật gói học của bạn (tự động sau mỗi 2.5 giây).
          </p>

          <div class="polling-indicator-box">
            <div class="polling-pulse-bar"></div>
            <p class="polling-hint">
              Vui lòng giữ nguyên màn hình hoặc bạn có thể quay lại học tập, hệ thống sẽ tự động kích hoạt khi có kết quả.
            </p>
          </div>

          <div class="status-actions">
            <RouterLink to="/home" class="btn-secondary-action">
              <span>Tiếp tục học trong khi chờ</span>
            </RouterLink>
          </div>
        </template>

        <!-- LIÊN HỆ HỖ TRỢ TRUNG THỰC -->
        <div class="support-footer">
          <p>
            Cần hỗ trợ về thanh toán? Liên hệ tổng đài <strong>1900 6868</strong> hoặc email
            <a href="mailto:hotro@adaptivelearning.edu.vn">hotro@adaptivelearning.edu.vn</a>
          </p>
        </div>
      </section>
    </div>
  </main>
</template>

<style scoped>
/* ==========================================================================
   PAYMENT STATUS PAGE - LIGHT AI EDUCATION THEME
   Tokens: Primary: #2563EB | Success: #16A34A | Warning: #D97706 | Error: #DC2626
   ========================================================================== */

.payment-status-page {
  min-height: 100vh;
  background: #F8FAFC;
  color: #0F172A;
  font-family: var(--font-sans);
  display: flex;
  flex-direction: column;
}

/* Navbar */
.payment-navbar {
  height: 60px;
  background: #FFFFFF;
  border-bottom: 1px solid #E2E8F0;
  display: flex;
  align-items: center;
}

.navbar-inner {
  width: 100%;
  max-width: 900px;
  margin: 0 auto;
  padding: 0 20px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.navbar-brand {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  text-decoration: none;
}

.brand-badge-icon {
  width: 28px;
  height: 28px;
  border-radius: 6px;
  background: #EFF6FF;
  color: #2563EB;
  display: grid;
  place-items: center;
}

.brand-text {
  font-size: 14px;
  font-weight: 700;
  color: #0F172A;
}

.navbar-tag {
  font-size: 12px;
  color: #64748B;
}

/* Wrapper */
.payment-status-wrapper {
  flex: 1;
  display: grid;
  place-items: center;
  padding: 32px 16px 56px;
}

.payment-status-card {
  width: 100%;
  max-width: 520px;
  background: #FFFFFF;
  border: 1px solid #E2E8F0;
  border-radius: 16px;
  padding: 36px clamp(20px, 4vw, 36px);
  text-align: center;
  box-shadow: 0 4px 24px rgba(15, 23, 42, 0.05);
  display: flex;
  flex-direction: column;
  align-items: center;
}

/* Status Icon Circle */
.status-icon-circle {
  width: 68px;
  height: 68px;
  border-radius: 50%;
  display: grid;
  place-items: center;
  margin-bottom: 18px;
  transition: transform 0.2s ease;
}

.success-circle {
  background: #F0FDF4;
  color: #16A34A;
  border: 2px solid #BBF7D0;
  box-shadow: 0 0 0 6px rgba(22, 163, 74, 0.08);
}

.pending-circle {
  background: #EFF6FF;
  color: #2563EB;
  border: 2px solid #BFDBFE;
  box-shadow: 0 0 0 6px rgba(37, 99, 235, 0.08);
}

.warning-circle {
  background: #FFFBEB;
  color: #D97706;
  border: 2px solid #FDE68A;
  box-shadow: 0 0 0 6px rgba(217, 119, 6, 0.08);
}

.danger-circle {
  background: #FEF2F2;
  color: #DC2626;
  border: 2px solid #FECACA;
  box-shadow: 0 0 0 6px rgba(220, 38, 38, 0.08);
}

.neutral-circle {
  background: #F1F5F9;
  color: #64748B;
  border: 2px solid #E2E8F0;
}

.spinner-icon {
  animation: spin 0.9s linear infinite;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

/* Kickers */
.status-kicker {
  margin: 0 0 6px 0;
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.06em;
  text-transform: uppercase;
}

.success-kicker {
  color: #16A34A;
}

.pending-kicker {
  color: #2563EB;
}

.warning-kicker {
  color: #D97706;
}

.danger-kicker {
  color: #DC2626;
}

.neutral-kicker {
  color: #64748B;
}

/* Typography */
.status-title {
  margin: 0;
  font-size: 22px;
  line-height: 1.3;
  font-weight: 700;
  color: #0F172A;
}

.status-description {
  margin: 8px 0 0 0;
  font-size: 13px;
  line-height: 1.6;
  color: #64748B;
  max-width: 440px;
}

/* Info Callout */
.info-callout {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  width: 100%;
  box-sizing: border-box;
  padding: 12px 14px;
  border-radius: 8px;
  margin-top: 20px;
  text-align: left;
  font-size: 12px;
  line-height: 1.5;
}

.info-callout p {
  margin: 0;
}

.callout-icon {
  flex-shrink: 0;
  margin-top: 1px;
}

.neutral-callout {
  background: #F8FAFC;
  border: 1px solid #E2E8F0;
  color: #475569;
}

.warning-callout {
  background: #FFFBEB;
  border: 1px solid #FDE68A;
  color: #92400E;
}

.danger-callout {
  background: #FEF2F2;
  border: 1px solid #FECACA;
  color: #991B1B;
}

/* Receipt Card (Thành công) */
.receipt-card {
  width: 100%;
  box-sizing: border-box;
  margin-top: 20px;
  padding: 14px 16px;
  border-radius: 10px;
  background: #F8FAFC;
  border: 1px solid #E2E8F0;
  display: flex;
  flex-direction: column;
  gap: 9px;
  text-align: left;
}

.receipt-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 12px;
}

.receipt-label {
  color: #64748B;
}

.receipt-value {
  color: #0F172A;
  font-variant-numeric: tabular-nums;
}

.highlight-amount {
  font-size: 14px;
  font-weight: 700;
  color: #16A34A;
}

.text-ai {
  color: #7C3AED;
}

.font-semibold {
  font-weight: 600;
}

/* Unlocked Features Box */
.unlocked-features-box {
  width: 100%;
  box-sizing: border-box;
  margin-top: 14px;
  padding: 14px 16px;
  border-radius: 10px;
  background: #F5F3FF;
  border: 1px solid #DDD6FE;
  text-align: left;
}

.unlocked-heading {
  display: block;
  font-size: 11px;
  font-weight: 700;
  color: #7C3AED;
  text-transform: uppercase;
  letter-spacing: 0.04em;
  margin-bottom: 8px;
}

.unlocked-list {
  list-style: none;
  padding: 0;
  margin: 0;
  display: flex;
  flex-direction: column;
  gap: 7px;
}

.unlocked-list li {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  color: #334155;
}

.check-icon {
  color: #7C3AED;
  flex-shrink: 0;
}

/* Polling Indicator Box */
.polling-indicator-box {
  width: 100%;
  box-sizing: border-box;
  margin-top: 22px;
  padding: 14px;
  border-radius: 8px;
  background: #F8FAFC;
  border: 1px solid #E2E8F0;
}

.polling-pulse-bar {
  height: 4px;
  background: #E2E8F0;
  border-radius: 2px;
  overflow: hidden;
  position: relative;
}

.polling-pulse-bar::after {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  height: 100%;
  width: 40%;
  background: #2563EB;
  border-radius: 2px;
  animation: pulse-move 1.5s ease-in-out infinite alternate;
}

@keyframes pulse-move {
  0% {
    left: 0%;
  }
  100% {
    left: 60%;
  }
}

.polling-hint {
  margin: 10px 0 0 0;
  font-size: 11px;
  color: #64748B;
  line-height: 1.5;
}

/* Actions */
.status-actions {
  width: 100%;
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin-top: 24px;
}

.btn-primary-action {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  width: 100%;
  height: 44px;
  border-radius: 8px;
  border: 0;
  background: #2563EB;
  color: #FFFFFF;
  font-size: 13px;
  font-weight: 600;
  text-decoration: none;
  cursor: pointer;
  transition: background 0.15s ease;
}

.btn-primary-action:hover {
  background: #1D4ED8;
}

.btn-success-action {
  background: #16A34A;
}

.btn-success-action:hover {
  background: #15803D;
}

.btn-secondary-action {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  width: 100%;
  height: 42px;
  border-radius: 8px;
  border: 1px solid #CBD5E1;
  background: #FFFFFF;
  color: #334155;
  font-size: 13px;
  font-weight: 600;
  text-decoration: none;
  cursor: pointer;
  transition: all 0.15s ease;
}

.btn-secondary-action:hover {
  background: #F1F5F9;
  border-color: #94A3B8;
}

/* Support footer */
.support-footer {
  margin-top: 24px;
  font-size: 11px;
  color: #94A3B8;
  line-height: 1.5;
}

.support-footer p {
  margin: 0;
}

.support-footer a {
  color: #2563EB;
  text-decoration: none;
}

.support-footer a:hover {
  text-decoration: underline;
}

/* Responsive */
@media (max-width: 480px) {
  .payment-status-card {
    padding: 28px 16px;
  }

  .status-title {
    font-size: 20px;
  }
}
</style>
