<script setup>
import { computed, inject, ref } from 'vue'
import { useRouter } from 'vue-router'
import {
  AlertCircle,
  ArrowRight,
  Award,
  BookOpen,
  CalendarDays,
  Check,
  CheckCircle2,
  Clock,
  Clock3,
  Crown,
  FileQuestion,
  FileText,
  HelpCircle,
  History,
  Layers,
  Loader2,
  Plus,
  RotateCcw,
  Search,
  Sparkles,
  Trophy,
  X,
  Zap,
} from 'lucide-vue-next'
import { homeContextKey } from '../../composables/homeContext'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'

const router = useRouter()
const home = inject(homeContextKey)

const activeFilter = ref('all') // 'all' | 'in_progress' | 'templates' | 'exams' | 'history'

// Điểm cao nhất từng đạt được cho mỗi bài thi (dựa trên dữ liệu backend thật)
const bestScoresMap = computed(() => {
  const map = {}
  if (!home.history) return map
  for (const item of home.history) {
    if (item.score != null) {
      const current = map[item.examId]
      map[item.examId] = current != null ? Math.max(current, Number(item.score)) : Number(item.score)
    }
  }
  return map
})

// Danh sách ID các bài thi người học đã hoàn thành
const completedExamIds = computed(() => {
  const set = new Set()
  if (!home.history) return set
  for (const item of home.history) {
    if (['GRADED', 'SUBMITTED', 'VIOLATED'].includes(item.status)) {
      set.add(item.examId)
    }
  }
  return set
})

function getTemplateQuestionCount(template) {
  if (!template.sections || !template.sections.length) return 0
  return template.sections.reduce((sum, s) => sum + Number(s.questionCount || 0), 0)
}

function getAttemptBadgeVariant(status) {
  if (status === 'GRADED') return 'success'
  if (status === 'IN_PROGRESS') return 'warning'
  if (status === 'VIOLATED') return 'destructive'
  if (status === 'SUBMITTED') return 'default'
  return 'secondary'
}

function clearSearch() {
  home.searchQuery = ''
}
</script>

<template>
  <section class="tests-page-view" aria-label="Kho bài kiểm tra và luyện thi">
    <!-- 1. TIÊU ĐỀ TRANG VÀ THANH TÌM KIẾM (PAGE HEADER & SEARCH) -->
    <header class="tests-header-card">
      <div class="header-main-info">
        <Badge variant="ai" class="header-badge">
          <Sparkles :size="13" class="text-ai-accent" />
          LUYỆN TẬP VÀ ĐÁNH GIÁ NĂNG LỰC
        </Badge>
        <h1 class="header-title">Kho bài kiểm tra & Đề thi</h1>
        <p class="header-desc">
          Thực hiện bài kiểm tra chuẩn hóa, tạo đề luyện tập thích ứng theo cấu trúc cá nhân hoặc tiếp tục bài thi đang dở dang.
        </p>
      </div>

      <!-- Ô tìm kiếm bài kiểm tra -->
      <div class="search-bar-wrap">
        <div class="search-input-box">
          <Search :size="17" class="search-icon" />
          <input
            v-model="home.searchQuery"
            type="search"
            class="search-input"
            placeholder="Tìm kiếm bài kiểm tra theo tên hoặc mô tả..."
            aria-label="Tìm kiếm bài kiểm tra"
          />
          <button
            v-if="home.searchQuery"
            type="button"
            class="search-clear-btn"
            aria-label="Xóa tìm kiếm"
            @click="clearSearch"
          >
            <X :size="15" />
          </button>
        </div>
      </div>
    </header>

    <!-- 2. THÔNG BÁO TẠO ĐỀ / LỖI NẾU CÓ (NOTICES & ALERTS) -->
    <div v-if="home.practiceExamNotice" class="alert-banner alert-success" role="status">
      <CheckCircle2 :size="18" class="alert-icon text-emerald-600" />
      <span class="alert-text">{{ home.practiceExamNotice }}</span>
      <button
        type="button"
        class="alert-dismiss"
        aria-label="Đóng thông báo"
        @click="home.practiceExamNotice = ''"
      >
        <X :size="16" />
      </button>
    </div>

    <div v-if="home.pageError" class="alert-banner alert-error" role="alert">
      <AlertCircle :size="18" class="alert-icon text-rose-600" />
      <span class="alert-text">{{ home.pageError }}</span>
      <button
        type="button"
        class="alert-dismiss"
        aria-label="Đóng thông báo lỗi"
        @click="home.pageError = ''"
      >
        <X :size="16" />
      </button>
    </div>

    <!-- 3. BANNER HẠN MỨC GÓI MIỄN PHÍ (FREE QUOTA BANNER) -->
    <div
      v-if="home.entitlements && home.entitlements.plan === 'FREE'"
      class="free-quota-card"
      aria-label="Hạn mức tạo đề miễn phí"
    >
      <div class="quota-left">
        <span class="quota-icon-wrap">
          <Crown :size="18" class="text-amber-600" />
        </span>
        <div class="quota-content">
          <div class="quota-title-row">
            <strong>Gói Miễn phí (Free)</strong>
            <span class="quota-status-text">
              Đã sử dụng {{ home.entitlements.examsUsed }} / {{ home.entitlements.examLimit }} lượt tạo đề
            </span>
          </div>
          <div class="quota-track-bar">
            <div
              class="quota-fill-bar"
              :style="{
                width: `${Math.min(100, (home.entitlements.examsUsed / Math.max(1, home.entitlements.examLimit)) * 100)}%`,
              }"
            ></div>
          </div>
        </div>
      </div>

      <div class="quota-right">
        <Button
          v-if="home.entitlements.examsRemaining <= 0"
          variant="ai"
          size="sm"
          @click="router.push('/pricing')"
        >
          <Crown :size="14" /> Nâng cấp Premium
        </Button>
        <span v-else class="quota-badge-pill">
          Còn <strong>{{ home.entitlements.examsRemaining }}</strong> lượt tạo đề
        </span>
      </div>
    </div>

    <!-- 4. THANH BỘ LỌC PHÂN LOẠI (FILTER TABS) -->
    <nav class="filter-tabs-row" aria-label="Bộ lọc phân loại bài thi">
      <button
        type="button"
        class="filter-tab-btn"
        :class="{ active: activeFilter === 'all' }"
        @click="activeFilter = 'all'"
      >
        Tất cả
        <span class="tab-count-pill">{{ home.filteredExams.length + home.examTemplates.length }}</span>
      </button>

      <button
        v-if="home.activeAttempts.length"
        type="button"
        class="filter-tab-btn"
        :class="{ active: activeFilter === 'in_progress' }"
        @click="activeFilter = 'in_progress'"
      >
        Đang làm dở
        <span class="tab-count-pill alert-pill">{{ home.activeAttempts.length }}</span>
      </button>

      <button
        v-if="home.examTemplates.length"
        type="button"
        class="filter-tab-btn"
        :class="{ active: activeFilter === 'templates' }"
        @click="activeFilter = 'templates'"
      >
        Tạo đề thích ứng
        <span class="tab-count-pill">{{ home.examTemplates.length }}</span>
      </button>

      <button
        v-if="home.filteredExams.length"
        type="button"
        class="filter-tab-btn"
        :class="{ active: activeFilter === 'exams' }"
        @click="activeFilter = 'exams'"
      >
        Đề sẵn sàng
        <span class="tab-count-pill">{{ home.filteredExams.length }}</span>
      </button>

      <button
        v-if="home.history.length"
        type="button"
        class="filter-tab-btn"
        :class="{ active: activeFilter === 'history' }"
        @click="activeFilter = 'history'"
      >
        Lịch sử làm bài
        <span class="tab-count-pill">{{ home.history.length }}</span>
      </button>
    </nav>

    <!-- TRẠNG THÁI ĐANG TẢI (SKELETON LOADING) -->
    <div v-if="home.loading" class="tests-loading-grid" aria-busy="true">
      <div class="skeleton-card skeleton-banner"></div>
      <div class="skeleton-grid-row">
        <div class="skeleton-card skeleton-item"></div>
        <div class="skeleton-card skeleton-item"></div>
        <div class="skeleton-card skeleton-item"></div>
      </div>
    </div>

    <!-- NỘI DUNG DANH SÁCH BÀI THI -->
    <div v-else class="tests-sections-stack">
      <!-- 5. BÀI THI ĐANG LÀM DỞ DANG (ACTIVE ATTEMPTS) -->
      <section
        v-if="home.activeAttempts.length && ['all', 'in_progress'].includes(activeFilter)"
        class="tests-section active-attempts-section"
        aria-labelledby="section-in-progress-title"
      >
        <div class="section-header-bar">
          <div class="section-title-wrap">
            <span class="pulse-indicator-dot"></span>
            <h2 id="section-in-progress-title" class="section-heading-title">Bài thi đang thực hiện dở dang</h2>
          </div>
          <p class="section-heading-sub">Tiếp tục trả lời để hoàn thành bài thi trước khi hết thời gian</p>
        </div>

        <div class="attempts-cards-grid">
          <article
            v-for="item in home.activeAttempts"
            :key="item.attemptId"
            class="attempt-focus-card"
          >
            <div class="attempt-card-top">
              <span class="attempt-badge-icon bg-amber-50 text-amber-700 border-amber-200">
                <Clock :size="20" />
              </span>
              <div class="attempt-title-area">
                <h3 class="attempt-exam-title">
                  {{ home.exams.find((exam) => exam.examId === item.examId)?.title || `Bài thi #${item.examId}` }}
                </h3>
                <div class="attempt-meta-tags">
                  <span class="meta-tag-item">
                    <CalendarDays :size="12" /> Bắt đầu: {{ home.formatDate(item.startedAt) }}
                  </span>
                  <Badge variant="warning" size="sm">
                    {{ home.localizeStatus(item.status) }}
                  </Badge>
                </div>
              </div>
            </div>

            <div class="attempt-card-action">
              <Button
                variant="default"
                size="sm"
                :disabled="home.loading || home.attempt"
                @click="home.resumeAttempt(item)"
              >
                Tiếp tục làm bài <ArrowRight :size="15" />
              </Button>
            </div>
          </article>
        </div>
      </section>

      <!-- 6. TẠO ĐỀ LUYỆN TẬP CÁ NHÂN HÓA TỪ MẪU (EXAM TEMPLATES) -->
      <section
        v-if="home.examTemplates.length && ['all', 'templates'].includes(activeFilter)"
        class="tests-section"
        aria-labelledby="section-templates-title"
      >
        <div class="section-header-bar">
          <div>
            <div class="flex items-center gap-2">
              <h2 id="section-templates-title" class="section-heading-title">Tạo đề luyện tập cá nhân hóa</h2>
              <Badge variant="ai" size="sm">Thích ứng</Badge>
            </div>
            <p class="section-heading-sub">
              Hệ thống AI tự động sinh đề mới theo cấu trúc chuẩn và các phần kiến thức đã chọn
            </p>
          </div>
        </div>

        <div class="templates-grid">
          <article
            v-for="template in home.examTemplates"
            :key="template.templateId"
            class="template-card"
          >
            <div class="template-card-top">
              <span class="template-icon bg-violet-50 text-ai-accent border-violet-200/60">
                <Sparkles :size="20" />
              </span>
              <Badge variant="outline" size="sm">
                {{ (template.sections || []).length }} phần kiến thức
              </Badge>
            </div>

            <div class="template-card-body">
              <h3 class="template-title">{{ template.title }}</h3>
              <p class="template-desc">
                {{ template.description || 'Đề luyện tập cá nhân hóa được cấu hình chuẩn theo chuẩn kiến thức.' }}
              </p>

              <!-- Các chỉ số thực tế từ template (chỉ hiển thị khi backend có dữ liệu) -->
              <div class="template-meta-row">
                <span v-if="template.durationMinutes" class="meta-pill">
                  <Clock3 :size="13" /> {{ template.durationMinutes }} phút
                </span>
                <span v-if="getTemplateQuestionCount(template)" class="meta-pill">
                  <FileQuestion :size="13" /> {{ getTemplateQuestionCount(template) }} câu hỏi
                </span>
                <span v-if="template.passingScore" class="meta-pill highlight">
                  <Award :size="13" /> Điểm đạt: {{ template.passingScore }}
                </span>
              </div>
            </div>

            <div class="template-card-footer">
              <Button
                v-if="home.entitlements?.plan !== 'PREMIUM' && home.entitlements?.examsRemaining <= 0"
                variant="outline"
                size="sm"
                class="w-full"
                @click="router.push('/pricing')"
              >
                <Crown :size="14" class="text-amber-600" /> Nâng cấp để tạo thêm
              </Button>
              <Button
                v-else
                variant="default"
                size="sm"
                class="w-full"
                :disabled="home.creatingExamTemplateId != null"
                @click="home.createPracticeExam(template)"
              >
                <Loader2 v-if="home.creatingExamTemplateId === template.templateId" :size="14" class="animate-spin" />
                <span v-if="home.creatingExamTemplateId === template.templateId">Đang tạo đề…</span>
                <span v-else class="flex items-center gap-1.5">Tạo đề ngay <ArrowRight :size="14" /></span>
              </Button>
            </div>
          </article>
        </div>
      </section>

      <!-- 7. DANH SÁCH ĐỀ THI SẴN SÀNG (PUBLISHED EXAMS) -->
      <section
        v-if="['all', 'exams'].includes(activeFilter)"
        class="tests-section"
        aria-labelledby="section-exams-title"
      >
        <div class="section-header-bar">
          <div>
            <h2 id="section-exams-title" class="section-heading-title">Đề thi sẵn sàng làm</h2>
            <p class="section-heading-sub">
              Các bài kiểm tra chính thức và bài đánh giá năng lực tổng hợp
            </p>
          </div>
          <span class="count-indicator">
            {{ home.filteredExams.length }} bài thi
          </span>
        </div>

        <!-- Lưới thẻ đề thi nếu có dữ liệu -->
        <div v-if="home.filteredExams.length" class="exams-cards-grid">
          <article
            v-for="exam in home.filteredExams"
            :key="exam.examId"
            class="exam-card"
          >
            <div class="exam-card-top">
              <span class="exam-icon-wrap bg-blue-50 text-brand-primary border-blue-200/60">
                <BookOpen :size="20" />
              </span>

              <!-- Badge trạng thái hoặc lịch sử đã hoàn thành -->
              <div class="exam-badges-wrap">
                <Badge
                  v-if="completedExamIds.has(exam.examId)"
                  variant="success"
                  size="sm"
                >
                  <Check :size="12" /> Đã hoàn thành
                </Badge>
                <Badge v-else variant="outline" size="sm">
                  Sẵn sàng
                </Badge>
              </div>
            </div>

            <div class="exam-card-body">
              <h3 class="exam-card-title">{{ exam.title }}</h3>
              <p class="exam-card-desc">
                {{ exam.description || 'Bài đánh giá kiến thức tổng hợp và phân tích mức độ thành thạo kỹ năng.' }}
              </p>

              <!-- Các chỉ số thực tế từ backend (thời lượng, số câu, tổng điểm) -->
              <div class="exam-metrics-row">
                <span v-if="exam.durationMinutes" class="exam-metric-item">
                  <Clock3 :size="13" /> {{ exam.durationMinutes }} phút
                </span>
                <span v-if="exam.questionCount || exam.totalQuestions" class="exam-metric-item">
                  <FileText :size="13" /> {{ exam.questionCount || exam.totalQuestions }} câu hỏi
                </span>
                <span v-if="exam.totalPoints != null && exam.totalPoints > 0" class="exam-metric-item">
                  <Award :size="13" /> {{ exam.totalPoints }} điểm
                </span>
                <span v-if="exam.passingScore != null && exam.passingScore > 0" class="exam-metric-item highlight">
                  Điểm đạt: {{ exam.passingScore }}
                </span>
              </div>

              <!-- Lịch sử điểm cao nhất nếu từng làm -->
              <div v-if="bestScoresMap[exam.examId] != null" class="best-score-badge">
                <Trophy :size="12" class="text-amber-600" />
                <span>Điểm cao nhất: <strong>{{ bestScoresMap[exam.examId] }}</strong></span>
              </div>
            </div>

            <div class="exam-card-footer">
              <Button
                variant="default"
                size="sm"
                class="w-full"
                :disabled="home.loading"
                @click="home.startExam(exam)"
              >
                {{ completedExamIds.has(exam.examId) ? 'Làm lại đề này' : 'Bắt đầu làm bài' }}
                <ArrowRight :size="15" />
              </Button>
            </div>
          </article>
        </div>

        <!-- Empty State: Tìm kiếm không ra kết quả -->
        <div v-else-if="home.searchQuery" class="empty-state-card">
          <div class="empty-icon-circle bg-slate-100 text-slate-500">
            <Search :size="24" />
          </div>
          <div class="empty-text-wrap">
            <strong>Không tìm thấy bài kiểm tra nào</strong>
            <p>Không có kết quả nào phù hợp với từ khóa "{{ home.searchQuery }}". Hãy thử tìm kiếm với từ khóa khác.</p>
          </div>
          <Button variant="outline" size="sm" @click="clearSearch">
            <RotateCcw :size="14" /> Xóa tìm kiếm
          </Button>
        </div>

        <!-- Empty State: Không có đề thi nào trong hệ thống -->
        <div v-else class="empty-state-card">
          <div class="empty-icon-circle bg-blue-50 text-brand-primary">
            <BookOpen :size="24" />
          </div>
          <div class="empty-text-wrap">
            <strong>Chưa có bài kiểm tra nào được phát hành</strong>
            <p>Các bài kiểm tra chính thức do quản trị viên mở sẽ xuất hiện tại đây.</p>
          </div>
        </div>
      </section>

      <!-- 8. LỊCH SỬ LÀM BÀI GẦN ĐÂY (RECENT HISTORY) -->
      <section
        v-if="home.history.length && ['all', 'history'].includes(activeFilter)"
        class="tests-section history-section"
        aria-labelledby="section-history-title"
      >
        <div class="section-header-bar">
          <div>
            <h2 id="section-history-title" class="section-heading-title">Lịch sử làm bài gần đây</h2>
            <p class="section-heading-sub">Kết quả và điểm số các lượt thi gần nhất của bạn</p>
          </div>
          <button type="button" class="action-link-btn" @click="home.showView('history')">
            Xem toàn bộ lịch sử <ArrowRight :size="14" />
          </button>
        </div>

        <div class="history-list-wrap">
          <article
            v-for="item in home.history.slice(0, 4)"
            :key="item.attemptId"
            class="history-item-row"
          >
            <div class="history-col-icon">
              <span
                class="history-icon-pill"
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

            <div class="history-col-info">
              <h3 class="history-exam-title">
                {{ home.exams.find((exam) => exam.examId === item.examId)?.title || `Bài thi #${item.examId}` }}
              </h3>
              <div class="history-meta-sub">
                <span>{{ home.formatDate(item.startedAt) }}</span>
                <span class="meta-dot">•</span>
                <Badge :variant="getAttemptBadgeVariant(item.status)" size="sm">
                  {{ home.localizeStatus(item.status) }}
                </Badge>
              </div>
            </div>

            <div class="history-col-score">
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
                Xem lại
              </Button>
            </div>
          </article>
        </div>
      </section>
    </div>
  </section>
</template>

<style scoped>
.tests-page-view {
  display: flex;
  flex-direction: column;
  gap: 24px;
  width: 100%;
}

/* ==========================================================================
   PAGE HEADER & SEARCH
   ========================================================================== */
.tests-header-card {
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

.search-bar-wrap {
  width: 100%;
}

.search-input-box {
  position: relative;
  display: flex;
  align-items: center;
  width: 100%;
  max-width: 580px;
}

.search-icon {
  position: absolute;
  left: 14px;
  color: #94A3B8;
  pointer-events: none;
}

.search-input {
  width: 100%;
  height: 42px;
  padding: 0 38px 0 42px;
  border-radius: var(--radius-md, 8px);
  border: 1px solid var(--border-subtle, #E2E8F0);
  background-color: #ffffff;
  color: var(--text-primary, #0F172A);
  font: inherit;
  font-size: 13.5px;
  outline: none;
  box-shadow: 0 1px 3px rgba(15, 23, 42, 0.04);
  transition: border-color 0.15s ease, box-shadow 0.15s ease;
}

.search-input:focus {
  border-color: var(--brand-primary, #2563EB);
  box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.12);
}

.search-clear-btn {
  position: absolute;
  right: 12px;
  display: grid;
  place-items: center;
  width: 22px;
  height: 22px;
  border-radius: 50%;
  border: none;
  background-color: #E2E8F0;
  color: #64748B;
  cursor: pointer;
  transition: background-color 0.15s ease;
}

.search-clear-btn:hover {
  background-color: #CBD5E1;
  color: #0F172A;
}

/* ==========================================================================
   ALERTS & NOTICES
   ========================================================================== */
.alert-banner {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  border-radius: var(--radius-md, 8px);
  font-size: 13px;
  font-weight: 500;
}

.alert-success {
  background-color: #ECFDF5;
  border: 1px solid #A7F3D0;
  color: #065F46;
}

.alert-error {
  background-color: #FFF1F2;
  border: 1px solid #FECDD3;
  color: #9F1239;
}

.alert-text {
  flex: 1;
}

.alert-dismiss {
  display: grid;
  place-items: center;
  border: none;
  background: transparent;
  color: inherit;
  opacity: 0.7;
  cursor: pointer;
}

.alert-dismiss:hover {
  opacity: 1;
}

/* ==========================================================================
   FREE QUOTA BANNER
   ========================================================================== */
.free-quota-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  padding: 14px 18px;
  border-radius: var(--radius-md, 8px);
  background: linear-gradient(135deg, #FFFBEB 0%, #FEF3C7 100%);
  border: 1px solid #FDE68A;
  box-shadow: 0 1px 4px rgba(245, 158, 11, 0.08);
}

.quota-left {
  display: flex;
  align-items: center;
  gap: 14px;
  flex: 1;
  min-width: 0;
}

.quota-icon-wrap {
  display: grid;
  place-items: center;
  width: 38px;
  height: 38px;
  border-radius: var(--radius-md, 8px);
  background-color: #ffffff;
  border: 1px solid #FDE68A;
  flex-shrink: 0;
}

.quota-content {
  display: flex;
  flex-direction: column;
  gap: 6px;
  flex: 1;
  max-width: 520px;
}

.quota-title-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  font-size: 13px;
}

.quota-title-row strong {
  color: #92400E;
  font-weight: 700;
}

.quota-status-text {
  font-size: 12px;
  color: #B45309;
}

.quota-track-bar {
  width: 100%;
  height: 6px;
  border-radius: 3px;
  background-color: #FDE68A;
  overflow: hidden;
}

.quota-fill-bar {
  height: 100%;
  border-radius: 3px;
  background: linear-gradient(90deg, #F59E0B 0%, #D97706 100%);
  transition: width 0.3s ease;
}

.quota-right {
  flex-shrink: 0;
}

.quota-badge-pill {
  display: inline-flex;
  padding: 4px 10px;
  border-radius: 999px;
  background-color: #ffffff;
  border: 1px solid #FDE68A;
  font-size: 12px;
  color: #92400E;
}

/* ==========================================================================
   FILTER TABS ROW
   ========================================================================== */
.filter-tabs-row {
  display: flex;
  align-items: center;
  gap: 8px;
  overflow-x: auto;
  padding-bottom: 2px;
}

.filter-tab-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 8px 14px;
  border-radius: var(--radius-md, 8px);
  border: 1px solid var(--border-subtle, #E2E8F0);
  background-color: #ffffff;
  color: var(--text-secondary, #475569);
  font: inherit;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  white-space: nowrap;
  transition: all 0.15s ease;
}

.filter-tab-btn:hover {
  background-color: #F8FAFC;
  color: var(--text-primary, #0F172A);
  border-color: #CBD5E1;
}

.filter-tab-btn.active {
  background-color: var(--brand-primary, #2563EB);
  color: #ffffff;
  border-color: var(--brand-primary, #2563EB);
  font-weight: 600;
  box-shadow: 0 2px 6px rgba(37, 99, 235, 0.2);
}

.tab-count-pill {
  padding: 1px 6px;
  border-radius: 999px;
  font-size: 11px;
  font-weight: 700;
  background-color: #F1F5F9;
  color: #475569;
}

.filter-tab-btn.active .tab-count-pill {
  background-color: rgba(255, 255, 255, 0.25);
  color: #ffffff;
}

.tab-count-pill.alert-pill {
  background-color: #FEF3C7;
  color: #B45309;
}

/* ==========================================================================
   SECTIONS STACK & HEADINGS
   ========================================================================== */
.tests-sections-stack {
  display: flex;
  flex-direction: column;
  gap: 28px;
}

.tests-section {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.section-header-bar {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.section-title-wrap {
  display: flex;
  align-items: center;
  gap: 8px;
}

.section-heading-title {
  margin: 0;
  font-size: 17px;
  font-weight: 750;
  color: var(--text-primary, #0F172A);
  letter-spacing: -0.2px;
}

.section-heading-sub {
  margin: 2px 0 0;
  font-size: 12.5px;
  color: var(--text-muted, #64748B);
}

.count-indicator {
  font-size: 12.5px;
  font-weight: 600;
  color: var(--text-muted, #64748B);
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
   ACTIVE ATTEMPTS (IN-PROGRESS)
   ========================================================================== */
.active-attempts-section {
  padding: 18px 20px;
  border-radius: var(--radius-lg, 12px);
  background: linear-gradient(135deg, #FFFBEB 0%, #FEF3C7 40%, #ffffff 100%);
  border: 1px solid #FDE68A;
  box-shadow: 0 2px 8px rgba(245, 158, 11, 0.08);
}

.pulse-indicator-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background-color: #F59E0B;
  box-shadow: 0 0 0 3px rgba(245, 158, 11, 0.3);
  animation: pulse-ring 1.5s infinite;
}

@keyframes pulse-ring {
  0% { box-shadow: 0 0 0 0 rgba(245, 158, 11, 0.5); }
  70% { box-shadow: 0 0 0 6px rgba(245, 158, 11, 0); }
  100% { box-shadow: 0 0 0 0 rgba(245, 158, 11, 0); }
}

.attempts-cards-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(320px, 1fr));
  gap: 12px;
}

.attempt-focus-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 14px 16px;
  border-radius: var(--radius-md, 8px);
  background-color: #ffffff;
  border: 1px solid #FDE68A;
  box-shadow: 0 1px 3px rgba(15, 23, 42, 0.04);
}

.attempt-card-top {
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 0;
  flex: 1;
}

.attempt-badge-icon {
  display: grid;
  place-items: center;
  width: 38px;
  height: 38px;
  border-radius: var(--radius-md, 8px);
  border: 1px solid;
  flex-shrink: 0;
}

.attempt-title-area {
  display: flex;
  flex-direction: column;
  gap: 3px;
  min-width: 0;
}

.attempt-exam-title {
  margin: 0;
  font-size: 14px;
  font-weight: 750;
  color: var(--text-primary, #0F172A);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.attempt-meta-tags {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 11.5px;
  color: var(--text-muted, #64748B);
}

.meta-tag-item {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.attempt-card-action {
  flex-shrink: 0;
}

/* ==========================================================================
   TEMPLATES GRID (AI GENERATED PRACTICE EXAMS)
   ========================================================================== */
.templates-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 16px;
}

.template-card {
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  padding: 18px;
  border-radius: var(--radius-lg, 12px);
  background-color: #ffffff;
  border: 1px solid var(--border-subtle, #E2E8F0);
  box-shadow: 0 1px 3px rgba(15, 23, 42, 0.02);
  transition: transform 0.15s ease, box-shadow 0.15s ease, border-color 0.15s ease;
}

.template-card:hover {
  transform: translateY(-2px);
  border-color: #DDD6FE;
  box-shadow: 0 4px 14px rgba(124, 58, 237, 0.08);
}

.template-card-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  margin-bottom: 12px;
}

.template-icon {
  display: grid;
  place-items: center;
  width: 36px;
  height: 36px;
  border-radius: var(--radius-md, 8px);
  border: 1px solid;
}

.template-card-body {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-bottom: 16px;
}

.template-title {
  margin: 0;
  font-size: 15px;
  font-weight: 700;
  color: var(--text-primary, #0F172A);
  line-height: 1.35;
}

.template-desc {
  margin: 0;
  font-size: 12px;
  line-height: 1.5;
  color: var(--text-muted, #64748B);
  overflow: hidden;
  display: -webkit-box;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.template-meta-row {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: 4px;
}

.meta-pill {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 2px 8px;
  border-radius: 6px;
  font-size: 11px;
  font-weight: 500;
  background-color: #F8FAFC;
  border: 1px solid #E2E8F0;
  color: #475569;
}

.meta-pill.highlight {
  background-color: #EFF6FF;
  border-color: #BFDBFE;
  color: var(--brand-primary, #2563EB);
  font-weight: 600;
}

.template-card-footer {
  padding-top: 12px;
  border-top: 1px solid #F1F5F9;
}

/* ==========================================================================
   EXAMS CARDS GRID (PUBLISHED EXAMS)
   ========================================================================== */
.exams-cards-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 16px;
}

.exam-card {
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  padding: 18px;
  border-radius: var(--radius-lg, 12px);
  background-color: #ffffff;
  border: 1px solid var(--border-subtle, #E2E8F0);
  box-shadow: 0 1px 3px rgba(15, 23, 42, 0.02);
  transition: transform 0.15s ease, box-shadow 0.15s ease, border-color 0.15s ease;
}

.exam-card:hover {
  transform: translateY(-2px);
  border-color: #CBD5E1;
  box-shadow: 0 4px 14px rgba(15, 23, 42, 0.06);
}

.exam-card-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  margin-bottom: 12px;
}

.exam-icon-wrap {
  display: grid;
  place-items: center;
  width: 38px;
  height: 38px;
  border-radius: var(--radius-md, 8px);
  border: 1px solid;
}

.exam-badges-wrap {
  display: flex;
  align-items: center;
  gap: 6px;
}

.exam-card-body {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-bottom: 16px;
}

.exam-card-title {
  margin: 0;
  font-size: 15px;
  font-weight: 700;
  color: var(--text-primary, #0F172A);
  line-height: 1.35;
}

.exam-card-desc {
  margin: 0;
  font-size: 12.5px;
  line-height: 1.5;
  color: var(--text-muted, #64748B);
  overflow: hidden;
  display: -webkit-box;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.exam-metrics-row {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 4px;
}

.exam-metric-item {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 3px 8px;
  border-radius: 6px;
  font-size: 11.5px;
  font-weight: 500;
  background-color: #F8FAFC;
  border: 1px solid #E2E8F0;
  color: #475569;
}

.exam-metric-item.highlight {
  background-color: #EFF6FF;
  border-color: #BFDBFE;
  color: var(--brand-primary, #2563EB);
  font-weight: 600;
}

.best-score-badge {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 3px 8px;
  border-radius: 6px;
  background-color: #FFFBEB;
  border: 1px solid #FDE68A;
  font-size: 11.5px;
  color: #92400E;
  margin-top: 4px;
  width: fit-content;
}

.exam-card-footer {
  padding-top: 12px;
  border-top: 1px solid #F1F5F9;
}

/* ==========================================================================
   HISTORY LIST (RECENT EXAM ATTEMPTS)
   ========================================================================== */
.history-section {
  padding: 20px 22px;
  border-radius: var(--radius-lg, 12px);
  background-color: #ffffff;
  border: 1px solid var(--border-subtle, #E2E8F0);
  box-shadow: 0 1px 3px rgba(15, 23, 42, 0.02);
}

.history-list-wrap {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.history-item-row {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 12px 14px;
  border-radius: var(--radius-md, 8px);
  background-color: #F8FAFC;
  border: 1px solid var(--border-subtle, #E2E8F0);
  transition: background-color 0.15s ease;
}

.history-item-row:hover {
  background-color: #F1F5F9;
}

.history-col-icon {
  flex-shrink: 0;
}

.history-icon-pill {
  display: grid;
  place-items: center;
  width: 36px;
  height: 36px;
  border-radius: var(--radius-md, 8px);
  border: 1px solid;
}

.history-col-info {
  flex: 1;
  min-width: 0;
}

.history-exam-title {
  margin: 0 0 3px;
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
  font-size: 12px;
  color: var(--text-muted, #64748B);
}

.meta-dot {
  color: #CBD5E1;
}

.history-col-score {
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
  color: var(--text-muted, #94A3B8);
}

/* ==========================================================================
   EMPTY STATE STYLES
   ========================================================================== */
.empty-state-card {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 24px;
  border-radius: var(--radius-lg, 12px);
  background-color: #F8FAFC;
  border: 1px dashed var(--border-subtle, #E2E8F0);
}

.empty-icon-circle {
  display: grid;
  place-items: center;
  width: 48px;
  height: 48px;
  border-radius: 50%;
  flex-shrink: 0;
}

.empty-text-wrap {
  flex: 1;
  min-width: 0;
}

.empty-text-wrap strong {
  display: block;
  font-size: 14px;
  font-weight: 700;
  color: var(--text-primary, #0F172A);
}

.empty-text-wrap p {
  margin: 3px 0 0;
  font-size: 12.5px;
  line-height: 1.5;
  color: var(--text-muted, #64748B);
}

/* ==========================================================================
   SKELETON LOADING
   ========================================================================== */
.tests-loading-grid {
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

.skeleton-banner {
  height: 100px;
}

.skeleton-grid-row {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
}

.skeleton-item {
  height: 180px;
}

/* ==========================================================================
   RESPONSIVE DESIGN (TABLET & MOBILE)
   ========================================================================== */
@media (max-width: 900px) {
  .skeleton-grid-row {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .exams-cards-grid,
  .templates-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 640px) {
  .tests-header-card {
    padding: 18px 16px;
  }

  .free-quota-card {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
  }

  .quota-left {
    width: 100%;
  }

  .quota-right {
    width: 100%;
  }

  .quota-right .Button {
    width: 100%;
  }

  .exams-cards-grid,
  .templates-grid,
  .skeleton-grid-row {
    grid-template-columns: 1fr;
  }

  .attempt-focus-card {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
  }

  .attempt-card-action {
    width: 100%;
  }

  .attempt-card-action button {
    width: 100%;
  }

  .history-item-row {
    flex-direction: column;
    align-items: flex-start;
    gap: 10px;
  }

  .history-col-score {
    width: 100%;
    justify-content: space-between;
  }

  .empty-state-card {
    flex-direction: column;
    text-align: center;
  }
}
</style>
