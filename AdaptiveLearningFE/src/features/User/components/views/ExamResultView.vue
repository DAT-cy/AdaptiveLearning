<script setup>
import { computed, inject } from 'vue'
import {
  AlertCircle,
  ArrowRight,
  Award,
  BarChart3,
  BookOpen,
  Check,
  CheckCircle2,
  Clock3,
  HelpCircle,
  RotateCcw,
  Sparkles,
  Trophy,
  XCircle,
} from 'lucide-vue-next'
import { homeContextKey } from '../../composables/homeContext'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'

const home = inject(homeContextKey)

// Tìm attempt tương ứng từ lịch sử nếu có
const currentAttempt = computed(() => {
  if (!home.result?.attemptId || !home.history) return null
  return home.history.find((h) => String(h.attemptId) === String(home.result.attemptId))
})

// Tìm đề thi tương ứng để lấy tên và chuẩn điểm đạt (passingScore)
const currentExam = computed(() => {
  if (currentAttempt.value?.examId && home.exams) {
    return home.exams.find((e) => String(e.examId) === String(currentAttempt.value.examId))
  }
  return null
})

// Chuẩn điểm đạt từ đề thi nếu có
const passingScore = computed(() => {
  if (currentExam.value?.passingScore != null && currentExam.value.passingScore > 0) {
    return Number(currentExam.value.passingScore)
  }
  return null
})

// Đánh giá trạng thái Đạt / Chưa đạt theo quy tắc backend thực tế
const isPassed = computed(() => {
  if (home.result?.score == null) return null
  if (passingScore.value != null) {
    return Number(home.result.score) >= passingScore.value
  }
  if (home.result.maxScore && home.result.maxScore > 0) {
    return (Number(home.result.score) / Number(home.result.maxScore)) >= 0.5
  }
  return null
})

// Tỷ lệ % số câu trả lời đúng
const accuracyPercent = computed(() => {
  const total = home.result?.totalQuestions ||
    ((home.result?.correctCount || 0) + (home.result?.wrongCount || 0) + (home.result?.unansweredCount || 0))
  if (!total) return 0
  return Math.round(((home.result?.correctCount || 0) / total) * 100)
})

// Định dạng thời gian hoàn thành (giây -> phút:giây)
function formatTimeTaken(seconds) {
  if (!seconds || seconds <= 0) return 'Dưới 1 phút'
  const mins = Math.floor(seconds / 60)
  const secs = Math.round(seconds % 60)
  if (mins === 0) return `${secs} giây`
  if (secs === 0) return `${mins} phút`
  return `${mins} phút ${secs} giây`
}

// Phân tích điểm theo từng kỹ năng nếu backend trả về scoreBreakdownJson
const breakdownList = computed(() => {
  if (!home.result?.scoreBreakdownJson) return []
  try {
    const raw = typeof home.result.scoreBreakdownJson === 'string'
      ? JSON.parse(home.result.scoreBreakdownJson)
      : home.result.scoreBreakdownJson
    if (Array.isArray(raw)) return raw
    if (typeof raw === 'object' && raw !== null) {
      return Object.entries(raw).map(([key, val]) => ({
        skill: key,
        score: typeof val === 'object' ? (val.score ?? val.points ?? 0) : Number(val || 0),
        maxScore: typeof val === 'object' ? (val.maxScore ?? val.total ?? null) : null,
      }))
    }
  } catch {
    return []
  }
  return []
})
</script>

<template>
  <div class="result-page-container" aria-label="Kết quả bài kiểm tra">
    <!-- TRẠNG THÁI CÓ KẾT QUẢ TỪ BACKEND -->
    <main v-if="home.result" class="result-card-wrapper">
      <!-- 1. PHẦN ĐẦU KẾT QUẢ & HUY HIỆU ĐÁNH GIÁ (HERO HEADER) -->
      <section class="result-hero-section">
        <div
          class="result-hero-icon"
          :class="{
            'hero-success': isPassed === true,
            'hero-warning': isPassed === false,
            'hero-neutral': isPassed === null,
          }"
        >
          <Trophy v-if="isPassed === true" :size="36" />
          <Award v-else-if="isPassed === false" :size="36" />
          <CheckCircle2 v-else :size="36" />
        </div>

        <div class="result-badge-row">
          <Badge
            v-if="isPassed === true"
            variant="success"
            size="md"
            class="status-pill-badge"
          >
            <Check :size="13" /> ĐẠT CHUẨN ĐẦU RA
          </Badge>
          <Badge
            v-else-if="isPassed === false"
            variant="warning"
            size="md"
            class="status-pill-badge"
          >
            <AlertCircle :size="13" /> CHƯA ĐẠT CHUẨN
          </Badge>
          <Badge v-else variant="ai" size="md" class="status-pill-badge">
            <Sparkles :size="13" class="text-ai-accent" /> HOÀN THÀNH BÀI THI
          </Badge>

          <span v-if="home.result.bandScore != null" class="band-score-tag">
            Band {{ home.result.bandScore }}
          </span>
        </div>

        <h1 class="result-title">
          {{ isPassed === true ? 'Chúc mừng bạn đã hoàn thành xuất sắc bài thi!' : 'Bạn đã nộp bài thi thành công!' }}
        </h1>

        <p class="result-subtitle">
          {{ currentExam?.title || 'Bài đánh giá năng lực' }} • Kết quả đã được tự động lưu vào hồ sơ học tập và phân tích mô hình thích ứng.
        </p>

        <!-- KHỐI ĐIỂM SỐ CHÍNH (PRIMARY SCORE HERO) -->
        <div class="score-display-card">
          <div class="score-main-value">
            <span class="score-number">{{ home.result.score ?? 0 }}</span>
            <span v-if="home.result.maxScore != null" class="score-maximum">/ {{ home.result.maxScore }}</span>
          </div>
          <span class="score-caption">Tổng điểm đạt được</span>

          <div v-if="passingScore != null" class="score-passing-hint">
            <span>Chuẩn đầu ra yêu cầu: <strong>{{ passingScore }} điểm</strong></span>
          </div>
        </div>
      </section>

      <!-- 2. LƯỚI 4 CHỈ SỐ CHI TIẾT (4 METRICS QUAD) -->
      <section class="metrics-summary-grid" aria-label="Thống kê câu trả lời và thời gian">
        <!-- Số câu đúng -->
        <div class="metric-summary-card">
          <div class="metric-card-top">
            <span class="metric-icon-box bg-emerald-50 text-emerald-600 border border-emerald-200/60">
              <CheckCircle2 :size="18" />
            </span>
            <span class="metric-label">Số câu đúng</span>
          </div>
          <strong class="metric-value text-emerald-700">
            {{ home.result.correctCount ?? 0 }}
          </strong>
          <small class="metric-subtext">Độ chính xác: {{ accuracyPercent }}%</small>
        </div>

        <!-- Số câu sai -->
        <div class="metric-summary-card">
          <div class="metric-card-top">
            <span class="metric-icon-box bg-rose-50 text-rose-600 border border-rose-200/60">
              <XCircle :size="18" />
            </span>
            <span class="metric-label">Số câu sai</span>
          </div>
          <strong class="metric-value text-rose-700">
            {{ home.result.wrongCount ?? 0 }}
          </strong>
          <small class="metric-subtext">Cần củng cố kiến thức</small>
        </div>

        <!-- Số câu bỏ trống -->
        <div class="metric-summary-card">
          <div class="metric-card-top">
            <span class="metric-icon-box bg-slate-100 text-slate-600 border border-slate-200">
              <HelpCircle :size="18" />
            </span>
            <span class="metric-label">Chưa làm / Bỏ trống</span>
          </div>
          <strong class="metric-value text-slate-700">
            {{ home.result.unansweredCount ?? 0 }}
          </strong>
          <small class="metric-subtext">Tổng số: {{ home.result.totalQuestions || 0 }} câu hỏi</small>
        </div>

        <!-- Thời gian hoàn thành -->
        <div class="metric-summary-card">
          <div class="metric-card-top">
            <span class="metric-icon-box bg-sky-50 text-sky-600 border border-sky-200/60">
              <Clock3 :size="18" />
            </span>
            <span class="metric-label">Thời gian làm bài</span>
          </div>
          <strong class="metric-value text-sky-800">
            {{ formatTimeTaken(home.result.timeTakenSeconds) }}
          </strong>
          <small class="metric-subtext">Ghi nhận chính xác</small>
        </div>
      </section>

      <!-- 3. CẢNH BÁO VI PHẠM (NẾU CÓ DỮ LIỆU TỪ BACKEND) -->
      <div v-if="home.result.violationCount > 0" class="violation-notice-box" role="alert">
        <AlertCircle :size="18" class="text-amber-600 flex-shrink-0" />
        <div>
          <strong>Lưu ý về quy chế thi:</strong>
          <span> Hệ thống ghi nhận {{ home.result.violationCount }} lần chuyển cửa sổ hoặc cảnh báo trong quá trình thi.</span>
        </div>
      </div>

      <!-- 4. PHÂN TÍCH THEO CHUYÊN ĐỀ NẾU BACKEND CUNG CẤP (SCORE BREAKDOWN) -->
      <section v-if="breakdownList.length" class="breakdown-section" aria-labelledby="breakdown-title">
        <h2 id="breakdown-title" class="breakdown-title">Phân tích điểm số theo chuyên đề</h2>
        <div class="breakdown-items-list">
          <div v-for="item in breakdownList" :key="item.skill" class="breakdown-item-row">
            <div class="breakdown-item-header">
              <span class="breakdown-skill-name">{{ item.skill }}</span>
              <strong class="breakdown-score">
                {{ item.score }} <span v-if="item.maxScore">/ {{ item.maxScore }}</span>
              </strong>
            </div>
            <div class="breakdown-track">
              <div
                class="breakdown-fill"
                :style="{
                  width: `${item.maxScore ? Math.min(100, Math.round((item.score / item.maxScore) * 100)) : 100}%`,
                }"
              ></div>
            </div>
          </div>
        </div>
      </section>

      <!-- 5. HÀNG NÚT ĐIỀU HƯỚNG VÀ HÀNH ĐỘNG TIẾP THEO (ACTION BUTTONS) -->
      <footer class="result-actions-footer">
        <Button
          variant="default"
          size="md"
          class="action-btn"
          @click="home.showView('dashboard')"
        >
          Về trang chủ <ArrowRight :size="15" />
        </Button>

        <Button
          variant="outline"
          size="md"
          class="action-btn"
          @click="home.showView('tests')"
        >
          <RotateCcw :size="15" /> Làm bài thi khác
        </Button>

        <Button
          variant="ghost"
          size="md"
          class="action-btn"
          @click="home.showView('history')"
        >
          <BookOpen :size="15" /> Lịch sử làm bài
        </Button>

        <Button
          variant="ghost"
          size="md"
          class="action-btn"
          @click="home.showView('progress')"
        >
          <BarChart3 :size="15" /> Phân tích BKT
        </Button>
      </footer>
    </main>

    <!-- TRẠNG THÁI RỖNG / EMPTY STATE KHI CHƯA CÓ KẾT QUẢ -->
    <div v-else class="result-empty-box">
      <div class="empty-icon-circle bg-blue-50 text-brand-primary">
        <Award :size="32" />
      </div>
      <h2 class="empty-title">Chưa có kết quả bài thi nào</h2>
      <p class="empty-desc">
        Bạn chưa hoàn thành bài thi nào trong phiên làm việc này. Hãy chọn một đề thi để bắt đầu làm bài.
      </p>
      <Button variant="default" size="md" @click="home.showView('tests')">
        Xem kho bài kiểm tra <ArrowRight :size="15" />
      </Button>
    </div>
  </div>
</template>

<style scoped>
.result-page-container {
  display: flex;
  justify-content: center;
  align-items: flex-start;
  width: 100%;
  padding: 10px 0 40px;
}

.result-card-wrapper {
  display: flex;
  flex-direction: column;
  gap: 24px;
  width: 100%;
  max-width: 820px;
  background-color: #ffffff;
  border: 1px solid var(--border-subtle, #E2E8F0);
  border-radius: var(--radius-lg, 16px);
  padding: 36px 36px;
  box-shadow: 0 4px 20px rgba(15, 23, 42, 0.04);
}

/* ==========================================================================
   HERO HEADER
   ========================================================================== */
.result-hero-section {
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
}

.result-hero-icon {
  display: grid;
  place-items: center;
  width: 72px;
  height: 72px;
  border-radius: 50%;
  margin-bottom: 16px;
  box-shadow: 0 8px 24px rgba(15, 23, 42, 0.06);
}

.result-hero-icon.hero-success {
  background: linear-gradient(135deg, #ECFDF5 0%, #D1FAE5 100%);
  border: 2px solid #A7F3D0;
  color: #059669;
}

.result-hero-icon.hero-warning {
  background: linear-gradient(135deg, #FFFBEB 0%, #FEF3C7 100%);
  border: 2px solid #FDE68A;
  color: #D97706;
}

.result-hero-icon.hero-neutral {
  background: linear-gradient(135deg, #EFF6FF 0%, #DBEAFE 100%);
  border: 2px solid #BFDBFE;
  color: var(--brand-primary, #2563EB);
}

.result-badge-row {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 12px;
}

.status-pill-badge {
  padding: 4px 12px;
  font-weight: 700;
  letter-spacing: 0.3px;
}

.band-score-tag {
  display: inline-flex;
  padding: 3px 10px;
  border-radius: 999px;
  background-color: #F5F3FF;
  border: 1px solid #DDD6FE;
  color: #7C3AED;
  font-size: 12px;
  font-weight: 700;
}

.result-title {
  margin: 0 0 8px;
  font-size: clamp(20px, 2.4vw, 26px);
  font-weight: 800;
  color: var(--text-primary, #0F172A);
  letter-spacing: -0.4px;
  line-height: 1.3;
}

.result-subtitle {
  margin: 0 0 24px;
  font-size: 13.5px;
  color: var(--text-muted, #64748B);
  max-width: 600px;
  line-height: 1.55;
}

/* Primary Score Hero Card */
.score-display-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 20px 32px;
  border-radius: var(--radius-md, 12px);
  background: linear-gradient(135deg, #F8FAFC 0%, #F1F5F9 100%);
  border: 1px solid #E2E8F0;
  min-width: 240px;
}

.score-main-value {
  display: flex;
  align-items: baseline;
  gap: 4px;
  line-height: 1;
}

.score-number {
  font-size: clamp(40px, 4.5vw, 52px);
  font-weight: 900;
  color: var(--brand-primary, #2563EB);
  letter-spacing: -1px;
}

.score-maximum {
  font-size: 20px;
  font-weight: 700;
  color: var(--text-muted, #64748B);
}

.score-caption {
  margin-top: 6px;
  font-size: 12px;
  font-weight: 600;
  color: var(--text-secondary, #475569);
  text-transform: uppercase;
  letter-spacing: 0.6px;
}

.score-passing-hint {
  margin-top: 8px;
  padding-top: 8px;
  border-top: 1px solid #E2E8F0;
  font-size: 12px;
  color: var(--text-muted, #64748B);
}

.score-passing-hint strong {
  color: var(--text-primary, #0F172A);
}

/* ==========================================================================
   4 METRICS QUAD
   ========================================================================== */
.metrics-summary-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
}

.metric-summary-card {
  display: flex;
  flex-direction: column;
  padding: 16px;
  border-radius: var(--radius-md, 10px);
  background-color: #F8FAFC;
  border: 1px solid var(--border-subtle, #E2E8F0);
  transition: transform 0.15s ease, background-color 0.15s ease;
}

.metric-summary-card:hover {
  background-color: #ffffff;
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(15, 23, 42, 0.04);
}

.metric-card-top {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 10px;
}

.metric-icon-box {
  display: grid;
  place-items: center;
  width: 30px;
  height: 30px;
  border-radius: 6px;
  flex-shrink: 0;
}

.metric-label {
  font-size: 12px;
  font-weight: 600;
  color: var(--text-secondary, #475569);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.metric-value {
  font-size: 22px;
  font-weight: 800;
  line-height: 1.15;
}

.metric-subtext {
  margin-top: 4px;
  font-size: 11px;
  color: var(--text-muted, #64748B);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

/* ==========================================================================
   VIOLATION NOTICE
   ========================================================================== */
.violation-notice-box {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  border-radius: var(--radius-md, 8px);
  background-color: #FFFBEB;
  border: 1px solid #FDE68A;
  color: #92400E;
  font-size: 13px;
}

/* ==========================================================================
   SCORE BREAKDOWN (CHUYÊN ĐỀ)
   ========================================================================== */
.breakdown-section {
  padding: 18px 20px;
  border-radius: var(--radius-md, 10px);
  background-color: #F8FAFC;
  border: 1px solid var(--border-subtle, #E2E8F0);
}

.breakdown-title {
  margin: 0 0 14px;
  font-size: 14.5px;
  font-weight: 750;
  color: var(--text-primary, #0F172A);
}

.breakdown-items-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.breakdown-item-row {
  display: flex;
  flex-direction: column;
  gap: 5px;
}

.breakdown-item-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  font-size: 12.5px;
}

.breakdown-skill-name {
  color: var(--text-primary, #0F172A);
  font-weight: 600;
}

.breakdown-score {
  color: var(--brand-primary, #2563EB);
  font-weight: 700;
}

.breakdown-track {
  width: 100%;
  height: 6px;
  border-radius: 3px;
  background-color: #E2E8F0;
  overflow: hidden;
}

.breakdown-fill {
  height: 100%;
  border-radius: 3px;
  background: linear-gradient(90deg, #2563EB 0%, #3B82F6 100%);
  transition: width 0.3s ease;
}

/* ==========================================================================
   ACTIONS FOOTER
   ========================================================================== */
.result-actions-footer {
  display: flex;
  align-items: center;
  justify-content: center;
  flex-wrap: wrap;
  gap: 12px;
  padding-top: 16px;
  border-top: 1px solid #F1F5F9;
}

.action-btn {
  min-width: 140px;
}

/* ==========================================================================
   EMPTY STATE (NO RESULT)
   ========================================================================== */
.result-empty-box {
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  padding: 48px 24px;
  max-width: 500px;
  background-color: #ffffff;
  border: 1px dashed var(--border-subtle, #E2E8F0);
  border-radius: var(--radius-lg, 16px);
}

.empty-icon-circle {
  display: grid;
  place-items: center;
  width: 60px;
  height: 60px;
  border-radius: 50%;
  margin-bottom: 16px;
}

.empty-title {
  margin: 0 0 8px;
  font-size: 18px;
  font-weight: 750;
  color: var(--text-primary, #0F172A);
}

.empty-desc {
  margin: 0 0 20px;
  font-size: 13.5px;
  line-height: 1.55;
  color: var(--text-muted, #64748B);
}

/* ==========================================================================
   RESPONSIVE DESIGN (TABLET & MOBILE)
   ========================================================================== */
@media (max-width: 768px) {
  .result-card-wrapper {
    padding: 24px 18px;
  }

  .metrics-summary-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .result-actions-footer {
    flex-direction: column;
    width: 100%;
  }

  .action-btn {
    width: 100%;
  }
}

@media (max-width: 480px) {
  .metrics-summary-grid {
    grid-template-columns: 1fr;
  }
}
</style>
