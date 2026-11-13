<script setup>
import { inject } from 'vue'
import {
  Check,
  CheckCircle2,
  ChevronLeft,
  ChevronRight,
  ClipboardCheck,
  FileText,
  GraduationCap,
  LoaderCircle,
  Plus,
  RefreshCw,
  RotateCcw,
  Search,
  X
} from 'lucide-vue-next'
import { adminContextKey } from '../../composables/adminContext'

const admin = inject(adminContextKey)

const {
  loading,
  exams,
  examSearch,
  examProgramFilter,
  examStatusFilter,
  examPage,
  examPageSize,
  filteredExams,
  examTotalPages,
  paginatedExams,
  programs,
  resetExamFilters,
  showExamBuilder,
  formatAdminNumber,
  formatAttemptDate,
  programName,
  topicName,
  loadData,
  topics
} = admin
</script>

<template>
<section  class="admin-exams-section">
          <!-- TOP HEADING PANEL -->
          <div class="admin-panel exams-hero-panel">
            <div class="exams-hero-content">
              <div class="exams-hero-badge">
                <ClipboardCheck :size="15" />
                <span>DANH MỤC BÀI KIỂM TRA</span>
              </div>
              <h2>Quản trị Đề thi & Bài kiểm tra</h2>
              <p>Quản lý toàn bộ đề thi chuẩn hóa, cấu hình câu hỏi, phân loại theo kỹ năng và phát hành cho học viên.</p>
            </div>
            <div class="exams-hero-actions">
              <button
                type="button"
                class="admin-primary-button"
                :disabled="!programs.length || !topics.length"
                @click="showExamBuilder = true"
              >
                <Plus :size="16" />
                <span>Tạo bài kiểm tra</span>
              </button>
              <button
                type="button"
                class="admin-secondary-button"
                :disabled="loading"
                title="Tải lại danh sách đề thi"
                @click="loadData"
              >
                <RefreshCw :size="15" :class="{ 'overlay-spinner': loading }" />
                <span>Làm mới dữ liệu</span>
              </button>
            </div>
          </div>

          <!-- KEY METRICS CARDS -->
          <div class="exams-metrics-grid">
            <article class="exams-stat-card blue">
              <div class="exams-stat-icon">
                <ClipboardCheck :size="20" />
              </div>
              <div class="exams-stat-info">
                <span>TỔNG SỐ ĐỀ THI</span>
                <strong>{{ exams.length }}</strong>
                <small>Đề kiểm tra trên hệ thống</small>
              </div>
            </article>

            <article class="exams-stat-card emerald">
              <div class="exams-stat-icon">
                <CheckCircle2 :size="20" />
              </div>
              <div class="exams-stat-info">
                <span>ĐÃ PHÁT HÀNH</span>
                <strong>{{ exams.filter((e) => e.status === 'PUBLISHED').length }}</strong>
                <small>Học viên có thể làm bài</small>
              </div>
            </article>

            <article class="exams-stat-card amber">
              <div class="exams-stat-icon">
                <FileText :size="20" />
              </div>
              <div class="exams-stat-info">
                <span>BẢN NHÁP / ĐANG XÂY</span>
                <strong>{{ exams.filter((e) => e.status !== 'PUBLISHED').length }}</strong>
                <small>Chưa mở cho người học</small>
              </div>
            </article>

            <article class="exams-stat-card purple">
              <div class="exams-stat-icon">
                <GraduationCap :size="20" />
              </div>
              <div class="exams-stat-info">
                <span>CHƯƠNG TRÌNH ÁP DỤNG</span>
                <strong>{{ new Set(exams.map((e) => e.certificateId)).size }}</strong>
                <small>Kỳ thi có bộ đề</small>
              </div>
            </article>
          </div>

          <!-- MAIN EXAMS PANEL -->
          <div class="admin-panel admin-table-panel">
            <div class="admin-panel-heading">
              <div>
                <h2>Danh sách đề kiểm tra</h2>
                <p>{{ filteredExams.length }} đề thi sẵn sàng đánh giá năng lực học viên.</p>
              </div>
              <div class="exams-table-filters">
                <div class="exams-search-box">
                  <Search :size="15" />
                  <input
                    v-model="examSearch"
                    type="search"
                    placeholder="Tìm theo tiêu đề, mô tả..."
                    aria-label="Tìm kiếm đề thi"
                  />
                  <button
                    v-if="examSearch"
                    type="button"
                    class="account-search-clear"
                    aria-label="Xóa tìm kiếm"
                    @click="examSearch = ''"
                  >
                    <X :size="14" />
                  </button>
                </div>
                <select v-model="examProgramFilter" class="exams-select-filter" aria-label="Lọc theo kỳ thi">
                  <option value="all">Tất cả kỳ thi</option>
                  <option v-for="program in programs" :key="program.certificateId" :value="String(program.certificateId)">
                    {{ program.name }} ({{ program.code }})
                  </option>
                </select>
                <select v-model="examStatusFilter" class="exams-select-filter" aria-label="Lọc theo trạng thái">
                  <option value="all">Tất cả trạng thái</option>
                  <option value="PUBLISHED">Đã phát hành</option>
                  <option value="DRAFT">Bản nháp</option>
                </select>
                <button
                  v-if="examSearch || examProgramFilter !== 'all' || examStatusFilter !== 'all'"
                  type="button"
                  class="account-reset-filter-btn"
                  @click="resetExamFilters"
                >
                  <RotateCcw :size="13" />
                  <span>Đặt lại</span>
                </button>
              </div>
            </div>

            <div class="account-meta-summary" style="padding: 10px 20px 14px;">
              <span>Hiển thị <strong>{{ paginatedExams.length }}</strong> / <strong>{{ filteredExams.length }}</strong> đề kiểm tra</span>
            </div>

            <div v-if="loading" class="admin-empty admin-empty-loading">
              <LoaderCircle class="overlay-spinner" :size="24" />
              <span>Đang tải danh sách bài kiểm tra…</span>
            </div>

            <div v-else-if="!exams.length" class="admin-empty">
              <div class="empty-icon-wrap">
                <ClipboardCheck :size="28" />
              </div>
              <strong>Chưa có bài kiểm tra nào trong hệ thống</strong>
              <p>Bắt đầu tạo bài kiểm tra đầu tiên để phục vụ luyện tập và đánh giá năng lực học viên.</p>
              <button
                type="button"
                class="admin-primary-button"
                style="margin-top: 10px;"
                :disabled="!programs.length || !topics.length"
                @click="showExamBuilder = true"
              >
                <Plus :size="15" />
                <span>Tạo bài kiểm tra đầu tiên</span>
              </button>
            </div>

            <div v-else-if="!filteredExams.length" class="admin-empty">
              <div class="empty-icon-wrap">
                <Search :size="28" />
              </div>
              <strong>Không tìm thấy bài kiểm tra phù hợp</strong>
              <p>Không có đề thi nào khớp với bộ lọc hoặc từ khóa tìm kiếm hiện tại.</p>
              <button
                type="button"
                class="overlay-button overlay-button-secondary"
                style="margin-top: 8px;"
                @click="resetExamFilters"
              >
                Xóa bộ lọc tìm kiếm
              </button>
            </div>

            <div v-else>
              <!-- Desktop Table View -->
              <div class="admin-table-wrap exams-desktop-table">
                <table class="admin-table">
                  <thead>
                    <tr>
                      <th style="min-width: 240px;">Đề kiểm tra</th>
                      <th style="min-width: 190px;">Kỳ thi / Kỹ năng</th>
                      <th style="min-width: 140px;">Mục tiêu</th>
                      <th style="min-width: 120px;">Thời lượng</th>
                      <th style="min-width: 110px;">Số câu</th>
                      <th style="min-width: 130px;">Trạng thái</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr v-for="exam in paginatedExams" :key="exam.examId">
                      <td>
                        <div class="results-exam-cell">
                          <strong>{{ exam.title }}</strong>
                          <small class="table-secondary">
                            {{ exam.description || 'Bài đánh giá học tập theo kỹ năng' }}
                          </small>
                        </div>
                      </td>
                      <td>
                        <div class="exam-program-cell">
                          <span class="exam-prog-name">{{ programName(exam.certificateId) }}</span>
                          <span class="exam-topic-name">· {{ topicName(exam.topicId) }}</span>
                        </div>
                      </td>
                      <td>
                        <span class="program-passing-pill">
                          {{ exam.targetScoreMin != null ? `${exam.targetScoreMin}–${exam.targetScoreMax} ${exam.targetScoreUnit || ''}` : '—' }}
                        </span>
                      </td>
                      <td>
                        <span class="exam-duration-badge">
                          {{ exam.durationMinutes || 0 }} phút
                        </span>
                      </td>
                      <td>
                        <span class="order-count-badge">
                          {{ exam.questionCount || exam.totalQuestions || 0 }} câu
                        </span>
                      </td>
                      <td>
                        <span
                          class="results-status-pill"
                          :class="exam.status === 'PUBLISHED' ? 'published' : 'draft'"
                        >
                          <span class="state-dot" aria-hidden="true"></span>
                          <span>{{ exam.status === 'PUBLISHED' ? 'Đã phát hành' : exam.status || 'Bản nháp' }}</span>
                        </span>
                      </td>
                    </tr>
                  </tbody>
                </table>
              </div>

              <!-- Mobile Card View -->
              <div class="exams-mobile-grid" style="padding: 14px;">
                <article
                  v-for="exam in paginatedExams"
                  :key="`m-exam-${exam.examId}`"
                  class="account-mobile-card"
                >
                  <div class="account-card-header">
                    <div>
                      <strong>{{ exam.title }}</strong>
                      <small class="table-secondary" style="display: block; margin-top: 2px;">
                        {{ programName(exam.certificateId) }} · {{ topicName(exam.topicId) }}
                      </small>
                    </div>
                    <span
                      class="results-status-pill"
                      :class="exam.status === 'PUBLISHED' ? 'published' : 'draft'"
                    >
                      <span class="state-dot" aria-hidden="true"></span>
                      <span>{{ exam.status === 'PUBLISHED' ? 'Đã phát hành' : exam.status || 'Bản nháp' }}</span>
                    </span>
                  </div>

                  <div class="account-card-body">
                    <div class="card-field-row">
                      <span class="card-field-label">Thời lượng & Số câu:</span>
                      <div style="display: flex; gap: 8px;">
                        <span class="exam-duration-badge">{{ exam.durationMinutes || 0 }} phút</span>
                        <span class="order-count-badge">{{ exam.questionCount || exam.totalQuestions || 0 }} câu</span>
                      </div>
                    </div>

                    <div class="card-field-row">
                      <span class="card-field-label">Điểm mục tiêu:</span>
                      <span class="program-passing-pill">
                        {{ exam.targetScoreMin != null ? `${exam.targetScoreMin}–${exam.targetScoreMax} ${exam.targetScoreUnit || ''}` : '—' }}
                      </span>
                    </div>

                    <div v-if="exam.description" class="card-field-row">
                      <span class="card-field-label">Mô tả:</span>
                      <small class="table-secondary">{{ exam.description }}</small>
                    </div>
                  </div>
                </article>
              </div>

              <!-- Pagination for Exams -->
              <div v-if="examTotalPages > 1 || filteredExams.length > 10" class="account-pagination" style="padding: 14px 20px;">
                <div class="pagination-size">
                  <span>Hiển thị</span>
                  <select v-model.number="examPageSize" class="pagination-select" aria-label="Số dòng mỗi trang">
                    <option :value="10">10 đề thi</option>
                    <option :value="25">25 đề thi</option>
                    <option :value="50">50 đề thi</option>
                  </select>
                </div>
                <div class="pagination-nav">
                  <button
                    type="button"
                    class="pagination-btn"
                    :disabled="examPage <= 1"
                    aria-label="Trang trước"
                    @click="examPage = Math.max(1, examPage - 1)"
                  >
                    <ChevronLeft :size="16" />
                    <span>Trước</span>
                  </button>
                  <span class="pagination-status">Trang <strong>{{ examPage }}</strong> / {{ examTotalPages }}</span>
                  <button
                    type="button"
                    class="pagination-btn"
                    :disabled="examPage >= examTotalPages"
                    aria-label="Trang sau"
                    @click="examPage = Math.min(examTotalPages, examPage + 1)"
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
  /* PROGRAMS & EXAMS MODULE STYLES */
  .admin-programs-section,
  .admin-exams-section {
    display: flex;
    flex-direction: column;
    gap: 20px;
  }
  .programs-hero-panel,
  .exams-hero-panel {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 20px;
    padding: 24px 28px;
    background: #FFFFFF;
    border: 1px solid #E2E8F0;
    border-radius: 12px;
  }
  .programs-hero-content,
  .exams-hero-content {
    max-width: 680px;
  }
  .programs-hero-badge,
  .exams-hero-badge {
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
  .programs-hero-content h2,
  .exams-hero-content h2 {
    margin: 0 0 6px;
    color: #0F172A;
    font-size: 19px;
    font-weight: 700;
    letter-spacing: -0.02em;
  }
  .programs-hero-content p,
  .exams-hero-content p {
    margin: 0;
    color: #64748B;
    font-size: 13.5px;
    line-height: 1.5;
  }
  .programs-hero-actions,
  .exams-hero-actions {
    display: flex;
    align-items: center;
    gap: 10px;
    flex-shrink: 0;
  }
  .programs-metrics-grid,
  .exams-metrics-grid {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
    gap: 16px;
  }
  .programs-stat-card,
  .exams-stat-card {
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
  .programs-stat-card:hover,
  .exams-stat-card:hover {
    transform: translateY(-1px);
    box-shadow: 0 4px 12px rgba(15, 23, 42, 0.05);
  }
  .programs-stat-icon,
  .exams-stat-icon {
    display: grid;
    width: 44px;
    height: 44px;
    flex: 0 0 auto;
    place-items: center;
    border-radius: 10px;
  }
  .programs-stat-card.blue .programs-stat-icon,
  .exams-stat-card.blue .exams-stat-icon {
    background: #EFF6FF;
    color: #2563EB;
  }
  .programs-stat-card.emerald .programs-stat-icon,
  .exams-stat-card.emerald .exams-stat-icon {
    background: #ECFDF5;
    color: #059669;
  }
  .programs-stat-card.purple .programs-stat-icon,
  .exams-stat-card.purple .exams-stat-icon {
    background: #FAF5FF;
    color: #7C3AED;
  }
  .exams-stat-card.amber .exams-stat-icon {
    background: #FFFBEB;
    color: #D97706;
  }
  .programs-stat-info,
  .exams-stat-info {
    display: flex;
    flex-direction: column;
    gap: 2px;
    min-width: 0;
  }
  .programs-stat-info span,
  .exams-stat-info span {
    color: #64748B;
    font-size: 11px;
    font-weight: 600;
    letter-spacing: 0.04em;
  }
  .programs-stat-info strong,
  .exams-stat-info strong {
    color: #0F172A;
    font-size: 22px;
    font-weight: 700;
    letter-spacing: -0.02em;
    font-variant-numeric: tabular-nums;
  }
  .programs-stat-info small,
  .exams-stat-info small {
    color: #94A3B8;
    font-size: 11.5px;
  }
  .programs-table-filters,
  .exams-table-filters {
    display: flex;
    align-items: center;
    gap: 10px;
    flex-wrap: wrap;
  }
  .programs-search-box,
  .exams-search-box {
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
  .programs-search-box input,
  .exams-search-box input {
    border: 0;
    outline: 0;
    font: inherit;
    font-size: 13px;
    color: #0F172A;
    width: 210px;
  }
  .exams-select-filter {
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
  .programs-desktop-table,
  .exams-desktop-table {
    display: block;
  }
  .programs-mobile-grid,
  .exams-mobile-grid {
    display: none;
    flex-direction: column;
    gap: 12px;
  }
  .program-title-cell strong {
    color: #0F172A;
    font-size: 13.5px;
    font-weight: 700;
  }
  .program-code-badge {
    display: inline-block;
    padding: 2px 7px;
    border-radius: 4px;
    background: #EFF6FF;
    color: #2563EB;
    border: 1px solid #DBEAFE;
    font-size: 11.5px;
    font-weight: 700;
    letter-spacing: 0.02em;
  }
  .program-score-pill {
    color: #0F172A;
    font-size: 13px;
    font-weight: 600;
  }
  .program-passing-pill {
    display: inline-block;
    padding: 2px 8px;
    border-radius: 6px;
    background: #F8FAFC;
    color: #334155;
    border: 1px solid #E2E8F0;
    font-size: 12px;
    font-weight: 600;
  }
  .program-skills-tags {
    display: flex;
    align-items: center;
    gap: 5px;
    flex-wrap: wrap;
  }
  .skill-chip-tag {
    display: inline-block;
    padding: 2px 6px;
    border-radius: 4px;
    background: #F1F5F9;
    color: #475569;
    font-size: 11.5px;
    font-weight: 600;
  }
  .exam-program-cell {
    display: flex;
    align-items: center;
    gap: 5px;
  }
  .exam-prog-name {
    color: #0F172A;
    font-size: 13px;
    font-weight: 600;
  }
  .exam-topic-name {
    color: #64748B;
    font-size: 12.5px;
  }
  .exam-duration-badge {
    display: inline-flex;
    align-items: center;
    gap: 4px;
    padding: 2px 8px;
    border-radius: 6px;
    background: #F8FAFC;
    color: #334155;
    border: 1px solid #E2E8F0;
    font-size: 12px;
    font-weight: 600;
  }
  .preset-actions-grid {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
    gap: 12px;
    max-width: 720px;
    margin: 0 auto;
  }
  .preset-card-btn {
    display: flex;
    align-items: center;
    gap: 12px;
    padding: 12px 14px;
    border: 1px solid #E2E8F0;
    border-radius: 10px;
    background: #FFFFFF;
    cursor: pointer;
    text-align: left;
    transition: all 140ms ease;
  }
  .preset-card-btn:hover {
    border-color: #2563EB;
    background: #EFF6FF;
    transform: translateY(-1px);
    box-shadow: 0 4px 12px rgba(37, 99, 235, 0.08);
  }
  .preset-card-icon {
    display: grid;
    width: 38px;
    height: 38px;
    flex: 0 0 auto;
    place-items: center;
    border-radius: 8px;
    background: #EFF6FF;
    color: #2563EB;
  }
  .preset-card-btn:hover .preset-card-icon {
    background: #2563EB;
    color: #FFFFFF;
  }
  .preset-card-text strong {
    display: block;
    color: #0F172A;
    font-size: 13.5px;
    font-weight: 700;
  }
  .preset-card-text small {
    display: block;
    color: #64748B;
    font-size: 11.5px;
    margin-top: 2px;
  }
  .secondary-text {
    display: block;
    margin-top: 4px;
    color: var(--text-muted);
    font-size: 12px;
  }
  .description-cell {
    max-width: 330px;
    overflow: hidden;
    text-overflow: ellipsis;
  }
  .form-row {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(min(220px, 100%), 1fr));
    gap: 16px;
  }
  .admin-scrim,
  .admin-menu-button {
    display: none;
  }
  @media (max-width: 1050px) {
    .admin-content {
      padding: 24px 20px 40px;
    }
    .admin-overview-grid {
      grid-template-columns: minmax(0, 1fr);
    }
    .overview-kpi-grid {
      grid-template-columns: repeat(2, minmax(0, 1fr));
    }
    .overview-detail-grid {
      grid-template-columns: minmax(0, 1fr);
      gap: 20px;
    }
    .overview-pillars-grid {
      grid-template-columns: repeat(2, minmax(0, 1fr));
    }
  }
  @media (max-width: 800px) {
    .admin-page-heading { align-items: flex-start; flex-direction: column; }
    .analytics-controls { width: 100%; justify-content: flex-start; }
    .overview-pillars-grid { grid-template-columns: minmax(0, 1fr); }
    .overview-insight-banner { flex-direction: column; align-items: flex-start; gap: 8px; }
    .learner-summary-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); }
    .learner-report-toolbar { align-items: stretch; flex-direction: column; }
    .learner-report-filters { flex-wrap: wrap; }
    .knowledge-layout { gap: 12px; }
    .knowledge-upload-item { grid-template-columns: 32px minmax(0, 1fr) 30px; }
    .knowledge-upload-item .queued-title { grid-column: 2 / 3; }
    .knowledge-upload-item .knowledge-icon-button { grid-column: 3; grid-row: 1 / span 2; }
    .knowledge-document { grid-template-columns: 36px minmax(0, 1fr); }
    .knowledge-document-actions { grid-column: 2; justify-content: flex-end; }
    .knowledge-page { gap: 14px; }
  }
  @media (max-width: 520px) {
    .overview-kpi-grid { grid-template-columns: minmax(0, 1fr); }
    .analytics-controls { align-items: stretch; flex-direction: column; gap: 8px; width: 100%; }
    .analytics-time-filter { width: 100%; justify-content: stretch; }
    .analytics-time-filter button { min-width: 0; flex: 1; }
    .analytics-exam-filter select { width: 100%; max-width: none; }
    .overview-refresh-button { width: 100%; }
    .overview-card-header { flex-direction: column; gap: 10px; }
    .overview-chart-legend { width: 100%; justify-content: flex-start; }
    .analytics-chart-label.label-hidden-mobile { display: none; }
    .overview-error-state { flex-direction: column; align-items: stretch; gap: 14px; }
    .learner-summary-grid { grid-template-columns: minmax(0, 1fr); gap: 10px; }
    .learner-summary-card { min-height: auto; }
    .learner-report-filters { display: grid; grid-template-columns: minmax(0, 1fr); }
    .learner-report-filters label,
    .learner-report-filters select { width: 100%; max-width: none; }
    .skill-progress-row { grid-template-columns: minmax(75px, 100px) minmax(40px, 1fr) 42px; gap: 8px; font-size: 12px; }
    .account-actions-cell { align-items: flex-start; flex-direction: column; }
    .knowledge-filter button { font-size: 12px; gap: 4px; padding: 0 4px; }
    .knowledge-card-heading { padding-right: 12px; padding-left: 12px; }
    .knowledge-library-tools { padding-right: 10px; padding-left: 10px; }
    .knowledge-document-list { padding-right: 10px; padding-left: 10px; }
    .knowledge-document-meta { gap: 5px; }
    .knowledge-upload-item { grid-template-columns: 32px minmax(0, 1fr) 30px; }
    .queued-title { grid-column: 2 / 3; }
    .knowledge-form-footer { align-items: flex-start; flex-direction: column; }
    .knowledge-submit { width: 100%; }
  }
  @media (max-width: 1024px) {
    .admin-sidebar {
      z-index: 45;
      width: min(280px, 86vw);
      transform: translateX(-100%);
      transition: transform 0.22s cubic-bezier(0.16, 1, 0.3, 1);
      box-shadow: 4px 0 24px rgba(15, 23, 42, 0.12);
    }
    .admin-sidebar.admin-sidebar-open {
      transform: translateX(0);
    }
    .admin-sidebar-close {
      display: grid;
    }
    .admin-workspace {
      width: 100%;
      margin-left: 0;
    }
    .admin-menu-button {
      display: grid;
      flex: 0 0 auto;
    }
    .admin-scrim {
      position: fixed;
      z-index: 40;
      inset: 0;
      display: block;
      background: rgba(15, 23, 42, 0.35);
      backdrop-filter: blur(2px);
    }
    .admin-topbar {
      gap: 12px;
      padding: 0 clamp(16px, 3vw, 32px);
    }
    .admin-search {
      width: auto;
      flex: 1 1 200px;
    }
    .admin-top-actions {
      gap: 8px;
    }
    .admin-overview-grid {
      grid-template-columns: minmax(0, 1fr);
    }
  }
  @media (max-width: 640px) {
    .breadcrumb-badge { display: none; }
    .admin-breadcrumb { max-width: 130px; overflow: hidden; text-overflow: ellipsis; }
    .admin-preview-learner-link span { display: none; }
    .admin-preview-learner-link { padding: 6px 8px; }
    .admin-profile div,
    .admin-profile > svg { display: none; }
    .admin-topbar { gap: 8px; padding-right: 14px; padding-left: 14px; }
    .admin-top-actions { gap: 4px; }
  }
  @media (max-width: 520px) {
    .admin-content {
      padding: 20px 16px 32px;
    }
    .admin-page-heading {
      align-items: flex-start;
      flex-direction: column;
    }
    .admin-page-heading h1 {
      font-size: 21px;
    }
    .admin-stats article {
      min-height: 90px;
    }
    .admin-panel {
      padding: 11px;
    }
    .admin-table { min-width: 680px; }
    .admin-table-panel {
      padding: 11px;
    }
    .form-row {
      grid-template-columns: minmax(0, 1fr);
    }
  }

</style>
