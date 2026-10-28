<script setup>
import { inject } from 'vue'
import {
  ArrowRight,
  Award,
  BarChart3,
  BookMarked,
  BookOpen,
  Brain,
  CalendarDays,
  Check,
  CheckCircle2,
  Clock,
  Compass,
  Flame,
  GraduationCap,
  Plus,
  Sparkles,
  Target,
  Trophy,
  Zap,
} from 'lucide-vue-next'
import { homeContextKey } from '../../composables/homeContext'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'

const home = inject(homeContextKey)

function getMasteryBadgeVariant(value) {
  if (value >= 80) return 'success'
  if (value >= 50) return 'default'
  return 'warning'
}

function getMasteryLabel(value) {
  if (value >= 80) return 'Thành thạo'
  if (value >= 50) return 'Đang học'
  return 'Cần củng cố'
}
</script>

<template>
  <div class="dashboard-wrapper">
    <!-- TRẠNG THÁI ĐANG TẢI (SKELETON LOADING) -->
    <div v-if="home.loading" class="dashboard-grid skeleton-grid" aria-busy="true">
      <div class="main-column">
        <div class="skeleton-card skeleton-banner"></div>
        <div class="skeleton-card skeleton-block"></div>
        <div class="skeleton-card skeleton-block"></div>
        <div class="skeleton-card skeleton-block"></div>
        <div class="skeleton-card skeleton-block"></div>
      </div>
      <aside class="right-column">
        <div class="skeleton-card skeleton-panel"></div>
        <div class="skeleton-card skeleton-panel"></div>
        <div class="skeleton-card skeleton-panel"></div>
      </aside>
    </div>

    <!-- NỘI DUNG CHÍNH DASHBOARD (LIGHT AI EDUCATION) -->
    <div v-else class="dashboard-grid">
      <!-- CỘT CHÍNH (MAIN COLUMN) -->
      <div class="main-column">
        <!-- 1. LỜI CHÀO & TÓM TẮT HỌC TẬP (WELCOME CARD) -->
        <section class="welcome-card" aria-label="Lời chào và tóm tắt học tập">
          <div class="welcome-content">
            <Badge variant="ai" class="welcome-badge">
              <Sparkles :size="13" class="text-ai-accent" />
              KHÔNG GIAN HỌC TẬP THÍCH ỨNG
            </Badge>

            <h1 class="welcome-title">
              {{ home.greeting }}, {{ home.greetingName }}!
            </h1>

            <p class="welcome-desc">
              Hệ thống AI đã cập nhật trạng thái học tập của bạn. Hãy tiếp tục lộ trình để củng cố kỹ năng và bứt phá mục tiêu.
            </p>

            <!-- 4 Thẻ chỉ số nhanh (Quick Stats từ backend thực tế) -->
            <div class="quick-stats-row">
              <div
                class="stat-pill"
                role="button"
                tabindex="0"
                @click="home.showView('plans')"
                @keydown.enter="home.showView('plans')"
              >
                <span class="stat-icon-wrap bg-blue-50 text-brand-primary border border-blue-200/60">
                  <BookMarked :size="18" />
                </span>
                <div class="stat-info">
                  <strong>{{ home.activePlans.length }}</strong>
                  <small>Kế hoạch đang học</small>
                </div>
              </div>

              <div
                class="stat-pill"
                role="button"
                tabindex="0"
                @click="home.showView('tests')"
                @keydown.enter="home.showView('tests')"
              >
                <span class="stat-icon-wrap bg-emerald-50 text-emerald-600 border border-emerald-200/60">
                  <CheckCircle2 :size="18" />
                </span>
                <div class="stat-info">
                  <strong>{{ home.doneCount }}</strong>
                  <small>Bài thi hoàn thành</small>
                </div>
              </div>

              <div
                class="stat-pill"
                role="button"
                tabindex="0"
                @click="home.showView('tests')"
                @keydown.enter="home.showView('tests')"
              >
                <span class="stat-icon-wrap bg-sky-50 text-sky-600 border border-sky-200/60">
                  <BookOpen :size="18" />
                </span>
                <div class="stat-info">
                  <strong>{{ home.publishedExams.length }}</strong>
                  <small>Đề thi sẵn sàng</small>
                </div>
              </div>

              <div
                class="stat-pill"
                role="button"
                tabindex="0"
                @click="home.showView('progress')"
                @keydown.enter="home.showView('progress')"
              >
                <span class="stat-icon-wrap bg-violet-50 text-ai-accent border border-violet-200/60">
                  <Brain :size="18" />
                </span>
                <div class="stat-info">
                  <strong>{{ home.activeRecommendations.length }}</strong>
                  <small>Gợi ý AI cần xem</small>
                </div>
              </div>
            </div>
          </div>
        </section>

        <!-- 2. BÀI THI ĐANG LÀM DỞ DANG (NẾU CÓ ACTIVE ATTEMPT) -->
        <section v-if="home.activeAttempts.length" class="active-attempt-banner" aria-label="Bài thi đang làm dở dang">
          <div class="active-attempt-left">
            <span class="attempt-pulse-indicator"></span>
            <span class="attempt-icon-pill bg-amber-50 text-amber-700 border border-amber-200/60">
              <Clock :size="18" />
            </span>
            <div class="attempt-text">
              <strong class="attempt-title-text">Bạn có lượt thi đang thực hiện dở dang</strong>
              <p class="attempt-subtitle-text">
                {{ home.exams.find((exam) => exam.examId === home.activeAttempts[0].examId)?.title || `Bài thi #${home.activeAttempts[0].examId}` }}
                • Bắt đầu: {{ home.formatDate(home.activeAttempts[0].startedAt) }}
              </p>
            </div>
          </div>
          <Button
            variant="default"
            size="sm"
            :disabled="home.loading || home.attempt"
            @click="home.resumeAttempt(home.activeAttempts[0])"
          >
            Tiếp tục làm bài <ArrowRight :size="15" />
          </Button>
        </section>

        <!-- 3. KẾ HOẠCH / HOẠT ĐỘNG HỌC TIẾP THEO (ACTIVE PLAN) -->
        <section class="dashboard-card" aria-labelledby="section-continue-title">
          <div class="card-header-bar">
            <div>
              <h2 id="section-continue-title" class="card-title">Kế hoạch học tập tiếp theo</h2>
              <p class="card-subtitle">Kế hoạch học tập đang hoạt động của bạn</p>
            </div>
            <button type="button" class="action-text-btn" @click="home.showView('plans')">
              Xem tất cả <ArrowRight :size="15" />
            </button>
          </div>

          <!-- Có lộ trình đang học -->
          <article v-if="home.activePlan" class="active-plan-card">
            <div class="plan-visual-badge">
              <span class="plan-badge-icon">
                <Compass :size="28" :stroke-width="2" />
              </span>
              <span class="plan-badge-caption">KẾ HOẠCH</span>
            </div>

            <div class="plan-body">
              <div class="plan-title-row">
                <h3 class="plan-title">{{ home.activePlan.title }}</h3>
                <Badge variant="success" size="sm">
                  <span class="live-dot-green"></span>
                  Đang học
                </Badge>
              </div>

              <div class="plan-tags">
                <span class="tag-pill">
                  <GraduationCap :size="12" />
                  {{ home.selectedCertificate?.name || 'Mục tiêu cá nhân' }}
                </span>
                <span class="tag-pill">
                  <Clock :size="12" />
                  {{ home.activePlan.hoursPerWeek || 0 }} giờ / tuần
                </span>
                <span v-if="home.activePlan.targetScore" class="tag-pill highlight">
                  <Target :size="12" />
                  Mục tiêu {{ home.activePlan.targetScore }} điểm
                </span>
              </div>

              <!-- Thanh tiến độ hoàn thành nhiệm vụ -->
              <div class="plan-progress-container">
                <div class="progress-bar-track">
                  <div
                    class="progress-bar-fill"
                    :style="{ width: `${home.completedPlanRatio}%` }"
                  ></div>
                </div>
                <div class="progress-labels">
                  <strong>{{ home.completedPlanRatio }}% hoàn thành</strong>
                  <span>{{ home.activePlan.completedCount || 0 }}/{{ home.activePlan.taskCount || 0 }} nhiệm vụ</span>
                </div>
              </div>

              <div class="plan-meta-row">
                <span class="meta-item">
                  <CalendarDays :size="14" />
                  Ngày thi: {{ home.formatDate(home.activePlan.examDate) }}
                </span>
                <Button variant="default" size="sm" @click="home.showView('plans')">
                  Mở kế hoạch <ArrowRight :size="15" />
                </Button>
              </div>
            </div>
          </article>

          <!-- Empty State: Chưa có lộ trình -->
          <div v-else class="empty-state-box">
            <div class="empty-icon-circle bg-blue-50 text-brand-primary">
              <BookMarked :size="24" />
            </div>
            <div class="empty-text-wrap">
              <strong>Chưa có kế hoạch học đang hoạt động</strong>
              <p>Tạo mục tiêu theo ngày thi và thời gian học mỗi tuần để theo dõi các nhiệm vụ học tập.</p>
            </div>
            <Button
              variant="outline"
              size="sm"
              @click="home.showView('plans'); home.showPlanForm = true"
            >
              <Plus :size="15" /> Tạo kế hoạch mới
            </Button>
          </div>
        </section>

        <!-- 4. KHÓA HỌC & CHỨNG CHỈ ĐANG THEO ĐUỔI (COURSES & CERTIFICATES) -->
        <section class="dashboard-card" aria-labelledby="section-courses-title">
          <div class="card-header-bar">
            <div>
              <h2 id="section-courses-title" class="card-title">Khóa học & Chứng chỉ</h2>
              <p class="card-subtitle">Chương trình học tập và đánh giá chuẩn hóa</p>
            </div>
            <button type="button" class="action-text-btn" @click="home.showView('courses')">
              Tất cả khóa học <ArrowRight :size="15" />
            </button>
          </div>

          <!-- Danh sách chứng chỉ/khóa học nếu có dữ liệu -->
          <div v-if="home.certificates.length" class="courses-preview-grid">
            <article
              v-for="(cert, index) in home.certificates.slice(0, 2)"
              :key="cert.certificateId"
              class="course-preview-card"
            >
              <div class="course-card-top">
                <span
                  class="course-card-icon"
                  :class="index % 2 === 0 ? 'bg-blue-50 text-brand-primary border-blue-200/60' : 'bg-violet-50 text-ai-accent border-violet-200/60'"
                >
                  <GraduationCap :size="20" />
                </span>
                <Badge variant="outline" size="sm">
                  {{ home.certificateProgress(cert).total }} bài học
                </Badge>
              </div>

              <div class="course-card-body">
                <h3 class="course-title">{{ cert.name }}</h3>
                <p class="course-desc">
                  {{ cert.description || 'Chương trình đào tạo và kiểm tra năng lực cá nhân hóa theo mô hình thích ứng.' }}
                </p>

                <!-- Tiến độ hoàn thành bài học thực tế -->
                <div class="course-progress-block">
                  <div class="course-progress-labels">
                    <span>{{ home.certificateProgress(cert).completed }}/{{ home.certificateProgress(cert).total }} bài đã làm</span>
                    <strong>{{ home.certificateProgress(cert).percent }}%</strong>
                  </div>
                  <div class="course-progress-track">
                    <div
                      class="course-progress-fill"
                      :style="{ width: `${home.certificateProgress(cert).percent}%` }"
                    ></div>
                  </div>
                </div>
              </div>

              <div class="course-card-footer">
                <span v-if="cert.passingScore" class="course-pass-score">
                  <Award :size="13" /> Chuẩn đầu ra: {{ cert.passingScore }} điểm
                </span>
                <span v-else class="course-pass-score">
                  <Award :size="13" /> Đánh giá thích ứng
                </span>
                <Button
                  variant="ghost"
                  size="sm"
                  class="course-cta-btn"
                  @click="home.showView('tests'); home.searchQuery = ''"
                >
                  {{ home.certificateProgress(cert).completed ? 'Tiếp tục' : 'Bắt đầu' }} <ArrowRight :size="14" />
                </Button>
              </div>
            </article>
          </div>

          <!-- Empty State: Chưa có khóa học -->
          <div v-else class="empty-state-box">
            <div class="empty-icon-circle bg-sky-50 text-sky-600">
              <GraduationCap :size="24" />
            </div>
            <div class="empty-text-wrap">
              <strong>Chưa có khóa học hoặc chứng chỉ nào</strong>
              <p>Các chương trình học tập được quản trị viên cấu hình sẽ hiển thị tại đây.</p>
            </div>
            <Button variant="outline" size="sm" @click="home.showView('tests')">
              Xem đề thi <ArrowRight :size="15" />
            </Button>
          </div>
        </section>

        <!-- 5. CHỈ SỐ TIẾN ĐỘ & NĂNG LỰC BKT (SKILL MASTERY THEO MÔ HÌNH BKT) -->
        <section class="dashboard-card" aria-labelledby="section-mastery-title">
          <div class="card-header-bar">
            <div>
              <h2 id="section-mastery-title" class="card-title">Chỉ số tiến độ & Năng lực học tập</h2>
              <p class="card-subtitle">
                {{ home.selectedCertificate?.name || 'Mức độ thành thạo kỹ năng theo mô hình Bayesian Knowledge Tracing' }}
              </p>
            </div>
            <button type="button" class="action-text-btn" @click="home.showView('progress')">
              Báo cáo chi tiết <ArrowRight :size="15" />
            </button>
          </div>

          <div class="mastery-dashboard-layout">
            <!-- Điểm trung bình / Vòng tròn tổng kết -->
            <div class="score-summary-card">
              <div
                class="score-radial-circle"
                :style="{
                  background: `conic-gradient(var(--brand-primary, #2563EB) ${home.averageScorePercent}%, #E2E8F0 0)`,
                }"
              >
                <div class="score-circle-inner">
                  <span class="score-number">
                    {{ home.progress?.averageScore != null ? Number(home.progress.averageScore).toFixed(1) : (home.averageScore != null ? home.averageScore.toFixed(1) : '—') }}
                  </span>
                  <small class="score-label">Điểm trung bình</small>
                </div>
              </div>
              <p class="score-meta-text">
                {{ home.progress?.totalAttempts ?? home.history.length }} lượt làm bài đã nộp
              </p>
            </div>

            <!-- Danh sách thanh mức độ thành thạo từng kỹ năng -->
            <div v-if="home.masterySkills.length" class="skills-track-list">
              <div
                v-for="skill in home.masterySkills.slice(0, 4)"
                :key="skill.name"
                class="skill-item-row"
              >
                <div class="skill-info-header">
                  <span class="skill-name">{{ skill.name }}</span>
                  <div class="skill-badge-wrap">
                    <Badge :variant="getMasteryBadgeVariant(skill.value)" size="sm">
                      {{ getMasteryLabel(skill.value) }}
                    </Badge>
                    <strong class="skill-percent">{{ Math.round(skill.value) }}%</strong>
                  </div>
                </div>

                <div class="skill-bar-track">
                  <div
                    class="skill-bar-fill"
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

            <!-- Empty State: Chưa đủ dữ liệu BKT -->
            <div v-else class="empty-state-box skills-empty-card">
              <div class="empty-icon-circle bg-violet-50 text-ai-accent">
                <Sparkles :size="24" />
              </div>
              <div class="empty-text-wrap">
                <strong>Chưa đủ dữ liệu phân tích kỹ năng</strong>
                <p>Hãy hoàn thành bài kiểm tra đầu tiên để mô hình Bayesian Knowledge Tracing phân tích chi tiết mức độ thành thạo các kỹ năng.</p>
              </div>
              <Button variant="outline" size="sm" @click="home.showView('tests')">
                Làm bài kiểm tra <ArrowRight :size="15" />
              </Button>
            </div>
          </div>
        </section>

        <!-- 6. HOẠT ĐỘNG & BÀI THI GẦN ĐÂY (RECENT ACTIVITIES / EXAMS) -->
        <section class="dashboard-card" aria-labelledby="section-recent-title">
          <div class="card-header-bar">
            <div>
              <h2 id="section-recent-title" class="card-title">Hoạt động & Bài kiểm tra gần đây</h2>
              <p class="card-subtitle">Kết quả các lượt làm bài kiểm tra gần nhất của bạn</p>
            </div>
            <button type="button" class="action-text-btn" @click="home.showView('history')">
              Xem tất cả lịch sử <ArrowRight :size="15" />
            </button>
          </div>

          <!-- Danh sách các lượt thi gần đây nếu có -->
          <div v-if="home.history.length" class="recent-attempts-list">
            <article
              v-for="item in home.history.slice(0, 3)"
              :key="item.attemptId"
              class="recent-attempt-item"
            >
              <div class="attempt-icon-col">
                <span
                  class="attempt-icon-box"
                  :class="{
                    'bg-emerald-50 text-emerald-600 border-emerald-200/60': item.status === 'GRADED',
                    'bg-amber-50 text-amber-700 border-amber-200/60': item.status === 'IN_PROGRESS',
                    'bg-slate-50 text-slate-600 border-slate-200/60': !['GRADED', 'IN_PROGRESS'].includes(item.status),
                  }"
                >
                  <CheckCircle2 v-if="item.status === 'GRADED'" :size="17" />
                  <Clock v-else-if="item.status === 'IN_PROGRESS'" :size="17" />
                  <BookOpen v-else :size="17" />
                </span>
              </div>

              <div class="attempt-info-col">
                <h3 class="attempt-title">
                  {{ home.exams.find((exam) => exam.examId === item.examId)?.title || `Bài thi #${item.examId}` }}
                </h3>
                <p class="attempt-meta">
                  {{ home.formatDate(item.startedAt) }}
                  <span class="meta-dot">•</span>
                  <Badge
                    :variant="item.status === 'GRADED' ? 'success' : item.status === 'IN_PROGRESS' ? 'warning' : 'secondary'"
                    size="sm"
                  >
                    {{ home.localizeStatus(item.status) }}
                  </Badge>
                </p>
              </div>

              <div class="attempt-score-col">
                <div v-if="item.score != null" class="score-display">
                  <strong class="score-val">{{ item.score }}</strong>
                  <small v-if="item.maxScore != null" class="max-score">/ {{ item.maxScore }}</small>
                </div>
                <Button
                  v-if="item.status === 'IN_PROGRESS'"
                  variant="outline"
                  size="sm"
                  :disabled="home.loading || home.attempt"
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

          <!-- Empty State: Chưa có lượt làm bài nào -->
          <div v-else class="empty-state-box">
            <div class="empty-icon-circle bg-sky-50 text-sky-600">
              <BookOpen :size="24" />
            </div>
            <div class="empty-text-wrap">
              <strong>Chưa có bài thi nào được ghi nhận</strong>
              <p>Luyện tập với các đề thi có sẵn để ghi nhận điểm số và theo dõi mức độ tiến bộ theo thời gian.</p>
            </div>
            <Button variant="outline" size="sm" @click="home.showView('tests')">
              Xem kho đề thi <ArrowRight :size="15" />
            </Button>
          </div>
        </section>

        <!-- 7. BÀI HỌC VÀ NHIỆM VỤ ĐƯỢC AI GỢI Ý (AI RECOMMENDATIONS) -->
        <section id="recommendations" class="dashboard-card" aria-labelledby="section-recommend-title">
          <div class="card-header-bar">
            <div>
              <div class="title-with-pill">
                <h2 id="section-recommend-title" class="card-title">Bài học AI gợi ý</h2>
                <Badge variant="ai" size="sm">Cá nhân hóa</Badge>
              </div>
              <p class="card-subtitle">Hệ thống gợi ý bài học và chủ đề cần củng cố dựa trên kết quả thực tế</p>
            </div>
            <button type="button" class="action-text-btn" @click="home.showView('progress')">
              Xem phân tích <ArrowRight :size="15" />
            </button>
          </div>

          <!-- Danh sách gợi ý nếu có -->
          <div v-if="home.activeRecommendations.length" class="recommendations-grid">
            <article
              v-for="(item, index) in home.activeRecommendations.slice(0, 3)"
              :key="item.recommendationId || index"
              class="recommendation-card"
            >
              <div class="recommend-top">
                <span
                  class="recommend-icon-box"
                  :class="{
                    'bg-violet-50 text-ai-accent border-violet-200/60': index % 3 === 0,
                    'bg-blue-50 text-brand-primary border-blue-200/60': index % 3 === 1,
                    'bg-sky-50 text-sky-600 border-sky-200/60': index % 3 === 2,
                  }"
                >
                  <Target v-if="index % 3 === 0" :size="18" />
                  <Brain v-else-if="index % 3 === 1" :size="18" />
                  <BookOpen v-else :size="18" />
                </span>
                <Badge
                  :variant="item.priority === 'HIGH' ? 'warning' : 'secondary'"
                  size="sm"
                >
                  {{ home.localizeStatus(item.priority || item.status || 'Đề xuất') }}
                </Badge>
              </div>

              <div class="recommend-body">
                <h3 class="recommend-title">{{ home.recommendationTitle(item, index) }}</h3>
                <p class="recommend-reason">
                  {{ item.reason || 'Gợi ý củng cố kiến thức dựa trên kết quả làm bài gần nhất.' }}
                </p>
              </div>

              <div class="recommend-footer">
                <Button
                  v-if="item.status === 'STARTED'"
                  variant="secondary"
                  size="sm"
                  disabled
                  class="w-full"
                >
                  <Check :size="14" /> Đang ôn luyện
                </Button>
                <Button
                  v-else
                  variant="outline"
                  size="sm"
                  class="w-full"
                  @click="home.markRecommendationStarted(item)"
                >
                  Bắt đầu học <ArrowRight :size="14" />
                </Button>
              </div>
            </article>
          </div>

          <!-- Empty State: Chưa có đề xuất mới -->
          <div v-else class="empty-state-box">
            <div class="empty-icon-circle bg-violet-50 text-ai-accent">
              <Sparkles :size="24" />
            </div>
            <div class="empty-text-wrap">
              <strong>Chưa có đề xuất học tập mới</strong>
              <p>Làm thêm các bài luyện tập để mô hình AI phân tích và tự động đề xuất các chuyên đề phù hợp với bạn.</p>
            </div>
            <Button variant="outline" size="sm" @click="home.showView('tests')">
              Chọn bài kiểm tra <ArrowRight :size="15" />
            </Button>
          </div>
        </section>
      </div>

      <!-- CỘT PHẢI (ASIDE / RIGHT COLUMN) -->
      <aside class="right-column">
        <!-- 1. PANEL GIA SƯ AI ĐỒNG HÀNH -->
        <section class="dashboard-card ai-tutor-panel" aria-labelledby="section-tutor-title">
          <div class="tutor-header">
            <span class="tutor-avatar-mark">
              <Brain :size="22" />
            </span>
            <div>
              <h2 id="section-tutor-title" class="tutor-title">Gia sư AI 24/7</h2>
              <span class="tutor-subtitle">Sẵn sàng đồng hành cùng bạn</span>
            </div>
            <Badge variant="ai" size="sm" class="ml-auto">Cá nhân hóa</Badge>
          </div>

          <div class="tutor-message-bubble">
            <p>
              {{ home.activeRecommendations[0]?.reason || 'Chào bạn! Mình có thể giúp giải đáp thắc mắc bài học, phân tích lỗi sai và đồng hành cùng lộ trình của bạn.' }}
            </p>
          </div>

          <Button variant="ai" class="w-full shadow-sm" @click="home.showView('tutor')">
            <Sparkles :size="16" />
            Trò chuyện với Gia sư AI
            <ArrowRight :size="15" />
          </Button>
        </section>

        <!-- 2. PANEL TIẾN ĐỘ TỔNG QUAN -->
        <section class="dashboard-card" aria-labelledby="section-progress-title">
          <div class="card-header-bar">
            <div class="title-with-icon">
              <span class="header-icon-mini text-brand-primary">
                <BarChart3 :size="17" />
              </span>
              <h2 id="section-progress-title" class="card-title">Tiến độ tổng quan</h2>
            </div>
            <button type="button" class="action-text-btn" @click="home.showView('progress')">
              Chi tiết <ArrowRight :size="14" />
            </button>
          </div>

          <!-- Bộ chọn chứng chỉ nếu có -->
          <div v-if="home.certificates.length" class="cert-select-container">
            <label class="cert-select-label" for="dashboard-cert-select">Chứng chỉ:</label>
            <select
              id="dashboard-cert-select"
              v-model="home.selectedCertificateId"
              class="cert-select-input"
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

          <!-- Lưới 4 ô thống kê chỉ số thực tế -->
          <div class="metrics-quad-grid">
            <div class="metric-quad-cell">
              <span class="metric-quad-icon bg-sky-50 text-sky-600">
                <Trophy :size="16" />
              </span>
              <strong class="metric-quad-value">
                {{ home.progress?.totalAttempts ?? home.history.length }}
              </strong>
              <small class="metric-quad-label">Lượt làm bài</small>
            </div>

            <div class="metric-quad-cell">
              <span class="metric-quad-icon bg-blue-50 text-brand-primary">
                <CheckCircle2 :size="16" />
              </span>
              <strong class="metric-quad-value">
                {{ home.progress?.averageScore != null ? Number(home.progress.averageScore).toFixed(1) : (home.averageScore != null ? home.averageScore.toFixed(1) : '—') }}
              </strong>
              <small class="metric-quad-label">Điểm trung bình</small>
            </div>

            <div class="metric-quad-cell">
              <span class="metric-quad-icon bg-violet-50 text-ai-accent">
                <BookMarked :size="16" />
              </span>
              <strong class="metric-quad-value">{{ home.activePlans.length }}</strong>
              <small class="metric-quad-label">Kế hoạch đang học</small>
            </div>

            <div class="metric-quad-cell">
              <span class="metric-quad-icon bg-amber-50 text-amber-600">
                <Flame :size="16" />
              </span>
              <strong class="metric-quad-value">{{ home.activeRecommendations.length }}</strong>
              <small class="metric-quad-label">Gợi ý cần xem</small>
            </div>
          </div>
        </section>

        <!-- 3. PANEL MỤC TIÊU HỌC TẬP HIỆN TẠI -->
        <section class="dashboard-card" aria-labelledby="section-goal-title">
          <div class="card-header-bar">
            <div class="title-with-icon">
              <span class="header-icon-mini text-sky-600">
                <Target :size="17" />
              </span>
              <h2 id="section-goal-title" class="card-title">Mục tiêu hiện tại</h2>
            </div>
            <button type="button" class="action-text-btn" @click="home.showView('plans')">
              Chi tiết <ArrowRight :size="14" />
            </button>
          </div>

          <div v-if="home.activePlan" class="goal-active-box">
            <div class="goal-header-line">
              <strong class="goal-plan-name">{{ home.activePlan.title }}</strong>
            </div>

            <div class="goal-progress-wrap">
              <div class="goal-track-bg">
                <div
                  class="goal-track-fill"
                  :style="{ width: `${home.completedPlanRatio}%` }"
                ></div>
              </div>
            </div>

            <div class="goal-footer-info">
              <span>{{ home.completedPlanRatio }}% hoàn thành</span>
              <span>
                {{ home.activePlan.targetScore ? `Mục tiêu ${home.activePlan.targetScore} điểm` : home.formatDate(home.activePlan.examDate) }}
              </span>
            </div>
          </div>

          <div v-else class="empty-state-box p-4 text-center">
            <p class="text-xs text-slate-500 mb-2">Bạn chưa thiết lập mục tiêu học tập cụ thể.</p>
            <Button
              variant="outline"
              size="sm"
              @click="home.showView('plans'); home.showPlanForm = true"
            >
              Thiết lập mục tiêu <ArrowRight :size="14" />
            </Button>
          </div>
        </section>

        <!-- 4. THAO TÁC NHANH (QUICK ACTIONS) -->
        <section class="dashboard-card quick-actions-panel" aria-labelledby="section-shortcuts-title">
          <div class="card-header-bar mb-3">
            <div class="title-with-icon">
              <span class="header-icon-mini text-brand-primary">
                <Zap :size="17" />
              </span>
              <h2 id="section-shortcuts-title" class="card-title">Thao tác nhanh</h2>
            </div>
          </div>

          <div class="shortcuts-list">
            <button
              type="button"
              class="shortcut-btn"
              @click="home.showView('tests')"
            >
              <span class="shortcut-icon bg-sky-50 text-sky-600">
                <CheckCircle2 :size="16" />
              </span>
              <span class="shortcut-text">Luyện tập đề thi mới</span>
              <ArrowRight :size="14" class="shortcut-arrow" />
            </button>

            <button
              type="button"
              class="shortcut-btn"
              @click="home.showView('plans'); home.showPlanForm = true"
            >
              <span class="shortcut-icon bg-blue-50 text-brand-primary">
                <Plus :size="16" />
              </span>
              <span class="shortcut-text">Lập kế hoạch học tập mới</span>
              <ArrowRight :size="14" class="shortcut-arrow" />
            </button>

            <button
              type="button"
              class="shortcut-btn"
              @click="home.showView('progress')"
            >
              <span class="shortcut-icon bg-violet-50 text-violet-600">
                <BarChart3 :size="16" />
              </span>
              <span class="shortcut-text">Xem biểu đồ BKT chi tiết</span>
              <ArrowRight :size="14" class="shortcut-arrow" />
            </button>
          </div>
        </section>
      </aside>
    </div>
  </div>
</template>

<style scoped>
.dashboard-wrapper {
  width: 100%;
}

.dashboard-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.9fr) minmax(290px, 0.88fr);
  align-items: start;
  gap: 20px;
  width: 100%;
}

.main-column,
.right-column {
  display: flex;
  flex-direction: column;
  gap: 20px;
  min-width: 0;
}

/* ==========================================================================
   WELCOME BANNER (LIGHT AI EDUCATION)
   ========================================================================== */
.welcome-card {
  position: relative;
  padding: 24px 28px;
  border-radius: var(--radius-lg, 12px);
  background: linear-gradient(135deg, #FFFFFF 0%, #F0FDFA 60%, #F5F3FF 100%);
  border: 1px solid var(--border-subtle, #E2E8F0);
  box-shadow: 0 2px 10px rgba(15, 23, 42, 0.03);
  overflow: hidden;
}

.welcome-content {
  position: relative;
  z-index: 1;
}

.welcome-badge {
  margin-bottom: 12px;
}

.welcome-title {
  margin: 0 0 8px;
  font-size: clamp(22px, 2.3vw, 28px);
  font-weight: 800;
  line-height: 1.25;
  letter-spacing: -0.5px;
  color: var(--text-primary, #0F172A);
}

.welcome-desc {
  margin: 0 0 20px;
  font-size: 14px;
  line-height: 1.55;
  color: var(--text-secondary, #475569);
  max-width: 680px;
}

/* Quick Stats Row */
.quick-stats-row {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}

.stat-pill {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 14px;
  border-radius: var(--radius-md, 8px);
  background: rgba(255, 255, 255, 0.9);
  border: 1px solid rgba(226, 232, 240, 0.9);
  backdrop-filter: blur(6px);
  cursor: pointer;
  transition: transform 0.15s ease, box-shadow 0.15s ease, background 0.15s ease;
}

.stat-pill:hover {
  transform: translateY(-1px);
  background: #ffffff;
  box-shadow: 0 4px 12px rgba(15, 23, 42, 0.05);
}

.stat-icon-wrap {
  display: grid;
  place-items: center;
  width: 36px;
  height: 36px;
  border-radius: var(--radius-md, 8px);
  flex-shrink: 0;
}

.stat-info {
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.stat-info strong {
  font-size: 18px;
  font-weight: 750;
  color: var(--text-primary, #0F172A);
  line-height: 1.15;
}

.stat-info small {
  font-size: 11.5px;
  color: var(--text-muted, #64748B);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

/* ==========================================================================
   ACTIVE ATTEMPT BANNER (IN-PROGRESS EXAM)
   ========================================================================== */
.active-attempt-banner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 14px 18px;
  border-radius: var(--radius-md, 8px);
  background: linear-gradient(135deg, #FFFBEB 0%, #FEF3C7 100%);
  border: 1px solid #FDE68A;
  box-shadow: 0 2px 8px rgba(245, 158, 11, 0.1);
}

.active-attempt-left {
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 0;
}

.attempt-pulse-indicator {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background-color: #F59E0B;
  box-shadow: 0 0 0 3px rgba(245, 158, 11, 0.3);
  animation: pulse 1.5s infinite;
  flex-shrink: 0;
}

@keyframes pulse {
  0% {
    box-shadow: 0 0 0 0 rgba(245, 158, 11, 0.5);
  }
  70% {
    box-shadow: 0 0 0 6px rgba(245, 158, 11, 0);
  }
  100% {
    box-shadow: 0 0 0 0 rgba(245, 158, 11, 0);
  }
}

.attempt-icon-pill {
  display: grid;
  place-items: center;
  width: 34px;
  height: 34px;
  border-radius: var(--radius-md, 8px);
  flex-shrink: 0;
}

.attempt-text {
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.attempt-title-text {
  font-size: 13.5px;
  font-weight: 750;
  color: #92400E;
}

.attempt-subtitle-text {
  margin: 1px 0 0;
  font-size: 12px;
  color: #B45309;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

/* ==========================================================================
   SHARED CARD STYLES
   ========================================================================== */
.dashboard-card {
  padding: 20px 22px;
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
  margin-bottom: 16px;
}

.card-title {
  margin: 0;
  font-size: 16px;
  font-weight: 700;
  color: var(--text-primary, #0F172A);
  letter-spacing: -0.2px;
}

.card-subtitle {
  margin: 3px 0 0;
  font-size: 12.5px;
  color: var(--text-muted, #64748B);
}

.title-with-pill,
.title-with-icon {
  display: flex;
  align-items: center;
  gap: 8px;
}

.header-icon-mini {
  display: grid;
  place-items: center;
}

.action-text-btn {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 4px 6px;
  border: none;
  background: transparent;
  color: var(--brand-primary, #2563EB);
  font-size: 12.5px;
  font-weight: 600;
  cursor: pointer;
  border-radius: 4px;
  transition: all 0.15s ease;
  white-space: nowrap;
}

.action-text-btn:hover {
  color: var(--brand-primary-hover, #1D4ED8);
  text-decoration: underline;
}

/* ==========================================================================
   ACTIVE PLAN CARD
   ========================================================================== */
.active-plan-card {
  display: flex;
  gap: 16px;
  padding: 16px;
  border-radius: var(--radius-md, 8px);
  background-color: #F8FAFC;
  border: 1px solid var(--border-subtle, #E2E8F0);
}

.plan-visual-badge {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 4px;
  width: 90px;
  height: 90px;
  border-radius: var(--radius-md, 8px);
  background: linear-gradient(135deg, #2563EB 0%, #1D4ED8 100%);
  color: #ffffff;
  flex-shrink: 0;
  box-shadow: 0 4px 12px rgba(37, 99, 235, 0.2);
}

.plan-badge-caption {
  font-size: 9px;
  font-weight: 750;
  letter-spacing: 0.6px;
  opacity: 0.9;
}

.plan-body {
  display: flex;
  flex-direction: column;
  gap: 10px;
  min-width: 0;
  flex: 1;
}

.plan-title-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.plan-title {
  margin: 0;
  font-size: 15px;
  font-weight: 700;
  color: var(--text-primary, #0F172A);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.live-dot-green {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background-color: #10B981;
}

.plan-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.tag-pill {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 3px 8px;
  border-radius: 6px;
  font-size: 11.5px;
  font-weight: 500;
  background-color: #ffffff;
  border: 1px solid #E2E8F0;
  color: #475569;
}

.tag-pill.highlight {
  background-color: #EFF6FF;
  border-color: #BFDBFE;
  color: var(--brand-primary, #2563EB);
  font-weight: 600;
}

.plan-progress-container {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.progress-bar-track {
  width: 100%;
  height: 7px;
  border-radius: 4px;
  background-color: #E2E8F0;
  overflow: hidden;
}

.progress-bar-fill {
  height: 100%;
  border-radius: 4px;
  background: linear-gradient(90deg, #2563EB 0%, #3B82F6 100%);
  transition: width 0.3s ease;
}

.progress-labels {
  display: flex;
  justify-content: space-between;
  font-size: 12px;
  color: var(--text-secondary, #475569);
}

.plan-meta-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-top: 4px;
  padding-top: 10px;
  border-top: 1px solid #E2E8F0;
}

.meta-item {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: var(--text-muted, #64748B);
}

/* ==========================================================================
   COURSES & CERTIFICATES PREVIEW GRID
   ========================================================================== */
.courses-preview-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.course-preview-card {
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  padding: 16px;
  border-radius: var(--radius-md, 8px);
  background-color: #F8FAFC;
  border: 1px solid var(--border-subtle, #E2E8F0);
  transition: transform 0.15s ease, box-shadow 0.15s ease, background-color 0.15s ease;
}

.course-preview-card:hover {
  transform: translateY(-1px);
  background-color: #ffffff;
  box-shadow: 0 4px 12px rgba(15, 23, 42, 0.04);
}

.course-card-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  margin-bottom: 10px;
}

.course-card-icon {
  display: grid;
  place-items: center;
  width: 36px;
  height: 36px;
  border-radius: var(--radius-md, 8px);
  border: 1px solid;
}

.course-card-body {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 6px;
  margin-bottom: 12px;
}

.course-title {
  margin: 0;
  font-size: 14px;
  font-weight: 700;
  color: var(--text-primary, #0F172A);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.course-desc {
  margin: 0;
  font-size: 12px;
  line-height: 1.5;
  color: var(--text-muted, #64748B);
  overflow: hidden;
  display: -webkit-box;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.course-progress-block {
  margin-top: 6px;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.course-progress-labels {
  display: flex;
  justify-content: space-between;
  font-size: 11px;
  color: var(--text-secondary, #475569);
}

.course-progress-track {
  width: 100%;
  height: 5px;
  border-radius: 3px;
  background-color: #E2E8F0;
  overflow: hidden;
}

.course-progress-fill {
  height: 100%;
  border-radius: 3px;
  background: linear-gradient(90deg, #2563EB 0%, #3B82F6 100%);
  transition: width 0.3s ease;
}

.course-card-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  padding-top: 8px;
  border-top: 1px solid #E2E8F0;
}

.course-pass-score {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 11px;
  color: var(--text-muted, #64748B);
}

.course-cta-btn {
  padding: 4px 8px;
  font-size: 12px;
}

/* ==========================================================================
   MASTERY & SKILL TRACK SECTION (BKT)
   ========================================================================== */
.mastery-dashboard-layout {
  display: grid;
  grid-template-columns: 190px minmax(0, 1fr);
  gap: 20px;
  align-items: center;
}

.score-summary-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 18px 12px;
  border-radius: var(--radius-md, 8px);
  background-color: #F8FAFC;
  border: 1px solid var(--border-subtle, #E2E8F0);
  text-align: center;
}

.score-radial-circle {
  position: relative;
  display: grid;
  place-items: center;
  width: 96px;
  height: 96px;
  border-radius: 50%;
  margin-bottom: 10px;
}

.score-circle-inner {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  width: 78px;
  height: 78px;
  border-radius: 50%;
  background-color: #ffffff;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.04);
}

.score-number {
  font-size: 20px;
  font-weight: 800;
  color: var(--brand-primary, #2563EB);
  line-height: 1.1;
}

.score-label {
  font-size: 9.5px;
  color: var(--text-muted, #64748B);
}

.score-meta-text {
  margin: 0;
  font-size: 11.5px;
  color: var(--text-secondary, #475569);
}

.skills-track-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.skill-item-row {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.skill-info-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.skill-name {
  font-size: 13px;
  font-weight: 600;
  color: var(--text-primary, #0F172A);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.skill-badge-wrap {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}

.skill-percent {
  font-size: 12.5px;
  font-weight: 700;
  color: var(--text-primary, #0F172A);
}

.skill-bar-track {
  width: 100%;
  height: 6px;
  border-radius: 3px;
  background-color: #E2E8F0;
  overflow: hidden;
}

.skill-bar-fill {
  height: 100%;
  border-radius: 3px;
  transition: width 0.3s ease;
}

.skill-bar-fill.fill-success {
  background-color: #10B981;
}

.skill-bar-fill.fill-primary {
  background-color: var(--brand-primary, #2563EB);
}

.skill-bar-fill.fill-warning {
  background-color: #F59E0B;
}

.skills-empty-card {
  grid-column: 2;
}

/* ==========================================================================
   RECENT ATTEMPTS & EXAM ACTIVITIES
   ========================================================================== */
.recent-attempts-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.recent-attempt-item {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 12px 14px;
  border-radius: var(--radius-md, 8px);
  background-color: #F8FAFC;
  border: 1px solid var(--border-subtle, #E2E8F0);
  transition: background-color 0.15s ease;
}

.recent-attempt-item:hover {
  background-color: #F1F5F9;
}

.attempt-icon-box {
  display: grid;
  place-items: center;
  width: 36px;
  height: 36px;
  border-radius: var(--radius-md, 8px);
  border: 1px solid;
  flex-shrink: 0;
}

.attempt-info-col {
  flex: 1;
  min-width: 0;
}

.attempt-title {
  margin: 0 0 3px;
  font-size: 13.5px;
  font-weight: 700;
  color: var(--text-primary, #0F172A);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.attempt-meta {
  display: flex;
  align-items: center;
  gap: 6px;
  margin: 0;
  font-size: 12px;
  color: var(--text-muted, #64748B);
}

.meta-dot {
  color: #CBD5E1;
}

.attempt-score-col {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-shrink: 0;
}

.score-display {
  display: flex;
  align-items: baseline;
  gap: 2px;
}

.score-val {
  font-size: 16px;
  font-weight: 800;
  color: var(--brand-primary, #2563EB);
}

.max-score {
  font-size: 11.5px;
  color: var(--text-muted, #64748B);
}

/* ==========================================================================
   AI RECOMMENDATIONS GRID
   ========================================================================== */
.recommendations-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
}

.recommendation-card {
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  padding: 16px;
  border-radius: var(--radius-md, 8px);
  background-color: #F8FAFC;
  border: 1px solid var(--border-subtle, #E2E8F0);
  transition: transform 0.15s ease, box-shadow 0.15s ease, border-color 0.15s ease;
}

.recommendation-card:hover {
  transform: translateY(-2px);
  background-color: #ffffff;
  border-color: #CBD5E1;
  box-shadow: 0 4px 14px rgba(15, 23, 42, 0.05);
}

.recommend-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  margin-bottom: 12px;
}

.recommend-icon-box {
  display: grid;
  place-items: center;
  width: 36px;
  height: 36px;
  border-radius: var(--radius-md, 8px);
  border: 1px solid;
}

.recommend-body {
  flex: 1;
  margin-bottom: 14px;
}

.recommend-title {
  margin: 0 0 6px;
  font-size: 13.5px;
  font-weight: 700;
  color: var(--text-primary, #0F172A);
  line-height: 1.4;
  overflow: hidden;
  display: -webkit-box;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.recommend-reason {
  margin: 0;
  font-size: 12px;
  line-height: 1.5;
  color: var(--text-secondary, #475569);
  overflow: hidden;
  display: -webkit-box;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 3;
}

/* ==========================================================================
   RIGHT COLUMN PANELS (TUTOR, PROGRESS, GOALS, SHORTCUTS)
   ========================================================================== */
.ai-tutor-panel {
  background: linear-gradient(145deg, #FFFFFF 0%, #FAF5FF 100%);
  border-color: #E9D5FF;
}

.tutor-header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 14px;
}

.tutor-avatar-mark {
  display: grid;
  place-items: center;
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background-color: #F5F3FF;
  border: 1px solid #DDD6FE;
  color: #7C3AED;
}

.tutor-title {
  margin: 0;
  font-size: 14.5px;
  font-weight: 750;
  color: var(--text-primary, #0F172A);
}

.tutor-subtitle {
  font-size: 11.5px;
  color: #7C3AED;
}

.tutor-message-bubble {
  padding: 12px 14px;
  border-radius: var(--radius-md, 8px);
  background-color: #ffffff;
  border: 1px solid #E9D5FF;
  box-shadow: 0 2px 6px rgba(124, 58, 237, 0.05);
  margin-bottom: 16px;
}

.tutor-message-bubble p {
  margin: 0;
  font-size: 12.5px;
  line-height: 1.55;
  color: #334155;
}

/* Progress Metrics Quad Grid */
.cert-select-container {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 14px;
}

.cert-select-label {
  font-size: 12px;
  font-weight: 600;
  color: var(--text-secondary, #475569);
}

.cert-select-input {
  flex: 1;
  min-width: 0;
  height: 34px;
  padding: 0 10px;
  border: 1px solid var(--border-subtle, #E2E8F0);
  border-radius: var(--radius-sm, 6px);
  background-color: #F8FAFC;
  color: var(--text-primary, #0F172A);
  font: inherit;
  font-size: 12px;
  outline: none;
  transition: border-color 0.15s ease;
}

.cert-select-input:focus {
  border-color: var(--brand-primary, #2563EB);
  background-color: #ffffff;
}

.metrics-quad-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
}

.metric-quad-cell {
  display: flex;
  flex-direction: column;
  padding: 12px;
  border-radius: var(--radius-md, 8px);
  background-color: #F8FAFC;
  border: 1px solid var(--border-subtle, #E2E8F0);
}

.metric-quad-icon {
  display: grid;
  place-items: center;
  width: 30px;
  height: 30px;
  border-radius: 50%;
  margin-bottom: 8px;
}

.metric-quad-value {
  font-size: 18px;
  font-weight: 800;
  color: var(--text-primary, #0F172A);
  line-height: 1.15;
}

.metric-quad-label {
  margin-top: 2px;
  font-size: 11px;
  color: var(--text-muted, #64748B);
}

/* Goal Active Box */
.goal-active-box {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 12px;
  border-radius: var(--radius-md, 8px);
  background-color: #F8FAFC;
  border: 1px solid var(--border-subtle, #E2E8F0);
}

.goal-plan-name {
  font-size: 13px;
  font-weight: 700;
  color: var(--text-primary, #0F172A);
}

.goal-track-bg {
  width: 100%;
  height: 6px;
  border-radius: 3px;
  background-color: #E2E8F0;
  overflow: hidden;
}

.goal-track-fill {
  height: 100%;
  border-radius: 3px;
  background-color: var(--brand-primary, #2563EB);
}

.goal-footer-info {
  display: flex;
  justify-content: space-between;
  font-size: 11px;
  color: var(--text-muted, #64748B);
}

/* Quick Shortcuts */
.shortcuts-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.shortcut-btn {
  display: flex;
  align-items: center;
  gap: 10px;
  width: 100%;
  padding: 10px 12px;
  border-radius: var(--radius-md, 8px);
  background-color: #F8FAFC;
  border: 1px solid var(--border-subtle, #E2E8F0);
  color: var(--text-primary, #0F172A);
  font: inherit;
  font-size: 12.5px;
  font-weight: 500;
  cursor: pointer;
  text-align: left;
  transition: all 0.15s ease;
}

.shortcut-btn:hover {
  background-color: #ffffff;
  border-color: #CBD5E1;
  box-shadow: 0 2px 6px rgba(15, 23, 42, 0.04);
}

.shortcut-btn:hover .shortcut-arrow {
  transform: translateX(2px);
  color: var(--brand-primary, #2563EB);
}

.shortcut-icon {
  display: grid;
  place-items: center;
  width: 28px;
  height: 28px;
  border-radius: 6px;
  flex-shrink: 0;
}

.shortcut-text {
  flex: 1;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.shortcut-arrow {
  color: #94A3B8;
  transition: transform 0.15s ease, color 0.15s ease;
}

/* ==========================================================================
   EMPTY STATE STYLES
   ========================================================================== */
.empty-state-box {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 16px;
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
  font-size: 13px;
  font-weight: 700;
  color: var(--text-primary, #0F172A);
}

.empty-text-wrap p {
  margin: 3px 0 0;
  font-size: 12px;
  line-height: 1.5;
  color: var(--text-muted, #64748B);
}

/* ==========================================================================
   SKELETON LOADING
   ========================================================================== */
.skeleton-card {
  border-radius: var(--radius-lg, 12px);
  background: linear-gradient(90deg, #F1F5F9 25%, #E2E8F0 50%, #F1F5F9 75%);
  background-size: 200% 100%;
  animation: shimmer 1.5s infinite;
}

@keyframes shimmer {
  0% {
    background-position: 200% 0;
  }
  100% {
    background-position: -200% 0;
  }
}

.skeleton-banner {
  height: 180px;
}

.skeleton-block {
  height: 160px;
}

.skeleton-panel {
  height: 140px;
}

/* ==========================================================================
   RESPONSIVE (TABLET & MOBILE)
   ========================================================================== */
@media (max-width: 1100px) {
  .dashboard-grid {
    grid-template-columns: 1fr;
  }

  .quick-stats-row {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .courses-preview-grid {
    grid-template-columns: 1fr;
  }

  .recommendations-grid {
    grid-template-columns: 1fr;
  }

  .mastery-dashboard-layout {
    grid-template-columns: 1fr;
  }

  .skills-empty-card {
    grid-column: 1;
  }
}

@media (max-width: 640px) {
  .welcome-card {
    padding: 18px 16px;
  }

  .quick-stats-row {
    grid-template-columns: 1fr;
  }

  .active-attempt-banner {
    flex-direction: column;
    align-items: flex-start;
  }

  .active-plan-card {
    flex-direction: column;
  }

  .plan-visual-badge {
    width: 100%;
    height: 54px;
    flex-direction: row;
  }

  .plan-meta-row {
    flex-direction: column;
    align-items: flex-start;
    gap: 8px;
  }

  .recent-attempt-item {
    flex-direction: column;
    align-items: flex-start;
    gap: 10px;
  }

  .attempt-score-col {
    width: 100%;
    justify-content: space-between;
  }

  .empty-state-box {
    flex-direction: column;
    text-align: center;
  }
}
</style>
