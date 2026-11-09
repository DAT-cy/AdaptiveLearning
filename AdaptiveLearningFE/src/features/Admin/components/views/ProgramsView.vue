<script setup>
import { inject } from 'vue'
import {
  ClipboardCheck,
  GraduationCap,
  Layers,
  LoaderCircle,
  Pencil,
  Plus,
  RefreshCw,
  RotateCcw,
  Search,
  Trash2,
  X
} from 'lucide-vue-next'
import { adminContextKey } from '../../composables/adminContext'

const admin = inject(adminContextKey)

const {
  loading,
  programs,
  programSearch,
  filteredPrograms,
  resetProgramFilters,
  openProgram,
  deleteProgram,
  parseSkillArray,
  formatAdminNumber,
  loadData,
  exams,
  certificatePresets,
  openPreset
} = admin
</script>

<template>
<section  class="admin-programs-section">
          <!-- TOP HEADING PANEL -->
          <div class="admin-panel programs-hero-panel">
            <div class="programs-hero-content">
              <div class="programs-hero-badge">
                <GraduationCap :size="15" />
                <span>CHỨNG CHỈ & KỲ THI</span>
              </div>
              <h2>Quản lý Chứng chỉ & Chương trình thi</h2>
              <p>Cấu hình danh mục chứng chỉ chuẩn hóa (IELTS, TOEIC, THPT Quốc Gia), thang điểm và bộ kỹ năng đào tạo.</p>
            </div>
            <div class="programs-hero-actions">
              <button
                type="button"
                class="admin-primary-button"
                @click="openProgram()"
              >
                <Plus :size="16" />
                <span>Tạo chứng chỉ / kỳ thi</span>
              </button>
              <button
                type="button"
                class="admin-secondary-button"
                :disabled="loading"
                title="Tải lại danh sách chứng chỉ"
                @click="loadData"
              >
                <RefreshCw :size="15" :class="{ 'overlay-spinner': loading }" />
                <span>Làm mới dữ liệu</span>
              </button>
            </div>
          </div>

          <!-- KEY METRICS CARDS -->
          <div class="programs-metrics-grid">
            <article class="programs-stat-card blue">
              <div class="programs-stat-icon">
                <GraduationCap :size="20" />
              </div>
              <div class="programs-stat-info">
                <span>TỔNG CHỨNG CHỈ / KỲ THI</span>
                <strong>{{ programs.length }}</strong>
                <small>Chương trình đang áp dụng</small>
              </div>
            </article>

            <article class="programs-stat-card emerald">
              <div class="programs-stat-icon">
                <ClipboardCheck :size="20" />
              </div>
              <div class="programs-stat-info">
                <span>TỔNG ĐỀ KIỂM TRA ĐANG CÓ</span>
                <strong>{{ exams.length }}</strong>
                <small>Đề thi phân theo kỹ năng</small>
              </div>
            </article>

            <article class="programs-stat-card purple">
              <div class="programs-stat-icon">
                <Layers :size="20" />
              </div>
              <div class="programs-stat-info">
                <span>MẪU CATALOG CHUẨN</span>
                <strong>{{ certificatePresets.length }}</strong>
                <small>IELTS, TOEIC, THPT Quốc Gia</small>
              </div>
            </article>
          </div>

          <!-- MAIN PROGRAMS PANEL -->
          <div class="admin-panel admin-table-panel">
            <div class="admin-panel-heading">
              <div>
                <h2>Danh sách kỳ thi trong hệ thống</h2>
                <p>{{ filteredPrograms.length }} danh mục kỳ thi đang sẵn sàng phục vụ học viên.</p>
              </div>
              <div class="programs-table-filters">
                <div class="programs-search-box">
                  <Search :size="15" />
                  <input
                    v-model="programSearch"
                    type="search"
                    placeholder="Tìm theo tên hoặc mã kỳ thi..."
                    aria-label="Tìm kiếm kỳ thi"
                  />
                  <button
                    v-if="programSearch"
                    type="button"
                    class="account-search-clear"
                    aria-label="Xóa tìm kiếm"
                    @click="programSearch = ''"
                  >
                    <X :size="14" />
                  </button>
                </div>
                <button
                  v-if="programSearch"
                  type="button"
                  class="account-reset-filter-btn"
                  @click="resetProgramFilters"
                >
                  <RotateCcw :size="13" />
                  <span>Đặt lại</span>
                </button>
              </div>
            </div>

            <div class="account-meta-summary" style="padding: 10px 20px 14px;">
              <span>Hiển thị <strong>{{ filteredPrograms.length }}</strong> / <strong>{{ programs.length }}</strong> kỳ thi</span>
            </div>

            <div v-if="loading" class="admin-empty admin-empty-loading">
              <LoaderCircle class="overlay-spinner" :size="24" />
              <span>Đang tải danh sách kỳ thi…</span>
            </div>

            <!-- EMPTY CATALOG PRESET STATE -->
            <div v-else-if="!programs.length" class="admin-empty">
              <div class="catalog-empty-state">
                <div class="empty-icon-wrap" style="margin-bottom: 12px;">
                  <GraduationCap :size="28" />
                </div>
                <strong>Khởi tạo catalog kỳ thi mẫu</strong>
                <p style="color: #64748B; font-size: 13.5px; margin: 4px 0 16px;">
                  Chọn bộ dữ liệu chuẩn theo thang điểm và kỹ năng trong tài liệu nghiệp vụ để khởi tạo nhanh.
                </p>
                <div class="preset-actions-grid">
                  <button
                    v-for="preset in certificatePresets"
                    :key="preset.code"
                    type="button"
                    class="preset-card-btn"
                    @click="openPreset(preset)"
                  >
                    <div class="preset-card-icon">
                      <GraduationCap :size="18" />
                    </div>
                    <div class="preset-card-text">
                      <strong>{{ preset.name }}</strong>
                      <small>{{ preset.code }} · Thang {{ preset.totalScore }} điểm</small>
                    </div>
                  </button>
                </div>
              </div>
            </div>

            <div v-else-if="!filteredPrograms.length" class="admin-empty">
              <div class="empty-icon-wrap">
                <Search :size="28" />
              </div>
              <strong>Không tìm thấy kỳ thi phù hợp</strong>
              <p>Không có chương trình nào khớp với từ khóa tìm kiếm "{{ programSearch }}".</p>
              <button
                type="button"
                class="overlay-button overlay-button-secondary"
                style="margin-top: 8px;"
                @click="resetProgramFilters"
              >
                Xóa bộ lọc tìm kiếm
              </button>
            </div>

            <div v-else>
              <!-- Desktop Table View -->
              <div class="admin-table-wrap programs-desktop-table">
                <table class="admin-table">
                  <thead>
                    <tr>
                      <th style="min-width: 220px;">Chứng chỉ / Kỳ thi</th>
                      <th style="min-width: 100px;">Mã</th>
                      <th style="min-width: 200px;">Mô tả</th>
                      <th style="min-width: 120px;">Tổng điểm</th>
                      <th style="min-width: 130px;">Điểm đạt / Band</th>
                      <th style="min-width: 220px;">Kỹ năng / Môn</th>
                      <th style="min-width: 100px;">Số đề thi</th>
                      <th style="text-align: right; min-width: 150px;">Thao tác</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr v-for="program in filteredPrograms" :key="program.certificateId">
                      <td>
                        <div class="program-title-cell">
                          <strong>{{ program.name }}</strong>
                        </div>
                      </td>
                      <td>
                        <span class="program-code-badge">{{ program.code }}</span>
                      </td>
                      <td class="description-cell">
                        <span :title="program.description">{{ program.description || '—' }}</span>
                      </td>
                      <td>
                        <span class="program-score-pill">{{ program.totalScore }} điểm</span>
                      </td>
                      <td>
                        <span class="program-passing-pill">{{ program.passingBand || program.passingScore }}</span>
                      </td>
                      <td>
                        <div class="program-skills-tags">
                          <span
                            v-for="sk in parseSkillArray(program.skills)"
                            :key="sk.code"
                            class="skill-chip-tag"
                          >
                            {{ sk.label }}
                          </span>
                        </div>
                      </td>
                      <td>
                        <span class="order-count-badge">
                          {{ exams.filter((e) => e.certificateId === program.certificateId).length }} đề
                        </span>
                      </td>
                      <td>
                        <div class="table-row-actions" style="justify-content: flex-end;">
                          <button
                            type="button"
                            class="table-action"
                            title="Chỉnh sửa kỳ thi"
                            @click="openProgram(program)"
                          >
                            <Pencil :size="13" />
                            <span>Sửa</span>
                          </button>
                          <button
                            type="button"
                            class="table-action danger"
                            title="Xóa kỳ thi"
                            @click="deleteProgram(program)"
                          >
                            <Trash2 :size="13" />
                            <span>Xóa</span>
                          </button>
                        </div>
                      </td>
                    </tr>
                  </tbody>
                </table>
              </div>

              <!-- Mobile Card View -->
              <div class="programs-mobile-grid" style="padding: 14px;">
                <article
                  v-for="program in filteredPrograms"
                  :key="`m-prog-${program.certificateId}`"
                  class="account-mobile-card"
                >
                  <div class="account-card-header">
                    <div>
                      <strong>{{ program.name }}</strong>
                      <small class="table-secondary" style="display: block; margin-top: 2px;">{{ program.description || 'Chương trình luyện thi' }}</small>
                    </div>
                    <span class="program-code-badge">{{ program.code }}</span>
                  </div>

                  <div class="account-card-body">
                    <div class="card-field-row">
                      <span class="card-field-label">Thang điểm:</span>
                      <div>
                        <span>Tổng: {{ program.totalScore }} điểm · Đạt: {{ program.passingBand || program.passingScore }}</span>
                      </div>
                    </div>

                    <div class="card-field-row">
                      <span class="card-field-label">Số đề thi:</span>
                      <span class="order-count-badge">
                        {{ exams.filter((e) => e.certificateId === program.certificateId).length }} đề thi
                      </span>
                    </div>

                    <div class="card-field-row" style="align-items: flex-start;">
                      <span class="card-field-label">Kỹ năng:</span>
                      <div class="program-skills-tags" style="justify-content: flex-end;">
                        <span
                          v-for="sk in parseSkillArray(program.skills)"
                          :key="sk.code"
                          class="skill-chip-tag"
                        >
                          {{ sk.label }}
                        </span>
                      </div>
                    </div>
                  </div>

                  <div class="account-card-actions">
                    <button
                      type="button"
                      class="table-action mobile-card-btn"
                      @click="openProgram(program)"
                    >
                      <Pencil :size="14" />
                      <span>Chỉnh sửa</span>
                    </button>
                    <button
                      type="button"
                      class="table-action mobile-card-btn danger"
                      @click="deleteProgram(program)"
                    >
                      <Trash2 :size="14" />
                      <span>Xóa kỳ thi</span>
                    </button>
                  </div>
                </article>
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
    .programs-desktop-table { display: none; }
    .programs-mobile-grid { display: flex; flex-direction: column; gap: 12px; }
    .admin-page-heading { align-items: flex-start; flex-direction: column; }
    .programs-hero-panel,
    .exams-hero-panel { align-items: stretch; flex-direction: column; padding: 18px; }
    .programs-hero-content,
    .exams-hero-content { min-width: 0; }
    .programs-hero-actions,
    .exams-hero-actions { width: 100%; align-items: stretch; flex-direction: column; flex-shrink: 1; }
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
    .programs-hero-panel,
    .exams-hero-panel { padding: 16px; }
    .programs-hero-actions > button,
    .exams-hero-actions > button { width: 100%; justify-content: center; }
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
