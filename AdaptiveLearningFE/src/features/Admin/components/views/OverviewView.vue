<script setup>
import { inject } from 'vue'
import {
  ArrowRight,
  BarChart3,
  BookOpen,
  Calendar,
  CheckCircle2,
  ChevronRight,
  CircleCheck,
  CreditCard,
  DollarSign,
  GraduationCap,
  Layers,
  LayoutDashboard,
  LoaderCircle,
  Plus,
  RefreshCw,
  Search,
  Sparkles,
  TrendingUp,
  UserCheck,
  Users,
  Wallet
} from 'lucide-vue-next'
import { adminContextKey } from '../../composables/adminContext'

const admin = inject(adminContextKey)

const {
  activeSection,
  analyticsPeriod,
  analyticsPeriods,
  analyticsCertificateId,
  analyticsLoading,
  analyticsError,
  analyticsData,
  programs,
  loadAnalytics,
  openSection,
  openAccount,
  openProgram,
  formatAdminNumber,
  formatPercent,
  formatSignedPoint,
  formatTrendDate,
  formatMoney,
  openLearnerReport,
  analyticsInsight,
  displayAnalyticsData,
  knowledgeChunkCount,
  knowledgeDocuments,
  activeKnowledgeCount,
  studentAccountsCount,
  activeAccountsCount,
  premiumAccountsCount,
  analyticsChartPoints,
  analyticsScorePath,
  analyticsMaxAttempts,
  exams
} = admin
</script>

<template>
  <div class="admin-overview-view">
          <div class="overview-container">
            <!-- 1. Real-Data KPI Summary Cards -->
            <div class="overview-kpi-grid">
              <!-- KPI 1: Học viên hệ thống -->
              <div class="overview-kpi-card is-interactive" @click="openSection('accounts')">
                <div class="overview-kpi-header">
                  <div class="overview-kpi-icon icon-blue">
                    <Users :size="20" />
                  </div>
                  <span class="overview-kpi-action">Quản lý →</span>
                </div>
                <div class="overview-kpi-info">
                  <span class="overview-kpi-label">Học viên hệ thống</span>
                  <strong class="overview-kpi-value">{{ formatAdminNumber(studentAccountsCount) }}</strong>
                  <div class="overview-kpi-meta">
                    <span class="status-dot online"></span>
                    <span>{{ formatAdminNumber(activeAccountsCount) }} hoạt động · {{ formatAdminNumber(premiumAccountsCount) }} Premium</span>
                  </div>
                </div>
              </div>

              <!-- KPI 2: Lượt làm bài trong kỳ -->
              <div class="overview-kpi-card">
                <div class="overview-kpi-header">
                  <div class="overview-kpi-icon icon-teal">
                    <ClipboardCheck :size="20" />
                  </div>
                  <span
                    v-if="displayAnalyticsData?.completionRateChange != null"
                    :class="['overview-delta-badge', displayAnalyticsData.completionRateChange > 0 ? 'delta-positive' : displayAnalyticsData.completionRateChange < 0 ? 'delta-negative' : 'delta-neutral']"
                  >
                    {{ displayAnalyticsData.completionRateChange > 0 ? '+' : displayAnalyticsData.completionRateChange < 0 ? '−' : '' }}{{ formatSignedPoint(displayAnalyticsData.completionRateChange) }}%
                  </span>
                </div>
                <div class="overview-kpi-info">
                  <span class="overview-kpi-label">Lượt thi trong kỳ ({{ analyticsPeriods.find((p) => p.id === analyticsPeriod)?.label }})</span>
                  <strong class="overview-kpi-value">{{ formatAdminNumber(displayAnalyticsData?.startedAttempts ?? 0) }}</strong>
                  <div class="overview-kpi-meta">
                    <span>{{ formatAdminNumber(displayAnalyticsData?.completedAttempts ?? 0) }} đã nộp bài ({{ formatPercent(displayAnalyticsData?.completionRate) }})</span>
                  </div>
                </div>
              </div>

              <!-- KPI 3: Điểm trung bình & Tỷ lệ đạt -->
              <div class="overview-kpi-card">
                <div class="overview-kpi-header">
                  <div class="overview-kpi-icon icon-green">
                    <TrendingUp :size="20" />
                  </div>
                  <span
                    v-if="displayAnalyticsData?.averageScoreChange != null"
                    :class="['overview-delta-badge', displayAnalyticsData.averageScoreChange > 0 ? 'delta-positive' : displayAnalyticsData.averageScoreChange < 0 ? 'delta-negative' : 'delta-neutral']"
                  >
                    {{ displayAnalyticsData.averageScoreChange > 0 ? '+' : displayAnalyticsData.averageScoreChange < 0 ? '−' : '' }}{{ formatSignedPoint(displayAnalyticsData.averageScoreChange) }}đ
                  </span>
                </div>
                <div class="overview-kpi-info">
                  <span class="overview-kpi-label">Điểm trung bình lượt thi</span>
                  <strong class="overview-kpi-value">{{ formatPercent(displayAnalyticsData?.averageScorePercent) }}</strong>
                  <div class="overview-kpi-meta">
                    <span>Tỷ lệ đạt chuẩn: <strong>{{ formatPercent(displayAnalyticsData?.passRate) }}</strong></span>
                  </div>
                </div>
              </div>

              <!-- KPI 4: Học liệu & Tri thức RAG -->
              <div class="overview-kpi-card is-interactive" @click="openSection('knowledge')">
                <div class="overview-kpi-header">
                  <div class="overview-kpi-icon icon-purple">
                    <Database :size="20" />
                  </div>
                  <span class="overview-kpi-action">Kho tri thức →</span>
                </div>
                <div class="overview-kpi-info">
                  <span class="overview-kpi-label">Học liệu & Tri thức RAG</span>
                  <strong class="overview-kpi-value">{{ formatAdminNumber(exams.length) }} <span class="overview-kpi-unit">đề thi</span></strong>
                  <div class="overview-kpi-meta">
                    <span>{{ programs.length }} chứng chỉ · {{ knowledgeDocuments.length }} tài liệu ({{ formatAdminNumber(knowledgeChunkCount) }} chunks)</span>
                  </div>
                </div>
              </div>
            </div>

            <!-- 2. Loading State -->
            <div v-if="analyticsLoading" class="overview-status-card overview-loading-state" role="status" aria-live="polite">
              <LoaderCircle :size="32" class="overlay-spinner" />
              <div class="status-text">
                <h3>Đang tổng hợp dữ liệu học tập...</h3>
                <p>Hệ thống đang truy vấn số liệu thi và tiến độ người học.</p>
              </div>
            </div>

            <!-- 3. Error State -->
            <div v-else-if="analyticsError" class="overview-status-card overview-error-state" role="alert">
              <div class="error-info">
                <Info :size="24" class="error-icon" />
                <div>
                  <h3>Không thể tải báo cáo thống kê</h3>
                  <p>{{ analyticsError }}</p>
                </div>
              </div>
              <button type="button" class="admin-secondary-button" @click="loadAnalytics">
                <RefreshCw :size="15" />
                <span>Thử lại</span>
              </button>
            </div>

            <!-- 4. Content Area (When not loading and no error) -->
            <div v-else class="overview-content-wrapper">
              <!-- AI Insight Banner (when real attempts exist) -->
              <div v-if="displayAnalyticsData && displayAnalyticsData.startedAttempts > 0" class="overview-insight-banner">
                <div class="insight-badge">
                  <Sparkles :size="15" />
                  <span>PHÂN TÍCH NHANH</span>
                </div>
                <p class="insight-content">{{ analyticsInsight }}</p>
                <span class="insight-period">{{ analyticsPeriods.find((p) => p.id === analyticsPeriod)?.label }} qua · {{ programs.find((program) => String(program.certificateId) === String(analyticsCertificateId))?.name || 'Tất cả kỳ thi' }}</span>
              </div>

              <!-- Notice Banner when 0 attempts recorded -->
              <div v-else class="overview-notice-banner">
                <Info :size="18" class="notice-icon" />
                <div class="notice-content">
                  <strong>Chưa có lượt thi trong khoảng thời gian {{ analyticsPeriods.find((p) => p.id === analyticsPeriod)?.label.toLowerCase() }}</strong>
                  <p>Khi học viên làm bài kiểm tra, biểu đồ xu hướng điểm và danh sách học viên cần hỗ trợ sẽ được tính toán tự động.</p>
                </div>
              </div>

              <!-- 2-Column Analytics Details Grid -->
              <div class="overview-detail-grid">
                <!-- Column 1: SVG Trend Chart -->
                <section class="overview-card" aria-labelledby="overview-chart-title">
                  <div class="overview-card-header">
                    <div>
                      <h2 id="overview-chart-title" class="overview-card-title">Xu hướng điểm số & Lượt thi</h2>
                      <p class="overview-card-subtitle">Điểm trung bình (%) và số lượt nộp bài theo từng mốc thời gian</p>
                    </div>
                    <div class="overview-chart-legend">
                      <span class="legend-item">
                        <span class="legend-color line-color"></span>
                        Điểm TB (%)
                      </span>
                      <span class="legend-item">
                        <span class="legend-color bar-color"></span>
                        Số lượt thi
                      </span>
                    </div>
                  </div>

                  <div class="overview-card-body">
                    <div v-if="!analyticsChartPoints.some((point) => point.y != null)" class="overview-empty-box">
                      <BarChart3 :size="36" class="empty-icon" />
                      <h4>Chưa có bài thi được chấm điểm</h4>
                      <p>Không có bài thi nào được hoàn thành và chấm điểm trong kỳ này để biểu diễn trên biểu đồ.</p>
                    </div>

                    <div v-else class="overview-chart-wrap">
                      <svg class="analytics-chart" viewBox="0 0 720 300" role="img" aria-label="Biểu đồ điểm trung bình và lượt thi">
                        <g class="analytics-chart-grid">
                          <template v-for="tick in [0, 25, 50, 75, 100]" :key="tick">
                            <line x1="48" :y1="22 + (100 - tick) * 2.1" x2="672" :y2="22 + (100 - tick) * 2.1" />
                            <text x="36" :y="26 + (100 - tick) * 2.1">{{ tick }}%</text>
                          </template>
                        </g>
                        <g class="analytics-chart-bars" aria-hidden="true">
                          <rect v-for="(point, index) in analyticsChartPoints" :key="`bar-${index}`" :x="point.x - 10" :y="256 - Math.max(3, point.attempts / analyticsMaxAttempts * 32)" width="20" :height="Math.max(3, point.attempts / analyticsMaxAttempts * 32)" rx="3" />
                        </g>
                        <path v-if="analyticsScorePath" class="analytics-score-line" :d="analyticsScorePath" />
                        <g v-for="(point, index) in analyticsChartPoints" :key="`point-${index}`">
                          <circle v-if="point.y != null" class="analytics-score-point" :cx="point.x" :cy="point.y" r="4.5">
                            <title>{{ point.label }}: {{ formatPercent(point.averageScorePercent) }} ({{ point.attempts }} lượt)</title>
                          </circle>
                          <g v-if="point.annotation" class="analytics-annotation">
                            <title>{{ point.annotation }} · {{ point.label }}</title>
                            <line :x1="point.x" :y1="256 - Math.max(3, point.attempts / analyticsMaxAttempts * 32) - 2" :x2="point.x" :y2="256 - Math.max(3, point.attempts / analyticsMaxAttempts * 32) - 14" />
                            <rect :x="Math.min(625, Math.max(48, point.x - 42))" :y="256 - Math.max(3, point.attempts / analyticsMaxAttempts * 32) - 34" width="84" height="20" rx="4" />
                            <text :x="Math.min(625, Math.max(48, point.x - 42)) + 42" :y="256 - Math.max(3, point.attempts / analyticsMaxAttempts * 32) - 20">{{ point.annotation }}</text>
                          </g>
                          <text :class="['analytics-chart-label', { 'label-hidden-mobile': analyticsChartPoints.length > 8 && index % 2 === 1 }]" :x="point.x" y="280">{{ point.label }}</text>
                        </g>
                        <text class="analytics-chart-unit" x="48" y="16">Thang điểm (%)</text>
                      </svg>
                    </div>
                  </div>
                </section>

                <!-- Column 2: Learners Needing Support Table -->
                <section class="overview-card" aria-labelledby="overview-learners-title">
                  <div class="overview-card-header">
                    <div>
                      <h2 id="overview-learners-title" class="overview-card-title">Học viên cần theo dõi</h2>
                      <p class="overview-card-subtitle">Xếp theo điểm trung bình thấp nhất trong kỳ</p>
                    </div>
                    <span v-if="displayAnalyticsData?.learnersToSupport?.length" class="overview-badge-count">
                      {{ displayAnalyticsData.learnersToSupport.length }}
                    </span>
                  </div>

                  <div class="overview-card-body p-0">
                    <div v-if="!displayAnalyticsData?.learnersToSupport?.length" class="overview-empty-box">
                      <UserCheck :size="36" class="empty-icon icon-success" />
                      <h4>Không có học viên dưới chuẩn</h4>
                      <p>Tất cả học viên hoàn thành bài thi đều đạt kết quả tốt hoặc chưa có dữ liệu điểm thấp cần hỗ trợ.</p>
                    </div>

                    <div v-else class="overview-table-wrap">
                      <table class="overview-table">
                        <thead>
                          <tr>
                            <th>Học viên</th>
                            <th class="text-center">Lượt thi</th>
                            <th class="text-right">Điểm TB</th>
                            <th class="text-right">Báo cáo</th>
                          </tr>
                        </thead>
                        <tbody>
                          <tr v-for="learner in displayAnalyticsData.learnersToSupport" :key="learner.userId">
                            <td>
                              <div class="learner-cell">
                                <div class="learner-avatar">
                                  {{ (learner.username || 'U').slice(0, 2).toUpperCase() }}
                                </div>
                                <div class="learner-meta">
                                  <span class="learner-name" :title="learner.username">{{ learner.username }}</span>
                                  <small class="learner-date">{{ learner.lastAttemptAt ? formatAttemptDate(learner.lastAttemptAt) : 'Chưa có ngày thi' }}</small>
                                </div>
                              </div>
                            </td>
                            <td class="text-center">
                              <span class="attempts-pill">{{ learner.attempts }}</span>
                            </td>
                            <td class="text-right">
                              <span
                                :class="[
                                  'score-badge',
                                  learner.averageScorePercent < 50 ? 'score-low' : learner.averageScorePercent < 70 ? 'score-medium' : 'score-high'
                                ]"
                              >
                                {{ formatPercent(learner.averageScorePercent) }}
                              </span>
                            </td>
                            <td class="text-right">
                              <button
                                type="button"
                                class="overview-action-btn"
                                title="Xem chi tiết kết quả học tập"
                                @click="openLearnerReport(learner)"
                              >
                                Xem kết quả
                              </button>
                            </td>
                          </tr>
                        </tbody>
                      </table>
                    </div>
                  </div>
                </section>
              </div>

              <!-- 3 System Operations Pillars -->
              <div class="overview-pillars-grid">
                <!-- Pillar 1: Accounts -->
                <div class="overview-pillar-card" @click="openSection('accounts')">
                  <div class="pillar-header">
                    <div class="pillar-icon pillar-icon-blue">
                      <Users :size="20" />
                    </div>
                    <div>
                      <h3 class="pillar-title">Vận hành Tài khoản</h3>
                      <p class="pillar-desc">Quản lý người học, phân quyền và trạng thái</p>
                    </div>
                  </div>
                  <div class="pillar-stats">
                    <div class="pillar-stat-item">
                      <span class="stat-number">{{ formatAdminNumber(totalAccountsCount) }}</span>
                      <span class="stat-label">Tổng tài khoản</span>
                    </div>
                    <div class="pillar-stat-item">
                      <span class="stat-number">{{ formatAdminNumber(studentAccountsCount) }}</span>
                      <span class="stat-label">Học viên</span>
                    </div>
                    <div class="pillar-stat-item">
                      <span class="stat-number">{{ formatAdminNumber(activeAccountsCount) }}</span>
                      <span class="stat-label">Hoạt động</span>
                    </div>
                  </div>
                  <div class="pillar-footer">
                    <span>Xem danh sách tài khoản</span>
                    <span class="arrow-icon">→</span>
                  </div>
                </div>

                <!-- Pillar 2: Exams & Programs -->
                <div class="overview-pillar-card" @click="openSection('exams')">
                  <div class="pillar-header">
                    <div class="pillar-icon pillar-icon-emerald">
                      <GraduationCap :size="20" />
                    </div>
                    <div>
                      <h3 class="pillar-title">Đào tạo & Khảo thí</h3>
                      <p class="pillar-desc">Ngân hàng đề thi, bài kiểm tra và chứng chỉ</p>
                    </div>
                  </div>
                  <div class="pillar-stats">
                    <div class="pillar-stat-item">
                      <span class="stat-number">{{ formatAdminNumber(programs.length) }}</span>
                      <span class="stat-label">Chứng chỉ</span>
                    </div>
                    <div class="pillar-stat-item">
                      <span class="stat-number">{{ formatAdminNumber(exams.length) }}</span>
                      <span class="stat-label">Bài kiểm tra</span>
                    </div>
                    <div class="pillar-stat-item">
                      <span class="stat-number">{{ formatAdminNumber(displayAnalyticsData?.activeLearners ?? 0) }}</span>
                      <span class="stat-label">Thí sinh trong kỳ</span>
                    </div>
                  </div>
                  <div class="pillar-footer">
                    <span>Quản lý đề thi & kỳ thi</span>
                    <span class="arrow-icon">→</span>
                  </div>
                </div>

                <!-- Pillar 3: Knowledge Base & RAG -->
                <div class="overview-pillar-card" @click="openSection('knowledge')">
                  <div class="pillar-header">
                    <div class="pillar-icon pillar-icon-purple">
                      <Database :size="20" />
                    </div>
                    <div>
                      <h3 class="pillar-title">Kho tri thức & RAG AI</h3>
                      <p class="pillar-desc">Tài liệu kiến thức nạp vào hệ thống AI Tutor</p>
                    </div>
                  </div>
                  <div class="pillar-stats">
                    <div class="pillar-stat-item">
                      <span class="stat-number">{{ formatAdminNumber(knowledgeDocuments.length) }}</span>
                      <span class="stat-label">Tài liệu RAG</span>
                    </div>
                    <div class="pillar-stat-item">
                      <span class="stat-number">{{ formatAdminNumber(activeKnowledgeCount) }}</span>
                      <span class="stat-label">Đang hiệu lực</span>
                    </div>
                    <div class="pillar-stat-item">
                      <span class="stat-number">{{ formatAdminNumber(knowledgeChunkCount) }}</span>
                      <span class="stat-label">Đoạn tri thức</span>
                    </div>
                  </div>
                  <div class="pillar-footer">
                    <span>Quản lý kho tri thức RAG</span>
                    <span class="arrow-icon">→</span>
                  </div>
                </div>
              </div>
            </div>
          </div>
      </div>
</template>

<style scoped>
  /* ========================================================
     ADMIN OVERVIEW / DASHBOARD — LIGHT AI EDUCATION
     ======================================================== */
  .overview-container {
    display: flex;
    flex-direction: column;
    gap: 20px;
    width: 100%;
  }

  /* Real-Data KPI Summary Cards */
  .overview-kpi-grid {
    display: grid;
    grid-template-columns: repeat(4, minmax(0, 1fr));
    gap: 16px;
  }
  .overview-kpi-card {
    display: flex;
    flex-direction: column;
    justify-content: space-between;
    gap: 12px;
    padding: 18px 20px;
    background: #FFFFFF;
    border: 1px solid #E2E8F0;
    border-radius: 12px;
    box-shadow: 0 1px 3px rgba(15, 23, 42, 0.03);
    transition: all 0.2s ease;
  }
  .overview-kpi-card.is-interactive {
    cursor: pointer;
  }
  .overview-kpi-card.is-interactive:hover {
    border-color: #CBD5E1;
    box-shadow: 0 4px 12px rgba(15, 23, 42, 0.06);
    transform: translateY(-1px);
  }
  .overview-kpi-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
  }
  .overview-kpi-icon {
    display: grid;
    place-items: center;
    width: 42px;
    height: 42px;
    border-radius: 10px;
    flex-shrink: 0;
  }
  .overview-kpi-icon.icon-blue {
    background: #EFF6FF;
    color: #2563EB;
    border: 1px solid #DBEAFE;
  }
  .overview-kpi-icon.icon-teal {
    background: #F0F9FF;
    color: #0284C7;
    border: 1px solid #E0F2FE;
  }
  .overview-kpi-icon.icon-green {
    background: #F0FDF4;
    color: #16A34A;
    border: 1px solid #DCFCE7;
  }
  .overview-kpi-icon.icon-purple {
    background: #F5F3FF;
    color: #7C3AED;
    border: 1px solid #EDE9FE;
  }
  .overview-kpi-action {
    font-size: 12px;
    font-weight: 600;
    color: #2563EB;
    transition: transform 0.15s ease;
  }
  .overview-kpi-card.is-interactive:hover .overview-kpi-action {
    transform: translateX(2px);
  }
  .overview-kpi-info {
    display: flex;
    flex-direction: column;
    gap: 4px;
    min-width: 0;
  }
  .overview-kpi-label {
    font-size: 12.5px;
    font-weight: 500;
    color: #64748B;
  }
  .overview-kpi-value {
    font-size: 26px;
    font-weight: 700;
    color: #0F172A;
    line-height: 1.2;
    letter-spacing: -0.02em;
    font-variant-numeric: tabular-nums;
  }
  .overview-kpi-unit {
    font-size: 14px;
    font-weight: 500;
    color: #64748B;
  }
  .overview-kpi-meta {
    display: flex;
    align-items: center;
    gap: 8px;
    font-size: 12px;
    color: #64748B;
    margin-top: 2px;
  }
  .status-dot {
    width: 7px;
    height: 7px;
    border-radius: 50%;
    display: inline-block;
  }
  .status-dot.online {
    background: #16A34A;
    box-shadow: 0 0 0 2px #DCFCE7;
  }
  .overview-delta-badge {
    display: inline-flex;
    align-items: center;
    padding: 2px 7px;
    border-radius: 4px;
    font-size: 11px;
    font-weight: 600;
    font-variant-numeric: tabular-nums;
  }
  .overview-delta-badge.delta-positive {
    background: #F0FDF4;
    color: #16A34A;
    border: 1px solid #DCFCE7;
  }
  .overview-delta-badge.delta-negative {
    background: #FEF2F2;
    color: #DC2626;
    border: 1px solid #FEE2E2;
  }
  .overview-delta-badge.delta-neutral {
    background: #F1F5F9;
    color: #64748B;
    border: 1px solid #E2E8F0;
  }

  /* Status Cards (Loading & Error) */
  .overview-status-card {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    text-align: center;
    gap: 14px;
    padding: 44px 24px;
    background: #FFFFFF;
    border: 1px solid #E2E8F0;
    border-radius: 12px;
  }
  .overview-status-card .status-text h3 {
    margin: 0 0 6px;
    font-size: 16px;
    font-weight: 600;
    color: #0F172A;
  }
  .overview-status-card .status-text p {
    margin: 0;
    font-size: 13px;
    color: #64748B;
  }
  .overview-error-state {
    background: #FEF2F2;
    border-color: #FECACA;
    flex-direction: row;
    justify-content: space-between;
    text-align: left;
    padding: 24px 28px;
  }
  .overview-error-state .error-info {
    display: flex;
    align-items: flex-start;
    gap: 14px;
  }
  .overview-error-state .error-icon {
    color: #DC2626;
    flex-shrink: 0;
    margin-top: 2px;
  }
  .overview-error-state h3 {
    margin: 0 0 4px;
    font-size: 15px;
    font-weight: 600;
    color: #991B1B;
  }
  .overview-error-state p {
    margin: 0;
    font-size: 13px;
    color: #B91C1C;
  }

  /* Insight & Notice Banners */
  .overview-insight-banner {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 16px;
    padding: 12px 18px;
    background: #F5F3FF;
    border: 1px solid #EDE9FE;
    border-radius: 10px;
  }
  .insight-badge {
    display: inline-flex;
    align-items: center;
    gap: 6px;
    padding: 3px 9px;
    background: #EDE9FE;
    color: #7C3AED;
    border-radius: 6px;
    font-size: 11px;
    font-weight: 700;
    letter-spacing: 0.04em;
    white-space: nowrap;
    flex-shrink: 0;
  }
  .insight-content {
    margin: 0;
    font-size: 13.5px;
    font-weight: 500;
    color: #0F172A;
    line-height: 1.5;
    flex: 1;
  }
  .insight-period {
    font-size: 12px;
    font-weight: 500;
    color: #7C3AED;
    white-space: nowrap;
    flex-shrink: 0;
  }

  .overview-notice-banner {
    display: flex;
    align-items: flex-start;
    gap: 14px;
    padding: 14px 18px;
    background: #EFF6FF;
    border: 1px solid #DBEAFE;
    border-radius: 10px;
  }
  .notice-icon {
    color: #2563EB;
    flex-shrink: 0;
    margin-top: 2px;
  }
  .notice-content strong {
    display: block;
    font-size: 13.5px;
    font-weight: 600;
    color: #1E40AF;
    margin-bottom: 2px;
  }
  .notice-content p {
    margin: 0;
    font-size: 13px;
    color: #3B82F6;
    line-height: 1.45;
  }

  /* 2-Column Analytics Details */
  .overview-detail-grid {
    display: grid;
    grid-template-columns: minmax(0, 1.45fr) minmax(340px, 1fr);
    gap: 20px;
  }
  .overview-card {
    display: flex;
    flex-direction: column;
    background: #FFFFFF;
    border: 1px solid #E2E8F0;
    border-radius: 12px;
    box-shadow: 0 1px 3px rgba(15, 23, 42, 0.03);
    overflow: hidden;
  }
  .overview-card-header {
    display: flex;
    align-items: flex-start;
    justify-content: space-between;
    gap: 16px;
    padding: 18px 20px 14px;
    border-bottom: 1px solid #F1F5F9;
  }
  .overview-card-title {
    margin: 0;
    font-size: 15.5px;
    font-weight: 600;
    color: #0F172A;
    line-height: 1.3;
  }
  .overview-card-subtitle {
    margin: 3px 0 0;
    font-size: 12px;
    color: #64748B;
  }
  .overview-badge-count {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    min-width: 24px;
    height: 24px;
    padding: 0 8px;
    border-radius: 12px;
    background: #EFF6FF;
    color: #2563EB;
    font-size: 12px;
    font-weight: 600;
  }
  .overview-card-body {
    padding: 20px;
    flex: 1;
    display: flex;
    flex-direction: column;
  }
  .overview-card-body.p-0 {
    padding: 0;
  }
  .overview-chart-legend {
    display: flex;
    align-items: center;
    gap: 14px;
    font-size: 12px;
    color: #64748B;
  }
  .overview-chart-legend .legend-item {
    display: inline-flex;
    align-items: center;
    gap: 6px;
  }
  .overview-chart-legend .legend-color {
    display: inline-block;
    border-radius: 2px;
  }
  .overview-chart-legend .line-color {
    width: 16px;
    height: 3px;
    background: #2563EB;
  }
  .overview-chart-legend .bar-color {
    width: 10px;
    height: 10px;
    background: #CBD5E1;
  }

  .overview-empty-box {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    text-align: center;
    padding: 44px 20px;
    color: #64748B;
    margin: auto;
  }
  .overview-empty-box .empty-icon {
    color: #94A3B8;
    margin-bottom: 12px;
  }
  .overview-empty-box .empty-icon.icon-success {
    color: #16A34A;
  }
  .overview-empty-box h4 {
    margin: 0 0 6px;
    font-size: 14.5px;
    font-weight: 600;
    color: #0F172A;
  }
  .overview-empty-box p {
    margin: 0;
    font-size: 12.5px;
    color: #64748B;
    max-width: 340px;
    line-height: 1.5;
  }

  /* SVG Chart */
  .overview-chart-wrap {
    width: 100%;
    overflow: hidden;
  }
  .analytics-chart {
    display: block;
    width: 100%;
    height: auto;
    overflow: visible;
  }
  .analytics-chart-grid line {
    stroke: #F1F5F9;
    stroke-width: 1;
  }
  .analytics-chart-grid text {
    fill: #94A3B8;
    font-family: inherit;
    font-size: 11px;
    text-anchor: end;
  }
  .analytics-chart-unit {
    fill: #64748B;
    font-family: inherit;
    font-size: 11px;
    font-weight: 500;
    text-anchor: start;
  }
  .analytics-chart-bars rect {
    fill: #E2E8F0;
    transition: fill 0.15s ease;
  }
  .analytics-chart-bars rect:hover {
    fill: #CBD5E1;
  }
  .analytics-score-line {
    fill: none;
    stroke: #2563EB;
    stroke-linecap: round;
    stroke-linejoin: round;
    stroke-width: 2.5;
  }
  .analytics-score-point {
    fill: #FFFFFF;
    stroke: #2563EB;
    stroke-width: 2.5;
    cursor: pointer;
    transition: r 0.15s ease;
  }
  .analytics-score-point:hover {
    r: 6;
  }
  .analytics-chart-label {
    fill: #64748B;
    font-family: inherit;
    font-size: 11px;
    text-anchor: middle;
  }
  .analytics-annotation line {
    stroke: #2563EB;
    stroke-dasharray: 2 3;
    stroke-width: 1;
  }
  .analytics-annotation rect {
    fill: #FFFFFF;
    stroke: #93C5FD;
    stroke-width: 1;
  }
  .analytics-annotation text {
    fill: #2563EB;
    font-family: inherit;
    font-size: 10px;
    font-weight: 600;
    text-anchor: middle;
  }

  /* Table for Learners to Support */
  .overview-table-wrap {
    width: 100%;
    overflow-x: auto;
  }
  .overview-table {
    width: 100%;
    border-collapse: collapse;
    text-align: left;
  }
  .overview-table th {
    padding: 10px 16px;
    background: #F8FAFC;
    color: #64748B;
    font-size: 11.5px;
    font-weight: 600;
    border-bottom: 1px solid #E2E8F0;
    text-transform: uppercase;
    letter-spacing: 0.03em;
    white-space: nowrap;
  }
  .overview-table td {
    padding: 12px 16px;
    border-bottom: 1px solid #F1F5F9;
    color: #334155;
    font-size: 13px;
    vertical-align: middle;
  }
  .overview-table tr:last-child td {
    border-bottom: none;
  }
  .overview-table tr:hover td {
    background: #F8FAFC;
  }
  .learner-cell {
    display: flex;
    align-items: center;
    gap: 10px;
    min-width: 140px;
  }
  .learner-avatar {
    width: 32px;
    height: 32px;
    border-radius: 8px;
    background: #EFF6FF;
    color: #2563EB;
    display: grid;
    place-items: center;
    font-size: 11px;
    font-weight: 700;
    flex-shrink: 0;
    border: 1px solid #DBEAFE;
  }
  .learner-meta {
    display: flex;
    flex-direction: column;
    min-width: 0;
  }
  .learner-name {
    font-weight: 600;
    color: #0F172A;
    font-size: 13px;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
    max-width: 140px;
  }
  .learner-date {
    font-size: 11px;
    color: #94A3B8;
  }
  .attempts-pill {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    min-width: 24px;
    height: 22px;
    padding: 0 6px;
    border-radius: 6px;
    background: #F1F5F9;
    color: #475569;
    font-size: 12px;
    font-weight: 600;
  }
  .score-badge {
    display: inline-flex;
    align-items: center;
    padding: 2px 8px;
    border-radius: 6px;
    font-size: 12px;
    font-weight: 600;
    font-variant-numeric: tabular-nums;
  }
  .score-badge.score-low {
    background: #FEF2F2;
    color: #DC2626;
    border: 1px solid #FEE2E2;
  }
  .score-badge.score-medium {
    background: #FFFBEB;
    color: #D97706;
    border: 1px solid #FEF3C7;
  }
  .score-badge.score-high {
    background: #F0FDF4;
    color: #16A34A;
    border: 1px solid #DCFCE7;
  }
  .overview-action-btn {
    display: inline-flex;
    align-items: center;
    padding: 4px 10px;
    border-radius: 6px;
    border: 1px solid #E2E8F0;
    background: #FFFFFF;
    color: #2563EB;
    font: inherit;
    font-size: 12px;
    font-weight: 500;
    cursor: pointer;
    transition: all 0.15s ease;
    white-space: nowrap;
  }
  .overview-action-btn:hover {
    background: #EFF6FF;
    border-color: #BFDBFE;
  }

  /* 3 System Operations Pillars */
  .overview-pillars-grid {
    display: grid;
    grid-template-columns: repeat(3, minmax(0, 1fr));
    gap: 16px;
    margin-top: 4px;
  }
  .overview-pillar-card {
    display: flex;
    flex-direction: column;
    gap: 14px;
    padding: 18px 20px;
    background: #FFFFFF;
    border: 1px solid #E2E8F0;
    border-radius: 12px;
    box-shadow: 0 1px 3px rgba(15, 23, 42, 0.03);
    cursor: pointer;
    transition: all 0.2s ease;
  }
  .overview-pillar-card:hover {
    border-color: #CBD5E1;
    box-shadow: 0 4px 12px rgba(15, 23, 42, 0.06);
    transform: translateY(-1px);
  }
  .pillar-header {
    display: flex;
    align-items: flex-start;
    gap: 12px;
  }
  .pillar-icon {
    display: grid;
    place-items: center;
    width: 38px;
    height: 38px;
    border-radius: 10px;
    flex-shrink: 0;
  }
  .pillar-icon.pillar-icon-blue {
    background: #EFF6FF;
    color: #2563EB;
    border: 1px solid #DBEAFE;
  }
  .pillar-icon.pillar-icon-emerald {
    background: #F0FDF4;
    color: #16A34A;
    border: 1px solid #DCFCE7;
  }
  .pillar-icon.pillar-icon-purple {
    background: #F5F3FF;
    color: #7C3AED;
    border: 1px solid #EDE9FE;
  }
  .pillar-title {
    margin: 0;
    font-size: 14.5px;
    font-weight: 600;
    color: #0F172A;
  }
  .pillar-desc {
    margin: 2px 0 0;
    font-size: 12px;
    color: #64748B;
  }
  .pillar-stats {
    display: grid;
    grid-template-columns: repeat(3, minmax(0, 1fr));
    gap: 8px;
    padding: 10px 12px;
    background: #F8FAFC;
    border-radius: 8px;
    border: 1px solid #F1F5F9;
  }
  .pillar-stat-item {
    display: flex;
    flex-direction: column;
    gap: 2px;
    text-align: center;
  }
  .pillar-stat-item .stat-number {
    font-size: 15px;
    font-weight: 700;
    color: #0F172A;
    font-variant-numeric: tabular-nums;
  }
  .pillar-stat-item .stat-label {
    font-size: 11px;
    color: #64748B;
    white-space: nowrap;
  }
  .pillar-footer {
    display: flex;
    align-items: center;
    justify-content: space-between;
    font-size: 12px;
    font-weight: 600;
    color: #2563EB;
    margin-top: auto;
    padding-top: 4px;
  }
  .pillar-footer .arrow-icon {
    font-size: 14px;
    transition: transform 0.15s ease;
  }
  .overview-pillar-card:hover .pillar-footer .arrow-icon {
    transform: translateX(3px);
  }
  .admin-primary-button,
  .admin-secondary-button {
    display: inline-flex;
    height: 38px;
    box-sizing: border-box;
    align-items: center;
    justify-content: center;
    gap: 7px;
    padding: 0 14px;
    border: 1px solid transparent;
    border-radius: 8px;
    font: inherit;
    font-size: 13px;
    font-weight: 500;
    cursor: pointer;
    transition: all 0.15s ease;
    white-space: nowrap;
  }
  .admin-primary-button {
    background: #2563EB;
    color: #FFFFFF;
    box-shadow: 0 1px 2px rgba(0, 0, 0, 0.05);
  }
  .admin-primary-button:hover:not(:disabled) {
    background: #1D4ED8;
  }
  .admin-primary-button:disabled {
    opacity: 0.6;
    cursor: not-allowed;
  }
  .admin-secondary-button {
    border-color: #E2E8F0;
    background: #FFFFFF;
    color: #334155;
    box-shadow: 0 1px 2px rgba(0, 0, 0, 0.04);
  }
  .admin-secondary-button:hover {
    background: #F8FAFC;
    border-color: #CBD5E1;
    color: #0F172A;
  }
  .admin-stats {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(min(220px, 100%), 1fr));
    gap: 16px;
    margin-bottom: 24px;
  }
  .admin-stats article {
    display: flex;
    min-width: 0;
    min-height: 104px;
    align-items: flex-start;
    gap: 14px;
    padding: 16px 18px;
    border: 1px solid #E2E8F0;
    border-radius: 10px;
    background: #FFFFFF;
    box-shadow: 0 1px 2px rgba(0, 0, 0, 0.04);
    transition: all 0.15s ease;
  }
  .admin-stats article:hover {
    border-color: #CBD5E1;
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
  }
  .admin-stat-icon {
    display: grid;
    width: 38px;
    height: 38px;
    flex: 0 0 auto;
    place-items: center;
    border-radius: 8px;
    background: #F1F5F9;
    color: #2563EB;
    border: 1px solid #E2E8F0;
  }
  .admin-stat-icon.coral,
  .admin-stat-icon.violet,
  .admin-stat-icon.amber {
    background: #F1F5F9;
    color: #334155;
    border: 1px solid #E2E8F0;
  }
  .admin-stats article > div {
    display: grid;
    min-width: 0;
    gap: 2px;
    flex: 1 1 120px;
  }
  .admin-stats small {
    color: #64748B;
    font-size: 12.5px;
    font-weight: 500;
  }
  .admin-stats strong {
    color: #0F172A;
    font-size: 24px;
    line-height: 30px;
    font-weight: 700;
    font-variant-numeric: tabular-nums;
  }
  .stat-caption,
  .stat-link {
    display: flex;
    align-items: center;
    min-height: 24px;
    margin-left: 52px;
    color: #64748B;
    font-size: 12px;
  }
  .stat-link {
    gap: 4px;
    border: 0;
    background: none;
    color: #2563EB;
    font-weight: 500;
    cursor: pointer;
  }
  .admin-overview-grid {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(min(340px, 100%), 1fr));
    gap: 20px;
  }

  @media (max-width: 900px) {
    .overview-kpi-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); }
    .overview-detail-grid { grid-template-columns: minmax(0, 1fr); }
    .overview-pillars-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); }
  }

  @media (max-width: 520px) {
    .overview-kpi-grid { gap: 10px; }
    .overview-kpi-card { padding: 14px; }
    .overview-pillars-grid { grid-template-columns: minmax(0, 1fr); }
    .overview-card-header { flex-wrap: wrap; }
  }
  .admin-panel {
    min-width: 0;
    border: 1px solid #E2E8F0;
    border-radius: 12px;
    background: #FFFFFF;
    box-shadow: 0 1px 3px 0 rgba(0, 0, 0, 0.04), 0 1px 2px -1px rgba(0, 0, 0, 0.02);
  }
</style>
