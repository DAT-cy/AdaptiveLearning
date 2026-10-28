<script setup>
import { computed, inject, ref } from 'vue'
import {
  AlertCircle,
  ArrowRight,
  Award,
  BarChart3,
  BookOpen,
  CalendarDays,
  Check,
  CheckCircle2,
  Clock,
  Compass,
  FileText,
  Filter,
  History,
  RotateCcw,
  Search,
  Sparkles,
  Target,
  Trophy,
  X,
} from 'lucide-vue-next'
import { homeContextKey } from '../../composables/homeContext'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'

const home = inject(homeContextKey)

// Bộ lọc tìm kiếm và trạng thái cho bảng lịch sử
const historySearch = ref('')
const historyStatusFilter = ref('ALL') // 'ALL' | 'GRADED' | 'IN_PROGRESS' | 'SUBMITTED'

// Lọc danh sách lịch sử theo từ khóa và trạng thái
const filteredHistory = computed(() => {
  if (!home.history) return []
  return home.history.filter((item) => {
    if (historyStatusFilter.value !== 'ALL' && item.status !== historyStatusFilter.value) {
      return false
    }
    if (historySearch.value.trim()) {
      const q = historySearch.value.trim().toLowerCase()
      const title = home.exams.find((e) => e.examId === item.examId)?.title || `Bài thi #${item.examId}`
      return title.toLowerCase().includes(q)
    }
    return true
  })
})

function getStatusBadgeVariant(status) {
  if (status === 'GRADED') return 'success'
  if (status === 'IN_PROGRESS') return 'warning'
  if (status === 'VIOLATED') return 'destructive'
  if (status === 'SUBMITTED') return 'default'
  return 'secondary'
}

function getMasteryBadgeVariant(value) {
  if (value >= 80) return 'success'
  if (value >= 50) return 'default'
  return 'warning'
}

function getMasteryLabel(value) {
  if (value >= 80) return 'Thành thạo'
  if (value >= 50) return 'Đang phát triển'
  return 'Cần củng cố'
}

function clearHistorySearch() {
  historySearch.value = ''
  historyStatusFilter.value = 'ALL'
}
</script>

<template>
  <div class="progress-page-wrapper" aria-label="Thống kê tiến độ và lịch sử học tập">
    <!-- 1. TIÊU ĐỀ TRANG VÀ BỘ CHUYỂN TAB (PAGE HEADER & VIEW SWITCHER) -->
    <header class="progress-header-card">
      <div class="header-main-info">
        <Badge variant="ai" class="header-badge">
          <Sparkles :size="13" class="text-ai-accent" />
          PHÂN TÍCH & THEO DÕI NĂNG LỰC
        </Badge>
        <h1 class="header-title">
          {{ home.activeView === 'history' ? 'Lịch sử làm bài kiểm tra' : 'Thống kê tiến độ học tập' }}
        </h1>
        <p class="header-desc">
          Theo dõi xác suất thành thạo kỹ năng theo mô hình Bayesian Knowledge Tracing và quản lý chi tiết kết quả các lượt thi.
        </p>
      </div>

      <!-- Thanh công cụ: Chọn Chứng chỉ & Bộ chuyển Tab Tiến độ / Lịch sử -->
      <div class="header-controls-row">
        <!-- Chuyển tab giữa Tiến độ BKT và Lịch sử thi -->
        <nav class="view-switch-tabs" aria-label="Chuyển chế độ xem">
          <button
            type="button"
            class="switch-tab-btn"
            :class="{ active: home.activeView === 'progress' }"
            @click="home.showView('progress')"
          >
            <BarChart3 :size="16" />
            <span>Thống kê tiến độ</span>
          </button>
          <button
            type="button"
            class="switch-tab-btn"
            :class="{ active: home.activeView === 'history' }"
            @click="home.showView('history')"
          >
            <History :size="16" />
            <span>Lịch sử làm bài</span>
            <span class="tab-count-pill">{{ home.history?.length || 0 }}</span>
          </button>
        </nav>

        <!-- Bộ chọn chứng chỉ (nếu có) -->
        <div v-if="home.certificates.length" class="cert-picker-box">
          <label for="progress-cert-select" class="cert-picker-label">Chứng chỉ:</label>
          <select
            id="progress-cert-select"
            v-model="home.selectedCertificateId"
            class="cert-picker-select"
            @change="home.loadProgress"
          >
            <option
              v-for="cert in home.certificates"
              :key="cert.certificateId"
              :value="String(cert.certificateId)"
            >
              {{ cert.name }}
            </option>
          </select>
        </div>
      </div>
    </header>

    <!-- 2. BỐ CỤC 4 CHỈ SỐ TIẾN ĐỘ TỔNG QUAN (4 METRICS SUMMARY) -->
    <section class="summary-metrics-grid" aria-label="Chỉ số tiến độ tổng quan">
      <!-- Điểm trung bình -->
      <div class="summary-card">
        <div class="summary-card-top">
          <span class="summary-icon bg-blue-50 text-brand-primary border border-blue-200/60">
            <Trophy :size="20" />
          </span>
          <span class="summary-label">Điểm trung bình</span>
        </div>
        <strong class="summary-value text-blue-900">
          {{ home.progress?.averageScore != null ? Number(home.progress.averageScore).toFixed(1) : home.averageScore != null ? home.averageScore.toFixed(1) : '—' }}
        </strong>
        <small class="summary-hint">
          {{ home.selectedCertificate?.name ? `Theo ${home.selectedCertificate.name}` : 'Tích lũy từ các bài thi' }}
        </small>
      </div>

      <!-- Lượt làm bài đã nộp -->
      <div class="summary-card">
        <div class="summary-card-top">
          <span class="summary-icon bg-emerald-50 text-emerald-600 border border-emerald-200/60">
            <CheckCircle2 :size="20" />
          </span>
          <span class="summary-label">Bài thi đã nộp</span>
        </div>
        <strong class="summary-value text-emerald-800">
          {{ home.progress?.totalAttempts ?? home.history?.length ?? 0 }}
        </strong>
        <small class="summary-hint">Lượt hoàn thành ghi nhận</small>
      </div>

      <!-- Lộ trình đang học -->
      <div class="summary-card">
        <div class="summary-card-top">
          <span class="summary-icon bg-violet-50 text-ai-accent border border-violet-200/60">
            <Compass :size="20" />
          </span>
          <span class="summary-label">Kế hoạch học</span>
        </div>
        <strong class="summary-value text-purple-900">
          {{ home.activePlans?.length || 0 }}
        </strong>
        <small class="summary-hint">Kế hoạch đang hoạt động</small>
      </div>

      <!-- Đề thi sẵn sàng -->
      <div class="summary-card">
        <div class="summary-card-top">
          <span class="summary-icon bg-sky-50 text-sky-600 border border-sky-200/60">
            <BookOpen :size="20" />
          </span>
          <span class="summary-label">Đề thi sẵn sàng</span>
        </div>
        <strong class="summary-value text-sky-800">
          {{ home.publishedExams?.length || 0 }}
        </strong>
        <small class="summary-hint">Trong kho đề luyện tập</small>
      </div>
    </section>

    <!-- TRẠNG THÁI LOADING SKELETON -->
    <div v-if="home.loading" class="progress-loading-grid" aria-busy="true">
      <div class="skeleton-card skeleton-banner"></div>
      <div class="skeleton-card skeleton-panel"></div>
    </div>

    <!-- 3. PHẦN A: NỘI DUNG THỐNG KÊ TIẾN ĐỘ (PROGRESS VIEW) -->
    <div v-else-if="home.activeView === 'progress'" class="progress-content-stack">
      <!-- BỐ CỤC 2 CỘT: BIỂU ĐỒ XU HƯỚNG & VÒNG TRÒN TỔNG QUAN -->
      <div class="charts-two-columns">
        <!-- 3.1. BIỂU ĐỒ XU HƯỚNG ĐIỂM SỐ (SCORE TREND BAR CHART) -->
        <section class="progress-section-card trend-chart-card" aria-labelledby="section-trend-title">
          <div class="card-header-bar">
            <div>
              <h2 id="section-trend-title" class="card-heading-title">Xu hướng điểm số</h2>
              <p class="card-heading-sub">Điểm số qua các lượt kiểm tra gần nhất của bạn</p>
            </div>
            <span class="trend-badge-info">Tối đa 7 lượt gần nhất</span>
          </div>

          <!-- Biểu đồ cột SVG / CSS thuần không cần thư viện ngoài -->
          <div v-if="home.scoreTrend.length" class="custom-bar-chart-container">
            <!-- Trục ngang và các đường lưới tỉ lệ -->
            <div class="chart-gridlines-bg" aria-hidden="true">
              <span class="gridline-marker marker-100"><i>100%</i></span>
              <span class="gridline-marker marker-75"><i>75%</i></span>
              <span class="gridline-marker marker-50"><i>50%</i></span>
              <span class="gridline-marker marker-25"><i>25%</i></span>
              <span class="gridline-marker marker-0"><i>0%</i></span>
            </div>

            <!-- Cột dữ liệu cho từng lượt thi -->
            <div class="chart-bars-row">
              <div
                v-for="(entry, index) in home.scoreTrend"
                :key="`${entry.label}-${index}`"
                class="chart-bar-column"
                :title="`${entry.label}: ${entry.score} / ${entry.maxScore} điểm (${Math.round((entry.score / entry.maxScore) * 100)}%)`"
              >
                <!-- Giá trị điểm hiển thị trên đầu cột -->
                <span class="bar-value-label">{{ entry.score }}</span>

                <!-- Khung thanh cột -->
                <div class="bar-pillar-track">
                  <div
                    class="bar-pillar-fill"
                    :style="{
                      height: `${Math.max(8, Math.min(100, Math.round((entry.score / entry.maxScore) * 100)))}%`,
                    }"
                  ></div>
                </div>

                <!-- Nhãn ngày tháng dưới chân cột -->
                <span class="bar-axis-label">{{ entry.label }}</span>
              </div>
            </div>
          </div>

          <!-- Empty State: Chưa có lượt làm bài để vẽ biểu đồ -->
          <div v-else class="chart-empty-state">
            <div class="empty-icon-circle bg-sky-50 text-sky-600">
              <BarChart3 :size="24" />
            </div>
            <div class="empty-text-wrap">
              <strong>Chưa có dữ liệu xu hướng điểm số</strong>
              <p>Hoàn thành ít nhất một bài kiểm tra để hệ thống tổng hợp biểu đồ diễn biến điểm số của bạn.</p>
            </div>
            <Button variant="outline" size="sm" @click="home.showView('tests')">
              Làm bài kiểm tra ngay <ArrowRight :size="15" />
            </Button>
          </div>

          <!-- Chú thích biểu đồ -->
          <div v-if="home.scoreTrend.length" class="chart-legend-footer">
            <span class="legend-indicator">
              <i class="legend-box bg-blue-600"></i> Điểm đạt được
            </span>
            <span class="legend-indicator">
              <i class="legend-box bg-slate-200"></i> Thang điểm tối đa
            </span>
          </div>
        </section>

        <!-- 3.2. VÒNG TRÒN ĐIỂM SỐ & CHUẨN ĐẦU RA (RADIAL GAUGE) -->
        <section class="progress-section-card score-gauge-card" aria-labelledby="section-gauge-title">
          <div class="card-header-bar">
            <div>
              <h2 id="section-gauge-title" class="card-heading-title">Tổng quan năng lực</h2>
              <p class="card-heading-sub">{{ home.selectedCertificate?.name || 'Đánh giá chung' }}</p>
            </div>
          </div>

          <div class="radial-gauge-wrapper">
            <div
              class="radial-gauge-circle"
              :style="{
                background: `conic-gradient(var(--brand-primary, #2563EB) ${home.averageScorePercent}%, #E2E8F0 0)`,
              }"
            >
              <div class="radial-gauge-inner">
                <span class="radial-score-num">
                  {{ home.progress?.averageScore != null ? Number(home.progress.averageScore).toFixed(1) : (home.averageScore != null ? home.averageScore.toFixed(1) : '—') }}
                </span>
                <span class="radial-score-label">Điểm TB</span>
              </div>
            </div>

            <div class="radial-gauge-meta">
              <div class="gauge-meta-row">
                <span class="gauge-meta-title">Tỷ lệ hoàn thành chuẩn:</span>
                <strong class="text-blue-900">{{ home.averageScorePercent }}%</strong>
              </div>
              <div v-if="home.selectedCertificate?.passingScore" class="gauge-meta-row">
                <span class="gauge-meta-title">Mục tiêu chuẩn đầu ra:</span>
                <strong>{{ home.selectedCertificate.passingScore }} điểm</strong>
              </div>
              <div class="gauge-meta-row">
                <span class="gauge-meta-title">Tổng lượt thi đã nộp:</span>
                <strong>{{ home.progress?.totalAttempts ?? home.history?.length ?? 0 }} bài</strong>
              </div>
            </div>
          </div>
        </section>
      </div>

      <!-- 3.3. PHÂN TÍCH MỨC ĐỘ THÀNH THẠO KỸ NĂNG (BKT MASTERY SKILLS) -->
      <section class="progress-section-card" aria-labelledby="section-mastery-title">
        <div class="card-header-bar">
          <div>
            <div class="flex items-center gap-2">
              <h2 id="section-mastery-title" class="card-heading-title">Mức độ thành thạo kỹ năng</h2>
              <Badge variant="ai" size="sm">Bayesian Knowledge Tracing</Badge>
            </div>
            <p class="card-heading-sub">
              Mô hình BKT tính toán xác suất thành thạo từng kỹ năng từ kết quả bài thi để cá nhân hóa lộ trình
            </p>
          </div>
        </div>

        <!-- Danh sách thanh đo kỹ năng BKT -->
        <div v-if="home.masterySkills.length" class="skills-mastery-grid">
          <div
            v-for="(skill, index) in home.masterySkills"
            :key="skill.name || index"
            class="skill-mastery-card"
          >
            <div class="skill-card-top">
              <div class="skill-name-group">
                <span class="skill-icon-pill">
                  <Target :size="16" />
                </span>
                <strong class="skill-title-text">{{ skill.name }}</strong>
              </div>

              <div class="skill-badges-group">
                <Badge :variant="getMasteryBadgeVariant(skill.value)" size="sm">
                  {{ getMasteryLabel(skill.value) }}
                </Badge>
                <strong class="skill-value-percent">{{ Math.round(skill.value) }}%</strong>
              </div>
            </div>

            <!-- Thanh tiến độ kỹ năng -->
            <div class="skill-progress-track">
              <div
                class="skill-progress-fill"
                :class="{
                  'fill-success': skill.value >= 80,
                  'fill-primary': skill.value >= 50 && skill.value < 80,
                  'fill-warning': skill.value < 50,
                }"
                :style="{ width: `${Math.max(0, Math.min(100, skill.value))}%` }"
              ></div>
            </div>
          </div>
        </div>

        <!-- Empty State: Chưa có dữ liệu phân tích BKT -->
        <div v-else class="empty-state-box">
          <div class="empty-icon-circle bg-violet-50 text-ai-accent">
            <Sparkles :size="24" />
          </div>
          <div class="empty-text-wrap">
            <strong>Chưa có dữ liệu phân tích kỹ năng theo chứng chỉ này</strong>
            <p>Hãy hoàn thành bài kiểm tra thuộc chứng chỉ để hệ thống Bayesian Knowledge Tracing phân tích chi tiết mức độ thành thạo các kỹ năng.</p>
          </div>
          <Button variant="outline" size="sm" @click="home.showView('tests')">
            Làm bài kiểm tra <ArrowRight :size="15" />
          </Button>
        </div>
      </section>

      <!-- 3.4. KẾT QUẢ CÁC BÀI THI GẦN ĐÂY TRONG TIẾN ĐỘ -->
      <section class="progress-section-card" aria-labelledby="section-recent-attempts-title">
        <div class="card-header-bar">
          <div>
            <h2 id="section-recent-attempts-title" class="card-heading-title">Bài kiểm tra gần nhất</h2>
            <p class="card-heading-sub">Kết quả các lượt làm bài mới nhất của bạn</p>
          </div>
          <button type="button" class="action-link-btn" @click="home.showView('history')">
            Xem toàn bộ lịch sử ({{ home.history?.length || 0 }}) <ArrowRight :size="14" />
          </button>
        </div>

        <!-- Danh sách bài thi gần đây -->
        <div v-if="home.history?.length" class="history-list-compact">
          <article
            v-for="item in home.history.slice(0, 4)"
            :key="item.attemptId"
            class="history-row-compact"
          >
            <div class="history-icon-col">
              <span
                class="history-status-icon"
                :class="{
                  'bg-emerald-50 text-emerald-600 border-emerald-200': item.status === 'GRADED',
                  'bg-amber-50 text-amber-700 border-amber-200': item.status === 'IN_PROGRESS',
                  'bg-rose-50 text-rose-600 border-rose-200': item.status === 'VIOLATED',
                  'bg-slate-50 text-slate-600 border-slate-200': !['GRADED', 'IN_PROGRESS', 'VIOLATED'].includes(item.status),
                }"
              >
                <CheckCircle2 v-if="item.status === 'GRADED'" :size="17" />
                <Clock v-else-if="item.status === 'IN_PROGRESS'" :size="17" />
                <AlertCircle v-else-if="item.status === 'VIOLATED'" :size="17" />
                <BookOpen v-else :size="17" />
              </span>
            </div>

            <div class="history-info-col">
              <strong class="history-exam-title">
                {{ home.exams.find((exam) => exam.examId === item.examId)?.title || `Bài thi #${item.examId}` }}
              </strong>
              <p class="history-meta-sub">
                {{ home.formatDate(item.startedAt) }}
                <span class="meta-dot">•</span>
                <Badge :variant="getStatusBadgeVariant(item.status)" size="sm">
                  {{ home.localizeStatus(item.status) }}
                </Badge>
              </p>
            </div>

            <div class="history-score-col">
              <div v-if="item.score != null" class="score-num-wrap">
                <span class="score-val">{{ item.score }}</span>
                <small v-if="item.maxScore != null" class="score-max">/ {{ item.maxScore }}</small>
              </div>
              <span v-else class="score-pending">—</span>

              <Button
                v-if="item.status === 'IN_PROGRESS'"
                variant="outline"
                size="sm"
                @click="home.resumeAttempt(item)"
              >
                Tiếp tục
              </Button>
              <Button
                v-else
                variant="ghost"
                size="sm"
                @click="home.showView('history')"
              >
                Xem chi tiết
              </Button>
            </div>
          </article>
        </div>

        <div v-else class="empty-state-box">
          <div class="empty-icon-circle bg-sky-50 text-sky-600">
            <BookOpen :size="24" />
          </div>
          <div class="empty-text-wrap">
            <strong>Chưa có lượt làm bài nào được ghi nhận</strong>
            <p>Hãy làm một bài kiểm tra để bắt đầu theo dõi tiến độ học tập và phân tích điểm số.</p>
          </div>
          <Button variant="outline" size="sm" @click="home.showView('tests')">
            Vào kho bài kiểm tra <ArrowRight :size="15" />
          </Button>
        </div>
      </section>
    </div>

    <!-- 4. PHẦN B: NỘI DUNG LỊCH SỬ LÀM BÀI ĐẦY ĐỦ (HISTORY VIEW) -->
    <div v-else class="history-content-stack">
      <section class="progress-section-card" aria-labelledby="section-full-history-title">
        <div class="card-header-bar history-header-bar">
          <div>
            <h2 id="section-full-history-title" class="card-heading-title">Toàn bộ lịch sử làm bài</h2>
            <p class="card-heading-sub">Chi tiết kết quả, thời gian và điểm số tất cả các lượt thi của bạn</p>
          </div>
          <span class="history-count-tag">
            {{ filteredHistory.length }} / {{ home.history?.length || 0 }} lượt thi
          </span>
        </div>

        <!-- Thanh công cụ tìm kiếm và lọc trạng thái trong lịch sử -->
        <div class="history-filter-toolbar">
          <div class="history-search-box">
            <Search :size="16" class="search-icon" />
            <input
              v-model="historySearch"
              type="search"
              class="history-search-input"
              placeholder="Tìm kiếm theo tên bài thi..."
              aria-label="Tìm kiếm lịch sử bài thi"
            />
            <button
              v-if="historySearch"
              type="button"
              class="clear-search-btn"
              aria-label="Xóa tìm kiếm"
              @click="historySearch = ''"
            >
              <X :size="14" />
            </button>
          </div>

          <!-- Lọc theo trạng thái thi -->
          <div class="status-filter-pills">
            <button
              type="button"
              class="filter-pill-btn"
              :class="{ active: historyStatusFilter === 'ALL' }"
              @click="historyStatusFilter = 'ALL'"
            >
              Tất cả
            </button>
            <button
              type="button"
              class="filter-pill-btn"
              :class="{ active: historyStatusFilter === 'GRADED' }"
              @click="historyStatusFilter = 'GRADED'"
            >
              Đã chấm điểm
            </button>
            <button
              type="button"
              class="filter-pill-btn"
              :class="{ active: historyStatusFilter === 'IN_PROGRESS' }"
              @click="historyStatusFilter = 'IN_PROGRESS'"
            >
              Đang làm dở
            </button>
            <button
              type="button"
              class="filter-pill-btn"
              :class="{ active: historyStatusFilter === 'SUBMITTED' }"
              @click="historyStatusFilter = 'SUBMITTED'"
            >
              Đã nộp bài
            </button>
          </div>
        </div>

        <!-- BẢNG LỊCH SỬ THI TRÊN DESKTOP (DATA TABLE) -->
        <div v-if="filteredHistory.length" class="history-table-container">
          <table class="history-data-table" role="table">
            <thead>
              <tr>
                <th scope="col" class="th-exam">Tên bài kiểm tra</th>
                <th scope="col" class="th-date">Thời gian bắt đầu</th>
                <th scope="col" class="th-status">Trạng thái</th>
                <th scope="col" class="th-score">Điểm số</th>
                <th scope="col" class="th-violations">Quy chế</th>
                <th scope="col" class="th-action">Thao tác</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="item in filteredHistory" :key="item.attemptId" class="history-table-row">
                <!-- Tên bài thi -->
                <td class="td-exam">
                  <div class="table-exam-info">
                    <span class="exam-icon-mini bg-blue-50 text-brand-primary">
                      <BookOpen :size="16" />
                    </span>
                    <div>
                      <strong class="table-exam-title">
                        {{ home.exams.find((exam) => exam.examId === item.examId)?.title || `Bài thi #${item.examId}` }}
                      </strong>
                      <small class="table-attempt-id">Lượt thi #{{ item.attemptId }}</small>
                    </div>
                  </div>
                </td>

                <!-- Ngày thi -->
                <td class="td-date">
                  <span class="table-date-text">{{ home.formatDate(item.startedAt) }}</span>
                </td>

                <!-- Trạng thái -->
                <td class="td-status">
                  <Badge :variant="getStatusBadgeVariant(item.status)" size="sm">
                    {{ home.localizeStatus(item.status) }}
                  </Badge>
                </td>

                <!-- Điểm số -->
                <td class="td-score">
                  <div v-if="item.score != null" class="table-score-display">
                    <strong class="table-score-val">{{ item.score }}</strong>
                    <span v-if="item.maxScore != null" class="table-max-val">/ {{ item.maxScore }}</span>
                    <Badge v-if="item.bandScore != null" variant="ai" size="sm" class="ml-1">
                      Band {{ item.bandScore }}
                    </Badge>
                  </div>
                  <span v-else class="text-slate-400">—</span>
                </td>

                <!-- Vi phạm nếu có -->
                <td class="td-violations">
                  <span v-if="item.violationCount > 0" class="violation-pill-warn">
                    {{ item.violationCount }} cảnh báo
                  </span>
                  <span v-else class="text-slate-400 text-xs">Chuẩn quy chế</span>
                </td>

                <!-- Thao tác -->
                <td class="td-action">
                  <Button
                    v-if="item.status === 'IN_PROGRESS'"
                    variant="default"
                    size="sm"
                    @click="home.resumeAttempt(item)"
                  >
                    Tiếp tục làm bài
                  </Button>
                  <Button
                    v-else
                    variant="outline"
                    size="sm"
                    @click="home.showView('tests')"
                  >
                    Làm lại đề này
                  </Button>
                </td>
              </tr>
            </tbody>
          </table>
        </div>

        <!-- Empty State: Tìm kiếm không ra kết quả -->
        <div v-else-if="historySearch || historyStatusFilter !== 'ALL'" class="empty-state-box">
          <div class="empty-icon-circle bg-slate-100 text-slate-500">
            <Search :size="24" />
          </div>
          <div class="empty-text-wrap">
            <strong>Không tìm thấy lượt làm bài phù hợp</strong>
            <p>Không có kết quả nào khớp với điều kiện lọc hiện tại. Hãy thử tìm kiếm từ khóa khác.</p>
          </div>
          <Button variant="outline" size="sm" @click="clearHistorySearch">
            <RotateCcw :size="14" /> Xóa bộ lọc
          </Button>
        </div>

        <!-- Empty State: Chưa có bất kỳ lượt thi nào -->
        <div v-else class="empty-state-box">
          <div class="empty-icon-circle bg-sky-50 text-sky-600">
            <BookOpen :size="24" />
          </div>
          <div class="empty-text-wrap">
            <strong>Chưa có lịch sử làm bài thi nào</strong>
            <p>Các kết quả làm bài kiểm tra sau khi nộp sẽ được lưu trữ và hiển thị đầy đủ tại đây.</p>
          </div>
          <Button variant="default" size="sm" @click="home.showView('tests')">
            Vào kho bài kiểm tra <ArrowRight :size="15" />
          </Button>
        </div>
      </section>
    </div>
  </div>
</template>

<style scoped>
.progress-page-wrapper {
  display: flex;
  flex-direction: column;
  gap: 24px;
  width: 100%;
}

/* ==========================================================================
   PAGE HEADER & CONTROLS
   ========================================================================== */
.progress-header-card {
  display: flex;
  flex-direction: column;
  gap: 20px;
  padding: 24px 28px;
  border-radius: var(--radius-lg, 12px);
  background: linear-gradient(135deg, #FFFFFF 0%, #F0FDFA 60%, #F5F3FF 100%);
  border: 1px solid var(--border-subtle, #E2E8F0);
  box-shadow: 0 2px 10px rgba(15, 23, 42, 0.03);
}

.header-badge {
  margin-bottom: 12px;
}

.header-title {
  margin: 0 0 8px;
  font-size: clamp(22px, 2.3vw, 28px);
  font-weight: 800;
  line-height: 1.25;
  letter-spacing: -0.5px;
  color: var(--text-primary, #0F172A);
}

.header-desc {
  margin: 0;
  font-size: 14px;
  line-height: 1.55;
  color: var(--text-secondary, #475569);
  max-width: 680px;
}

.header-controls-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  flex-wrap: wrap;
  padding-top: 14px;
  border-top: 1px solid rgba(226, 232, 240, 0.8);
}

/* Tab Switcher */
.view-switch-tabs {
  display: flex;
  align-items: center;
  background-color: #F1F5F9;
  padding: 4px;
  border-radius: var(--radius-md, 8px);
  gap: 4px;
}

.switch-tab-btn {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  padding: 7px 14px;
  border-radius: 6px;
  border: none;
  background: transparent;
  color: var(--text-secondary, #475569);
  font: inherit;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.15s ease;
}

.switch-tab-btn:hover {
  color: var(--text-primary, #0F172A);
}

.switch-tab-btn.active {
  background-color: #ffffff;
  color: var(--brand-primary, #2563EB);
  box-shadow: 0 1px 4px rgba(15, 23, 42, 0.08);
}

.tab-count-pill {
  padding: 1px 6px;
  border-radius: 999px;
  font-size: 11px;
  font-weight: 700;
  background-color: #E2E8F0;
  color: #475569;
}

.switch-tab-btn.active .tab-count-pill {
  background-color: #EFF6FF;
  color: var(--brand-primary, #2563EB);
}

/* Cert Picker Select */
.cert-picker-box {
  display: flex;
  align-items: center;
  gap: 8px;
}

.cert-picker-label {
  font-size: 12.5px;
  font-weight: 600;
  color: var(--text-secondary, #475569);
}

.cert-picker-select {
  height: 36px;
  padding: 0 12px;
  border-radius: var(--radius-sm, 6px);
  border: 1px solid var(--border-subtle, #E2E8F0);
  background-color: #ffffff;
  color: var(--text-primary, #0F172A);
  font: inherit;
  font-size: 13px;
  outline: none;
  cursor: pointer;
  transition: border-color 0.15s ease;
}

.cert-picker-select:focus {
  border-color: var(--brand-primary, #2563EB);
}

/* ==========================================================================
   4 SUMMARY METRICS QUAD
   ========================================================================== */
.summary-metrics-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
}

.summary-card {
  display: flex;
  flex-direction: column;
  padding: 16px 18px;
  border-radius: var(--radius-lg, 12px);
  background-color: #ffffff;
  border: 1px solid var(--border-subtle, #E2E8F0);
  box-shadow: 0 1px 3px rgba(15, 23, 42, 0.02);
  transition: transform 0.15s ease, box-shadow 0.15s ease;
}

.summary-card:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(15, 23, 42, 0.04);
}

.summary-card-top {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 10px;
}

.summary-icon {
  display: grid;
  place-items: center;
  width: 36px;
  height: 36px;
  border-radius: var(--radius-md, 8px);
  flex-shrink: 0;
}

.summary-label {
  font-size: 12.5px;
  font-weight: 600;
  color: var(--text-secondary, #475569);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.summary-value {
  font-size: 24px;
  font-weight: 800;
  line-height: 1.15;
}

.summary-hint {
  margin-top: 4px;
  font-size: 11px;
  color: var(--text-muted, #64748B);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* ==========================================================================
   PROGRESS SECTIONS (CHARTS & BKT MASTERY)
   ========================================================================== */
.progress-content-stack,
.history-content-stack {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.progress-section-card {
  padding: 22px 24px;
  border-radius: var(--radius-lg, 12px);
  background-color: #ffffff;
  border: 1px solid var(--border-subtle, #E2E8F0);
  box-shadow: 0 1px 3px rgba(15, 23, 42, 0.02);
}

.card-header-bar {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 14px;
  margin-bottom: 18px;
}

.card-heading-title {
  margin: 0;
  font-size: 16px;
  font-weight: 750;
  color: var(--text-primary, #0F172A);
  letter-spacing: -0.2px;
}

.card-heading-sub {
  margin: 3px 0 0;
  font-size: 12.5px;
  color: var(--text-muted, #64748B);
}

.trend-badge-info {
  font-size: 11.5px;
  color: var(--text-muted, #64748B);
}

.charts-two-columns {
  display: grid;
  grid-template-columns: minmax(0, 1.8fr) minmax(280px, 1fr);
  gap: 20px;
}

/* ==========================================================================
   CUSTOM BAR CHART (SCORE TREND)
   ========================================================================== */
.custom-bar-chart-container {
  position: relative;
  display: flex;
  flex-direction: column;
  height: 220px;
  padding: 10px 0 0 36px;
}

.chart-gridlines-bg {
  position: absolute;
  top: 10px;
  bottom: 30px;
  left: 36px;
  right: 0;
  pointer-events: none;
}

.gridline-marker {
  position: absolute;
  left: -36px;
  right: 0;
  border-top: 1px dashed #E2E8F0;
  font-size: 10px;
  color: #94A3B8;
  display: flex;
  align-items: flex-end;
}

.gridline-marker i {
  position: absolute;
  left: 0;
  top: -8px;
  font-style: normal;
}

.marker-100 { top: 0%; }
.marker-75  { top: 25%; }
.marker-50  { top: 50%; }
.marker-25  { top: 75%; }
.marker-0   { top: 100%; border-style: solid; border-color: #CBD5E1; }

.chart-bars-row {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: flex-end;
  justify-content: space-around;
  height: 100%;
  padding-bottom: 30px;
}

.chart-bar-column {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: flex-end;
  height: 100%;
  width: min(44px, 12%);
  position: relative;
  cursor: pointer;
}

.bar-value-label {
  font-size: 11px;
  font-weight: 750;
  color: var(--text-primary, #0F172A);
  margin-bottom: 4px;
}

.bar-pillar-track {
  display: flex;
  align-items: flex-end;
  width: 100%;
  height: 100%;
  border-radius: 4px 4px 0 0;
  background-color: rgba(226, 232, 240, 0.4);
  overflow: hidden;
}

.bar-pillar-fill {
  width: 100%;
  border-radius: 4px 4px 0 0;
  background: linear-gradient(180deg, #2563EB 0%, #3B82F6 100%);
  transition: height 0.4s ease, opacity 0.15s ease;
}

.chart-bar-column:hover .bar-pillar-fill {
  opacity: 0.85;
}

.bar-axis-label {
  position: absolute;
  bottom: -24px;
  font-size: 11px;
  color: var(--text-muted, #64748B);
  white-space: nowrap;
}

.chart-legend-footer {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 20px;
  margin-top: 24px;
  padding-top: 10px;
  border-top: 1px solid #F1F5F9;
}

.legend-indicator {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 11.5px;
  color: var(--text-secondary, #475569);
}

.legend-box {
  width: 12px;
  height: 12px;
  border-radius: 3px;
}

.chart-empty-state {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 24px;
  border-radius: var(--radius-md, 8px);
  background-color: #F8FAFC;
  border: 1px dashed var(--border-subtle, #E2E8F0);
}

/* ==========================================================================
   RADIAL GAUGE (SCORE OVERVIEW)
   ========================================================================== */
.radial-gauge-wrapper {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
  padding: 10px 0;
}

.radial-gauge-circle {
  display: grid;
  place-items: center;
  width: 120px;
  height: 120px;
  border-radius: 50%;
  box-shadow: 0 4px 12px rgba(15, 118, 110, 0.1);
}

.radial-gauge-inner {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  width: 96px;
  height: 96px;
  border-radius: 50%;
  background-color: #ffffff;
  box-shadow: inset 0 2px 4px rgba(0, 0, 0, 0.04);
}

.radial-score-num {
  font-size: 26px;
  font-weight: 850;
  color: var(--brand-primary, #2563EB);
  line-height: 1;
}

.radial-score-label {
  font-size: 10.5px;
  color: var(--text-muted, #64748B);
  margin-top: 2px;
}

.radial-gauge-meta {
  display: flex;
  flex-direction: column;
  gap: 8px;
  width: 100%;
  padding: 12px;
  border-radius: var(--radius-md, 8px);
  background-color: #F8FAFC;
  border: 1px solid var(--border-subtle, #E2E8F0);
}

.gauge-meta-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 12px;
}

.gauge-meta-title {
  color: var(--text-secondary, #475569);
}

/* ==========================================================================
   BKT MASTERY SKILLS GRID
   ========================================================================== */
.skills-mastery-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.skill-mastery-card {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 14px 16px;
  border-radius: var(--radius-md, 8px);
  background-color: #F8FAFC;
  border: 1px solid var(--border-subtle, #E2E8F0);
}

.skill-card-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.skill-name-group {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
  flex: 1;
}

.skill-icon-pill {
  display: grid;
  place-items: center;
  color: var(--brand-primary, #2563EB);
  flex-shrink: 0;
}

.skill-title-text {
  font-size: 13.5px;
  font-weight: 650;
  color: var(--text-primary, #0F172A);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.skill-badges-group {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}

.skill-value-percent {
  font-size: 13px;
  font-weight: 750;
  color: var(--text-primary, #0F172A);
}

.skill-progress-track {
  width: 100%;
  height: 6px;
  border-radius: 3px;
  background-color: #E2E8F0;
  overflow: hidden;
}

.skill-progress-fill {
  height: 100%;
  border-radius: 3px;
  transition: width 0.3s ease;
}

.skill-progress-fill.fill-success {
  background-color: #10B981;
}

.skill-progress-fill.fill-primary {
  background-color: var(--brand-primary, #2563EB);
}

.skill-progress-fill.fill-warning {
  background-color: #F59E0B;
}

/* ==========================================================================
   COMPACT HISTORY LIST (INSIDE PROGRESS VIEW)
   ========================================================================== */
.history-list-compact {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.history-row-compact {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 12px 14px;
  border-radius: var(--radius-md, 8px);
  background-color: #F8FAFC;
  border: 1px solid var(--border-subtle, #E2E8F0);
  transition: background-color 0.15s ease;
}

.history-row-compact:hover {
  background-color: #F1F5F9;
}

.history-icon-col {
  flex-shrink: 0;
}

.history-status-icon {
  display: grid;
  place-items: center;
  width: 36px;
  height: 36px;
  border-radius: var(--radius-md, 8px);
  border: 1px solid;
}

.history-info-col {
  flex: 1;
  min-width: 0;
}

.history-exam-title {
  display: block;
  font-size: 13.5px;
  font-weight: 700;
  color: var(--text-primary, #0F172A);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.history-meta-sub {
  display: flex;
  align-items: center;
  gap: 6px;
  margin: 2px 0 0;
  font-size: 12px;
  color: var(--text-muted, #64748B);
}

.meta-dot {
  color: #CBD5E1;
}

.history-score-col {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-shrink: 0;
}

.score-num-wrap {
  display: flex;
  align-items: baseline;
  gap: 2px;
}

.score-val {
  font-size: 16px;
  font-weight: 800;
  color: var(--brand-primary, #2563EB);
}

.score-max {
  font-size: 11.5px;
  color: var(--text-muted, #64748B);
}

.score-pending {
  font-size: 14px;
  color: #94A3B8;
}

.action-link-btn {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  border: none;
  background: transparent;
  color: var(--brand-primary, #2563EB);
  font: inherit;
  font-size: 12.5px;
  font-weight: 600;
  cursor: pointer;
}

.action-link-btn:hover {
  text-decoration: underline;
}

/* ==========================================================================
   FULL HISTORY VIEW (DATA TABLE & FILTER TOOLBAR)
   ========================================================================== */
.history-header-bar {
  margin-bottom: 14px;
}

.history-count-tag {
  font-size: 12.5px;
  font-weight: 600;
  color: var(--text-muted, #64748B);
}

.history-filter-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  flex-wrap: wrap;
  margin-bottom: 16px;
  padding-bottom: 14px;
  border-bottom: 1px solid #F1F5F9;
}

.history-search-box {
  position: relative;
  display: flex;
  align-items: center;
  width: 100%;
  max-width: 320px;
}

.history-search-box .search-icon {
  position: absolute;
  left: 12px;
  color: #94A3B8;
  pointer-events: none;
}

.history-search-input {
  width: 100%;
  height: 36px;
  padding: 0 34px 0 36px;
  border-radius: var(--radius-sm, 6px);
  border: 1px solid var(--border-subtle, #E2E8F0);
  background-color: #ffffff;
  font: inherit;
  font-size: 13px;
  color: var(--text-primary, #0F172A);
  outline: none;
  transition: border-color 0.15s ease;
}

.history-search-input:focus {
  border-color: var(--brand-primary, #2563EB);
}

.clear-search-btn {
  position: absolute;
  right: 10px;
  display: grid;
  place-items: center;
  border: none;
  background: transparent;
  color: #94A3B8;
  cursor: pointer;
}

.clear-search-btn:hover {
  color: var(--text-primary, #0F172A);
}

.status-filter-pills {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
}

.filter-pill-btn {
  padding: 5px 12px;
  border-radius: 999px;
  border: 1px solid var(--border-subtle, #E2E8F0);
  background-color: #F8FAFC;
  color: var(--text-secondary, #475569);
  font: inherit;
  font-size: 12px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.15s ease;
}

.filter-pill-btn:hover {
  background-color: #ffffff;
  color: var(--text-primary, #0F172A);
  border-color: #CBD5E1;
}

.filter-pill-btn.active {
  background-color: var(--brand-primary, #2563EB);
  color: #ffffff;
  border-color: var(--brand-primary, #2563EB);
  font-weight: 600;
}

/* History Data Table */
.history-table-container {
  width: 100%;
  overflow-x: auto;
}

.history-data-table {
  width: 100%;
  border-collapse: separate;
  border-spacing: 0;
  font-size: 13px;
  text-align: left;
}

.history-data-table th {
  padding: 10px 14px;
  background-color: #F8FAFC;
  border-bottom: 1.5px solid var(--border-subtle, #E2E8F0);
  color: var(--text-secondary, #475569);
  font-weight: 700;
  font-size: 12px;
  white-space: nowrap;
}

.history-data-table td {
  padding: 12px 14px;
  border-bottom: 1px solid #F1F5F9;
  vertical-align: middle;
}

.history-table-row:hover td {
  background-color: #F8FAFC;
}

.table-exam-info {
  display: flex;
  align-items: center;
  gap: 10px;
}

.exam-icon-mini {
  display: grid;
  place-items: center;
  width: 32px;
  height: 32px;
  border-radius: 6px;
  flex-shrink: 0;
}

.table-exam-title {
  display: block;
  font-size: 13.5px;
  color: var(--text-primary, #0F172A);
}

.table-attempt-id {
  font-size: 11px;
  color: var(--text-muted, #64748B);
}

.table-date-text {
  color: var(--text-secondary, #475569);
  white-space: nowrap;
}

.table-score-display {
  display: flex;
  align-items: baseline;
  gap: 2px;
  white-space: nowrap;
}

.table-score-val {
  font-size: 15px;
  font-weight: 800;
  color: var(--brand-primary, #2563EB);
}

.table-max-val {
  font-size: 11.5px;
  color: var(--text-muted, #64748B);
}

.violation-pill-warn {
  display: inline-flex;
  padding: 2px 6px;
  border-radius: 4px;
  background-color: #FFF1F2;
  color: #E11D48;
  font-size: 11px;
  font-weight: 600;
}

/* ==========================================================================
   EMPTY STATE & SKELETON LOADING
   ========================================================================== */
.empty-state-box {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 24px;
  border-radius: var(--radius-md, 8px);
  background-color: #F8FAFC;
  border: 1px dashed var(--border-subtle, #E2E8F0);
}

.empty-icon-circle {
  display: grid;
  place-items: center;
  width: 44px;
  height: 44px;
  border-radius: 50%;
  flex-shrink: 0;
}

.empty-text-wrap {
  flex: 1;
  min-width: 0;
}

.empty-text-wrap strong {
  display: block;
  font-size: 13.5px;
  font-weight: 700;
  color: var(--text-primary, #0F172A);
}

.empty-text-wrap p {
  margin: 3px 0 0;
  font-size: 12px;
  line-height: 1.5;
  color: var(--text-muted, #64748B);
}

.progress-loading-grid {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.skeleton-card {
  border-radius: var(--radius-lg, 12px);
  background: linear-gradient(90deg, #F1F5F9 25%, #E2E8F0 50%, #F1F5F9 75%);
  background-size: 200% 100%;
  animation: shimmer 1.5s infinite;
}

@keyframes shimmer {
  0% { background-position: 200% 0; }
  100% { background-position: -200% 0; }
}

.skeleton-banner { height: 160px; }
.skeleton-panel { height: 260px; }

/* ==========================================================================
   RESPONSIVE DESIGN (TABLET & MOBILE)
   ========================================================================== */
@media (max-width: 1024px) {
  .summary-metrics-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .charts-two-columns {
    grid-template-columns: 1fr;
  }

  .skills-mastery-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 640px) {
  .progress-header-card {
    padding: 18px 16px;
  }

  .summary-metrics-grid {
    grid-template-columns: 1fr;
  }

  .header-controls-row {
    flex-direction: column;
    align-items: flex-start;
  }

  .view-switch-tabs {
    width: 100%;
  }

  .switch-tab-btn {
    flex: 1;
    justify-content: center;
  }

  .cert-picker-box {
    width: 100%;
  }

  .cert-picker-select {
    flex: 1;
  }

  .history-filter-toolbar {
    flex-direction: column;
    align-items: flex-start;
  }

  .history-search-box {
    max-width: 100%;
  }

  .history-row-compact {
    flex-direction: column;
    align-items: flex-start;
    gap: 10px;
  }

  .history-score-col {
    width: 100%;
    justify-content: space-between;
  }
}
</style>
