<script setup>
import { inject } from 'vue'
import {
  ChevronLeft,
  ChevronRight,
  Eye,
  GraduationCap,
  LoaderCircle,
  Plus,
  RefreshCw,
  RotateCcw,
  Search,
  Sparkles,
  User,
  UserCheck,
  Users,
  UserX,
  X
} from 'lucide-vue-next'
import { adminContextKey } from '../../composables/adminContext'

const admin = inject(adminContextKey)

const {
  loading,
  users,
  query,
  accountRoleFilter,
  accountStatusFilter,
  accountPage,
  accountPageSize,
  filteredUsers,
  accountTotalPages,
  paginatedUsers,
  programs,
  openAccount,
  resetAccountFilters,
  openLearnerReport,
  updateRole,
  updateExamApprovalPermission,
  updateStatus,
  updateUserPlan,
  formatAdminNumber,
  plans,
  totalAccountsCount,
  accountSearchQuery,
  accountPlanFilter,
  studentAccountsCount,
  activeAccountsCount,
  premiumAccountsCount,
  loadData
} = admin
</script>

<template>
<section
          
          class="admin-accounts-section"
        >
          <!-- KPI Summary Cards -->
          <div class="account-kpi-grid">
            <div class="account-kpi-card">
              <div class="account-kpi-icon icon-total">
                <Users :size="20" />
              </div>
              <div class="account-kpi-info">
                <span class="account-kpi-label">Tổng tài khoản</span>
                <strong class="account-kpi-value">{{ formatAdminNumber(totalAccountsCount) }}</strong>
                <small class="account-kpi-hint">Toàn bộ tài khoản hệ thống</small>
              </div>
            </div>

            <div class="account-kpi-card">
              <div class="account-kpi-icon icon-students">
                <GraduationCap :size="20" />
              </div>
              <div class="account-kpi-info">
                <span class="account-kpi-label">Học viên</span>
                <strong class="account-kpi-value">{{ formatAdminNumber(studentAccountsCount) }}</strong>
                <small class="account-kpi-hint">Tài khoản vai trò Người học</small>
              </div>
            </div>

            <div class="account-kpi-card">
              <div class="account-kpi-icon icon-active">
                <UserCheck :size="20" />
              </div>
              <div class="account-kpi-info">
                <span class="account-kpi-label">Đang hoạt động</span>
                <strong class="account-kpi-value">{{ formatAdminNumber(activeAccountsCount) }}</strong>
                <small class="account-kpi-hint">Đã kích hoạt & xác thực email</small>
              </div>
            </div>

            <div class="account-kpi-card">
              <div class="account-kpi-icon icon-premium">
                <Sparkles :size="20" />
              </div>
              <div class="account-kpi-info">
                <span class="account-kpi-label">Gói Premium</span>
                <strong class="account-kpi-value">{{ formatAdminNumber(premiumAccountsCount) }}</strong>
                <small class="account-kpi-hint">Học viên gói dịch vụ nâng cao</small>
              </div>
            </div>
          </div>

          <!-- Main Account Management Panel -->
          <div class="admin-panel account-main-panel">
            <div class="admin-panel-heading account-panel-header">
              <div>
                <h2>Danh sách người dùng & Phân quyền</h2>
                <p>Quản trị vai trò (ROLE_USER, ROLE_ADMIN), gói dịch vụ và trạng thái hoạt động của tài khoản.</p>
              </div>
              <div class="account-header-actions">
                <button
                  type="button"
                  class="admin-secondary-button"
                  :disabled="loading"
                  title="Tải lại danh sách tài khoản"
                  @click="loadData"
                >
                  <RefreshCw :size="15" :class="{ 'overlay-spinner': loading }" />
                  <span>Làm mới</span>
                </button>
                <button
                  type="button"
                  class="admin-primary-button"
                  @click="openAccount"
                >
                  <Plus :size="16" />
                  <span>Tạo tài khoản</span>
                </button>
              </div>
            </div>

            <!-- Filter & Search Toolbar -->
            <div class="account-toolbar">
              <div class="account-search-wrap">
                <Search :size="16" class="account-search-icon" />
                <input
                  v-model="accountSearchQuery"
                  type="search"
                  placeholder="Tìm theo tên đăng nhập hoặc ID..."
                  aria-label="Tìm kiếm người dùng"
                  class="account-search-input"
                />
                <button
                  v-if="accountSearchQuery"
                  type="button"
                  class="account-search-clear"
                  aria-label="Xóa tìm kiếm"
                  @click="accountSearchQuery = ''"
                >
                  <X :size="14" />
                </button>
              </div>

              <div class="account-filters">
                <label class="account-filter-item">
                  <span class="filter-label">Vai trò</span>
                  <select v-model="accountRoleFilter" class="filter-select">
                    <option value="all">Tất cả vai trò</option>
                    <option value="ROLE_USER">Người học (ROLE_USER)</option>
                    <option value="ROLE_ADMIN">Quản trị viên (ROLE_ADMIN)</option>
                    <option value="ROLE_TEACHER">Giáo viên (ROLE_TEACHER)</option>
                  </select>
                </label>

                <label class="account-filter-item">
                  <span class="filter-label">Gói cước</span>
                  <select v-model="accountPlanFilter" class="filter-select">
                    <option value="all">Tất cả gói</option>
                    <option value="FREE">Gói Miễn phí (Free)</option>
                    <option value="PREMIUM">⭐ Gói Cao cấp (Premium)</option>
                  </select>
                </label>

                <label class="account-filter-item">
                  <span class="filter-label">Trạng thái</span>
                  <select v-model="accountStatusFilter" class="filter-select">
                    <option value="all">Tất cả trạng thái</option>
                    <option value="active">Đang hoạt động</option>
                    <option value="disabled">Đã khóa</option>
                    <option value="unverified">Chờ xác minh email</option>
                  </select>
                </label>

                <button
                  v-if="accountSearchQuery || accountRoleFilter !== 'all' || accountPlanFilter !== 'all' || accountStatusFilter !== 'all'"
                  type="button"
                  class="account-reset-filter-btn"
                  @click="resetAccountFilters"
                >
                  <RotateCcw :size="13" />
                  <span>Đặt lại</span>
                </button>
              </div>
            </div>

            <!-- Count & Meta summary -->
            <div class="account-meta-summary">
              <span>Hiển thị <strong>{{ paginatedUsers.length }}</strong> / <strong>{{ filteredUsers.length }}</strong> tài khoản phù hợp (Tổng số: {{ totalAccountsCount }})</span>
            </div>

            <!-- Loading State -->
            <div v-if="loading" class="admin-empty admin-empty-loading">
              <LoaderCircle class="overlay-spinner" :size="24" />
              <span>Đang tải danh sách tài khoản từ hệ thống…</span>
            </div>

            <!-- Empty State -->
            <div v-else-if="!filteredUsers.length" class="admin-empty account-empty-state">
              <div class="empty-icon-wrap">
                <UserX :size="28" />
              </div>
              <strong>Không tìm thấy tài khoản phù hợp</strong>
              <p>Không có người dùng nào khớp với bộ lọc hoặc từ khóa tìm kiếm hiện tại.</p>
              <button
                v-if="accountSearchQuery || accountRoleFilter !== 'all' || accountPlanFilter !== 'all' || accountStatusFilter !== 'all'"
                type="button"
                class="overlay-button overlay-button-secondary"
                style="margin-top: 8px;"
                @click="resetAccountFilters"
              >
                Xóa bộ lọc tìm kiếm
              </button>
            </div>

            <!-- Table & Card Views -->
            <div v-else class="account-data-container">
              <!-- Desktop Table View -->
              <div class="admin-table-wrap account-desktop-table">
                <table class="admin-table">
                  <thead>
                    <tr>
                      <th style="min-width: 220px;">Tài khoản</th>
                      <th style="min-width: 150px;">Vai trò</th>
                      <th style="min-width: 130px;">Quyền duyệt</th>
                      <th style="min-width: 180px;">Gói học & Hạn mức</th>
                      <th style="min-width: 150px;">Trạng thái</th>
                      <th style="text-align: right; min-width: 160px;">Thao tác</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr v-for="user in paginatedUsers" :key="user.userId" :class="{ 'row-disabled': !user.enabled }">
                      <td>
                        <div class="table-account">
                          <span
                            class="account-avatar"
                            :class="{ 'administrator': user.role === 'ROLE_ADMIN' }"
                          >
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
                          class="role-select"
                          :value="user.role"
                          aria-label="Thay đổi vai trò người dùng"
                          @change="updateRole(user, $event)"
                        >
                          <option value="ROLE_USER">Người học</option>
                          <option value="ROLE_ADMIN">Quản trị viên</option>
                          <option value="ROLE_TEACHER">Giáo viên</option>
                        </select>
                      </td>

                      <td>
                        <label class="teacher-approval-toggle">
                          <input v-if="user.role === 'ROLE_TEACHER'" type="checkbox" :checked="user.canApproveExams" @change="updateExamApprovalPermission(user, $event)" />
                          <span>{{ user.role === 'ROLE_TEACHER' ? 'Duyệt đề' : '—' }}</span>
                        </label>
                      </td>

                      <td>
                        <select
                          class="role-select plan-select"
                          :class="{ 'plan-premium-selected': user.plan === 'PREMIUM' }"
                          :value="user.plan || 'FREE'"
                          aria-label="Thay đổi gói cước người dùng"
                          @change="updateUserPlan(user, $event.target.value)"
                        >
                          <option value="FREE">Gói Free ({{ Math.max(0, Number(user.freeExamLimit || 0) - Number(user.freeExamsUsed || 0)) }}/{{ user.freeExamLimit || 0 }} lượt)</option>
                          <option value="PREMIUM">⭐ Gói Premium</option>
                        </select>
                      </td>

                      <td>
                        <span
                          :class="[
                            'account-state',
                            !user.enabled ? 'disabled' : user.emailVerified === false ? 'pending' : 'enabled',
                          ]"
                        >
                          <span class="state-dot" aria-hidden="true"></span>
                          <span>{{ !user.enabled ? 'Đã khóa' : user.emailVerified === false ? 'Chờ xác minh' : 'Hoạt động' }}</span>
                        </span>
                      </td>

                      <td>
                        <div class="account-actions-cell" style="justify-content: flex-end;">
                          <button
                            v-if="user.role === 'ROLE_USER'"
                            type="button"
                            class="table-action"
                            title="Xem chi tiết tiến độ học tập và lịch sử làm bài"
                            @click="openLearnerReport(user)"
                          >
                            <Eye :size="13" />
                            <span>Tiến độ</span>
                          </button>
                          <button
                            type="button"
                            class="table-action"
                            :class="user.enabled ? 'danger' : 'primary-action'"
                            :title="user.enabled ? 'Khóa truy cập của tài khoản' : 'Kích hoạt lại tài khoản'"
                            @click="updateStatus(user)"
                          >
                            <component :is="user.enabled ? Lock : Unlock" :size="13" />
                            <span>{{ user.enabled ? 'Khóa' : 'Mở khóa' }}</span>
                          </button>
                        </div>
                      </td>
                    </tr>
                  </tbody>
                </table>
              </div>

              <!-- Mobile Card View -->
              <div class="account-mobile-grid">
                <article
                  v-for="user in paginatedUsers"
                  :key="`m-${user.userId}`"
                  class="account-mobile-card"
                  :class="{ 'card-disabled': !user.enabled }"
                >
                  <div class="account-card-header">
                    <div class="table-account">
                      <span
                        class="account-avatar"
                        :class="{ 'administrator': user.role === 'ROLE_ADMIN' }"
                      >
                        {{ user.username.slice(0, 1).toUpperCase() }}
                      </span>
                      <div class="table-account-details">
                        <strong class="account-username">{{ user.username }}</strong>
                        <small class="account-id">ID: #{{ user.userId }}</small>
                      </div>
                    </div>

                    <span
                      :class="[
                        'account-state',
                        !user.enabled ? 'disabled' : user.emailVerified === false ? 'pending' : 'enabled',
                      ]"
                    >
                      <span class="state-dot" aria-hidden="true"></span>
                      <span>{{ !user.enabled ? 'Đã khóa' : user.emailVerified === false ? 'Chờ xác minh' : 'Hoạt động' }}</span>
                    </span>
                  </div>

                  <div class="account-card-body">
                    <div class="card-field-row">
                      <span class="card-field-label">Vai trò:</span>
                      <select
                        class="role-select"
                        :value="user.role"
                        @change="updateRole(user, $event)"
                      >
                        <option value="ROLE_USER">Người học</option>
                        <option value="ROLE_ADMIN">Quản trị viên</option>
                        <option value="ROLE_TEACHER">Giáo viên</option>
                      </select>
                    </div>
                    <label v-if="user.role === 'ROLE_TEACHER'" class="teacher-approval-toggle">
                      <input type="checkbox" :checked="user.canApproveExams" @change="updateExamApprovalPermission(user, $event)" />
                      <span>Duyệt đề</span>
                    </label>

                    <div class="card-field-row">
                      <span class="card-field-label">Gói học:</span>
                      <select
                        class="role-select plan-select"
                        :class="{ 'plan-premium-selected': user.plan === 'PREMIUM' }"
                        :value="user.plan || 'FREE'"
                        @change="updateUserPlan(user, $event.target.value)"
                      >
                        <option value="FREE">Gói Free ({{ Math.max(0, Number(user.freeExamLimit || 0) - Number(user.freeExamsUsed || 0)) }}/{{ user.freeExamLimit || 0 }} lượt)</option>
                        <option value="PREMIUM">⭐ Gói Premium</option>
                      </select>
                    </div>
                  </div>

                  <div class="account-card-actions">
                    <button
                      v-if="user.role === 'ROLE_USER'"
                      type="button"
                      class="table-action mobile-card-btn"
                      @click="openLearnerReport(user)"
                    >
                      <Eye :size="14" />
                      <span>Xem tiến độ</span>
                    </button>
                    <button
                      type="button"
                      class="table-action mobile-card-btn"
                      :class="user.enabled ? 'danger' : 'primary-action'"
                      :title="user.enabled ? 'Khóa truy cập của tài khoản' : 'Kích hoạt lại tài khoản'"
                      @click="updateStatus(user)"
                    >
                      <component :is="user.enabled ? Lock : Unlock" :size="14" />
                      <span>{{ user.enabled ? 'Khóa' : 'Mở khóa' }}</span>
                    </button>
                  </div>
                </article>
              </div>

              <!-- Pagination for Accounts -->
              <div v-if="accountTotalPages > 1 || filteredUsers.length > 10" class="account-pagination">
                <div class="pagination-size">
                  <span>Hiển thị</span>
                  <select v-model.number="accountPageSize" class="pagination-select" aria-label="Số dòng mỗi trang">
                    <option :value="10">10 tài khoản</option>
                    <option :value="25">25 tài khoản</option>
                    <option :value="50">50 tài khoản</option>
                  </select>
                </div>
                <div class="pagination-nav">
                  <button
                    type="button"
                    class="pagination-btn"
                    :disabled="accountPage <= 1"
                    aria-label="Trang trước"
                    @click="accountPage = Math.max(1, accountPage - 1)"
                  >
                    <ChevronLeft :size="16" />
                    <span>Trước</span>
                  </button>
                  <span class="pagination-status">Trang <strong>{{ accountPage }}</strong> / {{ accountTotalPages }}</span>
                  <button
                    type="button"
                    class="pagination-btn"
                    :disabled="accountPage >= accountTotalPages"
                    aria-label="Trang sau"
                    @click="accountPage = Math.min(accountTotalPages, accountPage + 1)"
                  >
                    <span>Sau</span>
                    <ChevronRight :size="16" />
                  </button>
                </div>
              </div>
            </div>
          </div>
        </section>
</template>

<style scoped>
  /* ========================================================
     ADMIN ACCOUNT MANAGEMENT MODULE (LIGHT AI EDUCATION)
     ======================================================== */
  .admin-accounts-section {
    display: flex;
    flex-direction: column;
    gap: 20px;
    min-width: 0;
  }
  .account-kpi-grid {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(210px, 1fr));
    gap: 14px;
  }
  .account-kpi-card {
    display: flex;
    align-items: center;
    gap: 14px;
    padding: 16px 18px;
    border: 1px solid #E2E8F0;
    border-radius: 10px;
    background: #FFFFFF;
    box-shadow: 0 1px 3px rgba(15, 23, 42, 0.03);
    transition: box-shadow 0.15s ease, border-color 0.15s ease;
  }
  .account-kpi-card:hover {
    border-color: #CBD5E1;
    box-shadow: 0 2px 6px rgba(15, 23, 42, 0.06);
  }
  .account-kpi-icon {
    display: grid;
    width: 42px;
    height: 42px;
    flex: 0 0 auto;
    place-items: center;
    border-radius: 10px;
  }
  .account-kpi-icon.icon-total {
    background: #EFF6FF;
    color: #2563EB;
    border: 1px solid #DBEAFE;
  }
  .account-kpi-icon.icon-students {
    background: #F0F9FF;
    color: #0284C7;
    border: 1px solid #E0F2FE;
  }
  .account-kpi-icon.icon-active {
    background: #F0FDF4;
    color: #16A34A;
    border: 1px solid #DCFCE7;
  }
  .account-kpi-icon.icon-premium {
    background: #F5F3FF;
    color: #7C3AED;
    border: 1px solid #EDE9FE;
  }
  .account-kpi-info {
    display: flex;
    flex-direction: column;
    min-width: 0;
    gap: 2px;
  }
  .account-kpi-label {
    font-size: 12px;
    font-weight: 500;
    color: #64748B;
  }
  .account-kpi-value {
    font-size: 22px;
    line-height: 26px;
    font-weight: 700;
    color: #0F172A;
    font-variant-numeric: tabular-nums;
  }
  .account-kpi-hint {
    font-size: 11px;
    color: #94A3B8;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }
  .account-main-panel {
    padding: 20px 24px;
    border-radius: 12px;
    background: #FFFFFF;
    border: 1px solid #E2E8F0;
    box-shadow: 0 1px 3px rgba(15, 23, 42, 0.03);
  }
  .account-panel-header {
    display: flex;
    align-items: flex-start;
    justify-content: space-between;
    flex-wrap: wrap;
    gap: 14px;
    margin-bottom: 0;
  }
  .account-header-actions {
    display: flex;
    align-items: center;
    gap: 8px;
    flex-shrink: 0;
  }
  .account-toolbar {
    display: flex;
    align-items: center;
    flex-wrap: wrap;
    gap: 12px;
    margin-top: 18px;
    padding-top: 16px;
    border-top: 1px solid #F1F5F9;
  }
  .account-search-wrap {
    position: relative;
    flex: 1 1 240px;
    min-width: 200px;
  }
  .account-search-icon {
    position: absolute;
    top: 50%;
    left: 10px;
    transform: translateY(-50%);
    color: #94A3B8;
    pointer-events: none;
  }
  .account-search-input {
    width: 100%;
    height: 36px;
    padding: 0 28px 0 34px;
    border: 1px solid #E2E8F0;
    border-radius: 8px;
    background: #FFFFFF;
    color: #0F172A;
    font: inherit;
    font-size: 13px;
    transition: all 0.15s ease;
  }
  .account-search-input:focus {
    outline: none;
    border-color: #2563EB;
    box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.12);
  }
  .account-search-clear {
    position: absolute;
    top: 50%;
    right: 8px;
    transform: translateY(-50%);
    display: grid;
    width: 20px;
    height: 20px;
    place-items: center;
    border: 0;
    border-radius: 50%;
    background: #F1F5F9;
    color: #64748B;
    cursor: pointer;
  }
  .account-search-clear:hover {
    background: #E2E8F0;
    color: #0F172A;
  }
  .account-filters {
    display: flex;
    align-items: center;
    flex-wrap: wrap;
    gap: 8px;
  }
  .account-filter-item {
    display: flex;
    align-items: center;
    gap: 6px;
  }
  .filter-label {
    font-size: 12px;
    color: #64748B;
    font-weight: 500;
  }
  .filter-select {
    height: 36px;
    padding: 0 10px;
    border: 1px solid #E2E8F0;
    border-radius: 6px;
    background: #FFFFFF;
    color: #0F172A;
    font: inherit;
    font-size: 12.5px;
    cursor: pointer;
    transition: border-color 0.15s ease;
  }
  .filter-select:focus {
    outline: none;
    border-color: #2563EB;
    box-shadow: 0 0 0 2px rgba(37, 99, 235, 0.12);
  }
  .account-reset-filter-btn {
    display: inline-flex;
    align-items: center;
    gap: 5px;
    height: 36px;
    padding: 0 11px;
    border: 1px solid #E2E8F0;
    border-radius: 6px;
    background: #F8FAFC;
    color: #475569;
    font: inherit;
    font-size: 12px;
    font-weight: 500;
    cursor: pointer;
    transition: all 0.15s ease;
  }
  .account-reset-filter-btn:hover {
    background: #F1F5F9;
    color: #0F172A;
    border-color: #CBD5E1;
  }
  .account-meta-summary {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 12px 0 14px;
    font-size: 12.5px;
    color: #64748B;
  }
  .account-meta-summary strong {
    color: #0F172A;
    font-weight: 600;
  }
  .table-account-details {
    display: flex;
    flex-direction: column;
    min-width: 0;
    gap: 1px;
  }
  .account-username {
    font-size: 13.5px;
    font-weight: 600;
    color: #0F172A;
  }
  .account-id {
    font-size: 11px;
    color: #94A3B8;
  }
  .row-disabled {
    background: #F8FAFC;
    opacity: 0.75;
  }
  .row-disabled:hover {
    background: #F1F5F9 !important;
  }
  .account-mobile-grid {
    display: none;
    flex-direction: column;
    gap: 12px;
  }
  .account-mobile-card {
    display: flex;
    flex-direction: column;
    gap: 12px;
    padding: 14px 16px;
    border: 1px solid #E2E8F0;
    border-radius: 10px;
    background: #FFFFFF;
    box-shadow: 0 1px 2px rgba(15, 23, 42, 0.03);
  }
  .account-mobile-card.card-disabled {
    background: #F8FAFC;
    opacity: 0.75;
  }
  .account-card-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 10px;
  }
  .account-card-body {
    display: flex;
    flex-direction: column;
    gap: 10px;
    padding: 10px 0;
    border-top: 1px solid #F1F5F9;
    border-bottom: 1px solid #F1F5F9;
  }
  .card-field-row {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 10px;
  }
  .card-field-label {
    font-size: 12px;
    color: #64748B;
    font-weight: 500;
  }
  .account-card-actions {
    display: flex;
    gap: 8px;
  }
  .mobile-card-btn {
    flex: 1;
    justify-content: center;
    height: 34px;
    font-size: 12.5px;
  }
  .account-pagination {
    display: flex;
    align-items: center;
    justify-content: space-between;
    flex-wrap: wrap;
    gap: 12px;
    padding-top: 16px;
    margin-top: 14px;
    border-top: 1px solid #F1F5F9;
  }
  .pagination-size {
    display: flex;
    align-items: center;
    gap: 8px;
    font-size: 12.5px;
    color: #64748B;
  }
  .pagination-select {
    height: 32px;
    padding: 0 8px;
    border: 1px solid #E2E8F0;
    border-radius: 6px;
    background: #FFFFFF;
    color: #0F172A;
    font: inherit;
    font-size: 12px;
    cursor: pointer;
  }
  .pagination-nav {
    display: flex;
    align-items: center;
    gap: 10px;
  }
  .pagination-btn {
    display: inline-flex;
    align-items: center;
    gap: 4px;
    height: 32px;
    padding: 0 10px;
    border: 1px solid #E2E8F0;
    border-radius: 6px;
    background: #FFFFFF;
    color: #334155;
    font: inherit;
    font-size: 12px;
    font-weight: 500;
    cursor: pointer;
    transition: all 0.15s ease;
  }
  .pagination-btn:hover:not(:disabled) {
    background: #F8FAFC;
    color: #0F172A;
    border-color: #CBD5E1;
  }
  .pagination-btn:disabled {
    opacity: 0.45;
    cursor: not-allowed;
  }
  .pagination-status {
    font-size: 12.5px;
    color: #64748B;
  }
  .pagination-status strong {
    color: #0F172A;
  }

  @media (max-width: 768px) {
    .account-desktop-table { display: none; }
    .account-mobile-grid { display: flex; }
    .account-main-panel { padding: 16px; }
    .account-search-wrap { min-width: min(100%, 200px); }
    .account-filters { width: 100%; }
    .account-filter-item { flex: 1 1 160px; }
    .filter-select { width: 100%; min-width: 0; }
    .account-meta-summary { flex-wrap: wrap; gap: 8px; }
  }

  @media (max-width: 520px) {
    .account-main-panel { padding: 14px; }
    .account-panel-header { align-items: stretch; }
    .account-header-actions { flex-wrap: wrap; }
    .account-search-wrap { flex-basis: 100%; min-width: 0; }
    .account-filters > * { flex: 1 1 100%; }
    .account-reset-filter-btn { justify-content: center; }
    .account-mobile-card { padding: 13px; }
    .account-card-header { min-width: 0; align-items: flex-start; }
    .account-card-header .table-account { min-width: 0; flex: 1 1 0; }
    .account-card-header .account-state { flex: 0 0 auto; }
    .account-username { min-width: 0; overflow-wrap: anywhere; }
  }
</style>
