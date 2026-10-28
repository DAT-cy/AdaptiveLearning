<script setup>
import { computed, onMounted, ref } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import {
  AlertCircle,
  AlertTriangle,
  ArrowLeft,
  ArrowRight,
  BarChart3,
  Check,
  CheckCircle2,
  Clock,
  CreditCard,
  Crown,
  HelpCircle,
  Info,
  LoaderCircle,
  Lock,
  LockKeyhole,
  MessageSquare,
  ShieldCheck,
  Sparkles,
  X,
  Zap,
} from 'lucide-vue-next'
import { paymentService } from '@/services/paymentService'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'

const router = useRouter()
const route = useRoute()

const entitlements = ref(null)
const loading = ref(true)
const checkoutProvider = ref('')
const error = ref('')

// Xử lý lý do bị chuyển hướng từ Axios interceptor (Paywall / Quota)
const redirectReason = ref(route.query.reason || '')

function dismissReason() {
  redirectReason.value = ''
  // Dọn dẹp query param mà không reload trang
  router.replace({ path: '/pricing', query: {} })
}

// Quyền lợi thực tế
const freeFeatures = [
  'Nghe, Đọc và Viết (3 kỹ năng nền tảng)',
  'Tối đa 5 lượt tạo đề luyện tập AI',
  'Xem lại đề thi và kết quả đã lưu',
  'Đánh giá tiến độ học tập cơ bản',
]

const premiumFeatures = [
  'Đầy đủ 4 kỹ năng: Nghe, Đọc, Viết và Nói (Speaking)',
  'Trợ lý AI học tập 24/7 giải thích đáp án & lỗi sai',
  'Không giới hạn lượt tạo đề luyện tập thích ứng',
  'Phân tích năng lực chuyên sâu theo mô hình BKT',
  'Thanh toán một lần duy nhất · Mở khóa vĩnh viễn',
]

// Bảng so sánh chi tiết tính năng
const comparisonFeatures = [
  { name: 'Kỹ năng Nghe (Listening)', free: true, premium: true, note: 'Luyện tập theo chủ đề' },
  { name: 'Kỹ năng Đọc (Reading)', free: true, premium: true, note: 'Bài đọc hiểu phân cấp' },
  { name: 'Kỹ năng Viết (Writing)', free: true, premium: true, note: 'Tự luận & trắc nghiệm' },
  { name: 'Kỹ năng Nói (Speaking)', free: false, premium: true, note: 'Thu âm & AI đánh giá phát âm' },
  { name: 'Số lượt tạo đề thi AI', free: 'Tối đa 5 lượt', premium: 'Không giới hạn', note: 'Đề thi cá nhân hóa thích ứng' },
  { name: 'Trợ lý AI Tutor 24/7', free: false, premium: true, note: 'Hỏi đáp & phân tích lỗi sai tức thì' },
  { name: 'Phân tích năng lực BKT', free: 'Cơ bản', premium: 'Chuyên sâu 4 kỹ năng', note: 'Mô hình Bayesian Knowledge Tracing' },
  { name: 'Thời hạn gói học', free: 'Miễn phí trọn đời', premium: 'Thanh toán 1 lần · Vĩnh viễn', note: 'Không thu phí gia hạn định kỳ' },
]

const availableMethods = computed(() => entitlements.value?.paymentMethods || [])

function money(value, currency) {
  if (value == null || !currency) return 'Chưa cấu hình giá'
  try {
    return new Intl.NumberFormat('vi-VN', { style: 'currency', currency }).format(Number(value))
  } catch {
    return `${value} ${currency}`
  }
}

function messageFor(exception) {
  const code = exception?.response?.data?.responseCode
  if (code === 'PAYMENT-ERR-503') return 'Thanh toán chưa được cấu hình trên máy chủ. Vui lòng thử lại sau.'
  if (code === 'PAYMENT-ERR-504') return 'Cổng thanh toán chưa phản hồi. Không phát sinh giao dịch, hãy thử lại.'
  if (code === 'PAYMENT-ERR-409') return 'Tài khoản của bạn đã được nâng cấp gói Premium từ trước.'
  return exception?.response?.data?.responseMessage || 'Không thể khởi tạo thanh toán. Vui lòng thử lại.'
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
    if (result?.redirectUrl) {
      window.location.assign(result.redirectUrl)
    } else {
      throw new Error('Không nhận được liên kết thanh toán từ máy chủ.')
    }
  } catch (exception) {
    error.value = messageFor(exception)
    checkoutProvider.value = ''
  }
}

onMounted(load)
</script>

<template>
  <main class="pricing-page">
    <!-- HEADER ĐẦU TRANG -->
    <header class="pricing-topbar">
      <div class="topbar-inner">
        <RouterLink to="/home" class="topbar-back-btn">
          <ArrowLeft :size="16" />
          <span>Về trang học tập</span>
        </RouterLink>

        <div class="topbar-brand">
          <span class="brand-badge-icon">
            <Crown :size="16" />
          </span>
          <span class="brand-name">Adaptive Learning</span>
        </div>

        <div class="topbar-status">
          <span v-if="entitlements" class="current-plan-badge" :class="{ 'is-premium': entitlements.plan === 'PREMIUM' }">
            <span class="status-dot"></span>
            Đang dùng: <strong>{{ entitlements.plan === 'PREMIUM' ? 'Gói Premium' : 'Gói Miễn phí' }}</strong>
          </span>
        </div>
      </div>
    </header>

    <!-- CONTAINER NỘI DUNG CHÍNH -->
    <div class="pricing-container">
      <!-- THÔNG BÁO PAYWALL / QUOTA (REDIRECT TỪ AXIOS INTERCEPTOR) -->
      <div
        v-if="redirectReason === 'PREMIUM_REQUIRED'"
        class="paywall-banner banner-warning"
        role="alert"
      >
        <Sparkles :size="20" class="banner-icon-svg text-ai" />
        <div class="banner-content">
          <h2 class="banner-title">Yêu cầu nâng cấp gói Premium</h2>
          <p class="banner-desc">
            Tính năng bạn vừa chọn (Kỹ năng Nói hoặc Trợ lý AI Tutor) chỉ dành riêng cho tài khoản Premium. Hãy nâng cấp để tiếp tục sử dụng không giới hạn.
          </p>
        </div>
        <button class="banner-close-btn" aria-label="Đóng thông báo" @click="dismissReason">
          <X :size="16" />
        </button>
      </div>

      <div
        v-else-if="redirectReason === 'FREE_LIMIT_REACHED'"
        class="paywall-banner banner-warning"
        role="alert"
      >
        <AlertCircle :size="20" class="banner-icon-svg text-amber" />
        <div class="banner-content">
          <h2 class="banner-title">Đã hết hạn ngạch tạo đề miễn phí</h2>
          <p class="banner-desc">
            Tài khoản Miễn phí của bạn đã sử dụng hết số lượt tạo đề luyện tập AI. Nâng cấp Premium để mở khóa không giới hạn lượt tạo đề và bài thi.
          </p>
        </div>
        <button class="banner-close-btn" aria-label="Đóng thông báo" @click="dismissReason">
          <X :size="16" />
        </button>
      </div>

      <!-- TIÊU ĐỀ KHU VỰC BẢNG GIÁ -->
      <section class="pricing-hero-header">
        <p class="hero-kicker">
          <Sparkles :size="13" class="kicker-icon" />
          BẢNG GIÁ & NÂNG CẤP
        </p>
        <h1 class="hero-title">Chọn gói học tập phù hợp với mục tiêu của bạn</h1>
        <p class="hero-subtitle">
          Luyện tập đầy đủ 4 kỹ năng ngôn ngữ với sự hỗ trợ từ Trợ lý AI thông minh và mô hình thích ứng cá nhân hóa.
        </p>
      </section>

      <!-- TRẠNG THÁI ĐANG TẢI (LOADING SKELETON) -->
      <div v-if="loading" class="pricing-loading-box" role="status" aria-busy="true">
        <LoaderCircle class="spinner-icon" :size="28" />
        <p>Đang tải thông tin gói học và bảng giá thực tế từ hệ thống…</p>
      </div>

      <!-- LỖI KẾT NỐI API -->
      <div v-else-if="error && !entitlements" class="pricing-error-box" role="alert">
        <AlertCircle :size="24" class="text-danger" />
        <div>
          <h3>Không thể tải thông tin gói học</h3>
          <p>{{ error }}</p>
        </div>
        <Button variant="outline" size="sm" @click="load">Thử lại</Button>
      </div>

      <!-- NỘI DUNG CHÍNH (CARDS SO SÁNH GÓI) -->
      <div v-else class="pricing-main-section">
        <div class="pricing-cards-grid">
          <!-- THẺ 1: GÓI CƠ BẢN (FREE) -->
          <section class="pricing-card free-card" aria-labelledby="free-plan-title">
            <div class="card-top">
              <div class="plan-type-row">
                <span class="plan-category-tag">KHỞI ĐẦU</span>
                <Badge :variant="entitlements?.plan === 'FREE' ? 'default' : 'secondary'">
                  {{ entitlements?.plan === 'FREE' ? 'Gói hiện tại' : 'Miễn phí' }}
                </Badge>
              </div>

              <h2 id="free-plan-title" class="plan-name">Gói Miễn phí</h2>
              <p class="plan-desc">Nền tảng thiết yếu để bắt đầu trải nghiệm và ôn luyện kiến thức.</p>

              <div class="price-block">
                <span class="price-number">0 đ</span>
                <span class="price-period">/ miễn phí trọn đời</span>
              </div>
            </div>

            <!-- Hạn ngạch tài khoản Free -->
            <div v-if="entitlements && entitlements.plan === 'FREE'" class="quota-progress-card">
              <div class="quota-header-row">
                <span class="quota-label">Lượt tạo đề AI còn lại:</span>
                <strong class="quota-count">
                  {{ entitlements.examsRemaining }} / {{ entitlements.examLimit }}
                </strong>
              </div>
              <div
                class="quota-track"
                role="progressbar"
                :aria-valuenow="entitlements.examsUsed"
                :aria-valuemax="entitlements.examLimit"
                aria-label="Lượt tạo đề đã sử dụng"
              >
                <div
                  class="quota-fill"
                  :style="{
                    width: `${Math.min(100, Math.round(((entitlements.examsUsed || 0) / Math.max(1, entitlements.examLimit || 5)) * 100))}%`
                  }"
                ></div>
              </div>
              <p class="quota-hint">Đã dùng {{ entitlements.examsUsed }} lượt tạo đề luyện tập.</p>
            </div>

            <div class="features-block">
              <span class="features-heading">Quyền lợi bao gồm:</span>
              <ul class="features-list">
                <li v-for="feat in freeFeatures" :key="feat">
                  <Check :size="16" class="check-icon" />
                  <span>{{ feat }}</span>
                </li>
                <li class="locked-feature">
                  <LockKeyhole :size="16" class="lock-icon" />
                  <span>Kỹ năng Nói (Speaking)</span>
                </li>
                <li class="locked-feature">
                  <LockKeyhole :size="16" class="lock-icon" />
                  <span>Trợ lý AI Tutor giải thích bài thi</span>
                </li>
              </ul>
            </div>

            <div class="card-footer">
              <Button
                v-if="entitlements?.plan === 'FREE'"
                variant="outline"
                class="btn-plan-status"
                disabled
              >
                <Check :size="15" />
                <span>Bạn đang sử dụng gói này</span>
              </Button>
              <RouterLink v-else to="/home" class="btn-return-link">
                Quay lại học tập
              </RouterLink>
            </div>
          </section>

          <!-- THẺ 2: GÓI CHUYÊN NGHIỆP (PREMIUM) -->
          <section class="pricing-card premium-card" aria-labelledby="premium-plan-title">
            <div class="premium-ribbon">
              <Sparkles :size="13" />
              <span>ĐƯỢC ĐỀ XUẤT CHO MỤC TIÊU CAO</span>
            </div>

            <div class="card-top">
              <div class="plan-type-row">
                <span class="plan-category-tag ai-tag">CHUYÊN NGHIỆP</span>
                <Badge variant="ai" class="premium-badge-tag">
                  <Crown :size="13" class="mr-1" />
                  Mở khóa vĩnh viễn
                </Badge>
              </div>

              <h2 id="premium-plan-title" class="plan-name">Gói Premium</h2>
              <p class="plan-desc">Mở khóa toàn bộ 4 kỹ năng và Trợ lý AI đồng hành xuyên suốt lộ trình.</p>

              <div class="price-block">
                <span class="price-number text-premium">
                  {{ money(entitlements?.premiumPrice, entitlements?.premiumCurrency) }}
                </span>
                <span class="price-period">/ thanh toán 1 lần duy nhất</span>
              </div>
            </div>

            <div class="features-block">
              <span class="features-heading">Mọi quyền lợi của Free, cộng thêm:</span>
              <ul class="features-list premium-list">
                <li v-for="feat in premiumFeatures" :key="feat">
                  <Check :size="16" class="check-icon-purple" />
                  <span class="font-medium">{{ feat }}</span>
                </li>
              </ul>
            </div>

            <!-- Báo lỗi thanh toán cục bộ nếu có -->
            <div v-if="error" class="card-error-alert" role="alert">
              <AlertCircle :size="16" />
              <span>{{ error }}</span>
            </div>

            <!-- Khối hành động thanh toán / Trạng thái gói -->
            <div class="card-footer">
              <!-- Trường hợp 1: Tài khoản đã có Premium -->
              <div v-if="entitlements?.plan === 'PREMIUM'" class="premium-active-box">
                <div class="active-badge-row">
                  <CheckCircle2 :size="18" class="text-success" />
                  <strong>Tài khoản của bạn đã sở hữu gói Premium</strong>
                </div>
                <p>Toàn bộ kỹ năng và Trợ lý AI đang được kích hoạt vĩnh viễn cho bạn.</p>
                <RouterLink to="/home" class="btn-primary-study">
                  Về trang học tập
                  <ArrowRight :size="16" />
                </RouterLink>
              </div>

              <!-- Trường hợp 2: Cổng thanh toán chưa khả dụng -->
              <div v-else-if="!entitlements?.premiumAvailable" class="payment-unavailable-box">
                <Info :size="18" class="text-secondary" />
                <p>Cổng thanh toán trực tuyến đang được bảo trì hoặc chưa cấu hình. Vui lòng liên hệ hỗ trợ hoặc quay lại sau.</p>
              </div>

              <!-- Trường hợp 3: Sẵn sàng thanh toán qua cổng -->
              <div v-else class="payment-buttons-stack">
                <p class="payment-choose-label">Chọn cổng thanh toán bảo mật:</p>

                <!-- Nút Stripe -->
                <button
                  v-if="availableMethods.includes('STRIPE')"
                  type="button"
                  class="checkout-btn stripe-btn"
                  :disabled="!!checkoutProvider"
                  @click="checkout('stripe')"
                >
                  <LoaderCircle v-if="checkoutProvider === 'stripe'" class="spinner-icon" :size="18" />
                  <template v-else>
                    <span class="provider-mark stripe-logo">S</span>
                    <span class="provider-label">Thanh toán với Stripe (Thẻ quốc tế)</span>
                    <ArrowRight :size="16" class="ml-auto" />
                  </template>
                </button>

                <!-- Nút PayPal -->
                <button
                  v-if="availableMethods.includes('PAYPAL')"
                  type="button"
                  class="checkout-btn paypal-btn"
                  :disabled="!!checkoutProvider"
                  @click="checkout('paypal')"
                >
                  <LoaderCircle v-if="checkoutProvider === 'paypal'" class="spinner-icon" :size="18" />
                  <template v-else>
                    <span class="provider-mark paypal-logo">P</span>
                    <span class="provider-label">Thanh toán với PayPal</span>
                    <ArrowRight :size="16" class="ml-auto" />
                  </template>
                </button>

                <div class="security-guarantee">
                  <ShieldCheck :size="15" class="text-success" />
                  <span>Giao dịch bảo mật chuẩn mã hóa SSL/TLS. Không lưu thông tin thẻ.</span>
                </div>
              </div>
            </div>
          </section>
        </div>

        <!-- BẢNG SO SÁNH CHI TIẾT TÍNH NĂNG (FEATURE COMPARISON MATRIX) -->
        <section class="comparison-matrix-card" aria-labelledby="matrix-title">
          <div class="matrix-header">
            <h2 id="matrix-title" class="matrix-title">So sánh chi tiết tính năng</h2>
            <p class="matrix-desc">Minh bạch quyền lợi giữa gói Miễn phí và gói Chuyên nghiệp Premium.</p>
          </div>

          <div class="matrix-table-wrap">
            <table class="matrix-table">
              <thead>
                <tr>
                  <th scope="col" class="th-feature">Tính năng & Quyền lợi</th>
                  <th scope="col" class="th-plan">Gói Miễn phí</th>
                  <th scope="col" class="th-plan th-premium">Gói Premium</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="item in comparisonFeatures" :key="item.name">
                  <td class="td-feature">
                    <strong class="feature-title">{{ item.name }}</strong>
                    <span class="feature-note">{{ item.note }}</span>
                  </td>
                  <td class="td-plan">
                    <template v-if="typeof item.free === 'boolean'">
                      <Check v-if="item.free" :size="18" class="text-success" />
                      <Lock v-else :size="16" class="text-muted" />
                    </template>
                    <span v-else class="text-badge">{{ item.free }}</span>
                  </td>
                  <td class="td-plan td-premium">
                    <template v-if="typeof item.premium === 'boolean'">
                      <Check v-if="item.premium" :size="18" class="text-ai" />
                      <Lock v-else :size="16" class="text-muted" />
                    </template>
                    <span v-else class="text-badge text-ai font-semibold">{{ item.premium }}</span>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </section>

        <!-- KHU VỰC CÂU HỎI THƯỜNG GẶP (FAQ) -->
        <section class="pricing-faq-section" aria-labelledby="faq-title">
          <div class="faq-header">
            <HelpCircle :size="20" class="text-blue" />
            <h2 id="faq-title" class="faq-title">Câu hỏi thường gặp về gói Premium</h2>
          </div>

          <div class="faq-grid">
            <div class="faq-item">
              <h3 class="faq-question">Gói Premium có tự động gia hạn trừ tiền hàng tháng không?</h3>
              <p class="faq-answer">
                Không. Gói Premium của Adaptive Learning áp dụng hình thức thanh toán một lần duy nhất. Bạn mở khóa vĩnh viễn và không phát sinh bất kỳ khoản phí gia hạn định kỳ nào.
              </p>
            </div>

            <div class="faq-item">
              <h3 class="faq-question">Sau khi thanh toán xong, tài khoản được kích hoạt trong bao lâu?</h3>
              <p class="faq-answer">
                Hệ thống tự động đồng bộ qua Webhook ngay khi Stripe hoặc PayPal phản hồi giao dịch thành công (thường chỉ mất từ 3 đến 5 giây). Bạn có thể quay lại trang học tập để sử dụng ngay.
              </p>
            </div>

            <div class="faq-item">
              <h3 class="faq-question">Tôi có thể thanh toán qua các phương thức nào?</h3>
              <p class="faq-answer">
                Hệ thống hỗ trợ thẻ quốc tế (Visa, MasterCard, JCB...) thông qua cổng thanh toán bảo mật Stripe và tài khoản ví điện tử quốc tế PayPal.
              </p>
            </div>

            <div class="faq-item">
              <h3 class="faq-question">Nếu gặp sự cố trong quá trình giao dịch thì liên hệ ai?</h3>
              <p class="faq-answer">
                Đội ngũ hỗ trợ của chúng tôi luôn sẵn sàng hỗ trợ qua tổng đài 1900 6868 hoặc gửi email đến hotro@adaptivelearning.edu.vn kèm mã giao dịch để được kiểm tra ngay lập tức.
              </p>
            </div>
          </div>
        </section>
      </div>

      <!-- FOOTNOTE BẢO MẬT & BẢN QUYỀN -->
      <footer class="pricing-footer">
        <p>Premium là khoản thanh toán một lần. Toàn bộ tính năng cao cấp sẽ được kích hoạt ngay sau khi hệ thống ghi nhận giao dịch thành công.</p>
        <p class="footer-sub">© 2026 Adaptive Learning. Hệ thống học tập thông minh định hướng Light AI Education.</p>
      </footer>
    </div>
  </main>
</template>

<style scoped>
/* ==========================================================================
   PRICING & UPGRADE PAGE - LIGHT AI EDUCATION SYSTEM
   Colors: Primary: #2563EB | AI Accent: #7C3AED | Neutral: #0F172A | Border: #E2E8F0
   ========================================================================== */

.pricing-page {
  min-height: 100vh;
  background: #F8FAFC;
  color: #0F172A;
  font-family: var(--font-sans);
  display: flex;
  flex-direction: column;
}

/* Header Topbar */
.pricing-topbar {
  height: 64px;
  background: #FFFFFF;
  border-bottom: 1px solid #E2E8F0;
  display: flex;
  align-items: center;
  position: sticky;
  top: 0;
  z-index: 40;
}

.topbar-inner {
  width: 100%;
  max-width: 1100px;
  margin: 0 auto;
  padding: 0 24px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.topbar-back-btn {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  font-weight: 600;
  color: #64748B;
  text-decoration: none;
  transition: color 0.15s ease;
}

.topbar-back-btn:hover {
  color: #2563EB;
}

.topbar-brand {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  font-size: 15px;
  font-weight: 700;
  color: #0F172A;
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

.current-plan-badge {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 12px;
  border-radius: 999px;
  background: #F1F5F9;
  border: 1px solid #E2E8F0;
  font-size: 12px;
  color: #475569;
}

.current-plan-badge.is-premium {
  background: #F5F3FF;
  border-color: #DDD6FE;
  color: #7C3AED;
}

.status-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #16A34A;
}

/* Container */
.pricing-container {
  width: 100%;
  max-width: 1100px;
  margin: 0 auto;
  padding: 40px 24px 64px;
  display: flex;
  flex-direction: column;
  gap: 32px;
}

/* Paywall / Quota Alert Banner */
.paywall-banner {
  display: flex;
  align-items: flex-start;
  gap: 14px;
  padding: 16px 20px;
  border-radius: 10px;
  background: #FFFBEB;
  border: 1px solid #FDE68A;
  color: #92400E;
}

.banner-icon-svg {
  flex-shrink: 0;
  margin-top: 2px;
}

.text-ai {
  color: #7C3AED;
}

.text-amber {
  color: #D97706;
}

.text-blue {
  color: #2563EB;
}

.banner-content {
  flex: 1;
}

.banner-title {
  margin: 0;
  font-size: 14px;
  font-weight: 700;
  color: #78350F;
}

.banner-desc {
  margin: 4px 0 0 0;
  font-size: 13px;
  line-height: 1.5;
  color: #92400E;
}

.banner-close-btn {
  border: 0;
  background: transparent;
  color: #B45309;
  cursor: pointer;
  padding: 4px;
  border-radius: 4px;
}

.banner-close-btn:hover {
  background: rgba(180, 83, 9, 0.1);
}

/* Hero Section */
.pricing-hero-header {
  text-align: center;
  max-width: 720px;
  margin: 0 auto;
}

.hero-kicker {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  margin: 0 0 8px 0;
  font-size: 12px;
  font-weight: 700;
  color: #2563EB;
  letter-spacing: 0.06em;
  text-transform: uppercase;
}

.hero-title {
  margin: 0;
  font-size: 32px;
  line-height: 1.25;
  font-weight: 800;
  color: #0F172A;
}

.hero-subtitle {
  margin: 12px 0 0 0;
  font-size: 15px;
  line-height: 1.6;
  color: #64748B;
}

/* Loading & Error */
.pricing-loading-box {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 280px;
  gap: 14px;
  color: #64748B;
  font-size: 14px;
}

.spinner-icon {
  animation: spin 0.9s linear infinite;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

.pricing-error-box {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 20px;
  border-radius: 10px;
  background: #FEF2F2;
  border: 1px solid #FECACA;
  color: #991B1B;
}

/* Cards Grid */
.pricing-cards-grid {
  display: grid;
  grid-template-columns: 1fr 1.22fr;
  gap: 24px;
  align-items: stretch;
}

.pricing-card {
  background: #FFFFFF;
  border: 1px solid #E2E8F0;
  border-radius: 16px;
  padding: 32px;
  display: flex;
  flex-direction: column;
  box-shadow: 0 1px 3px rgba(15, 23, 42, 0.04);
  position: relative;
}

/* Thẻ Premium nổi bật */
.pricing-card.premium-card {
  border-color: #DDD6FE;
  box-shadow: 0 8px 30px rgba(124, 58, 237, 0.08);
}

.premium-ribbon {
  position: absolute;
  top: -12px;
  left: 50%;
  transform: translateX(-50%);
  background: linear-gradient(135deg, #7C3AED 0%, #6D28D9 100%);
  color: #FFFFFF;
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.04em;
  padding: 4px 14px;
  border-radius: 999px;
  display: inline-flex;
  align-items: center;
  gap: 5px;
  box-shadow: 0 2px 8px rgba(109, 40, 217, 0.3);
  white-space: nowrap;
}

.plan-type-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.plan-category-tag {
  font-size: 11px;
  font-weight: 700;
  color: #64748B;
  letter-spacing: 0.05em;
}

.plan-category-tag.ai-tag {
  color: #7C3AED;
}

.premium-badge-tag {
  background: #F5F3FF;
  color: #7C3AED;
  border: 1px solid #DDD6FE;
}

.plan-name {
  margin: 0;
  font-size: 22px;
  font-weight: 700;
  color: #0F172A;
}

.plan-desc {
  margin: 6px 0 20px 0;
  font-size: 13px;
  line-height: 1.5;
  color: #64748B;
  min-height: 40px;
}

.price-block {
  display: flex;
  align-items: baseline;
  gap: 8px;
  padding-bottom: 20px;
  border-bottom: 1px solid #E2E8F0;
}

.price-number {
  font-size: 32px;
  font-weight: 800;
  color: #0F172A;
  font-variant-numeric: tabular-nums;
}

.price-number.text-premium {
  color: #7C3AED;
  font-size: 28px;
}

.price-period {
  font-size: 13px;
  color: #64748B;
}

/* Quota Progress Card */
.quota-progress-card {
  margin: 20px 0;
  padding: 14px;
  background: #F8FAFC;
  border: 1px solid #E2E8F0;
  border-radius: 8px;
}

.quota-header-row {
  display: flex;
  justify-content: space-between;
  font-size: 12px;
  margin-bottom: 6px;
}

.quota-label {
  color: #64748B;
}

.quota-count {
  color: #0F172A;
  font-variant-numeric: tabular-nums;
}

.quota-track {
  height: 6px;
  background: #E2E8F0;
  border-radius: 3px;
  overflow: hidden;
}

.quota-fill {
  height: 100%;
  background: #2563EB;
  border-radius: 3px;
  transition: width 0.3s ease;
}

.quota-hint {
  margin: 6px 0 0 0;
  font-size: 11px;
  color: #94A3B8;
}

/* Features Block */
.features-block {
  margin: 24px 0;
  flex: 1;
}

.features-heading {
  display: block;
  font-size: 12px;
  font-weight: 700;
  color: #475569;
  text-transform: uppercase;
  letter-spacing: 0.04em;
  margin-bottom: 14px;
}

.features-list {
  list-style: none;
  padding: 0;
  margin: 0;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.features-list li {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  font-size: 13px;
  color: #334155;
  line-height: 1.5;
}

.check-icon {
  color: #2563EB;
  flex-shrink: 0;
  margin-top: 2px;
}

.check-icon-purple {
  color: #7C3AED;
  flex-shrink: 0;
  margin-top: 2px;
}

.locked-feature {
  color: #94A3B8;
}

.lock-icon {
  color: #94A3B8;
  flex-shrink: 0;
  margin-top: 2px;
}

.font-medium {
  font-weight: 500;
}

/* Card Footer */
.card-footer {
  margin-top: auto;
  padding-top: 16px;
}

.btn-plan-status {
  width: 100%;
  height: 46px;
  font-size: 13px;
  background: #F8FAFC;
  color: #64748B;
  cursor: default;
}

.btn-return-link {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 46px;
  border: 1px solid #CBD5E1;
  border-radius: 8px;
  color: #334155;
  text-decoration: none;
  font-size: 13px;
  font-weight: 600;
  transition: all 0.15s ease;
}

.btn-return-link:hover {
  background: #F1F5F9;
}

/* Premium Active Box */
.premium-active-box {
  padding: 16px;
  border-radius: 10px;
  background: #F0FDF4;
  border: 1px solid #BBF7D0;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.active-badge-row {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #166534;
  font-size: 13px;
}

.premium-active-box p {
  margin: 0;
  font-size: 12px;
  color: #15803D;
  line-height: 1.5;
}

.btn-primary-study {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  margin-top: 4px;
  height: 42px;
  padding: 0 16px;
  border-radius: 8px;
  background: #16A34A;
  color: #FFFFFF;
  text-decoration: none;
  font-size: 13px;
  font-weight: 600;
  transition: background 0.15s ease;
}

.btn-primary-study:hover {
  background: #15803D;
}

/* Payment Unavailable Box */
.payment-unavailable-box {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding: 14px;
  border-radius: 8px;
  background: #F8FAFC;
  border: 1px solid #E2E8F0;
  font-size: 13px;
  color: #64748B;
  line-height: 1.5;
}

/* Payment Buttons Stack */
.payment-buttons-stack {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.payment-choose-label {
  margin: 0 0 2px 0;
  font-size: 12px;
  font-weight: 600;
  color: #475569;
}

.checkout-btn {
  display: flex;
  align-items: center;
  gap: 12px;
  height: 48px;
  padding: 0 16px;
  border-radius: 8px;
  border: 1px solid transparent;
  color: #FFFFFF;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.15s ease;
}

.checkout-btn:disabled {
  opacity: 0.65;
  cursor: wait;
}

.stripe-btn {
  background: #4F46E5;
  border-color: #4338CA;
}

.stripe-btn:hover:not(:disabled) {
  background: #4338CA;
}

.paypal-btn {
  background: #003087;
  border-color: #002366;
}

.paypal-btn:hover:not(:disabled) {
  background: #002366;
}

.provider-mark {
  width: 26px;
  height: 26px;
  border-radius: 6px;
  background: rgba(255, 255, 255, 0.2);
  display: grid;
  place-items: center;
  font-size: 14px;
  font-weight: 800;
  flex-shrink: 0;
}

.paypal-logo {
  font-style: italic;
}

.provider-label {
  font-weight: 600;
}

.ml-auto {
  margin-left: auto;
}

.security-guarantee {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 4px;
  font-size: 11px;
  color: #64748B;
}

.card-error-alert {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 12px;
  border-radius: 6px;
  background: #FEF2F2;
  border: 1px solid #FECACA;
  color: #DC2626;
  font-size: 12px;
  margin-bottom: 12px;
}

/* Comparison Matrix Section */
.comparison-matrix-card {
  background: #FFFFFF;
  border: 1px solid #E2E8F0;
  border-radius: 16px;
  padding: 32px;
  margin-top: 16px;
}

.matrix-header {
  margin-bottom: 24px;
}

.matrix-title {
  margin: 0;
  font-size: 20px;
  font-weight: 700;
  color: #0F172A;
}

.matrix-desc {
  margin: 4px 0 0 0;
  font-size: 13px;
  color: #64748B;
}

.matrix-table-wrap {
  overflow-x: auto;
}

.matrix-table {
  width: 100%;
  border-collapse: collapse;
  text-align: left;
  font-size: 13px;
}

.matrix-table th {
  padding: 14px 16px;
  border-bottom: 2px solid #E2E8F0;
  font-size: 12px;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.04em;
  color: #475569;
}

.matrix-table td {
  padding: 16px;
  border-bottom: 1px solid #F1F5F9;
  vertical-align: middle;
}

.th-feature {
  width: 48%;
}

.th-plan {
  width: 26%;
  text-align: center;
}

.th-premium {
  color: #7C3AED;
  background: #FAF5FF;
  border-top-left-radius: 8px;
  border-top-right-radius: 8px;
}

.td-plan {
  text-align: center;
}

.td-premium {
  background: #FAF5FF;
}

.feature-title {
  display: block;
  color: #0F172A;
  font-weight: 600;
}

.feature-note {
  display: block;
  font-size: 11px;
  color: #94A3B8;
  margin-top: 2px;
}

.text-badge {
  font-size: 12px;
  color: #475569;
}

/* FAQ Section */
.pricing-faq-section {
  background: #FFFFFF;
  border: 1px solid #E2E8F0;
  border-radius: 16px;
  padding: 32px;
}

.faq-header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 24px;
}

.faq-title {
  margin: 0;
  font-size: 20px;
  font-weight: 700;
  color: #0F172A;
}

.faq-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 24px;
}

.faq-item {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.faq-question {
  margin: 0;
  font-size: 14px;
  font-weight: 700;
  color: #1E293B;
  line-height: 1.4;
}

.faq-answer {
  margin: 0;
  font-size: 13px;
  line-height: 1.6;
  color: #64748B;
}

/* Footer */
.pricing-footer {
  text-align: center;
  font-size: 12px;
  color: #94A3B8;
  line-height: 1.6;
  padding-top: 16px;
}

.pricing-footer p {
  margin: 0;
}

.footer-sub {
  margin-top: 4px;
}

/* Responsive */
@media (max-width: 860px) {
  .pricing-cards-grid {
    grid-template-columns: 1fr;
  }

  .free-card {
    order: 2;
  }

  .premium-card {
    order: 1;
  }

  .faq-grid {
    grid-template-columns: 1fr;
  }

  .topbar-brand {
    display: none;
  }
}

@media (max-width: 580px) {
  .pricing-container {
    padding: 24px 16px 48px;
    gap: 24px;
  }

  .hero-title {
    font-size: 24px;
  }

  .pricing-card {
    padding: 24px 18px;
  }

  .price-number {
    font-size: 26px;
  }

  .comparison-matrix-card,
  .pricing-faq-section {
    padding: 20px 16px;
  }
}
</style>
