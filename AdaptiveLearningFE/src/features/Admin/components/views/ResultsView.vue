<script setup>
import { inject } from 'vue'
import {
  BarChart3,
  Check,
  CheckCircle2,
  ChevronLeft,
  ChevronRight,
  ClipboardCheck,
  Eye,
  GraduationCap,
  Info,
  LoaderCircle,
  RefreshCw,
  RotateCcw,
  Search,
  Sparkles,
  TrendingUp,
  User,
  X
} from 'lucide-vue-next'
import { adminContextKey } from '../../composables/adminContext'

const admin = inject(adminContextKey)

const {
  loading,
  attemptSearch,
  attemptStatusFilter,
  attemptPage,
  attemptPageSize,
  attemptTotalPages,
  resetAttemptFilters,
  viewAttemptResult,
  examTitle,
  formatAttemptScore,
  formatAttemptDate,
  formatPercent,
  formatAdminNumber,
  programs,
  loadData,
  students,
  selectedLearner,
  selectedLearnerId,
  learnerCertificateId,
  loadLearnerReport,
  learnerLoading,
  learnerError,
  learnerAttempts,
  learnerProgress,
  learnerScopedAttempts,
  completedLearnerAttempts,
  learnerAverageScore,
  learnerLatestAttempt,
  learnerScoreTrend,
  learnerSkillCount,
  filteredLearnerAttempts,
  paginatedLearnerAttempts,
  formatTrendDate
} = admin
</script>

<template>
<section  class="admin-results-section">
          <!-- TOP HEADING PANEL -->
          <div class="admin-panel results-hero-panel">
            <div class="results-hero-content">
              <div class="results-hero-badge">
                <BarChart3 :size="15" />
                <span>KẾT QUẢ & TIẾN ĐỘ HỌC TẬP</span>
              </div>
              <h2>Báo cáo kết quả và tiến độ học tập</h2>
              <p>Theo dõi lịch sử làm bài thi, phân tích kỹ năng và đánh giá tiến độ điểm số của từng học viên.</p>
            </div>
            <div class="results-hero-controls">
              <div class="results-filter-control">
                <span class="control-label"><User :size="14" /> Học viên:</span>
                <select v-model="selectedLearnerId" class="results-select" @change="learnerCertificateId = ''; loadLearnerReport()">
                  <option value="" disabled>Chọn tài khoản học viên</option>
                  <option v-for="student in students" :key="student.userId" :value="String(student.userId)">
                    {{ student.username }} (#{{ student.userId }})
                  </option>
                </select>
              </div>

              <div v-if="programs.length" class="results-filter-control">
                <span class="control-label"><GraduationCap :size="14" /> Kỳ thi:</span>
                <select v-model="learnerCertificateId" class="results-select" @change="loadLearnerReport">
                  <option v-for="program in programs" :key="program.certificateId" :value="String(program.certificateId)">
                    {{ program.name }} ({{ program.code }})
                  </option>
                </select>
              </div>

              <button
                type="button"
                class="admin-secondary-button"
                :disabled="learnerLoading"
                title="Tải lại báo cáo học tập"
                @click="loadLearnerReport"
              >
                <RefreshCw :size="15" :class="{ 'overlay-spinner': learnerLoading }" />
                <span>Làm mới</span>
              </button>
            </div>
          </div>

          <!-- ERROR STATE -->
          <div v-if="learnerError" class="admin-alert-error" role="alert">
            <Info :size="16" />
            <span>{{ learnerError }}</span>
          </div>

          <!-- LOADING STATE -->
          <div v-if="learnerLoading" class="admin-empty admin-empty-loading">
            <LoaderCircle class="overlay-spinner" :size="24" />
            <span>Đang tải báo cáo học tập của học viên…</span>
          </div>

          <!-- LEARNER SELECTED -->
          <template v-else-if="selectedLearner">
            <!-- 4 SUMMARY KPI CARDS -->
            <div class="results-metrics-grid">
              <article class="results-stat-card blue">
                <div class="results-stat-icon">
                  <CheckCircle2 :size="20" />
                </div>
                <div class="results-stat-info">
                  <span>LƯỢT THI ĐÃ HOÀN THÀNH</span>
                  <strong>{{ completedLearnerAttempts.length }}</strong>
                  <small>{{ learnerScopedAttempts.length }} lượt thi trong kỳ thi đang chọn</small>
                </div>
              </article>

              <article class="results-stat-card emerald">
                <div class="results-stat-icon">
                  <TrendingUp :size="20" />
                </div>
                <div class="results-stat-info">
                  <span>ĐIỂM TRUNG BÌNH</span>
                  <strong>{{ learnerAverageScore == null ? '—' : `${Math.round(learnerAverageScore)}%` }}</strong>
                  <small>Tỷ lệ điểm các lượt thi đã chấm</small>
                </div>
              </article>

              <article class="results-stat-card purple">
                <div class="results-stat-icon">
                  <Sparkles :size="20" />
                </div>
                <div class="results-stat-info">
                  <span>LẦN THI GẦN NHẤT</span>
                  <strong>{{ learnerLatestAttempt ? formatAdminNumber(learnerLatestAttempt.bandScore ?? learnerLatestAttempt.score) : '—' }}</strong>
                  <small>{{ learnerLatestAttempt ? formatAttemptDate(learnerLatestAttempt.submittedAt || learnerLatestAttempt.startedAt) : 'Chưa có lượt thi' }}</small>
                </div>
              </article>

              <article class="results-stat-card amber">
                <div class="results-stat-icon">
                  <BarChart3 :size="20" />
                </div>
                <div class="results-stat-info">
                  <span>KỸ NĂNG CÓ DỮ LIỆU</span>
                  <strong>{{ learnerSkillCount }}</strong>
                  <small>Đo lường từ kết quả các bài thi đã nộp</small>
                </div>
              </article>
            </div>

            <!-- LEARNER ATTEMPTS HISTORY PANEL -->
            <div class="admin-panel results-history-panel">
              <div class="admin-panel-heading results-history-heading">
                <div>
                  <h2>Lịch sử làm bài thi</h2>
                  <p>{{ selectedLearner.username }} · {{ filteredLearnerAttempts.length }} lượt làm bài phù hợp</p>
                </div>
                <div class="results-table-filters">
                  <div class="results-search-box">
                    <Search :size="15" />
                    <input
                      v-model="attemptSearch"
                      type="search"
                      placeholder="Tìm bài thi hoặc mã lượt thi..."
                      aria-label="Tìm kiếm bài thi"
                    />
                    <button
                      v-if="attemptSearch"
                      type="button"
                      class="account-search-clear"
                      aria-label="Xóa tìm kiếm"
                      @click="attemptSearch = ''"
                    >
                      <X :size="14" />
                    </button>
                  </div>
                  <select v-model="attemptStatusFilter" class="results-select-filter" aria-label="Lọc trạng thái bài thi">
                    <option value="all">Tất cả trạng thái</option>
                    <option value="GRADED">Đã chấm điểm</option>
                    <option value="SUBMITTED">Đã nộp bài</option>
                    <option value="IN_PROGRESS">Đang làm bài</option>
                  </select>
                  <button
                    v-if="attemptSearch || attemptStatusFilter !== 'all'"
                    type="button"
                    class="account-reset-filter-btn"
                    @click="resetAttemptFilters"
                  >
                    <RotateCcw :size="13" />
                    <span>Đặt lại</span>
                  </button>
                </div>
              </div>

              <div class="account-meta-summary" style="padding: 10px 20px 14px;">
                <span>Hiển thị <strong>{{ paginatedLearnerAttempts.length }}</strong> / <strong>{{ filteredLearnerAttempts.length }}</strong> lượt thi</span>
              </div>

              <div v-if="!learnerScopedAttempts.length" class="admin-empty">
                <div class="empty-icon-wrap">
                  <ClipboardCheck :size="28" />
                </div>
                <strong>Chưa có lượt làm bài nào</strong>
                <p>Học viên "{{ selectedLearner.username }}" chưa thực hiện lượt làm bài nào trong kỳ thi đang chọn.</p>
              </div>

              <div v-else-if="!filteredLearnerAttempts.length" class="admin-empty">
                <div class="empty-icon-wrap">
                  <Search :size="28" />
                </div>
                <strong>Không tìm thấy bài thi phù hợp</strong>
                <p>Không có lượt thi nào khớp với từ khóa tìm kiếm hoặc bộ lọc trạng thái hiện tại.</p>
                <button
                  type="button"
                  class="overlay-button overlay-button-secondary"
                  style="margin-top: 8px;"
                  @click="resetAttemptFilters"
                >
                  Xóa bộ lọc tìm kiếm
                </button>
              </div>

              <div v-else>
                <!-- Desktop Table View -->
                <div class="admin-table-wrap results-desktop-table">
                  <table class="admin-table">
                    <thead>
                      <tr>
                        <th style="min-width: 220px;">Bài kiểm tra</th>
                        <th style="min-width: 160px;">Thời gian nộp</th>
                        <th style="min-width: 130px;">Trạng thái</th>
                        <th style="min-width: 120px;">Điểm số</th>
                        <th style="min-width: 90px;">Band</th>
                        <th style="min-width: 110px;">Vi phạm</th>
                        <th style="text-align: right; min-width: 130px;">Thao tác</th>
                      </tr>
                    </thead>
                    <tbody>
                      <tr v-for="attempt in paginatedLearnerAttempts" :key="attempt.attemptId">
                        <td>
                          <div class="results-exam-cell">
                            <strong>{{ examTitle(attempt) }}</strong>
                            <small class="table-secondary">Mã lượt: #ATT-{{ attempt.attemptId }}</small>
                          </div>
                        </td>
                        <td>
                          <span class="payment-time">{{ formatAttemptDate(attempt.submittedAt || attempt.startedAt) }}</span>
                        </td>
                        <td>
                          <span
                            class="results-status-pill"
                            :class="String(attempt.status || '').toLowerCase()"
                          >
                            <span class="state-dot" aria-hidden="true"></span>
                            <span>{{
                              attempt.status === 'GRADED'
                                ? 'Đã chấm'
                                : attempt.status === 'SUBMITTED'
                                ? 'Đã nộp'
                                : attempt.status === 'IN_PROGRESS'
                                ? 'Đang làm'
                                : attempt.status || '—'
                            }}</span>
                          </span>
                        </td>
                        <td>
                          <strong class="results-score-value">{{ formatAttemptScore(attempt) }}</strong>
                        </td>
                        <td>
                          <span class="results-band-badge">{{ attempt.bandScore ?? '—' }}</span>
                        </td>
                        <td>
                          <span v-if="attempt.violationCount > 0" class="results-violation-badge">
                            {{ attempt.violationCount }} lần
                          </span>
                          <span v-else class="results-clean-badge">0</span>
                        </td>
                        <td>
                          <div class="table-row-actions" style="justify-content: flex-end;">
                            <button
                              type="button"
                              class="table-action"
                              :disabled="!attempt.submittedAt || attempt.score == null"
                              title="Xem chi tiết kết quả bài thi"
                              @click="viewAttemptResult(attempt)"
                            >
                              <Eye :size="13" />
                              <span>Chi tiết</span>
                            </button>
                          </div>
                        </td>
                      </tr>
                    </tbody>
                  </table>
                </div>

                <!-- Mobile Card View -->
                <div class="results-mobile-grid" style="padding: 14px;">
                  <article
                    v-for="attempt in paginatedLearnerAttempts"
                    :key="`m-attempt-${attempt.attemptId}`"
                    class="account-mobile-card"
                  >
                    <div class="account-card-header">
                      <div>
                        <strong>{{ examTitle(attempt) }}</strong>
                        <small class="table-secondary" style="display: block; margin-top: 2px;">#ATT-{{ attempt.attemptId }}</small>
                      </div>
                      <span
                        class="results-status-pill"
                        :class="String(attempt.status || '').toLowerCase()"
                      >
                        <span class="state-dot" aria-hidden="true"></span>
                        <span>{{
                          attempt.status === 'GRADED'
                            ? 'Đã chấm'
                            : attempt.status === 'SUBMITTED'
                            ? 'Đã nộp'
                            : attempt.status === 'IN_PROGRESS'
                            ? 'Đang làm'
                            : attempt.status || '—'
                        }}</span>
                      </span>
                    </div>

                    <div class="account-card-body">
                      <div class="card-field-row">
                        <span class="card-field-label">Điểm số / Band:</span>
                        <div style="display: flex; align-items: center; gap: 8px;">
                          <strong class="results-score-value">{{ formatAttemptScore(attempt) }}</strong>
                          <span v-if="attempt.bandScore != null" class="results-band-badge">Band {{ attempt.bandScore }}</span>
                        </div>
                      </div>

                      <div class="card-field-row">
                        <span class="card-field-label">Thời gian:</span>
                        <span class="payment-time">{{ formatAttemptDate(attempt.submittedAt || attempt.startedAt) }}</span>
                      </div>

                      <div class="card-field-row">
                        <span class="card-field-label">Vi phạm quy chế:</span>
                        <span :class="attempt.violationCount > 0 ? 'results-violation-badge' : 'results-clean-badge'">
                          {{ attempt.violationCount ?? 0 }} lần
                        </span>
                      </div>
                    </div>

                    <div class="account-card-actions">
                      <button
                        type="button"
                        class="table-action mobile-card-btn"
                        :disabled="!attempt.submittedAt || attempt.score == null"
                        @click="viewAttemptResult(attempt)"
                      >
                        <Eye :size="14" />
                        <span>Xem chi tiết kết quả</span>
                      </button>
                    </div>
                  </article>
                </div>

                <!-- Pagination for Attempts -->
                <div v-if="attemptTotalPages > 1 || filteredLearnerAttempts.length > 10" class="account-pagination" style="padding: 14px 20px;">
                  <div class="pagination-size">
                    <span>Hiển thị</span>
                    <select v-model.number="attemptPageSize" class="pagination-select" aria-label="Số dòng mỗi trang">
                      <option :value="10">10 lượt thi</option>
                      <option :value="25">25 lượt thi</option>
                      <option :value="50">50 lượt thi</option>
                    </select>
                  </div>
                  <div class="pagination-nav">
                    <button
                      type="button"
                      class="pagination-btn"
                      :disabled="attemptPage <= 1"
                      aria-label="Trang trước"
                      @click="attemptPage = Math.max(1, attemptPage - 1)"
                    >
                      <ChevronLeft :size="16" />
                      <span>Trước</span>
                    </button>
                    <span class="pagination-status">Trang <strong>{{ attemptPage }}</strong> / {{ attemptTotalPages }}</span>
                    <button
                      type="button"
                      class="pagination-btn"
                      :disabled="attemptPage >= attemptTotalPages"
                      aria-label="Trang sau"
                      @click="attemptPage = Math.min(attemptTotalPages, attemptPage + 1)"
                    >
                      <span>Sau</span>
                      <ChevronRight :size="16" />
                    </button>
                  </div>
                </div>
              </div>
            </div>

            <!-- TWO-COLUMN ANALYTICS: SKILLS & SCORE TREND -->
            <div class="results-analytics-row">
              <!-- SKILLS MASTERY -->
              <section class="admin-panel results-skill-panel">
                <div class="admin-panel-heading">
                  <div>
                    <h2>Tiến độ theo kỹ năng</h2>
                    <p>{{ programs.find((program) => String(program.certificateId) === String(learnerCertificateId))?.name || 'Kỳ thi đã chọn' }}</p>
                  </div>
                </div>
                <div v-if="learnerProgress?.masteryBySkillJson && Object.keys(parseJson(learnerProgress.masteryBySkillJson)).length" class="skill-progress-list">
                  <div
                    v-for="(value, skill) in parseJson(learnerProgress.masteryBySkillJson)"
                    :key="skill"
                    class="skill-progress-row"
                  >
                    <div class="skill-label-wrap">
                      <span class="skill-name">{{ skill }}</span>
                    </div>
                    <div class="skill-progress-track">
                      <i :style="{ width: `${Math.min(100, Math.max(0, Number(value) || 0))}%` }"></i>
                    </div>
                    <strong class="skill-percent-value">{{ Math.round(Number(value) || 0) }}%</strong>
                  </div>
                </div>
                <div v-else class="learner-progress-empty">
                  <BarChart3 :size="20" />
                  <span>Chưa có dữ liệu phân tích kỹ năng cho kỳ thi này.</span>
                </div>
              </section>

              <!-- SCORE TREND -->
              <section class="admin-panel results-trend-panel">
                <div class="admin-panel-heading">
                  <div>
                    <h2>Diễn biến điểm thi</h2>
                    <p>{{ learnerScoreTrend.length }} lượt đã chấm gần nhất</p>
                  </div>
                </div>
                <div v-if="learnerScoreTrend.length" class="learner-trend-chart" role="img" aria-label="Biểu đồ diễn biến điểm thi">
                  <div
                    v-for="item in learnerScoreTrend"
                    :key="item.attemptId"
                    class="learner-trend-point"
                    :title="`Lượt #${item.attemptId}: ${item.scorePercent}%`"
                  >
                    <strong>{{ Math.round(Number(item.scorePercent) || 0) }}%</strong>
                    <i :style="{ height: `${Math.max(8, Math.min(100, Number(item.scorePercent) || 0))}%` }"></i>
                    <small>{{ formatTrendDate(item.submittedAt) }}</small>
                  </div>
                </div>
                <div v-else class="learner-progress-empty">
                  <BarChart3 :size="20" />
                  <span>Chưa có điểm đã chấm để so sánh theo thời gian.</span>
                </div>
              </section>
            </div>
          </template>

          <!-- EMPTY STATE WHEN NO LEARNER SELECTED -->
          <div v-else class="admin-empty">
            <div class="empty-icon-wrap">
              <User :size="28" />
            </div>
            <strong>Chưa có tài khoản người học để hiển thị</strong>
            <p>Vui lòng tạo hoặc chọn một tài khoản học viên để xem chi tiết tiến độ.</p>
          </div>
        </section>
</template>

<style scoped>
  /* RESULTS & LEARNER PROGRESS STYLES */
  .admin-results-section {
    display: flex;
    flex-direction: column;
    gap: 20px;
  }
  .results-hero-panel {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 20px;
    padding: 24px 28px;
    background: #FFFFFF;
    border: 1px solid #E2E8F0;
    border-radius: 12px;
  }
  .results-hero-content {
    max-width: 680px;
  }
  .results-hero-badge {
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
  .results-hero-content h2 {
    margin: 0 0 6px;
    color: #0F172A;
    font-size: 19px;
    font-weight: 700;
    letter-spacing: -0.02em;
  }
  .results-hero-content p {
    margin: 0;
    color: #64748B;
    font-size: 13.5px;
    line-height: 1.5;
  }
  .results-hero-controls {
    display: flex;
    align-items: center;
    gap: 12px;
    flex-wrap: wrap;
  }
  .results-filter-control {
    display: flex;
    align-items: center;
    gap: 8px;
    color: #475569;
    font-size: 13px;
    font-weight: 500;
  }
  .results-filter-control .control-label {
    display: inline-flex;
    align-items: center;
    gap: 5px;
    color: #475569;
    font-size: 13px;
    font-weight: 600;
  }
  .results-select {
    height: 38px;
    max-width: 240px;
    padding: 0 12px;
    border: 1px solid #CBD5E1;
    border-radius: 8px;
    background: #FFFFFF;
    color: #0F172A;
    font: inherit;
    font-size: 13px;
    font-weight: 500;
    cursor: pointer;
    transition: border-color 0.15s ease;
  }
  .results-select:focus {
    outline: none;
    border-color: #2563EB;
    box-shadow: 0 0 0 2px rgba(37, 99, 235, 0.12);
  }
  .admin-alert-error {
    display: flex;
    align-items: center;
    gap: 10px;
    padding: 12px 16px;
    border: 1px solid #FECACA;
    border-radius: 8px;
    background: #FEF2F2;
    color: #991B1B;
    font-size: 13px;
  }
  .results-metrics-grid {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
    gap: 16px;
  }
  .results-stat-card {
    display: flex;
    align-items: center;
    gap: 14px;
    padding: 18px 20px;
    background: #FFFFFF;
    border: 1px solid #E2E8F0;
    border-radius: 12px;
    box-shadow: 0 1px 2px rgba(0, 0, 0, 0.03);
    transition: transform 140ms ease, box-shadow 140ms ease;
  }
  .results-stat-card:hover {
    transform: translateY(-1px);
    box-shadow: 0 4px 12px rgba(15, 23, 42, 0.05);
  }
  .results-stat-icon {
    display: grid;
    width: 44px;
    height: 44px;
    flex: 0 0 auto;
    place-items: center;
    border-radius: 10px;
  }
  .results-stat-card.blue .results-stat-icon {
    background: #EFF6FF;
    color: #2563EB;
  }
  .results-stat-card.emerald .results-stat-icon {
    background: #ECFDF5;
    color: #059669;
  }
  .results-stat-card.purple .results-stat-icon {
    background: #FAF5FF;
    color: #7C3AED;
  }
  .results-stat-card.amber .results-stat-icon {
    background: #FFFBEB;
    color: #D97706;
  }
  .results-stat-info {
    display: flex;
    flex-direction: column;
    gap: 2px;
    min-width: 0;
  }
  .results-stat-info span {
    color: #64748B;
    font-size: 11px;
    font-weight: 600;
    letter-spacing: 0.04em;
  }
  .results-stat-info strong {
    color: #0F172A;
    font-size: 22px;
    font-weight: 700;
    letter-spacing: -0.02em;
    font-variant-numeric: tabular-nums;
  }
  .results-stat-info small {
    color: #94A3B8;
    font-size: 11.5px;
  }
  .results-history-panel {
    background: #FFFFFF;
    border: 1px solid #E2E8F0;
    border-radius: 12px;
    overflow: hidden;
  }
  .results-history-heading {
    padding: 18px 24px;
  }
  .results-table-filters {
    display: flex;
    align-items: center;
    gap: 10px;
    flex-wrap: wrap;
  }
  .results-search-box {
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
  .results-search-box input {
    border: 0;
    outline: 0;
    font: inherit;
    font-size: 13px;
    color: #0F172A;
    width: 200px;
  }
  .results-select-filter {
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
  .results-desktop-table {
    display: block;
  }
  .results-mobile-grid {
    display: none;
    flex-direction: column;
    gap: 12px;
  }
  .results-exam-cell {
    display: flex;
    flex-direction: column;
    gap: 2px;
  }
  .results-exam-cell strong {
    color: #0F172A;
    font-size: 13.5px;
    font-weight: 600;
  }
  .results-status-pill {
    display: inline-flex;
    align-items: center;
    gap: 6px;
    padding: 3px 9px;
    border-radius: 9999px;
    font-size: 12px;
    font-weight: 600;
  }
  .results-status-pill.graded {
    background: #ECFDF5;
    color: #065F46;
    border: 1px solid #A7F3D0;
  }
  .results-status-pill.submitted {
    background: #EFF6FF;
    color: #1D4ED8;
    border: 1px solid #BFDBFE;
  }
  .results-status-pill.in_progress {
    background: #FFFBEB;
    color: #92400E;
    border: 1px solid #FDE68A;
  }
  .results-status-pill.published {
    background: #ECFDF5;
    color: #065F46;
    border: 1px solid #A7F3D0;
  }
  .results-status-pill.draft {
    background: #F1F5F9;
    color: #475569;
    border: 1px solid #E2E8F0;
  }
  .results-score-value {
    color: #0F172A;
    font-size: 13.5px;
    font-weight: 700;
    font-variant-numeric: tabular-nums;
  }
  .results-band-badge {
    display: inline-block;
    padding: 2px 7px;
    border-radius: 5px;
    background: #FAF5FF;
    color: #7C3AED;
    border: 1px solid #E9D5FF;
    font-size: 11.5px;
    font-weight: 700;
  }
  .results-violation-badge {
    display: inline-block;
    padding: 2px 7px;
    border-radius: 5px;
    background: #FEF2F2;
    color: #DC2626;
    border: 1px solid #FECACA;
    font-size: 11.5px;
    font-weight: 600;
  }
  .results-clean-badge {
    color: #94A3B8;
    font-size: 12px;
  }
  .results-analytics-row {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 20px;
  }
  .results-skill-panel,
  .results-trend-panel {
    background: #FFFFFF;
    border: 1px solid #E2E8F0;
    border-radius: 12px;
    padding: 20px 24px;
    display: flex;
    flex-direction: column;
    gap: 16px;
  }
  .results-skill-panel .admin-panel-heading,
  .results-trend-panel .admin-panel-heading {
    padding: 0;
  }
  .skill-progress-list {
    display: flex;
    flex-direction: column;
    gap: 12px;
  }
  .skill-progress-row {
    display: grid;
    grid-template-columns: minmax(90px, 150px) minmax(80px, 1fr) 48px;
    align-items: center;
    gap: 12px;
  }
  .skill-label-wrap {
    min-width: 0;
  }
  .skill-name {
    color: #334155;
    font-size: 13px;
    font-weight: 600;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
    display: block;
  }
  .skill-progress-track {
    height: 8px;
    overflow: hidden;
    border-radius: 9999px;
    background: #F1F5F9;
  }
  .skill-progress-track i {
    display: block;
    height: 100%;
    border-radius: inherit;
    background: #059669;
    transition: width 0.3s ease;
  }
  .skill-percent-value {
    color: #0F172A;
    font-size: 13px;
    font-weight: 700;
    text-align: right;
    font-variant-numeric: tabular-nums;
  }
  .learner-progress-empty {
    display: flex;
    align-items: center;
    gap: 10px;
    min-height: 80px;
    padding: 16px;
    border: 1px dashed #E2E8F0;
    border-radius: 8px;
    background: #F8FAFC;
    color: #64748B;
    font-size: 13px;
    justify-content: center;
  }
  .learner-progress-empty svg {
    color: #94A3B8;
  }
  .learner-trend-chart {
    display: flex;
    min-height: 170px;
    align-items: flex-end;
    gap: clamp(10px, 2vw, 22px);
    overflow-x: auto;
    padding: 16px 8px 4px;
    border-bottom: 1px solid #E2E8F0;
  }
  .learner-trend-point {
    display: grid;
    min-width: 42px;
    height: 160px;
    flex: 1;
    grid-template-rows: 20px minmax(14px, 1fr) 24px;
    justify-items: center;
    align-items: end;
    gap: 4px;
  }
  .learner-trend-point strong {
    color: #475569;
    font-size: 11px;
    font-weight: 700;
    font-variant-numeric: tabular-nums;
  }
  .learner-trend-point i {
    width: min(32px, 75%);
    min-height: 8px;
    align-self: end;
    border-radius: 4px 4px 2px 2px;
    background: #2563EB;
    transition: height 0.3s ease;
  }
  .learner-trend-point:hover i {
    background: #1D4ED8;
  }
  .learner-trend-point small {
    align-self: center;
    color: #94A3B8;
    font-size: 10.5px;
  }
  .attempt-result-detail { display: grid; gap: 16px; }
  .attempt-result-score { display: grid; gap: 4px; padding: 16px; border: 1px solid #A7F3D0; border-radius: 10px; background: #ECFDF5; }
  .attempt-result-score span { color: #065F46; font-size: 12.5px; font-weight: 500; }
  .attempt-result-score strong { color: #0F172A; font-size: 26px; line-height: 32px; font-variant-numeric: tabular-nums; font-weight: 700; }
  .attempt-result-score small { color: #059669; font-size: 12px; font-weight: 600; }
  .attempt-result-detail dl { display: grid; margin: 0; border: 1px solid #E2E8F0; border-radius: 8px; overflow: hidden; }
  .attempt-result-detail dl > div { display: flex; min-height: 44px; align-items: center; justify-content: space-between; gap: 16px; padding: 6px 14px; border-bottom: 1px solid #F1F5F9; }
  .attempt-result-detail dl > div:last-child { border: 0; }
  .attempt-result-detail dt { color: #64748B; font-size: 13px; }
  .attempt-result-detail dd { margin: 0; color: #0F172A; font-size: 13px; font-weight: 600; font-variant-numeric: tabular-nums; }

  @media (max-width: 768px) {
    .results-hero-panel { align-items: stretch; flex-direction: column; padding: 18px; }
    .results-hero-controls { align-items: stretch; flex-direction: column; }
    .results-filter-control { align-items: flex-start; flex-wrap: wrap; }
    .results-select { width: 100%; max-width: none; min-width: 0; }
    .results-history-heading { align-items: stretch; flex-direction: column; padding: 16px; }
    .results-table-filters { width: 100%; }
    .results-search-box { flex: 1 1 100%; min-width: 0; }
    .results-search-box input { width: 100%; min-width: 0; }
    .results-select-filter { flex: 1 1 100%; }
    .results-desktop-table { display: none; }
    .results-mobile-grid { display: flex; }
    .results-analytics-row { grid-template-columns: minmax(0, 1fr); }
  }

  @media (max-width: 520px) {
    .results-metrics-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 10px; }
    .results-stat-card { align-items: flex-start; gap: 10px; padding: 12px; }
    .results-stat-icon { width: 36px; height: 36px; }
    .results-stat-info strong { font-size: 20px; }
    .results-trend-chart { gap: 8px; }
  }

</style>
