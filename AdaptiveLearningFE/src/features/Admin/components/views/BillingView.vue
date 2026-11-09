<script setup>
import { inject } from 'vue'
import {
  Check,
  CheckCircle2,
  ChevronLeft,
  ChevronRight,
  CreditCard,
  Layers,
  LoaderCircle,
  Pencil,
  Plus,
  RefreshCw,
  RotateCcw,
  Search,
  Sparkles,
  Trash2,
  TrendingUp,
  User,
  Users,
  X
} from 'lucide-vue-next'
import { adminContextKey } from '../../composables/adminContext'

const admin = inject(adminContextKey)

const {
  billingSubTab,
  plans,
  loading,
  openPlanModal,
  togglePlanStatus,
  deletePlan,
  parseFeatures,
  formatMoney,
  userBillings,
  userBillingSearch,
  userBillingPlanFilter,
  userBillingPage,
  userBillingPageSize,
  filteredUserBillings,
  paginatedUserBillings,
  userBillingTotalPages,
  viewUserPayments,
  resetUserQuota,
  payments,
  loadingPayments,
  paymentSearch,
  paymentStatusFilter,
  paymentProviderFilter,
  paymentPage,
  paymentPageSize,
  filteredPayments,
  paymentTotalPages,
  paginatedPayments,
  formatPaymentDate,
  loadData,
  totalRevenue,
  succeededPaymentsCount,
  premiumUsersCount,
  users,
  updateUserPlan
} = admin
</script>

<template>
<section  class="admin-billing-section">
          <!-- TOP HEADING PANEL -->
          <div class="admin-panel billing-hero-panel">
            <div class="billing-hero-content">
              <div class="billing-hero-badge">
                <CreditCard :size="15" />
                <span>QUẢN LÝ GÓI CƯỚC & TÀI CHÍNH</span>
              </div>
              <h2>Quản trị gói cước & Tài chính hệ thống</h2>
              <p>Cấu hình danh mục gói học tập, quản lý hạn mức người học và theo dõi doanh thu thanh toán qua cổng Stripe & PayPal.</p>
            </div>
            <div class="billing-hero-actions">
              <button
                v-if="billingSubTab === 'plans'"
                type="button"
                class="admin-primary-button"
                @click="openPlanModal()"
              >
                <Plus :size="16" />
                <span>Thêm gói cước mới</span>
              </button>
              <button
                type="button"
                class="admin-secondary-button"
                :disabled="loading"
                title="Tải lại dữ liệu gói cước và giao dịch"
                @click="loadData"
              >
                <RefreshCw :size="15" :class="{ 'overlay-spinner': loading }" />
                <span>Làm mới dữ liệu</span>
              </button>
            </div>
          </div>

          <!-- KEY METRICS CARDS -->
          <div class="billing-metrics-grid">
            <article class="billing-stat-card revenue">
              <div class="billing-stat-icon">
                <TrendingUp :size="20" />
              </div>
              <div class="billing-stat-info">
                <span>TỔNG DOANH THU (USD)</span>
                <strong>{{ formatMoney(totalRevenue) }}</strong>
                <small>{{ succeededPaymentsCount }} giao dịch thành công</small>
              </div>
            </article>

            <article class="billing-stat-card success">
              <div class="billing-stat-icon">
                <CheckCircle2 :size="20" />
              </div>
              <div class="billing-stat-info">
                <span>ĐƠN THANH TOÁN THÀNH CÔNG</span>
                <strong>{{ succeededPaymentsCount }}</strong>
                <small>{{ payments.length ? Math.round((succeededPaymentsCount / payments.length) * 100) : 0 }}% tỷ lệ thành công</small>
              </div>
            </article>

            <article class="billing-stat-card premium">
              <div class="billing-stat-icon">
                <Sparkles :size="20" />
              </div>
              <div class="billing-stat-info">
                <span>HỌC VIÊN GÓI PREMIUM</span>
                <strong>{{ premiumUsersCount }}</strong>
                <small>{{ users.length ? Math.round((premiumUsersCount / users.length) * 100) : 0 }}% tổng số người dùng</small>
              </div>
            </article>

            <article class="billing-stat-card pending">
              <div class="billing-stat-icon">
                <Layers :size="20" />
              </div>
              <div class="billing-stat-info">
                <span>SỐ GÓI ĐANG MỞ BÁN</span>
                <strong>{{ plans.filter((p) => p.isActive).length }} / {{ plans.length }}</strong>
                <small>Gói cước trong hệ thống</small>
              </div>
            </article>
          </div>

          <!-- SUBTABS SWITCHER -->
          <div class="billing-tabs-nav" role="tablist">
            <button
              :class="['billing-tab-btn', { active: billingSubTab === 'plans' }]"
              role="tab"
              :aria-selected="billingSubTab === 'plans'"
              @click="billingSubTab = 'plans'"
            >
              <Layers :size="16" />
              <span>Quản lý các gói thanh toán</span>
              <span class="tab-badge">{{ plans.length }}</span>
            </button>
            <button
              :class="['billing-tab-btn', { active: billingSubTab === 'users' }]"
              role="tab"
              :aria-selected="billingSubTab === 'users'"
              @click="billingSubTab = 'users'"
            >
              <Users :size="16" />
              <span>Quản lý User thanh toán</span>
              <span class="tab-badge">{{ userBillings.length }}</span>
            </button>
            <button
              :class="['billing-tab-btn', { active: billingSubTab === 'transactions' }]"
              role="tab"
              :aria-selected="billingSubTab === 'transactions'"
              @click="billingSubTab = 'transactions'"
            >
              <CreditCard :size="16" />
              <span>Lịch sử giao dịch hệ thống</span>
              <span class="tab-badge">{{ payments.length }}</span>
            </button>
          </div>

          <!-- TAB 1: QUẢN LÝ CÁC GÓI THANH TOÁN -->
          <div v-if="billingSubTab === 'plans'" class="billing-tab-pane">
            <div v-if="loading" class="admin-empty admin-empty-loading">
              <LoaderCircle class="overlay-spinner" :size="24" />
              <span>Đang tải danh mục gói cước…</span>
            </div>
            <div v-else-if="!plans.length" class="admin-empty">
              <div class="empty-icon-wrap">
                <Layers :size="28" />
              </div>
              <strong>Chưa có gói cước nào trong hệ thống</strong>
              <p>Bạn có thể tạo gói cước đầu tiên để học viên có thể đăng ký và nâng cấp.</p>
              <button type="button" class="admin-primary-button" style="margin-top: 10px;" @click="openPlanModal()">
                <Plus :size="15" />
                <span>Thêm gói cước mới</span>
              </button>
            </div>
            <div v-else class="billing-plans-grid">
              <article
                v-for="plan in plans"
                :key="plan.planId"
                class="billing-plan-card"
                :class="{ premium: plan.code.includes('PREMIUM'), inactive: !plan.isActive }"
              >
                <div class="plan-card-header">
                  <div>
                    <span class="plan-badge" :class="{ 'premium-badge': plan.code.includes('PREMIUM') }">
                      {{ plan.code }}
                    </span>
                    <h3>{{ plan.name }}</h3>
                  </div>
                  <div class="plan-price-tag">
                    <strong>{{ Number(plan.price) === 0 ? 'Miễn phí' : formatMoney(plan.price, plan.currency) }}</strong>
                    <small>{{ plan.currency }} / {{ plan.billingCycle === 'LIFETIME' ? 'Trọn đời' : plan.billingCycle === 'MONTHLY' ? 'Tháng' : plan.billingCycle === 'ANNUAL' ? 'Năm' : plan.billingCycle }}</small>
                  </div>
                </div>

                <p class="plan-card-desc">{{ plan.description || 'Chưa có mô tả cho gói cước này.' }}</p>

                <div class="plan-quota-badge">
                  <component :is="plan.examLimit === -1 ? Sparkles : ClipboardCheck" :size="14" />
                  <span>{{ plan.examLimit === -1 ? 'Không giới hạn lượt làm bài thi' : `Giới hạn: ${plan.examLimit} lượt thi thử` }}</span>
                </div>

                <ul class="plan-features-list">
                  <li v-for="(feat, idx) in parseFeatures(plan.featuresJson)" :key="idx">
                    <Check :size="15" />
                    <span>{{ feat }}</span>
                  </li>
                </ul>

                <div class="plan-card-footer">
                  <div class="plan-status-indicator">
                    <span class="status-dot" :class="{ active: plan.isActive }"></span>
                    <small>{{ plan.isActive ? 'Đang mở bán' : 'Tạm ẩn' }}</small>
                  </div>
                  <div class="plan-actions">
                    <button
                      type="button"
                      class="table-action"
                      :title="plan.isActive ? 'Tạm ẩn gói khỏi trang Bảng giá' : 'Mở bán lại gói này'"
                      @click="togglePlanStatus(plan)"
                    >
                      {{ plan.isActive ? 'Tạm ẩn' : 'Mở bán' }}
                    </button>
                    <button
                      type="button"
                      class="table-action"
                      title="Chỉnh sửa thông tin gói cước"
                      @click="openPlanModal(plan)"
                    >
                      <Pencil :size="13" />
                      <span>Sửa</span>
                    </button>
                    <button
                      v-if="!['FREE', 'PREMIUM'].includes(plan.code)"
                      type="button"
                      class="table-action danger"
                      title="Xóa gói cước"
                      @click="deletePlan(plan)"
                    >
                      <Trash2 :size="13" />
                    </button>
                  </div>
                </div>
              </article>
            </div>
          </div>

          <!-- TAB 2: QUẢN LÝ USER THANH TOÁN -->
          <div v-else-if="billingSubTab === 'users'" class="billing-tab-pane">
            <div class="admin-panel admin-table-panel">
              <div class="admin-panel-heading">
                <div>
                  <h2>Danh sách học viên & Tình trạng gói cước</h2>
                  <p>Theo dõi hạn mức làm bài thi, gói dịch vụ và tổng chi tiêu của học viên.</p>
                </div>
                <div class="billing-table-filters">
                  <div class="billing-search-box">
                    <Search :size="15" />
                    <input
                      v-model="userBillingSearch"
                      type="search"
                      placeholder="Tìm theo ID, username..."
                      aria-label="Tìm học viên thanh toán"
                    />
                    <button
                      v-if="userBillingSearch"
                      type="button"
                      class="account-search-clear"
                      aria-label="Xóa tìm kiếm"
                      @click="userBillingSearch = ''"
                    >
                      <X :size="14" />
                    </button>
                  </div>
                  <select v-model="userBillingPlanFilter" class="billing-select-filter" aria-label="Lọc theo gói cước">
                    <option value="all">Tất cả gói</option>
                    <option value="PREMIUM">⭐ Gói Premium</option>
                    <option value="FREE">Gói Miễn Phí</option>
                  </select>
                  <button
                    v-if="userBillingSearch || userBillingPlanFilter !== 'all'"
                    type="button"
                    class="account-reset-filter-btn"
                    @click="userBillingSearch = ''; userBillingPlanFilter = 'all'"
                  >
                    <RotateCcw :size="13" />
                    <span>Đặt lại</span>
                  </button>
                </div>
              </div>

              <div class="account-meta-summary" style="padding: 10px 20px 14px;">
                <span>Hiển thị <strong>{{ paginatedUserBillings.length }}</strong> / <strong>{{ filteredUserBillings.length }}</strong> tài khoản học viên (Tổng số: {{ userBillings.length }})</span>
              </div>

              <div v-if="loading" class="admin-empty admin-empty-loading">
                <LoaderCircle class="overlay-spinner" :size="24" />
                <span>Đang tải dữ liệu học viên…</span>
              </div>
              <div v-else-if="!filteredUserBillings.length" class="admin-empty">
                <div class="empty-icon-wrap">
                  <Users :size="28" />
                </div>
                <strong>Không tìm thấy tài khoản học viên phù hợp</strong>
                <p>Không có học viên nào khớp với bộ lọc tìm kiếm hiện tại.</p>
              </div>
              <div v-else>
                <!-- Desktop Table View -->
                <div class="admin-table-wrap billing-desktop-table">
                  <table class="admin-table">
                    <thead>
                      <tr>
                        <th style="min-width: 200px;">Học viên</th>
                        <th style="min-width: 160px;">Gói cước hiện tại</th>
                        <th style="min-width: 150px;">Ngày kích hoạt</th>
                        <th style="min-width: 170px;">Lượt thi dùng</th>
                        <th style="min-width: 130px;">Tổng chi tiêu</th>
                        <th style="min-width: 110px;">Số đơn</th>
                        <th style="text-align: right; min-width: 190px;">Thao tác</th>
                      </tr>
                    </thead>
                    <tbody>
                      <tr v-for="user in paginatedUserBillings" :key="user.userId">
                        <td>
                          <div class="table-account">
                            <span class="account-avatar">
                              {{ user.username.slice(0, 1).toUpperCase() }}
                            </span>
                            <div class="table-account-details">
                              <strong class="account-username">{{ user.username }}</strong>
                              <small class="account-id">ID: #{{ user.userId }}</small>
                            </div>
                          </div>
                        </td>
                        <td>
                          <select
                            class="role-select plan-select"
                            :class="{ 'plan-premium-selected': user.plan === 'PREMIUM' }"
                            :value="user.plan || 'FREE'"
                            aria-label="Thay đổi gói cước"
                            @change="updateUserPlan(user, $event.target.value)"
                          >
                            <option value="FREE">Gói Free</option>
                            <option value="PREMIUM">⭐ Gói Premium</option>
                          </select>
                        </td>
                        <td>
                          <span v-if="user.premiumActivatedAt" class="payment-time">
                            {{ formatPaymentDate(user.premiumActivatedAt) }}
                          </span>
                          <span v-else class="table-secondary">—</span>
                        </td>
                        <td>
                          <div class="quota-progress-cell">
                            <strong v-if="user.plan === 'PREMIUM'" class="unlimited-quota">
                              <Sparkles :size="13" />
                              Không giới hạn
                            </strong>
                            <span v-else class="limited-quota">
                              {{ Math.max(0, Number(user.freeExamLimit || 0) - Number(user.freeExamsUsed || 0)) }} / {{ user.freeExamLimit || 0 }} lượt
                            </span>
                          </div>
                        </td>
                        <td>
                          <strong class="payment-amount">{{ formatMoney(user.totalSpent) }}</strong>
                        </td>
                        <td>
                          <span class="order-count-badge">{{ user.paymentCount || 0 }} đơn</span>
                        </td>
                        <td>
                          <div class="table-row-actions" style="justify-content: flex-end;">
                            <button
                              type="button"
                              class="table-action"
                              title="Xem chi tiết các đơn thanh toán của học viên"
                              @click="viewUserPayments(user)"
                            >
                              <CreditCard :size="13" />
                              <span>Lịch sử GD</span>
                            </button>
                            <button
                              v-if="user.plan !== 'PREMIUM'"
                              type="button"
                              class="table-action"
                              title="Reset số lượt thi đã dùng về 0"
                              @click="resetUserQuota(user)"
                            >
                              <RotateCcw :size="13" />
                              <span>Reset lượt</span>
                            </button>
                          </div>
                        </td>
                      </tr>
                    </tbody>
                  </table>
                </div>

                <!-- Mobile Card View -->
                <div class="billing-mobile-grid" style="padding: 14px;">
                  <article
                    v-for="user in paginatedUserBillings"
                    :key="`b-user-${user.userId}`"
                    class="account-mobile-card"
                  >
                    <div class="account-card-header">
                      <div class="table-account">
                        <span class="account-avatar">
                          {{ user.username.slice(0, 1).toUpperCase() }}
                        </span>
                        <div class="table-account-details">
                          <strong class="account-username">{{ user.username }}</strong>
                          <small class="account-id">ID: #{{ user.userId }}</small>
                        </div>
                      </div>
                      <span class="order-count-badge">{{ user.paymentCount || 0 }} đơn</span>
                    </div>

                    <div class="account-card-body">
                      <div class="card-field-row">
                        <span class="card-field-label">Gói cước:</span>
                        <select
                          class="role-select plan-select"
                          :class="{ 'plan-premium-selected': user.plan === 'PREMIUM' }"
                          :value="user.plan || 'FREE'"
                          @change="updateUserPlan(user, $event.target.value)"
                        >
                          <option value="FREE">Gói Free</option>
                          <option value="PREMIUM">⭐ Gói Premium</option>
                        </select>
                      </div>

                      <div class="card-field-row">
                        <span class="card-field-label">Lượt thi:</span>
                        <strong v-if="user.plan === 'PREMIUM'" class="unlimited-quota">
                          <Sparkles :size="13" />
                          Không giới hạn
                        </strong>
                        <span v-else class="limited-quota">
                          {{ Math.max(0, Number(user.freeExamLimit || 0) - Number(user.freeExamsUsed || 0)) }} / {{ user.freeExamLimit || 0 }} lượt
                        </span>
                      </div>

                      <div class="card-field-row">
                        <span class="card-field-label">Tổng chi tiêu:</span>
                        <strong class="payment-amount">{{ formatMoney(user.totalSpent) }}</strong>
                      </div>

                      <div v-if="user.premiumActivatedAt" class="card-field-row">
                        <span class="card-field-label">Kích hoạt:</span>
                        <span class="payment-time">{{ formatPaymentDate(user.premiumActivatedAt) }}</span>
                      </div>
                    </div>

                    <div class="account-card-actions">
                      <button
                        type="button"
                        class="table-action mobile-card-btn"
                        @click="viewUserPayments(user)"
                      >
                        <CreditCard :size="14" />
                        <span>Lịch sử GD</span>
                      </button>
                      <button
                        v-if="user.plan !== 'PREMIUM'"
                        type="button"
                        class="table-action mobile-card-btn"
                        @click="resetUserQuota(user)"
                      >
                        <RotateCcw :size="14" />
                        <span>Reset lượt thi</span>
                      </button>
                    </div>
                  </article>
                </div>

                <!-- Pagination for Users -->
                <div v-if="userBillingTotalPages > 1 || filteredUserBillings.length > 10" class="account-pagination" style="padding: 14px 20px;">
                  <div class="pagination-size">
                    <span>Hiển thị</span>
                    <select v-model.number="userBillingPageSize" class="pagination-select" aria-label="Số dòng mỗi trang">
                      <option :value="10">10 học viên</option>
                      <option :value="25">25 học viên</option>
                      <option :value="50">50 học viên</option>
                    </select>
                  </div>
                  <div class="pagination-nav">
                    <button
                      type="button"
                      class="pagination-btn"
                      :disabled="userBillingPage <= 1"
                      aria-label="Trang trước"
                      @click="userBillingPage = Math.max(1, userBillingPage - 1)"
                    >
                      <ChevronLeft :size="16" />
                      <span>Trước</span>
                    </button>
                    <span class="pagination-status">Trang <strong>{{ userBillingPage }}</strong> / {{ userBillingTotalPages }}</span>
                    <button
                      type="button"
                      class="pagination-btn"
                      :disabled="userBillingPage >= userBillingTotalPages"
                      aria-label="Trang sau"
                      @click="userBillingPage = Math.min(userBillingTotalPages, userBillingPage + 1)"
                    >
                      <span>Sau</span>
                      <ChevronRight :size="16" />
                    </button>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <!-- TAB 3: LỊCH SỬ GIAO DỊCH TOÀN HỆ THỐNG -->
          <div v-else-if="billingSubTab === 'transactions'" class="billing-tab-pane">
            <div class="admin-panel admin-table-panel billing-table-panel">
              <div class="admin-panel-heading">
                <div>
                  <h2>Toàn bộ lịch sử giao dịch</h2>
                  <p>{{ filteredPayments.length }} giao dịch được ghi nhận qua cổng Stripe & PayPal.</p>
                </div>
                <div class="billing-table-filters">
                  <div class="billing-search-box">
                    <Search :size="15" />
                    <input
                      v-model="paymentSearch"
                      type="search"
                      placeholder="Tìm theo mã đơn, học viên..."
                      aria-label="Tìm kiếm giao dịch"
                    />
                    <button
                      v-if="paymentSearch"
                      type="button"
                      class="account-search-clear"
                      aria-label="Xóa tìm kiếm"
                      @click="paymentSearch = ''"
                    >
                      <X :size="14" />
                    </button>
                  </div>
                  <select v-model="paymentStatusFilter" class="billing-select-filter" aria-label="Lọc theo trạng thái giao dịch">
                    <option value="all">Tất cả trạng thái</option>
                    <option value="SUCCEEDED">Thành công</option>
                    <option value="PENDING">Đang chờ</option>
                    <option value="FAILED">Thất bại</option>
                    <option value="REFUNDED">Hoàn tiền</option>
                  </select>
                  <select v-model="paymentProviderFilter" class="billing-select-filter" aria-label="Lọc theo cổng thanh toán">
                    <option value="all">Tất cả cổng</option>
                    <option value="STRIPE">Stripe</option>
                    <option value="PAYPAL">PayPal</option>
                  </select>
                  <button
                    v-if="paymentSearch || paymentStatusFilter !== 'all' || paymentProviderFilter !== 'all'"
                    type="button"
                    class="account-reset-filter-btn"
                    @click="paymentSearch = ''; paymentStatusFilter = 'all'; paymentProviderFilter = 'all'"
                  >
                    <RotateCcw :size="13" />
                    <span>Đặt lại</span>
                  </button>
                </div>
              </div>

              <div class="account-meta-summary" style="padding: 10px 20px 14px;">
                <span>Hiển thị <strong>{{ paginatedPayments.length }}</strong> / <strong>{{ filteredPayments.length }}</strong> giao dịch (Tổng số: {{ payments.length }})</span>
              </div>

              <div v-if="loading" class="admin-empty admin-empty-loading">
                <LoaderCircle class="overlay-spinner" :size="24" />
                <span>Đang tải lịch sử giao dịch…</span>
              </div>
              <div v-else-if="!filteredPayments.length" class="admin-empty">
                <div class="empty-icon-wrap">
                  <CreditCard :size="28" />
                </div>
                <strong>Chưa có giao dịch thanh toán nào phù hợp</strong>
                <p>Học viên có thể nâng cấp gói cước tại trang Bảng giá (/pricing) qua cổng Stripe hoặc PayPal.</p>
              </div>
              <div v-else>
                <!-- Desktop Table View -->
                <div class="admin-table-wrap billing-desktop-table">
                  <table class="admin-table">
                    <thead>
                      <tr>
                        <th style="min-width: 170px;">Mã giao dịch</th>
                        <th style="min-width: 190px;">Học viên</th>
                        <th style="min-width: 130px;">Cổng thanh toán</th>
                        <th style="min-width: 130px;">Số tiền</th>
                        <th style="min-width: 130px;">Trạng thái</th>
                        <th style="min-width: 150px;">Thời gian</th>
                      </tr>
                    </thead>
                    <tbody>
                      <tr v-for="payment in paginatedPayments" :key="payment.paymentId">
                        <td>
                          <span class="payment-id-badge">#PAY-{{ payment.paymentId }}</span>
                          <small v-if="payment.providerPaymentId" class="table-secondary">
                            Ref: {{ payment.providerPaymentId.slice(0, 18) }}...
                          </small>
                        </td>
                        <td>
                          <div class="table-account">
                            <span class="account-avatar">
                              {{ (payment.username || 'U').slice(0, 1).toUpperCase() }}
                            </span>
                            <div class="table-account-details">
                              <strong class="account-username">{{ payment.username || `User #${payment.userId}` }}</strong>
                              <small class="account-id">ID: #{{ payment.userId }}</small>
                            </div>
                          </div>
                        </td>
                        <td>
                          <span class="provider-badge" :class="payment.provider?.toLowerCase()">
                            {{ payment.provider }}
                          </span>
                        </td>
                        <td>
                          <strong class="payment-amount">{{ formatMoney(payment.amount, payment.currency) }}</strong>
                        </td>
                        <td>
                          <span
                            class="payment-status-pill"
                            :class="payment.status?.toLowerCase()"
                          >
                            <span class="state-dot" aria-hidden="true"></span>
                            <span>{{
                              payment.status === 'SUCCEEDED'
                                ? 'Thành công'
                                : payment.status === 'PENDING'
                                ? 'Đang chờ'
                                : payment.status === 'FAILED'
                                ? 'Thất bại'
                                : payment.status === 'REFUNDED'
                                ? 'Đã hoàn tiền'
                                : payment.status
                            }}</span>
                          </span>
                        </td>
                        <td>
                          <span class="payment-time">{{ formatPaymentDate(payment.createdAt) }}</span>
                        </td>
                      </tr>
                    </tbody>
                  </table>
                </div>

                <!-- Mobile Card View -->
                <div class="billing-mobile-grid" style="padding: 14px;">
                  <article
                    v-for="payment in paginatedPayments"
                    :key="`b-pay-${payment.paymentId}`"
                    class="account-mobile-card"
                  >
                    <div class="account-card-header">
                      <div>
                        <span class="payment-id-badge">#PAY-{{ payment.paymentId }}</span>
                        <small v-if="payment.providerPaymentId" class="table-secondary" style="display: block; margin-top: 2px;">
                          Ref: {{ payment.providerPaymentId.slice(0, 16) }}...
                        </small>
                      </div>
                      <span
                        class="payment-status-pill"
                        :class="payment.status?.toLowerCase()"
                      >
                        <span class="state-dot" aria-hidden="true"></span>
                        <span>{{
                          payment.status === 'SUCCEEDED'
                            ? 'Thành công'
                            : payment.status === 'PENDING'
                            ? 'Đang chờ'
                            : payment.status === 'FAILED'
                            ? 'Thất bại'
                            : payment.status === 'REFUNDED'
                            ? 'Đã hoàn tiền'
                            : payment.status
                        }}</span>
                      </span>
                    </div>

                    <div class="account-card-body">
                      <div class="card-field-row">
                        <span class="card-field-label">Học viên:</span>
                        <div class="table-account">
                          <span class="account-avatar" style="width: 26px; height: 26px; font-size: 11px;">
                            {{ (payment.username || 'U').slice(0, 1).toUpperCase() }}
                          </span>
                          <strong class="account-username">{{ payment.username || `User #${payment.userId}` }}</strong>
                        </div>
                      </div>

                      <div class="card-field-row">
                        <span class="card-field-label">Cổng & Số tiền:</span>
                        <div style="display: flex; align-items: center; gap: 8px;">
                          <span class="provider-badge" :class="payment.provider?.toLowerCase()">
                            {{ payment.provider }}
                          </span>
                          <strong class="payment-amount">{{ formatMoney(payment.amount, payment.currency) }}</strong>
                        </div>
                      </div>

                      <div class="card-field-row">
                        <span class="card-field-label">Thời gian:</span>
                        <span class="payment-time">{{ formatPaymentDate(payment.createdAt) }}</span>
                      </div>
                    </div>
                  </article>
                </div>

                <!-- Pagination for Transactions -->
                <div v-if="paymentTotalPages > 1 || filteredPayments.length > 10" class="account-pagination" style="padding: 14px 20px;">
                  <div class="pagination-size">
                    <span>Hiển thị</span>
                    <select v-model.number="paymentPageSize" class="pagination-select" aria-label="Số giao dịch mỗi trang">
                      <option :value="10">10 giao dịch</option>
                      <option :value="25">25 giao dịch</option>
                      <option :value="50">50 giao dịch</option>
                    </select>
                  </div>
                  <div class="pagination-nav">
                    <button
                      type="button"
                      class="pagination-btn"
                      :disabled="paymentPage <= 1"
                      aria-label="Trang trước"
                      @click="paymentPage = Math.max(1, paymentPage - 1)"
                    >
                      <ChevronLeft :size="16" />
                      <span>Trước</span>
                    </button>
                    <span class="pagination-status">Trang <strong>{{ paymentPage }}</strong> / {{ paymentTotalPages }}</span>
                    <button
                      type="button"
                      class="pagination-btn"
                      :disabled="paymentPage >= paymentTotalPages"
                      aria-label="Trang sau"
                      @click="paymentPage = Math.min(paymentTotalPages, paymentPage + 1)"
                    >
                      <span>Sau</span>
                      <ChevronRight :size="16" />
                    </button>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </section>
</template>

<style scoped>
  /* BILLING & SUBSCRIPTIONS STYLES */
  .admin-billing-section {
    display: flex;
    flex-direction: column;
    gap: 20px;
  }
  .billing-hero-panel {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 20px;
    padding: 24px 28px;
    background: #FFFFFF;
    border: 1px solid #E2E8F0;
    border-radius: 12px;
  }
  .billing-hero-content {
    max-width: 680px;
  }
  .billing-hero-badge {
    display: inline-flex;
    align-items: center;
    gap: 6px;
    padding: 3px 10px;
    margin-bottom: 8px;
    border-radius: 6px;
    background: #EFF6FF;
    color: #2563EB;
    font-size: 11px;
    font-weight: 700;
    letter-spacing: 0.04em;
  }
  .billing-hero-content h2 {
    margin: 0 0 6px;
    color: #0F172A;
    font-size: 19px;
    font-weight: 700;
    letter-spacing: -0.02em;
  }
  .billing-hero-content p {
    margin: 0;
    color: #64748B;
    font-size: 13.5px;
    line-height: 1.5;
  }
  .billing-metrics-grid {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
    gap: 16px;
  }
  .billing-stat-card {
    display: flex;
    align-items: center;
    gap: 14px;
    padding: 18px 20px;
    background: #FFFFFF;
    border: 1px solid #E2E8F0;
    border-radius: 12px;
    transition: transform 140ms ease, box-shadow 140ms ease;
  }
  .billing-stat-card:hover {
    transform: translateY(-1px);
    box-shadow: 0 4px 12px rgba(15, 23, 42, 0.05);
  }
  .billing-stat-icon {
    display: grid;
    width: 44px;
    height: 44px;
    flex: 0 0 auto;
    place-items: center;
    border-radius: 10px;
  }
  .billing-stat-card.revenue .billing-stat-icon {
    background: #ECFDF5;
    color: #059669;
  }
  .billing-stat-card.success .billing-stat-icon {
    background: #EFF6FF;
    color: #2563EB;
  }
  .billing-stat-card.pending .billing-stat-icon {
    background: #FFFBEB;
    color: #D97706;
  }
  .billing-stat-card.premium .billing-stat-icon {
    background: #FAF5FF;
    color: #7C3AED;
  }
  .billing-stat-info {
    display: flex;
    flex-direction: column;
    gap: 2px;
    min-width: 0;
  }
  .billing-stat-info span {
    color: #64748B;
    font-size: 11px;
    font-weight: 600;
    letter-spacing: 0.04em;
  }
  .billing-stat-info strong {
    color: #0F172A;
    font-size: 20px;
    font-weight: 700;
    letter-spacing: -0.02em;
  }
  .billing-stat-info small {
    color: #94A3B8;
    font-size: 11.5px;
  }
  .billing-plans-grid {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(320px, 1fr));
    gap: 20px;
  }
  .billing-plan-card {
    display: flex;
    flex-direction: column;
    gap: 16px;
    padding: 24px;
    background: #FFFFFF;
    border: 1px solid #E2E8F0;
    border-radius: 14px;
    transition: all 140ms ease;
  }
  .billing-plan-card.premium {
    border: 1.5px solid #C4B5FD;
    background: linear-gradient(180deg, #FAF5FF 0%, #FFFFFF 100%);
    box-shadow: 0 4px 20px rgba(124, 58, 237, 0.05);
  }
  .plan-card-header {
    display: flex;
    align-items: flex-start;
    justify-content: space-between;
    gap: 12px;
  }
  .plan-badge {
    display: inline-block;
    padding: 3px 8px;
    margin-bottom: 6px;
    border-radius: 5px;
    background: #F1F5F9;
    color: #475569;
    font-size: 10.5px;
    font-weight: 700;
    letter-spacing: 0.03em;
  }
  .plan-badge.premium-badge {
    background: linear-gradient(135deg, #7C3AED, #4F46E5);
    color: #FFFFFF;
  }
  .plan-card-header h3 {
    margin: 0;
    color: #0F172A;
    font-size: 17px;
    font-weight: 700;
  }
  .plan-price-tag {
    text-align: right;
  }
  .plan-price-tag strong {
    display: block;
    color: #0F172A;
    font-size: 22px;
    font-weight: 800;
  }
  .plan-price-tag small {
    color: #64748B;
    font-size: 11.5px;
  }
  .plan-card-desc {
    margin: 0;
    color: #475569;
    font-size: 13px;
    line-height: 1.5;
  }
  .plan-features-list {
    margin: 0;
    padding: 0;
    list-style: none;
    display: flex;
    flex-direction: column;
    gap: 10px;
  }
  .plan-features-list li {
    display: flex;
    align-items: center;
    gap: 9px;
    color: #334155;
    font-size: 13px;
    line-height: 1.4;
  }
  .plan-features-list li svg {
    flex: 0 0 auto;
    color: #10B981;
  }
  .plan-card-footer {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding-top: 14px;
    margin-top: auto;
    border-top: 1px solid #E2E8F0;
  }
  .plan-user-count {
    display: inline-flex;
    align-items: center;
    gap: 6px;
    color: #475569;
    font-size: 12.5px;
  }
  .plan-user-count strong {
    color: #0F172A;
    font-weight: 700;
  }
  .plan-upgrade-hint {
    color: #6366F1;
    font-size: 11.5px;
    font-weight: 500;
  }
  .billing-table-panel {
    background: #FFFFFF;
  }
  .billing-table-filters {
    display: flex;
    align-items: center;
    gap: 10px;
    flex-wrap: wrap;
  }
  .billing-search-box {
    display: flex;
    align-items: center;
    gap: 8px;
    padding: 0 11px;
    min-height: 36px;
    border: 1px solid #CBD5E1;
    border-radius: 8px;
    background: #FFFFFF;
    color: #64748B;
  }
  .billing-search-box input {
    border: 0;
    outline: 0;
    font: inherit;
    font-size: 13px;
    color: #0F172A;
    width: 200px;
  }
  .billing-select-filter {
    min-height: 36px;
    padding: 0 10px;
    border: 1px solid #CBD5E1;
    border-radius: 8px;
    background: #FFFFFF;
    color: #334155;
    font: inherit;
    font-size: 13px;
    font-weight: 500;
    cursor: pointer;
  }
  .payment-id-badge {
    display: inline-block;
    padding: 2px 7px;
    border-radius: 4px;
    background: #F1F5F9;
    color: #1E293B;
    font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
    font-size: 12px;
    font-weight: 600;
  }
  .provider-badge {
    display: inline-block;
    padding: 3px 9px;
    border-radius: 6px;
    font-size: 11.5px;
    font-weight: 700;
    letter-spacing: 0.03em;
  }
  .provider-badge.stripe {
    background: rgba(99, 91, 255, 0.1);
    color: #635BFF;
    border: 1px solid rgba(99, 91, 255, 0.2);
  }
  .provider-badge.paypal {
    background: rgba(0, 121, 193, 0.1);
    color: #0079C1;
    border: 1px solid rgba(0, 121, 193, 0.2);
  }
  .payment-amount {
    color: #0F172A;
    font-size: 14px;
    font-weight: 700;
  }
  .payment-status-pill {
    display: inline-block;
    padding: 3px 9px;
    border-radius: 9999px;
    font-size: 11.5px;
    font-weight: 600;
  }
  .payment-status-pill.succeeded {
    background: #DEF7EC;
    color: #03543F;
  }
  .payment-status-pill.pending {
    background: #FEF08A;
    color: #713F12;
  }
  .payment-status-pill.failed,
  .payment-status-pill.refunded {
    background: #FDE8E8;
    color: #9B1C1C;
  }
  .payment-time {
    color: #64748B;
    font-size: 12.5px;
  }
  .plan-select.plan-premium-selected {
    border-color: #C4B5FD;
    color: #6D28D9;
    font-weight: 600;
    background: #FAF5FF;
  }

  /* SUBTABS NAVIGATION */
  .billing-tabs-nav {
    display: flex;
    align-items: center;
    gap: 8px;
    padding: 6px;
    background: #F1F5F9;
    border-radius: 10px;
    width: fit-content;
    max-width: 100%;
    overflow-x: auto;
  }
  .billing-tab-btn {
    display: inline-flex;
    align-items: center;
    gap: 8px;
    padding: 8px 16px;
    border: 0;
    border-radius: 8px;
    background: transparent;
    color: #475569;
    font: inherit;
    font-size: 13.5px;
    font-weight: 600;
    cursor: pointer;
    transition: all 140ms ease;
    white-space: nowrap;
  }
  .billing-tab-btn:hover {
    color: #0F172A;
    background: rgba(255, 255, 255, 0.6);
  }
  .billing-tab-btn.active {
    background: #FFFFFF;
    color: #2563EB;
    box-shadow: 0 1px 3px rgba(15, 23, 42, 0.08);
  }
  .billing-tab-btn .tab-badge {
    display: inline-block;
    padding: 1px 7px;
    border-radius: 9999px;
    background: #E2E8F0;
    color: #475569;
    font-size: 11px;
    font-weight: 700;
  }
  .billing-tab-btn.active .tab-badge {
    background: #EFF6FF;
    color: #2563EB;
  }
  .billing-tab-pane {
    display: flex;
    flex-direction: column;
    gap: 20px;
  }

  /* PLAN CARDS ENHANCEMENT */
  .billing-plan-card.inactive {
    opacity: 0.72;
    background: #F8FAFC;
    border-style: dashed;
  }
  .plan-quota-badge {
    display: inline-flex;
    align-items: center;
    gap: 6px;
    padding: 5px 10px;
    background: #F8FAFC;
    border: 1px solid #E2E8F0;
    border-radius: 6px;
    color: #334155;
    font-size: 12px;
    font-weight: 600;
  }
  .plan-quota-badge svg {
    color: #2563EB;
  }
  .plan-status-indicator {
    display: inline-flex;
    align-items: center;
    gap: 6px;
    color: #64748B;
    font-size: 12px;
  }
  .status-dot {
    width: 8px;
    height: 8px;
    border-radius: 50%;
    background: #94A3B8;
  }
  .status-dot.active {
    background: #10B981;
    box-shadow: 0 0 0 2px #DEF7EC;
  }
  .plan-actions {
    display: flex;
    align-items: center;
    gap: 6px;
  }

  /* USER BILLINGS TABLE */
  .quota-progress-cell {
    display: flex;
    align-items: center;
  }
  .unlimited-quota {
    display: inline-block;
    color: #7C3AED;
    font-size: 13px;
    font-weight: 700;
  }
  .limited-quota {
    color: #475569;
    font-size: 13px;
    font-weight: 500;
  }
  .order-count-badge {
    display: inline-block;
    padding: 2px 8px;
    border-radius: 6px;
    background: #F1F5F9;
    color: #334155;
    font-size: 12px;
    font-weight: 600;
  }
  .table-row-actions {
    display: flex;
    align-items: center;
    gap: 6px;
  }

  .billing-hero-actions {
    display: flex;
    align-items: center;
    gap: 10px;
    flex-shrink: 0;
  }
  .billing-mobile-grid {
    display: none;
    flex-direction: column;
    gap: 12px;
  }

  /* USER PAYMENTS DRAWER */
  .user-payments-list {
    display: flex;
    flex-direction: column;
    gap: 12px;
    padding: 12px 0;
  }
  .user-payment-card {
    display: flex;
    flex-direction: column;
    gap: 8px;
    padding: 14px 16px;
    border: 1px solid #E2E8F0;
    border-radius: 10px;
    background: #FFFFFF;
  }
  .user-payment-head {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 10px;
  }
  .user-payment-meta {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 8px;
  }

  @media (max-width: 768px) {
    .billing-hero-panel {
      flex-direction: column;
      align-items: stretch;
      padding: 18px;
      gap: 16px;
    }
    .billing-hero-actions {
      width: 100%;
      flex-direction: column;
      gap: 8px;
    }
    .billing-hero-actions button {
      width: 100%;
      justify-content: center;
    }
    .billing-table-filters {
      width: 100%;
      flex-direction: column;
      align-items: stretch;
    }
    .billing-search-box {
      width: 100%;
    }
    .billing-search-box input {
      width: 100%;
    }
    .billing-select-filter {
      width: 100%;
    }
    .billing-tabs-nav {
      width: 100%;
    }
    .account-desktop-table,
    .billing-desktop-table,
    .results-desktop-table,
    .programs-desktop-table,
    .exams-desktop-table {
      display: none !important;
    }
    .account-mobile-grid,
    .billing-mobile-grid,
    .results-mobile-grid,
    .programs-mobile-grid,
    .exams-mobile-grid {
      display: flex !important;
    }
    .results-hero-panel,
    .programs-hero-panel,
    .exams-hero-panel {
      flex-direction: column;
      align-items: stretch;
      padding: 18px;
      gap: 16px;
    }
    .results-hero-controls,
    .programs-hero-actions,
    .exams-hero-actions {
      width: 100%;
      flex-direction: column;
      align-items: stretch;
      gap: 10px;
    }
    .results-filter-control {
      width: 100%;
      flex-direction: column;
      align-items: stretch;
    }
    .results-select {
      max-width: none;
      width: 100%;
    }
    .results-analytics-row {
      grid-template-columns: 1fr;
    }
    .billing-metrics-grid {
      grid-template-columns: 1fr;
    }
    .billing-plans-grid {
      grid-template-columns: 1fr;
    }
    .knowledge-workspace-grid {
      grid-template-columns: 1fr;
    }
    .knowledge-hero-panel {
      flex-direction: column;
      align-items: stretch;
      padding: 18px;
      gap: 16px;
    }
    .knowledge-hero-actions {
      width: 100%;
      flex-direction: column;
      align-items: stretch;
      gap: 10px;
    }
    .knowledge-hero-actions button,
    .knowledge-hero-actions a {
      width: 100%;
      justify-content: center;
    }
    .knowledge-metrics-grid {
      grid-template-columns: 1fr;
    }
    .knowledge-queue-item {
      grid-template-columns: 32px 1fr 28px;
    }
    .knowledge-queue-title-input-wrap {
      grid-column: 1 / -1;
    }
    .knowledge-doc-card {
      grid-template-columns: 1fr;
      gap: 12px;
    }
    .knowledge-doc-controls {
      justify-content: space-between;
      border-top: 1px solid #F1F5F9;
      padding-top: 8px;
      width: 100%;
    }
    .knowledge-pagination-bar {
      flex-direction: column;
      align-items: stretch;
      gap: 10px;
    }
    .knowledge-pagination-bar .pagination-actions {
      justify-content: space-between;
      width: 100%;
    }
  }
</style>
