<script setup>
import { computed, inject, ref } from 'vue'
import {
  AlertCircle,
  ArrowLeft,
  Check,
  CheckCircle2,
  Clock3,
  HelpCircle,
  Loader2,
  Send,
  Sparkles,
} from 'lucide-vue-next'
import { homeContextKey } from '../../composables/homeContext'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'

const home = inject(homeContextKey)

// Đếm số câu hỏi đã có câu trả lời
const answeredCount = computed(() => {
  if (!home.attemptQuestions) return 0
  return home.attemptQuestions.filter((q) => {
    const val = home.answers[q.questionId]
    return val !== undefined && val !== null && String(val).trim() !== ''
  }).length
})

// Tỷ lệ % hoàn thành câu hỏi
const progressPercent = computed(() => {
  if (!home.attemptQuestions?.length) return 0
  return Math.round((answeredCount.value / home.attemptQuestions.length) * 100)
})

// Kiểm tra một câu hỏi đã được trả lời hay chưa
function isQuestionAnswered(questionId) {
  const val = home.answers[questionId]
  return val !== undefined && val !== null && String(val).trim() !== ''
}

// Cuộn mượt đến câu hỏi được chọn
function scrollToQuestion(index) {
  const el = document.getElementById(`question-panel-${index}`)
  if (el) {
    el.scrollIntoView({ behavior: 'smooth', block: 'center' })
  }
}
</script>

<template>
  <div class="exam-room-container" aria-label="Phòng thi trực tuyến">
    <!-- 1. THANH TIÊU ĐỀ PHÒNG THI DÍNH TRÊN CÙNG (STICKY TOPBAR) -->
    <header class="exam-topbar">
      <div class="topbar-left">
        <Button
          variant="outline"
          size="sm"
          class="leave-btn"
          :disabled="home.submitting"
          @click="home.leaveAttempt"
        >
          <ArrowLeft :size="15" /> Rời phòng thi
        </Button>

        <div class="exam-title-stack">
          <div class="exam-badge-row">
            <Badge variant="ai" size="sm">
              <Sparkles :size="12" class="text-ai-accent" /> BÀI THI ĐANG DIỄN RA
            </Badge>

            <!-- Trạng thái tự động lưu đáp án (Autosave indicator) -->
            <span
              class="autosave-pill"
              :class="{
                'saving': home.saveState?.includes('Đang lưu'),
                'saved': home.saveState?.includes('Đã lưu'),
                'warning': home.saveState?.includes('hết thời gian') || home.saveState?.includes('khi nộp bài'),
              }"
              aria-live="polite"
            >
              <Loader2 v-if="home.saveState?.includes('Đang lưu')" :size="13" class="animate-spin text-amber-600" />
              <CheckCircle2 v-else-if="home.saveState?.includes('Đã lưu')" :size="13" class="text-emerald-600" />
              <AlertCircle v-else :size="13" class="text-slate-400" />
              <span>{{ home.saveState || 'Tự động lưu khi chọn đáp án' }}</span>
            </span>
          </div>

          <h1 class="exam-heading-text">
            {{ home.exams.find((exam) => exam.examId === home.attempt?.examId)?.title || 'Bài kiểm tra năng lực' }}
          </h1>
        </div>
      </div>

      <div class="topbar-right">
        <!-- Đồng hồ đếm ngược (Urgent khi < 5 phút) -->
        <div
          class="countdown-clock"
          :class="{ 'urgent': home.secondsLeft < 300 }"
          role="timer"
          aria-label="Thời gian làm bài còn lại"
        >
          <span class="clock-pulse-dot" v-if="home.secondsLeft < 300"></span>
          <Clock3 :size="18" />
          <span class="clock-digits">{{ home.formattedTimeLeft }}</span>
        </div>

        <!-- Nút nộp bài trên header -->
        <Button
          variant="default"
          size="sm"
          class="submit-header-btn"
          :disabled="home.submitting || !home.attemptQuestions?.length"
          @click="home.submitExam"
        >
          <Loader2 v-if="home.submitting" :size="15" class="animate-spin" />
          <Send v-else :size="15" />
          <span>{{ home.submitting ? 'Đang nộp bài…' : 'Nộp bài thi' }}</span>
        </Button>
      </div>
    </header>

    <!-- 2. BỐ CỤC KHÔNG GIAN THI (WORKSPACE GRID) -->
    <div class="exam-workspace-grid">
      <!-- CỘT CHÍNH: DANH SÁCH CÂU HỎI (QUESTIONS STREAM) -->
      <main class="questions-stream">
        <article
          v-for="(question, index) in home.attemptQuestions"
          :id="`question-panel-${index}`"
          :key="question.questionId"
          class="question-card"
          :class="{ 'answered-card': isQuestionAnswered(question.questionId) }"
        >
          <!-- Đầu thẻ câu hỏi -->
          <div class="question-header">
            <div class="question-identity">
              <span class="question-number-badge">
                Câu {{ String(index + 1).padStart(2, '0') }}
              </span>
              <span v-if="question.sectionName" class="section-tag-pill">
                {{ question.sectionName }}
              </span>
              <span v-if="question.points != null" class="points-tag-pill">
                {{ question.points }} điểm
              </span>
            </div>

            <!-- Badge trạng thái đã làm / chưa làm -->
            <div class="question-status-badge">
              <span
                v-if="isQuestionAnswered(question.questionId)"
                class="status-indicator answered"
              >
                <Check :size="12" /> Đã trả lời
              </span>
              <span v-else class="status-indicator pending">
                Chưa trả lời
              </span>
            </div>
          </div>

          <!-- Nội dung câu hỏi (Tương phản cao, dễ đọc) -->
          <div class="question-body">
            <p class="question-text">{{ question.content }}</p>
          </div>

          <!-- Khu vực lựa chọn đáp án trắc nghiệm -->
          <div
            v-if="home.parseOptions(question).length"
            class="options-container"
            role="radiogroup"
            :aria-label="`Đáp án cho câu hỏi ${index + 1}`"
          >
            <label
              v-for="(option, optIdx) in home.parseOptions(question)"
              :key="option.value"
              class="option-choice-card"
              :class="{
                'selected': String(home.answers[question.questionId]) === String(option.value),
              }"
            >
              <input
                v-model="home.answers[question.questionId]"
                type="radio"
                class="sr-only"
                :name="`question-${question.questionId}`"
                :value="option.value"
                @change="home.saveAnswer(question)"
              />
              <span class="option-letter-badge">
                {{ String.fromCharCode(65 + optIdx) }}
              </span>
              <span class="option-text-label">{{ option.label }}</span>
            </label>
          </div>

          <!-- Khu vực nhập câu trả lời tự luận (nếu không có optionsJson) -->
          <div v-else class="essay-answer-box">
            <label :for="`essay-input-${question.questionId}`" class="essay-label">
              Nhập nội dung câu trả lời của bạn:
            </label>
            <textarea
              :id="`essay-input-${question.questionId}`"
              v-model="home.answers[question.questionId]"
              class="essay-textarea"
              rows="4"
              placeholder="Nhập câu trả lời chi tiết tại đây..."
              @input="home.saveAnswer(question)"
            ></textarea>
            <span class="essay-hint">Đáp án được tự động lưu sau khi dừng gõ 350ms.</span>
          </div>
        </article>

        <!-- Thanh nộp bài dưới chân trang câu hỏi -->
        <div class="exam-bottom-submit-card">
          <div class="bottom-progress-info">
            <strong>Bạn đã hoàn thành {{ answeredCount }} / {{ home.attemptQuestions?.length || 0 }} câu hỏi</strong>
            <p v-if="answeredCount < (home.attemptQuestions?.length || 0)">
              Còn lại {{ (home.attemptQuestions?.length || 0) - answeredCount }} câu chưa có đáp án. Bạn có thể kiểm tra lại trước khi nộp.
            </p>
            <p v-else class="text-emerald-700">
              Tuyệt vời! Bạn đã trả lời toàn bộ các câu hỏi trong đề thi.
            </p>
          </div>

          <div class="bottom-action-buttons">
            <Button
              variant="outline"
              size="md"
              :disabled="home.submitting"
              @click="home.leaveAttempt"
            >
              Rời bài thi
            </Button>
            <Button
              variant="default"
              size="md"
              :disabled="home.submitting || !home.attemptQuestions?.length"
              @click="home.submitExam"
            >
              <Loader2 v-if="home.submitting" :size="16" class="animate-spin" />
              <Send v-else :size="16" />
              {{ home.submitting ? 'Đang nộp bài…' : 'Nộp bài thi ngay' }}
            </Button>
          </div>
        </div>
      </main>

      <!-- CỘT PHẢI: BẢNG ĐIỀU HƯỚNG CÂU HỎI (QUESTION NAVIGATOR ASIDE) -->
      <aside class="navigator-aside">
        <div class="navigator-card">
          <div class="nav-card-header">
            <h2 class="nav-title">Mục lục câu hỏi</h2>
            <span class="nav-ratio-badge">
              <strong>{{ answeredCount }}</strong>/{{ home.attemptQuestions?.length || 0 }}
            </span>
          </div>

          <!-- Thanh tiến độ hoàn thành câu hỏi -->
          <div class="nav-progress-wrap">
            <div class="nav-progress-bar">
              <div
                class="nav-progress-fill"
                :style="{ width: `${progressPercent}%` }"
              ></div>
            </div>
            <div class="nav-progress-labels">
              <span>Đã làm {{ progressPercent }}%</span>
              <span v-if="answeredCount < (home.attemptQuestions?.length || 0)">
                Còn {{ (home.attemptQuestions?.length || 0) - answeredCount }} câu
              </span>
            </div>
          </div>

          <!-- Lưới các ô số thứ tự câu hỏi -->
          <div
            class="questions-matrix-grid"
            role="navigation"
            aria-label="Chuyển nhanh đến câu hỏi"
          >
            <button
              v-for="(question, index) in home.attemptQuestions"
              :key="question.questionId"
              type="button"
              class="matrix-cell-btn"
              :class="{
                'answered': isQuestionAnswered(question.questionId),
                'pending': !isQuestionAnswered(question.questionId),
              }"
              :aria-label="`Chuyển đến câu ${index + 1}, ${isQuestionAnswered(question.questionId) ? 'đã trả lời' : 'chưa trả lời'}`"
              @click="scrollToQuestion(index)"
            >
              {{ index + 1 }}
            </button>
          </div>

          <!-- Chú thích trạng thái -->
          <div class="nav-legend-row">
            <span class="legend-item">
              <span class="legend-dot bg-blue-600"></span> Đã trả lời
            </span>
            <span class="legend-item">
              <span class="legend-dot bg-slate-200"></span> Chưa trả lời
            </span>
          </div>

          <!-- Nút nộp bài trực tiếp trên bảng điều hướng -->
          <div class="nav-card-submit">
            <Button
              variant="default"
              size="md"
              class="w-full shadow-sm"
              :disabled="home.submitting || !home.attemptQuestions?.length"
              @click="home.submitExam"
            >
              <Loader2 v-if="home.submitting" :size="16" class="animate-spin" />
              <Send v-else :size="16" />
              {{ home.submitting ? 'Đang nộp bài…' : 'Nộp bài thi' }}
            </Button>
          </div>
        </div>
      </aside>
    </div>
  </div>
</template>

<style scoped>
.exam-room-container {
  display: flex;
  flex-direction: column;
  gap: 20px;
  width: 100%;
  max-width: 1280px;
  margin: 0 auto;
}

/* ==========================================================================
   STICKY TOPBAR
   ========================================================================== */
.exam-topbar {
  position: sticky;
  top: 64px;
  z-index: 20;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 14px 20px;
  border-radius: var(--radius-lg, 12px);
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(8px);
  border: 1px solid var(--border-subtle, #E2E8F0);
  box-shadow: 0 4px 16px rgba(15, 23, 42, 0.04);
}

.topbar-left {
  display: flex;
  align-items: center;
  gap: 16px;
  min-width: 0;
  flex: 1;
}

.leave-btn {
  white-space: nowrap;
}

.exam-title-stack {
  display: flex;
  flex-direction: column;
  gap: 3px;
  min-width: 0;
}

.exam-badge-row {
  display: flex;
  align-items: center;
  gap: 10px;
}

.autosave-pill {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  font-size: 11.5px;
  color: var(--text-muted, #64748B);
  transition: color 0.15s ease;
}

.autosave-pill.saved {
  color: #059669;
}

.autosave-pill.saving {
  color: #D97706;
}

.autosave-pill.warning {
  color: #E11D48;
}

.exam-heading-text {
  margin: 0;
  font-size: 16px;
  font-weight: 750;
  color: var(--text-primary, #0F172A);
  letter-spacing: -0.2px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.topbar-right {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-shrink: 0;
}

.countdown-clock {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 14px;
  border-radius: var(--radius-md, 8px);
  background-color: #F8FAFC;
  border: 1px solid #E2E8F0;
  color: var(--text-primary, #0F172A);
  font-weight: 750;
  font-size: 16px;
  letter-spacing: 0.5px;
  font-variant-numeric: tabular-nums;
  box-shadow: 0 1px 2px rgba(15, 23, 42, 0.03);
}

.countdown-clock.urgent {
  background-color: #FFF1F2;
  border-color: #FECDD3;
  color: #E11D48;
  animation: urgent-pulse 1.2s infinite ease-in-out;
}

@keyframes urgent-pulse {
  0%, 100% {
    box-shadow: 0 0 0 0 rgba(225, 29, 72, 0.4);
  }
  50% {
    box-shadow: 0 0 0 4px rgba(225, 29, 72, 0);
  }
}

.clock-pulse-dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background-color: #E11D48;
}

.clock-digits {
  line-height: 1;
}

.submit-header-btn {
  white-space: nowrap;
}

/* ==========================================================================
   WORKSPACE GRID (2 COLUMNS)
   ========================================================================== */
.exam-workspace-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 290px;
  align-items: start;
  gap: 24px;
  width: 100%;
}

.questions-stream {
  display: flex;
  flex-direction: column;
  gap: 20px;
  min-width: 0;
}

/* ==========================================================================
   QUESTION CARD
   ========================================================================== */
.question-card {
  padding: 22px 24px;
  border-radius: var(--radius-lg, 12px);
  background-color: #ffffff;
  border: 1px solid var(--border-subtle, #E2E8F0);
  box-shadow: 0 1px 3px rgba(15, 23, 42, 0.02);
  transition: border-color 0.15s ease, box-shadow 0.15s ease;
}

.question-card:hover {
  border-color: #CBD5E1;
}

.question-card.answered-card {
  border-left: 4px solid var(--brand-primary, #2563EB);
}

.question-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 14px;
  padding-bottom: 12px;
  border-bottom: 1px solid #F1F5F9;
}

.question-identity {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.question-number-badge {
  display: inline-flex;
  align-items: center;
  padding: 3px 8px;
  border-radius: 6px;
  background-color: #0F172A;
  color: #ffffff;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.2px;
}

.section-tag-pill {
  display: inline-flex;
  padding: 3px 8px;
  border-radius: 6px;
  background-color: #F1F5F9;
  border: 1px solid #E2E8F0;
  color: #475569;
  font-size: 11.5px;
  font-weight: 500;
}

.points-tag-pill {
  font-size: 12px;
  color: var(--text-muted, #64748B);
  font-weight: 600;
}

.question-status-badge {
  flex-shrink: 0;
}

.status-indicator {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 2px 8px;
  border-radius: 999px;
  font-size: 11px;
  font-weight: 600;
}

.status-indicator.answered {
  background-color: #ECFDF5;
  color: #065F46;
  border: 1px solid #A7F3D0;
}

.status-indicator.pending {
  background-color: #F8FAFC;
  color: #64748B;
  border: 1px solid #E2E8F0;
}

.question-body {
  margin-bottom: 20px;
}

.question-text {
  margin: 0;
  font-size: 15px;
  line-height: 1.65;
  color: var(--text-primary, #0F172A);
  white-space: pre-line;
}

/* ==========================================================================
   ANSWER OPTIONS
   ========================================================================== */
.options-container {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.option-choice-card {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 13px 16px;
  border-radius: var(--radius-md, 8px);
  background-color: #F8FAFC;
  border: 1.5px solid var(--border-subtle, #E2E8F0);
  cursor: pointer;
  transition: all 0.15s ease;
  user-select: none;
}

.option-choice-card:hover {
  background-color: #ffffff;
  border-color: #94A3B8;
  box-shadow: 0 2px 6px rgba(15, 23, 42, 0.04);
}

.option-choice-card.selected {
  background-color: #EFF6FF;
  border-color: var(--brand-primary, #2563EB);
  box-shadow: 0 0 0 1px var(--brand-primary, #2563EB);
}

.option-letter-badge {
  display: grid;
  place-items: center;
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background-color: #ffffff;
  border: 1px solid #CBD5E1;
  color: #475569;
  font-size: 13px;
  font-weight: 700;
  flex-shrink: 0;
  transition: all 0.15s ease;
}

.option-choice-card.selected .option-letter-badge {
  background-color: var(--brand-primary, #2563EB);
  border-color: var(--brand-primary, #2563EB);
  color: #ffffff;
}

.option-text-label {
  font-size: 14px;
  line-height: 1.45;
  color: var(--text-primary, #0F172A);
  flex: 1;
}

.option-choice-card.selected .option-text-label {
  font-weight: 600;
  color: var(--brand-primary, #2563EB);
}

/* Essay Answer Box */
.essay-answer-box {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.essay-label {
  font-size: 13px;
  font-weight: 600;
  color: var(--text-secondary, #475569);
}

.essay-textarea {
  width: 100%;
  padding: 12px 14px;
  border-radius: var(--radius-md, 8px);
  border: 1.5px solid var(--border-subtle, #E2E8F0);
  background-color: #ffffff;
  color: var(--text-primary, #0F172A);
  font: inherit;
  font-size: 14px;
  line-height: 1.5;
  resize: vertical;
  outline: none;
  transition: border-color 0.15s ease, box-shadow 0.15s ease;
}

.essay-textarea:focus {
  border-color: var(--brand-primary, #2563EB);
  box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.12);
}

.essay-hint {
  font-size: 11.5px;
  color: var(--text-muted, #64748B);
}

/* ==========================================================================
   BOTTOM SUBMIT CARD
   ========================================================================== */
.exam-bottom-submit-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  padding: 20px 24px;
  border-radius: var(--radius-lg, 12px);
  background-color: #ffffff;
  border: 1px solid var(--border-subtle, #E2E8F0);
  box-shadow: 0 2px 8px rgba(15, 23, 42, 0.03);
}

.bottom-progress-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.bottom-progress-info strong {
  font-size: 14.5px;
  color: var(--text-primary, #0F172A);
}

.bottom-progress-info p {
  margin: 0;
  font-size: 12.5px;
  color: var(--text-secondary, #475569);
}

.bottom-action-buttons {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-shrink: 0;
}

/* ==========================================================================
   QUESTION NAVIGATOR ASIDE
   ========================================================================== */
.navigator-aside {
  position: sticky;
  top: 148px;
}

.navigator-card {
  padding: 20px;
  border-radius: var(--radius-lg, 12px);
  background-color: #ffffff;
  border: 1px solid var(--border-subtle, #E2E8F0);
  box-shadow: 0 2px 8px rgba(15, 23, 42, 0.03);
}

.nav-card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  margin-bottom: 12px;
}

.nav-title {
  margin: 0;
  font-size: 15px;
  font-weight: 750;
  color: var(--text-primary, #0F172A);
}

.nav-ratio-badge {
  font-size: 12.5px;
  color: var(--text-secondary, #475569);
}

.nav-ratio-badge strong {
  color: var(--brand-primary, #2563EB);
  font-size: 14px;
}

.nav-progress-wrap {
  display: flex;
  flex-direction: column;
  gap: 6px;
  margin-bottom: 16px;
}

.nav-progress-bar {
  width: 100%;
  height: 6px;
  border-radius: 3px;
  background-color: #E2E8F0;
  overflow: hidden;
}

.nav-progress-fill {
  height: 100%;
  border-radius: 3px;
  background: linear-gradient(90deg, #2563EB 0%, #3B82F6 100%);
  transition: width 0.3s ease;
}

.nav-progress-labels {
  display: flex;
  justify-content: space-between;
  font-size: 11.5px;
  color: var(--text-muted, #64748B);
}

.questions-matrix-grid {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 8px;
  margin-bottom: 16px;
  max-height: 280px;
  overflow-y: auto;
  padding-right: 2px;
}

.matrix-cell-btn {
  display: grid;
  place-items: center;
  height: 38px;
  border-radius: var(--radius-sm, 6px);
  font: inherit;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  border: 1px solid;
  transition: transform 0.15s ease, background-color 0.15s ease;
}

.matrix-cell-btn:hover {
  transform: scale(1.05);
}

.matrix-cell-btn.answered {
  background-color: var(--brand-primary, #2563EB);
  border-color: var(--brand-primary, #2563EB);
  color: #ffffff;
}

.matrix-cell-btn.pending {
  background-color: #F8FAFC;
  border-color: #E2E8F0;
  color: #475569;
}

.matrix-cell-btn.pending:hover {
  background-color: #F1F5F9;
  border-color: #CBD5E1;
}

.nav-legend-row {
  display: flex;
  align-items: center;
  justify-content: space-around;
  padding-top: 12px;
  border-top: 1px solid #F1F5F9;
  margin-bottom: 16px;
}

.legend-item {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 11.5px;
  color: var(--text-secondary, #475569);
}

.legend-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
}

.nav-card-submit {
  width: 100%;
}

/* ==========================================================================
   RESPONSIVE DESIGN (TABLET & MOBILE)
   ========================================================================== */
@media (max-width: 1024px) {
  .exam-topbar {
    top: 56px;
  }

  .exam-workspace-grid {
    grid-template-columns: 1fr;
  }

  .navigator-aside {
    position: static;
    order: -1;
  }

  .questions-matrix-grid {
    grid-template-columns: repeat(8, 1fr);
    max-height: 160px;
  }

  .nav-card-submit {
    display: none;
  }
}

@media (max-width: 640px) {
  .exam-topbar {
    flex-direction: column;
    align-items: flex-start;
    padding: 12px 14px;
    gap: 10px;
  }

  .topbar-left {
    width: 100%;
    justify-content: space-between;
  }

  .topbar-right {
    width: 100%;
    justify-content: space-between;
  }

  .question-card {
    padding: 16px 14px;
  }

  .question-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 8px;
  }

  .questions-matrix-grid {
    grid-template-columns: repeat(5, 1fr);
  }

  .exam-bottom-submit-card {
    flex-direction: column;
    align-items: flex-start;
    padding: 16px;
  }

  .bottom-action-buttons {
    width: 100%;
    justify-content: space-between;
  }

  .bottom-action-buttons button {
    flex: 1;
  }
}
</style>
